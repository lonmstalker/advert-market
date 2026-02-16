package com.advertmarket.identity.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advertmarket.identity.api.dto.CurrencyMode;
import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.UpdateLanguageRequest;
import com.advertmarket.identity.api.dto.UpdateSettingsRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.AuthPort;
import com.advertmarket.identity.api.port.UserPort;
import com.advertmarket.shared.model.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@DisplayName("ProfileController — /api/v1/profile endpoints")
class ProfileControllerTest {

    private MockMvc mockMvc;
    private UserPort userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UserId USER_ID = new UserId(42L);
    private static final UserProfile PROFILE = new UserProfile(
            42L, "johndoe", "John Doe", "en", "USD", CurrencyMode.AUTO,
            NotificationSettings.defaults(),
            true, List.of("tech"), Instant.parse("2026-01-01T00:00:00Z"));

    @BeforeEach
    void setUp() {
        userService = mock(UserPort.class);
        AuthPort authService = mock(AuthPort.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ProfileController(userService, authService))
                .setCustomArgumentResolvers(
                        new UserIdArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /profile should return user profile")
    void shouldReturnProfile() throws Exception {
        when(userService.getProfile(USER_ID)).thenReturn(PROFILE);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.displayName").value("John Doe"))
                .andExpect(jsonPath("$.displayCurrency").value("USD"))
                .andExpect(jsonPath("$.notificationSettings.deals.newOffers")
                        .value(true));
    }

    @Test
    @DisplayName("PUT /profile/language should update language and return profile")
    void shouldUpdateLanguage() throws Exception {
        UserProfile updated = new UserProfile(
                42L, "johndoe", "John Doe", "ru", "RUB", CurrencyMode.AUTO,
                NotificationSettings.defaults(),
                true, List.of("tech"),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(userService.updateLanguage(
                eq(USER_ID), any(UpdateLanguageRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/profile/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"languageCode\":\"ru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("ru"));
    }

    @Test
    @DisplayName("PUT /profile/language should return 400 for blank language code")
    void shouldReturn400ForBlankLanguage() throws Exception {
        mockMvc.perform(put("/api/v1/profile/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"languageCode\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /profile/settings should update settings and return profile")
    void shouldUpdateSettings() throws Exception {
        when(userService.updateSettings(
                eq(USER_ID), any(UpdateSettingsRequest.class)))
                .thenReturn(PROFILE);

        mockMvc.perform(put("/api/v1/profile/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currencyMode\":\"AUTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.currencyMode").value("AUTO"));
    }

    @Test
    @DisplayName("PUT /profile/settings should return 400 when MANUAL mode has no currency")
    void shouldReturn400WhenManualModeHasNoCurrency() throws Exception {
        mockMvc.perform(put("/api/v1/profile/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currencyMode\":\"MANUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Resolves @AuthenticationPrincipal UserId in standalone MockMvc tests.
     */
    private static class UserIdArgumentResolver
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(
                    AuthenticationPrincipal.class)
                    && parameter.getParameterType()
                    .equals(UserId.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return USER_ID;
        }
    }
}
