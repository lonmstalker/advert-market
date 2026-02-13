package com.advertmarket.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.port.InitDataValidatorPort;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthServiceImpl — login and logout flow")
class AuthServiceImplTest {

    private InitDataValidatorPort initDataValidator;
    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistPort tokenBlacklistPort;
    private MetricsFacade metricsFacade;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        initDataValidator = mock(InitDataValidatorPort.class);
        userRepository = mock(UserRepository.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        tokenBlacklistPort = mock(TokenBlacklistPort.class);
        metricsFacade = mock(MetricsFacade.class);
        authService = new AuthServiceImpl(
                initDataValidator, userRepository,
                jwtTokenProvider, tokenBlacklistPort,
                metricsFacade);
    }

    @Test
    @DisplayName("Should login successfully with full user data")
    void shouldLoginSuccessfully() {
        TelegramUserData userData = new TelegramUserData(
                42L, "John", "Doe", "johndoe", "en");
        when(initDataValidator.validate("test-init-data"))
                .thenReturn(userData);
        when(userRepository.upsert(userData)).thenReturn(false);
        when(jwtTokenProvider.generateToken(
                any(UserId.class), anyBoolean()))
                .thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationSeconds())
                .thenReturn(3600L);

        LoginResponse response = authService.login(
                new LoginRequest("test-init-data"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().username()).isEqualTo("johndoe");
        assertThat(response.user().displayName())
                .isEqualTo("John Doe");
        verify(userRepository).upsert(userData);
        verify(metricsFacade).incrementCounter(
                MetricNames.AUTH_LOGIN_SUCCESS);
    }

    @Test
    @DisplayName("Should build displayName without lastName")
    void shouldBuildDisplayNameWithoutLastName() {
        TelegramUserData userData = new TelegramUserData(
                42L, "Jane", null, null, "ru");
        when(initDataValidator.validate("init-data"))
                .thenReturn(userData);
        when(userRepository.upsert(userData)).thenReturn(false);
        when(jwtTokenProvider.generateToken(
                any(UserId.class), anyBoolean()))
                .thenReturn("token");
        when(jwtTokenProvider.getExpirationSeconds())
                .thenReturn(3600L);

        LoginResponse response = authService.login(
                new LoginRequest("init-data"));

        assertThat(response.user().displayName())
                .isEqualTo("Jane");
        assertThat(response.user().username()).isEmpty();
    }

    @Test
    @DisplayName("Should call upsert and pass operator flag to JWT")
    void shouldPassOperatorFlagToJwt() {
        TelegramUserData userData = new TelegramUserData(
                99L, "Admin", null, "admin", "en");
        when(initDataValidator.validate("init-data"))
                .thenReturn(userData);
        when(userRepository.upsert(userData)).thenReturn(true);
        when(jwtTokenProvider.generateToken(
                new UserId(99L), true))
                .thenReturn("operator-token");
        when(jwtTokenProvider.getExpirationSeconds())
                .thenReturn(3600L);

        LoginResponse response = authService.login(
                new LoginRequest("init-data"));

        assertThat(response.accessToken())
                .isEqualTo("operator-token");
        verify(jwtTokenProvider).generateToken(
                new UserId(99L), true);
    }

    @Test
    @DisplayName("Should blacklist token and increment metric on logout")
    void shouldLogout() {
        long expSeconds = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis()) + 3600L;
        authService.logout("jti-to-revoke", expSeconds);

        verify(tokenBlacklistPort).blacklist(
                eq("jti-to-revoke"), longThat(ttl -> ttl > 0));
        verify(metricsFacade).incrementCounter(MetricNames.AUTH_LOGOUT);
    }
}
