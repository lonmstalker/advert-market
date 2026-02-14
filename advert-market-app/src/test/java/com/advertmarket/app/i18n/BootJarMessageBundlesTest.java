package com.advertmarket.app.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("bootJar packaging")
class BootJarMessageBundlesTest {

    @Test
    @DisplayName("includes Telegram bot/notification message bundles in BOOT-INF/classes")
    void includesMessageBundles() throws IOException {
        Path bootJar = findBootJar();

        try (JarFile jar = new JarFile(bootJar.toFile())) {
            assertThat(jar.getEntry(
                    "BOOT-INF/classes/messages/bot_ru.properties"))
                    .as("bot_ru.properties must be packaged into bootJar classes")
                    .isNotNull();
            assertThat(jar.getEntry(
                    "BOOT-INF/classes/messages/bot_en.properties"))
                    .as("bot_en.properties must be packaged into bootJar classes")
                    .isNotNull();
            assertThat(jar.getEntry(
                    "BOOT-INF/classes/messages/notifications_ru.properties"))
                    .as("notifications_ru.properties must be packaged into bootJar classes")
                    .isNotNull();
            assertThat(jar.getEntry(
                    "BOOT-INF/classes/messages/notifications_en.properties"))
                    .as("notifications_en.properties must be packaged into bootJar classes")
                    .isNotNull();
        }
    }

    private static Path findBootJar() throws IOException {
        Path libsDir = Path.of("build", "libs");
        if (!Files.isDirectory(libsDir)) {
            throw new IllegalStateException(
                    "Expected bootJar output dir to exist: " + libsDir.toAbsolutePath());
        }

        try (Stream<Path> files = Files.list(libsDir)) {
            return files
                    .filter(p -> p.getFileName().toString()
                            .startsWith("advert-market-app-"))
                    .filter(p -> p.getFileName().toString()
                            .endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString()
                            .contains("-plain"))
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "bootJar not found in " + libsDir.toAbsolutePath()));
        }
    }
}

