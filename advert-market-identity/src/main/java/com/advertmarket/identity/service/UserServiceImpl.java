package com.advertmarket.identity.service;

import com.advertmarket.identity.api.dto.CurrencyMode;
import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UpdateLanguageRequest;
import com.advertmarket.identity.api.dto.UpdateSettingsRequest;
import com.advertmarket.identity.api.dto.UpdateTonAddressRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserPort;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.exception.ErrorCodes;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link UserPort}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserPort {

    private final UserRepository userRepository;
    private final MetricsFacade metricsFacade;
    private final LocaleCurrencyResolver localeCurrencyResolver;

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
        String displayCurrency = normalizeCurrency(request.displayCurrency());
        CurrencyMode requestedMode = request.currencyMode();

        if (requestedMode == CurrencyMode.MANUAL
                && displayCurrency == null) {
            throw new DomainException(
                    ErrorCodes.INVALID_PARAMETER,
                    "displayCurrency is required when currencyMode=MANUAL");
        }

        if (requestedMode == CurrencyMode.AUTO) {
            userRepository.updateCurrencyMode(
                    userId, CurrencyMode.AUTO);
            String languageCode = getProfile(userId).languageCode();
            userRepository.updateDisplayCurrency(
                    userId,
                    localeCurrencyResolver.resolve(languageCode));
        } else if (requestedMode == CurrencyMode.MANUAL) {
            userRepository.updateCurrencyMode(
                    userId, CurrencyMode.MANUAL);
            userRepository.updateDisplayCurrency(
                    userId, displayCurrency);
        } else if (displayCurrency != null) {
            userRepository.updateCurrencyMode(
                    userId, CurrencyMode.MANUAL);
            userRepository.updateDisplayCurrency(
                    userId, displayCurrency);
        }

        var notificationSettings = request.notificationSettings();
        if (notificationSettings != null) {
            userRepository.updateNotificationSettings(
                    userId, notificationSettings);
        }
        return getProfile(userId);
    }

    @Override
    public @NonNull UserProfile updateTonAddress(
            @NonNull UserId userId,
            @NonNull UpdateTonAddressRequest request) {
        userRepository.updateTonAddress(userId, request.tonAddress());
        return getProfile(userId);
    }

    @Override
    public void deleteAccount(@NonNull UserId userId) {
        userRepository.softDelete(userId);
        metricsFacade.incrementCounter(MetricNames.ACCOUNT_DELETED);
    }

    private static @Nullable String normalizeCurrency(
            @Nullable String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
