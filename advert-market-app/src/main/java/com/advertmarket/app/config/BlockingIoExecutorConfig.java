package com.advertmarket.app.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a dedicated executor for blocking I/O operations.
 *
 * <p>Used by services that fan-out to external APIs via {@link java.util.concurrent.CompletableFuture}.
 */
@Configuration
public class BlockingIoExecutorConfig {

    /**
     * Virtual thread executor for blocking I/O calls.
     *
     * <p>Explicitly injected to avoid using the common ForkJoinPool
     * and to keep concurrency policy centralized.
     */
    @Bean(value = "blockingIoExecutor", destroyMethod = "close")
    public ExecutorService blockingIoExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

