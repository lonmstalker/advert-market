package com.advertmarket.integration.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.shared.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validates parity between RU and EN locale property files
 * and ErrorCode coverage in error message bundles.
 */
@DisplayName("Locale properties parity tests")
class LocalePropertiesParityTest {

    private static final Set<String> REQUIRED_LOCALES =
            Set.of("ru", "en");

    static Stream<Arguments> propertyFilePairs() {
        return Stream.of(
                Arguments.of(
                        "messages/errors_ru.properties",
                        "messages/errors_en.properties",
                        "errors"),
                Arguments.of(
                        "messages/bot_ru.properties",
                        "messages/bot_en.properties",
                        "bot"),
                Arguments.of(
                        "messages/notifications_ru.properties",
                        "messages/notifications_en.properties",
                        "notifications")
        );
    }

    @ParameterizedTest(name = "{2}: RU and EN keys must match")
    @MethodSource("propertyFilePairs")
    @DisplayName("Locale property files must have identical key sets")
    void localeFileKeysMustMatch(
            String ruPath, String enPath, String bundle)
            throws IOException {
        Properties ru = loadProperties(ruPath);
        Properties en = loadProperties(enPath);

        Set<String> ruKeys = ru.stringPropertyNames();
        Set<String> enKeys = en.stringPropertyNames();

        assertThat(ruKeys)
                .as("Keys in %s must match keys in %s",
                        ruPath, enPath)
                .isEqualTo(enKeys);
    }

    @Test
    @DisplayName("Every ErrorCode must have title and detail keys"
            + " in both locale files")
    void errorCodeKeysMustExistInBothLocales()
            throws IOException {
        Properties ru = loadProperties(
                "messages/errors_ru.properties");
        Properties en = loadProperties(
                "messages/errors_en.properties");

        for (ErrorCode code : ErrorCode.values()) {
            String titleKey = code.titleKey();
            String detailKey = code.detailKey();

            assertThat(ru.containsKey(titleKey))
                    .as("RU must contain %s", titleKey)
                    .isTrue();
            assertThat(ru.containsKey(detailKey))
                    .as("RU must contain %s", detailKey)
                    .isTrue();
            assertThat(en.containsKey(titleKey))
                    .as("EN must contain %s", titleKey)
                    .isTrue();
            assertThat(en.containsKey(detailKey))
                    .as("EN must contain %s", detailKey)
                    .isTrue();
        }
    }

    private Properties loadProperties(String classpath)
            throws IOException {
        Properties props = new Properties();
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(classpath)) {
            assertThat(is)
                    .as("Resource %s must exist on classpath",
                            classpath)
                    .isNotNull();
            props.load(is);
        }
        return props;
    }
}
