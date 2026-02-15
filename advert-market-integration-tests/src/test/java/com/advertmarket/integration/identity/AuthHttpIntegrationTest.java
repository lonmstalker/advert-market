package com.advertmarket.integration.identity;

import static com.advertmarket.db.generated.tables.Users.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.InitDataValidatorPort;
import com.advertmarket.identity.api.port.LoginRateLimiterPort;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.config.AuthProperties;
import com.advertmarket.identity.mapper.LoginResponseMapper;
import com.advertmarket.identity.security.JwtAuthenticationFilter;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.identity.service.AuthServiceImpl;
import com.advertmarket.identity.service.TelegramInitDataValidator;
import com.advertmarket.identity.service.UserServiceImpl;
import com.advertmarket.identity.web.AuthController;
import com.advertmarket.identity.web.ProfileController;
import com.advertmarket.integration.marketplace.config.MarketplaceTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HTTP-level integration tests for Auth and Profile endpoints.
 *
 * <p>Uses a minimal Spring context with real PostgreSQL and Redis
 * via shared containers. Validates request/response contracts
 * including ProblemDetail format.
 */
@SpringBootTest(
        classes = AuthHttpIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@DisplayName("Auth HTTP — end-to-end integration")
class AuthHttpIntegrationTest {

    private static final long TEST_USER_ID = 42L;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @LocalServerPort
    private int port;

    private WebTestClient webClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        dsl.deleteFrom(USERS).execute();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with empty body returns 400 + ProblemDetail")
    void loginWithEmptyBody_returns400() {
        webClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.type").isNotEmpty()
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.error_code").isNotEmpty()
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/profile without Authorization header returns 403")
    void profileWithoutAuth_returnsForbidden() {
        webClient.get().uri("/api/v1/profile")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("GET /api/v1/profile with valid JWT returns 200 + UserProfile")
    void profileWithValidJwt_returns200() {
        userRepository.upsert(new TelegramUserData(
                TEST_USER_ID, "John", "Doe", "johndoe", "en"));
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);

        UserProfile profile = webClient.get()
                .uri("/api/v1/profile")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserProfile.class)
                .returnResult()
                .getResponseBody();

        assertThat(profile).isNotNull();
        assertThat(profile.id()).isEqualTo(TEST_USER_ID);
        assertThat(profile.username()).isEqualTo("johndoe");
        assertThat(profile.displayName())
                .isEqualTo("John Doe");
        assertThat(profile.languageCode()).isEqualTo("en");
        assertThat(profile.onboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("PUT /api/v1/profile/onboarding with valid JWT completes onboarding")
    void onboardingWithValidJwt_returns200() {
        userRepository.upsert(new TelegramUserData(
                TEST_USER_ID, "John", "Doe", "johndoe", "en"));
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);

        UserProfile profile = webClient.put()
                .uri("/api/v1/profile/onboarding")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OnboardingRequest(
                        List.of("advertiser")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserProfile.class)
                .returnResult()
                .getResponseBody();

        assertThat(profile).isNotNull();
        assertThat(profile.onboardingCompleted()).isTrue();
        assertThat(profile.interests())
                .containsExactly("advertiser");
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout then GET /api/v1/profile returns 403")
    void logoutThenProfile_returnsForbidden() {
        userRepository.upsert(new TelegramUserData(
                TEST_USER_ID, "John", "Doe", "johndoe", "en"));
        String token = TestDataFactory.jwt(
                jwtTokenProvider, TEST_USER_ID);

        webClient.post().uri("/api/v1/auth/logout")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(
                        HttpStatus.NO_CONTENT);

        webClient.get().uri("/api/v1/profile")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("ProblemDetail contains required fields")
    void problemDetail_hasRequiredFields() {
        Map<String, Object> body = webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(
                        new ParameterizedTypeReference
                                <Map<String, Object>>() {
                        })
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body).containsKey("type");
        assertThat(body).containsKey("title");
        assertThat(body).containsKey("status");
        assertThat(body).containsKey("error_code");
        assertThat(body).containsKey("timestamp");
        assertThat(body).containsKey("correlation_id");
        assertThat(body.get("error_code"))
                .isEqualTo("VALIDATION_FAILED");
    }

    /**
     * Minimal Spring Boot config for auth HTTP tests.
     */
    @Configuration
    @EnableAutoConfiguration
    @Import(MarketplaceTestConfig.class)
    static class TestConfig {

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                JwtAuthenticationFilter jwtFilter)
                throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(
                                    SessionCreationPolicy
                                            .STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/api/v1/auth/login")
                            .permitAll()
                            .requestMatchers("/api/v1/**")
                            .authenticated()
                            .anyRequest().denyAll())
                    .addFilterBefore(jwtFilter,
                            UsernamePasswordAuthenticationFilter
                                    .class)
                    .build();
        }

        @Bean
        InitDataValidatorPort telegramInitDataValidator(
                AuthProperties authProperties,
                JsonFacade jsonFacade) {
            return new TelegramInitDataValidator(
                    "123456:TEST-TOKEN",
                    authProperties, jsonFacade);
        }

        @Bean
        AuthServiceImpl authService(
                InitDataValidatorPort validator,
                UserRepository userRepository,
                JwtTokenProvider jwtTokenProvider,
                TokenBlacklistPort tokenBlacklistPort,
                MetricsFacade metricsFacade) {
            return new AuthServiceImpl(
                    validator, userRepository,
                    jwtTokenProvider, tokenBlacklistPort,
                    metricsFacade,
                    Mappers.getMapper(LoginResponseMapper.class));
        }

        @Bean
        UserServiceImpl userService(
                UserRepository userRepository,
                MetricsFacade metricsFacade) {
            return new UserServiceImpl(
                    userRepository, metricsFacade);
        }

        @Bean
        AuthController authController(
                AuthServiceImpl authService,
                LoginRateLimiterPort loginRateLimiterPort) {
            return new AuthController(
                    authService, loginRateLimiterPort);
        }

        @Bean
        ProfileController profileController(
                UserServiceImpl userService,
                AuthServiceImpl authService) {
            return new ProfileController(
                    userService, authService);
        }
    }
}
