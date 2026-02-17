package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Guards production-safe defaults for authentication settings.
 */
class AuthPropertiesDefaultsTest {

    private static final int MIN_ANTI_REPLAY_WINDOW_SECONDS = 3600;

    @Test
    void applicationConfig_setsReasonableAntiReplayWindow()
            throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "application", new ClassPathResource("application.yml"));

        Integer antiReplayWindowSeconds = null;
        for (PropertySource<?> source : sources) {
            Object value = source.getProperty(
                    "app.auth.anti-replay-window-seconds");
            if (value != null) {
                antiReplayWindowSeconds = Integer.valueOf(
                        value.toString());
                break;
            }
        }

        assertThat(antiReplayWindowSeconds).isNotNull();
        assertThat(antiReplayWindowSeconds)
                .isGreaterThanOrEqualTo(MIN_ANTI_REPLAY_WINDOW_SECONDS);
    }
}
