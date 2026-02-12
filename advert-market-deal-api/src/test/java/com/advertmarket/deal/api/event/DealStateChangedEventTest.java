package com.advertmarket.deal.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.model.ActorType;
import com.advertmarket.shared.model.DealStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DealStateChangedEvent")
class DealStateChangedEventTest {

    @Test
    @DisplayName("Nullable actorId for SYSTEM transitions")
    void nullableActorId_forSystem() {
        var event = new DealStateChangedEvent(
                DealStatus.AWAITING_PAYMENT,
                DealStatus.EXPIRED,
                null,
                ActorType.SYSTEM,
                1_000_000_000L,
                -1001234567890L);

        assertThat(event.actorId()).isNull();
        assertThat(event.actorType())
                .isEqualTo(ActorType.SYSTEM);
    }

    @Test
    @DisplayName("Non-null actorId for user transitions")
    void nonNullActorId_forUser() {
        var event = new DealStateChangedEvent(
                DealStatus.DRAFT,
                DealStatus.OFFER_PENDING,
                12345L,
                ActorType.ADVERTISER,
                500_000_000L,
                -1001234567890L);

        assertThat(event.actorId()).isEqualTo(12345L);
        assertThat(event.actorType())
                .isEqualTo(ActorType.ADVERTISER);
    }
}
