package com.advertmarket.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.OnboardingRequest;
import com.advertmarket.identity.api.dto.UpdateLanguageRequest;
import com.advertmarket.identity.api.dto.UpdateSettingsRequest;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.exception.EntityNotFoundException;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserServiceImpl — profile, onboarding, and account deletion")
class UserServiceImplTest {

    private UserRepository userRepository;
    private MetricsFacade metricsFacade;
    private UserServiceImpl userService;

    private static final UserId USER_ID = new UserId(42L);
    private static final UserProfile PROFILE = new UserProfile(
            42L, "johndoe", "John Doe", "en", "USD",
            NotificationSettings.defaults(),
            false, List.of(), Instant.parse("2026-01-01T00:00:00Z"));

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        metricsFacade = mock(MetricsFacade.class);
        userService = new UserServiceImpl(
                userRepository, metricsFacade);
    }

    @Test
    @DisplayName("Should return user profile by ID")
    void shouldReturnProfile() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(PROFILE));

        UserProfile result = userService.getProfile(USER_ID);

        assertThat(result).isEqualTo(PROFILE);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(USER_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Should complete onboarding and return updated profile")
    void shouldCompleteOnboarding() {
        List<String> interests = List.of("tech", "gaming");
        UserProfile updatedProfile = new UserProfile(
                42L, "johndoe", "John Doe", "en", "USD",
                NotificationSettings.defaults(),
                true, interests,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(updatedProfile));

        UserProfile result = userService.completeOnboarding(
                USER_ID, new OnboardingRequest(interests));

        verify(userRepository).completeOnboarding(
                USER_ID, interests);
        assertThat(result.onboardingCompleted()).isTrue();
        assertThat(result.interests())
                .containsExactly("tech", "gaming");
    }

    @Test
    @DisplayName("Should update language and return updated profile")
    void shouldUpdateLanguage() {
        UserProfile updated = new UserProfile(
                42L, "johndoe", "John Doe", "ru", "USD",
                NotificationSettings.defaults(),
                false, List.of(),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(updated));

        UserProfile result = userService.updateLanguage(
                USER_ID, new UpdateLanguageRequest("ru"));

        verify(userRepository).updateLanguage(USER_ID, "ru");
        assertThat(result.languageCode()).isEqualTo("ru");
    }

    @Test
    @DisplayName("Should update display currency via settings")
    void shouldUpdateDisplayCurrency() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(PROFILE));

        userService.updateSettings(
                USER_ID,
                new UpdateSettingsRequest("EUR", null));

        verify(userRepository).updateDisplayCurrency(
                USER_ID, "EUR");
    }

    @Test
    @DisplayName("Should update notification settings via settings")
    void shouldUpdateNotificationSettings() {
        NotificationSettings settings = new NotificationSettings(
                new NotificationSettings.DealNotifications(
                        false, true, true),
                new NotificationSettings.FinancialNotifications(
                        true, false, true),
                new NotificationSettings.DisputeNotifications(
                        true, true));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(PROFILE));

        userService.updateSettings(
                USER_ID,
                new UpdateSettingsRequest(null, settings));

        verify(userRepository).updateNotificationSettings(
                USER_ID, settings);
    }

    @Test
    @DisplayName("Should soft-delete account and increment metric")
    void shouldDeleteAccount() {
        userService.deleteAccount(USER_ID);

        verify(userRepository).softDelete(USER_ID);
        verify(metricsFacade).incrementCounter(
                MetricNames.ACCOUNT_DELETED);
    }
}
