package com.advertmarket.identity.service;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.port.AuthService;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link AuthService}.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TelegramInitDataValidator initDataValidator;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MetricsFacade metricsFacade;

    @Override
    @NonNull
    public LoginResponse login(@NonNull LoginRequest request) {
        TelegramUserData userData =
                initDataValidator.validate(request.initData());

        boolean isOperator = userRepository.upsert(userData);

        UserId userId = new UserId(userData.id());
        String token = jwtTokenProvider.generateToken(
                userId, isOperator);

        String displayName = buildDisplayName(userData);
        String un = userData.username();
        String username = un != null ? un : "";

        metricsFacade.incrementCounter("auth.login.success");

        return new LoginResponse(
                token,
                jwtTokenProvider.getExpirationSeconds(),
                new LoginResponse.UserSummary(
                        userData.id(), username, displayName));
    }

    private static String buildDisplayName(
            TelegramUserData userData) {
        String lastName = userData.lastName();
        if (lastName != null && !lastName.isBlank()) {
            return userData.firstName() + " " + lastName;
        }
        return userData.firstName();
    }
}
