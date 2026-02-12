package com.advertmarket.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TopicNames constants")
class TopicNamesTest {

    @Test
    @DisplayName("All topic names are non-blank")
    void allNonBlank() {
        getConstants().forEach(name ->
                assertThat(name).as("topic name").isNotBlank());
    }

    @Test
    @DisplayName("No duplicate topic names")
    void noDuplicates() {
        var names = getConstants();
        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Has exactly 8 topics")
    void hasExpectedCount() {
        assertThat(getConstants()).hasSize(8);
    }

    private List<String> getConstants() {
        return Arrays.stream(TopicNames.class.getDeclaredFields())
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
