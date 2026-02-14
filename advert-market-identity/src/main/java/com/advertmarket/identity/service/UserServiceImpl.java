package com.advertmarket.identity.service;

import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UpdateLanguageRequest;
import com.advertmarket.identity.api.dto.UpdateSettingsRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link UserPort}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserPort {

    private final UserRepository userRepository;
    private final MetricsFacade metricsFacade;

    @Override
    public @NonNull UserProfile getProfile(@NonNull UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCodes.USER_NOT_FOUND, "User",
                        String.valueOf(userId.value())));
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
    public @NonNull UserProfile updateLanguage(
            @NonNull UserId userId,
            @NonNull UpdateLanguageRequest request) {
        userRepository.updateLanguage(userId, request.languageCode());
        return getProfile(userId);
    }

    @Override
    public @NonNull UserProfile updateSettings(
            @NonNull UserId userId,
            @NonNull UpdateSettingsRequest request) {
        if (request.displayCurrency() != null) {
            userRepository.updateDisplayCurrency(
                    userId, request.displayCurrency());
        }
        if (request.notificationSettings() != null) {
            userRepository.updateNotificationSettings(
                    userId, request.notificationSettings());
        }
        return getProfile(userId);
    }

    @Override
    public void deleteAccount(@NonNull UserId userId) {
        userRepository.softDelete(userId);
        metricsFacade.incrementCounter(MetricNames.ACCOUNT_DELETED);
    }
}
