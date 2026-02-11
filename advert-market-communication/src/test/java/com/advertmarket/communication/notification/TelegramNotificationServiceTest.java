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
import com.advertmarket.shared.i18n.LocalizationService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TelegramNotificationServiceTest {

    private final TelegramSender sender =
            mock(TelegramSender.class);
    private final LocalizationService i18n =
            mock(LocalizationService.class);
    private final TelegramNotificationService service =
            new TelegramNotificationService(sender, i18n);

    @Test
    void send_substitutesVariables() {
        when(i18n.msg("notification.new_offer", "ru"))
                .thenReturn("<b>New offer</b>\n"
                        + "Offer for {channel_name}.");

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
    void send_handlesAllTypes() {
        for (var type : NotificationType.values()) {
            String key = "notification."
                    + type.name().toLowerCase();
            when(i18n.msg(eq(key), eq("ru")))
                    .thenReturn(type.name());
        }

        for (var type : NotificationType.values()) {
            var request = new NotificationRequest(
                    1L, type, Map.of());
            assertThat(service.send(request)).isTrue();
        }
    }

    @Test
    void send_returnsFalseOnError() {
        when(i18n.msg(anyString(), eq("ru")))
                .thenReturn("template");
        doThrow(new RuntimeException("fail"))
                .when(sender).send(anyLong(), anyString());

        var request = new NotificationRequest(
                1L, NotificationType.PUBLISHED, Map.of());

        assertThat(service.send(request)).isFalse();
    }
}
