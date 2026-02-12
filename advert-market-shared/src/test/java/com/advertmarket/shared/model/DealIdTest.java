package com.advertmarket.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DealId value object")
class DealIdTest {

    @Test
    @DisplayName("generate creates unique identifiers")
    void generate_createsUniqueIdentifiers() {
        DealId a = DealId.generate();
        DealId b = DealId.generate();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("of(UUID) wraps the given UUID")
    void ofUuid_wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        DealId dealId = DealId.of(uuid);
        assertThat(dealId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("of(String) parses UUID string")
    void ofString_parsesUuidString() {
        UUID uuid = UUID.randomUUID();
        DealId dealId = DealId.of(uuid.toString());
        assertThat(dealId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("of(String) rejects invalid UUID")
    void ofString_invalidUuid_throws() {
        assertThatThrownBy(() -> DealId.of("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null UUID is rejected")
    void constructor_null_throws() {
        assertThatThrownBy(() -> new DealId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("toString returns UUID string")
    void toString_returnsUuidString() {
        UUID uuid = UUID.randomUUID();
        assertThat(DealId.of(uuid).toString())
                .isEqualTo(uuid.toString());
    }
}
