package com.advertmarket.identity.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.port.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("AuthController — /api/v1/auth endpoints")
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .build();
    }

    @Test
    @DisplayName("Should return 200 with token on successful login")
    void shouldReturnTokenOnSuccessfulLogin() throws Exception {
        LoginResponse response = new LoginResponse(
                "jwt-token", 3600,
                new LoginResponse.UserSummary(
                        42L, "johndoe", "John Doe"));
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(
                                        "valid-init-data"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("jwt-token"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(3600))
                .andExpect(jsonPath("$.user.id")
                        .value(42))
                .andExpect(jsonPath("$.user.displayName")
                        .value("John Doe"));
    }

    @Test
    @DisplayName("Should return 400 when initData is missing")
    void shouldReturn400WhenInitDataMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when body is empty")
    void shouldReturn400WhenBodyEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
