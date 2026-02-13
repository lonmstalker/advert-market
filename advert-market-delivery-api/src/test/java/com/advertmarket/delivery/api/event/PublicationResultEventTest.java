package com.advertmarket.delivery.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.event.DomainEvent;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PublicationResultEvent")
class PublicationResultEventTest {

    @Test
    @DisplayName("Implements DomainEvent")
    void implementsDomainEvent() {
        var event = new PublicationResultEvent(
                true, 42L, -1001234567890L,
                "hash123", Instant.now(), null, null);
        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    @DisplayName("Success construction")
    void successConstruction() {
        var publishedAt = Instant.parse("2026-02-13T10:00:00Z");
        var event = new PublicationResultEvent(
                true, 123L, -1001234567890L,
                "content-hash", publishedAt, null, null);

        assertThat(event.success()).isTrue();
        assertThat(event.messageId()).isEqualTo(123L);
        assertThat(event.channelId()).isEqualTo(-1001234567890L);
        assertThat(event.contentHash()).isEqualTo("content-hash");
        assertThat(event.publishedAt()).isEqualTo(publishedAt);
        assertThat(event.error()).isNull();
        assertThat(event.details()).isNull();
    }

    @Test
    @DisplayName("Failure construction")
    void failureConstruction() {
        var event = new PublicationResultEvent(
                false, 0L, -1001234567890L,
                null, null, "Bot not admin",
                "Missing post_messages permission");

        assertThat(event.success()).isFalse();
        assertThat(event.messageId()).isZero();
        assertThat(event.contentHash()).isNull();
        assertThat(event.publishedAt()).isNull();
        assertThat(event.error()).isEqualTo("Bot not admin");
        assertThat(event.details())
                .isEqualTo("Missing post_messages permission");
    }
}
