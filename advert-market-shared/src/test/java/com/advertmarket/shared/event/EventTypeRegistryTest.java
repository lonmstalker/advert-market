package com.advertmarket.shared.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventTypeRegistry")
class EventTypeRegistryTest {

    private record TestEvent(String data) implements DomainEvent {
    }

    @Test
    @DisplayName("Register and resolve returns correct class")
    void registerAndResolve() {
        var registry = new EventTypeRegistry();
        registry.register("TEST", TestEvent.class);

        assertThat(registry.resolve("TEST"))
                .isEqualTo(TestEvent.class);
    }

    @Test
    @DisplayName("Resolve unknown type returns null")
    void resolveUnknown_returnsNull() {
        var registry = new EventTypeRegistry();

        assertThat(registry.resolve("UNKNOWN")).isNull();
    }

    @Test
    @DisplayName("Size reflects registered count")
    void size_reflectsCount() {
        var registry = new EventTypeRegistry();
        assertThat(registry.size()).isZero();

        registry.register("A", TestEvent.class);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Duplicate registration with same class is idempotent")
    void duplicateRegistration_sameClass_isIdempotent() {
        var registry = new EventTypeRegistry();
        registry.register("TEST", TestEvent.class);
        registry.register("TEST", TestEvent.class);

        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Duplicate registration with different class throws")
    void duplicateRegistration_differentClass_throws() {
        var registry = new EventTypeRegistry();
        registry.register("TEST", TestEvent.class);

        assertThatThrownBy(() -> registry.register(
                "TEST", OtherEvent.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    private record OtherEvent(int id) implements DomainEvent {
    }
}
