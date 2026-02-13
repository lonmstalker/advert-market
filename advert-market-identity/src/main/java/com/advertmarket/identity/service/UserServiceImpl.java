package com.advertmarket.identity.service;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.identity.api.port.UserService;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link UserService}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MetricsFacade metricsFacade;

    @Override
    public @NonNull UserProfile getProfile(@NonNull UserId userId) {
        UserProfile profile = userRepository.findById(userId);
        if (profile == null) {
            throw new EntityNotFoundException(
                    ErrorCodes.USER_NOT_FOUND, "User",
                    String.valueOf(userId.value()));
        }
        return profile;
    }

    @Override
    public @NonNull UserProfile completeOnboarding(
            @NonNull UserId userId,
            @NonNull OnboardingRequest request) {
        userRepository.completeOnboarding(
                userId, request.interests());
        return getProfile(userId);
    }

    @Override
    public void deleteAccount(@NonNull UserId userId) {
        userRepository.softDelete(userId);
        metricsFacade.incrementCounter(MetricNames.ACCOUNT_DELETED);
    }
}
