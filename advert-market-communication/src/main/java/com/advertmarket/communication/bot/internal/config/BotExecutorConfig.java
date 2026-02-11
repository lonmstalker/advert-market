package com.advertmarket.communication.bot.internal.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides executor beans for async bot processing.
 */
@Configuration
public class BotExecutorConfig {

    /** Virtual thread executor for async update processing. */
    @Bean("botUpdateExecutor")
    public ExecutorService botUpdateExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
