package com.advertmarket.communication.bot.internal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotExecutorConfig")
class BotExecutorConfigTest {

    private final BotExecutorConfig config =
            new BotExecutorConfig();

    @Test
    @DisplayName("Creates virtual thread executor")
    void botUpdateExecutor_createsVirtualThreadExecutor() {
        ExecutorService executor = config.botUpdateExecutor();

        assertThat(executor).isNotNull();
        assertThat(executor.isShutdown()).isFalse();
        executor.shutdown();
    }

    @Test
    @DisplayName("Executor accepts and runs tasks")
    void botUpdateExecutor_acceptsTasks() throws Exception {
        ExecutorService executor = config.botUpdateExecutor();

        var future = executor.submit(() -> "done");
        assertThat(future.get()).isEqualTo("done");

        executor.shutdown();
    }
}
