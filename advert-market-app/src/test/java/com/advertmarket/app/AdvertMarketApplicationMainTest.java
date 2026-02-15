package com.advertmarket.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdvertMarketApplication — entrypoint")
class AdvertMarketApplicationMainTest {

    @Test
    @DisplayName("main method must be public static void main(String[])")
    void mainMethodMustBePublicStatic() throws Exception {
        Method method = AdvertMarketApplication.class.getDeclaredMethod(
                "main", String[].class);

        int modifiers = method.getModifiers();
        assertThat(Modifier.isPublic(modifiers)).isTrue();
        assertThat(Modifier.isStatic(modifiers)).isTrue();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }
}

