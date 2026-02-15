package com.advertmarket.identity.service;

import com.advertmarket.identity.api.dto.LoginRequest;
import com.advertmarket.identity.api.dto.LoginResponse;
import com.advertmarket.identity.api.dto.TelegramUserData;
import com.advertmarket.identity.api.port.AuthPort;
import com.advertmarket.identity.api.port.InitDataValidatorPort;
import com.advertmarket.identity.api.port.TokenBlacklistPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.mapper.LoginResponseMapper;
import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link AuthPort}.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthPort {

    private final InitDataValidatorPort initDataValidator;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final MetricsFacade metricsFacade;
    private final LoginResponseMapper loginResponseMapper;

    @Override
    public @NonNull LoginResponse login(@NonNull LoginRequest request) {
        TelegramUserData userData =
                initDataValidator.validate(request.initData());

        boolean isOperator = userRepository.upsert(userData);

        UserId userId = new UserId(userData.id());
        String token = jwtTokenProvider.generateToken(
                userId, isOperator);

        metricsFacade.incrementCounter(MetricNames.AUTH_LOGIN_SUCCESS);

        return loginResponseMapper.toResponse(
                userData,
                token,
                jwtTokenProvider.getExpirationSeconds());
    }

    @Override
    public void logout(@NonNull String jti, long tokenExpSeconds) {
        long remainingTtl = tokenExpSeconds
                - TimeUnit.MILLISECONDS.toSeconds(
                        System.currentTimeMillis());
        if (remainingTtl > 0) {
            tokenBlacklistPort.blacklist(jti, remainingTtl);
        }
        metricsFacade.incrementCounter(MetricNames.AUTH_LOGOUT);
    }
}
