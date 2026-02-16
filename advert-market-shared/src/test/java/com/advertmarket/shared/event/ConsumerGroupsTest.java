package com.advertmarket.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsumerGroups constants")
class ConsumerGroupsTest {

    @Test
    @DisplayName("All consumer groups are non-blank")
    void allNonBlank() {
        getConstants().forEach(name ->
                assertThat(name).as("consumer group").isNotBlank());
    }

    @Test
    @DisplayName("No duplicate consumer groups")
    void noDuplicates() {
        var names = getConstants();
        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Has exactly 15 consumer groups")
    void hasExpectedCount() {
        assertThat(getConstants()).hasSize(15);
    }

    private List<String> getConstants() {
        return Arrays.stream(
                        ConsumerGroups.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> Modifier.isFinal(f.getModifiers()))
                .filter(f -> f.getType() == String.class)
                .map(this::getValue)
                .toList();
    }

    private String getValue(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
