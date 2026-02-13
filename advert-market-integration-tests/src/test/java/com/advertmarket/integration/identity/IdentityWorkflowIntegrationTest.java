package com.advertmarket.integration.identity;

import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.bot.internal.block.RedisUserBlockService;
import com.advertmarket.communication.bot.internal.block.UserBlockProperties;
import com.advertmarket.identity.adapter.JooqUserRepository;
import com.advertmarket.identity.adapter.RedisTokenBlacklist;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.identity.security.TelegramAuthentication;
import com.advertmarket.identity.service.AuthServiceImpl;
import com.advertmarket.identity.service.UserServiceImpl;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.RedisSupport;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserBlockCheckPort;
import com.advertmarket.shared.model.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Full workflow integration test for the identity module.
 *
 * <p>Wires real components (no Spring context) with real
 * PostgreSQL + Redis via shared containers to verify end-to-end
 * identity scenarios.
 */
@DisplayName("Identity workflow — full end-to-end scenarios")
class IdentityWorkflowIntegrationTest {

    private static DSLContext dsl;

    private StringRedisTemplate redisTemplate;
    private JooqUserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistPort tokenBlacklistPort;
    private UserBlockCheckPort userBlockCheckPort;
    private RedisUserBlockService blockService;
    private AuthServiceImpl authService;
    private UserServiceImpl userService;
    private JwtAuthenticationFilter filter;
    private MetricsFacade metricsFacade;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        dsl.deleteFrom(USERS).execute();

        redisTemplate = RedisSupport.redisTemplate();
        RedisSupport.flushAll();

        var authProperties = new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-key-at-least-32-bytes-long!!",
                        3600),
                300);
        jwtTokenProvider = new JwtTokenProvider(authProperties);
        tokenBlacklistPort = new RedisTokenBlacklist(redisTemplate);
        blockService = new RedisUserBlockService(
                redisTemplate,
                new UserBlockProperties("tg:block:"));
        userBlockCheckPort = blockService;
        metricsFacade = new MetricsFacade(new SimpleMeterRegistry());
        userRepository = new JooqUserRepository(dsl);
        authService = new AuthServiceImpl(
                null, userRepository,
                jwtTokenProvider, tokenBlacklistPort,
                metricsFacade);
        userService = new UserServiceImpl(
                userRepository, metricsFacade);
        filter = new JwtAuthenticationFilter(
                jwtTokenProvider, tokenBlacklistPort,
                userBlockCheckPort);
    }

    @Test
    @DisplayName("Login → use token → verify authenticated")
    void loginAndUseToken() throws Exception {
        userRepository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(42L), false);

        TelegramAuthentication auth = runFilter(token);

        assertThat(auth).isNotNull();
        assertThat(auth.getUserId()).isEqualTo(new UserId(42L));
        assertThat(auth.getJti()).isNotBlank();
    }

    @Test
    @DisplayName("Login → logout → token rejected by filter")
    void logoutRevokesToken() throws Exception {
        userRepository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(42L), false);

        TelegramAuthentication before = runFilter(token);
        assertThat(before).isNotNull();
        String jti = before.getJti();
        long tokenExpSeconds = before.getTokenExpSeconds();

        authService.logout(jti, tokenExpSeconds);

        TelegramAuthentication after = runFilter(token);
        assertThat(after).isNull();
    }

    @Test
    @DisplayName("Login → block user → token rejected by filter")
    void blockedUserRejected() throws Exception {
        userRepository.upsert(new TelegramUserData(
                99L, "Blocked", "User", "blocked", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(99L), false);

        assertThat(runFilter(token)).isNotNull();

        blockService.blockPermanently(99L, "spam");

        assertThat(runFilter(token)).isNull();
    }

    @Test
    @DisplayName("Block user → unblock → token works again")
    void unblockRestoresAccess() throws Exception {
        userRepository.upsert(new TelegramUserData(
                99L, "User", null, "user99", "en"));
        String token = jwtTokenProvider.generateToken(
                new UserId(99L), false);
        blockService.blockPermanently(99L, "spam");
        assertThat(runFilter(token)).isNull();

        blockService.unblock(99L);

        assertThat(runFilter(token)).isNotNull();
    }

    @Test
    @DisplayName("Delete account → profile gone, token blacklisted")
    void deleteAccountCleansUpEverything() throws Exception {
        long userId = 42L;
        userRepository.upsert(new TelegramUserData(
                userId, "John", "Doe", "johndoe", "en"));

        UserProfile profile = userRepository.findById(new UserId(userId));
        assertThat(profile).isNotNull();
        assertThat(profile.username()).isEqualTo("johndoe");

        String token = jwtTokenProvider.generateToken(
                new UserId(userId), false);
        userService.deleteAccount(new UserId(userId));
        TelegramAuthentication auth = runFilter(token);
        String jti = auth.getJti();
        authService.logout(jti, auth.getTokenExpSeconds());

        assertThat(userRepository.findById(new UserId(userId))).isNull();

        assertThat(runFilter(token)).isNull();
    }

    @Test
    @DisplayName("Delete account → re-login → account reactivated")
    void reloginAfterDeleteReactivatesAccount() throws Exception {
        userRepository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));
        userService.deleteAccount(new UserId(42L));
        assertThat(userRepository.findById(new UserId(42L))).isNull();

        userRepository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));

        UserProfile profile = userRepository.findById(new UserId(42L));
        assertThat(profile).isNotNull();
        assertThat(profile.username()).isEqualTo("johndoe");

        String newToken = jwtTokenProvider.generateToken(
                new UserId(42L), false);
        assertThat(runFilter(newToken)).isNotNull();
    }

    @Test
    @DisplayName("Delete account clears PII in database")
    void deleteAccountClearsPii() {
        userRepository.upsert(new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en"));

        userRepository.softDelete(new UserId(42L));

        var record = dsl.select(
                        USERS.FIRST_NAME, USERS.LAST_NAME,
                        USERS.USERNAME, USERS.IS_DELETED)
                .from(USERS)
                .where(USERS.ID.eq(42L))
                .fetchOne();
        assertThat(record).isNotNull();
        assertThat(record.get(USERS.FIRST_NAME))
                .isEqualTo("Deleted");
        assertThat(record.get(USERS.LAST_NAME)).isNull();
        assertThat(record.get(USERS.USERNAME)).isNull();
        assertThat(record.get(USERS.IS_DELETED)).isTrue();
    }

    /**
     * Simulates HTTP request through the JWT filter and returns
     * the resulting authentication, or null if not authenticated.
     */
    private TelegramAuthentication runFilter(String token)
            throws Exception {
        SecurityContextHolder.clearContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Bearer " + token);

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext()
                .getAuthentication();
        SecurityContextHolder.clearContext();

        return auth instanceof TelegramAuthentication tg ? tg : null;
    }
}
