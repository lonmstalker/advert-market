package com.advertmarket.communication.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.api.notification.NotificationRequest;
import com.advertmarket.communication.api.notification.NotificationType;
import com.advertmarket.communication.bot.internal.sender.TelegramSender;
import com.advertmarket.identity.api.dto.CurrencyMode;
import com.advertmarket.identity.api.dto.NotificationSettings;
import com.advertmarket.identity.api.dto.UserProfile;
import com.advertmarket.identity.api.port.UserRepository;
import com.advertmarket.shared.i18n.LocalizationService;
import com.advertmarket.shared.model.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TelegramNotificationService")
class TelegramNotificationServiceTest {

    private final TelegramSender sender =
            mock(TelegramSender.class);
    private final LocalizationService i18n =
            mock(LocalizationService.class);
    private final UserRepository userRepository =
            mock(UserRepository.class);
    private final TelegramNotificationService service =
            new TelegramNotificationService(
                    sender, i18n, userRepository);

    @Test
    @DisplayName("Uses user language code when user profile exists")
    void send_usesUserLanguageCodeWhenProfileExists() {
        when(userRepository.findById(new UserId(100L)))
                .thenReturn(Optional.of(userProfile(100L, "ru")));
        when(i18n.msg("notification.new_offer", "ru"))
                .thenReturn("Новый оффер для {channel_name}");

        var request = new NotificationRequest(
                100L,
                NotificationType.NEW_OFFER,
                Map.of("channel_name", "TestChannel"));

        boolean result = service.send(request);

        assertThat(result).isTrue();
        verify(i18n).msg("notification.new_offer", "ru");
    }

    @Test
    @DisplayName("Falls back to English when user profile is absent")
    void send_fallsBackToEnglishWhenProfileAbsent() {
        when(userRepository.findById(new UserId(100L)))
                .thenReturn(Optional.empty());
        when(i18n.msg("notification.new_offer", "en"))
                .thenReturn("New offer for {channel_name}");

        var request = new NotificationRequest(
                100L,
                NotificationType.NEW_OFFER,
                Map.of("channel_name", "TestChannel"));

        boolean result = service.send(request);

        assertThat(result).isTrue();
        verify(i18n).msg("notification.new_offer", "en");
    }

    @Test
    @DisplayName("Falls back to English when language code is blank")
    void send_fallsBackToEnglishWhenLanguageCodeBlank() {
        when(userRepository.findById(new UserId(100L)))
                .thenReturn(Optional.of(userProfile(100L, "   ")));
        when(i18n.msg("notification.new_offer", "en"))
                .thenReturn("New offer for {channel_name}");

        var request = new NotificationRequest(
                100L,
                NotificationType.NEW_OFFER,
                Map.of("channel_name", "TestChannel"));

        boolean result = service.send(request);

        assertThat(result).isTrue();
        verify(i18n).msg("notification.new_offer", "en");
    }

    @Test
    @DisplayName("Substitutes template variables")
    void send_substitutesVariables() {
        when(userRepository.findById(new UserId(100L)))
                .thenReturn(Optional.empty());
        when(i18n.msg("notification.new_offer", "en"))
                .thenReturn("*New offer*\n"
                        + "Offer for {channel_name}\\.");

        var request = new NotificationRequest(
                100L,
                NotificationType.NEW_OFFER,
                Map.of("channel_name", "TestChannel"));

        boolean result = service.send(request);

        assertThat(result).isTrue();
        verify(sender).send(eq(100L),
                contains("TestChannel"));
    }

    @Test
    @DisplayName("Handles all notification types")
    void send_handlesAllTypes() {
        for (var type : NotificationType.values()) {
            String key = "notification."
                    + type.name().toLowerCase();
            when(i18n.msg(eq(key), eq("en")))
                    .thenReturn(type.name());
        }
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.empty());

        for (var type : NotificationType.values()) {
            var request = new NotificationRequest(
                    1L, type, Map.of());
            assertThat(service.send(request)).isTrue();
        }
    }

    @Test
    @DisplayName("Returns false on send error")
    void send_returnsFalseOnError() {
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.empty());
        when(i18n.msg(anyString(), eq("en")))
                .thenReturn("template");
        doThrow(new RuntimeException("fail"))
                .when(sender).send(anyLong(), anyString());

        var request = new NotificationRequest(
                1L, NotificationType.PUBLISHED, Map.of());

        assertThat(service.send(request)).isFalse();
    }

    private static UserProfile userProfile(long id, String languageCode) {
        return new UserProfile(
                id,
                "user" + id,
                "User " + id,
                languageCode,
                "USD",
                CurrencyMode.AUTO,
                NotificationSettings.defaults(),
                true,
                java.util.List.of("tech"),
                null,
                Instant.now());
    }
}
