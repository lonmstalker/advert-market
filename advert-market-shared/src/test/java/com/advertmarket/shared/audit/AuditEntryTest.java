package com.advertmarket.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditEntry")
class AuditEntryTest {

    @Test
    @DisplayName("Builder creates valid entry with all fields")
    void builder_allFields_createsEntry() {
        AuditEntry entry = AuditEntry.builder()
                .actorType(AuditActorType.USER)
                .actorId("12345")
                .action("CREATE_DEAL")
                .entityType("DEAL")
                .entityId("deal-001")
                .oldValue(null)
                .newValue("{\"status\":\"DRAFT\"}")
                .ipAddress("192.168.1.1")
                .build();

        assertThat(entry.actorType())
                .isEqualTo(AuditActorType.USER);
        assertThat(entry.actorId()).isEqualTo("12345");
        assertThat(entry.action()).isEqualTo("CREATE_DEAL");
        assertThat(entry.entityType()).isEqualTo("DEAL");
        assertThat(entry.entityId()).isEqualTo("deal-001");
        assertThat(entry.oldValue()).isNull();
        assertThat(entry.newValue())
                .isEqualTo("{\"status\":\"DRAFT\"}");
        assertThat(entry.ipAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("Builder creates valid entry for system action")
    void builder_systemAction_createsEntry() {
        AuditEntry entry = AuditEntry.builder()
                .actorType(AuditActorType.SYSTEM)
                .action("EXPIRE_DEAL")
                .entityType("DEAL")
                .entityId("deal-002")
                .build();

        assertThat(entry.actorType())
                .isEqualTo(AuditActorType.SYSTEM);
        assertThat(entry.actorId()).isNull();
        assertThat(entry.ipAddress()).isNull();
    }

    @Test
    @DisplayName("Null actorType throws NullPointerException")
    void nullActorType_throws() {
        assertThatThrownBy(() -> AuditEntry.builder()
                .actorType(null)
                .action("CREATE")
                .entityType("DEAL")
                .entityId("1")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("actorType");
    }

    @Test
    @DisplayName("Blank action throws IllegalArgumentException")
    void blankAction_throws() {
        assertThatThrownBy(() -> AuditEntry.builder()
                .actorType(AuditActorType.USER)
                .action("  ")
                .entityType("DEAL")
                .entityId("1")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");
    }

    @Test
    @DisplayName("Blank entityType throws IllegalArgumentException")
    void blankEntityType_throws() {
        assertThatThrownBy(() -> AuditEntry.builder()
                .actorType(AuditActorType.USER)
                .action("CREATE")
                .entityType("")
                .entityId("1")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
    }

    @Test
    @DisplayName("Blank entityId throws IllegalArgumentException")
    void blankEntityId_throws() {
        assertThatThrownBy(() -> AuditEntry.builder()
                .actorType(AuditActorType.USER)
                .action("CREATE")
                .entityType("DEAL")
                .entityId("")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityId");
    }
}
