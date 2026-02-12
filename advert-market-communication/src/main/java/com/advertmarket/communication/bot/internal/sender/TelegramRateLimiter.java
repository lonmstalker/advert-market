package com.advertmarket.communication.bot.internal.sender;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rate limiter for Telegram Bot API calls.
 *
 * <p>Enforces both a global messages-per-second limit and a per-chat
 * messages-per-second limit using {@link Semaphore}s (virtual-thread
 * safe, no {@code synchronized} blocks).
 */
@Slf4j
@Component
public class TelegramRateLimiter implements RateLimiterPort {

    private static final long ACQUIRE_TIMEOUT_SECONDS = 5;

    private final Semaphore globalSemaphore;
    private final int globalPermits;
    private final int perChatPermits;
    private final Cache<Long, Semaphore> perChatSemaphores;

    /** Creates a rate limiter from the configured properties. */
    public TelegramRateLimiter(TelegramSenderProperties properties) {
        this.globalPermits = properties.globalPerSec();
        this.perChatPermits = properties.perChatPerSec();
        this.globalSemaphore = new Semaphore(globalPermits);
        this.perChatSemaphores = Caffeine.newBuilder()
                .expireAfterAccess(properties.cacheExpireAfterAccess())
                .maximumSize(properties.cacheMaximumSize())
                .build();
    }

    @Override
    public void acquire(long chatId) {
        try {
            if (!globalSemaphore.tryAcquire(
                    ACQUIRE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS)) {
                log.warn("Global rate limit timeout for chat={}",
                        chatId);
                return;
            }
            var chatSemaphore = perChatSemaphores.get(chatId,
                    _ -> new Semaphore(perChatPermits));
            if (!chatSemaphore.tryAcquire(
                    ACQUIRE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS)) {
                log.warn("Per-chat rate limit timeout for chat={}",
                        chatId);
                globalSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rate limiter interrupted for chat={}",
                    chatId);
        }
    }

    /** Replenishes global permits every second. */
    @Scheduled(fixedRateString =
            "${app.telegram.sender.replenish-fixed-rate-ms:1000}")
    void replenishGlobal() {
        int available = globalSemaphore.availablePermits();
        int toRelease = globalPermits - available;
        if (toRelease > 0) {
            globalSemaphore.release(toRelease);
        }
    }

    /** Replenishes per-chat permits every second. */
    @Scheduled(fixedRateString =
            "${app.telegram.sender.replenish-fixed-rate-ms:1000}")
    void replenishPerChat() {
        perChatSemaphores.asMap().forEach((_, semaphore) -> {
            int available = semaphore.availablePermits();
            int toRelease = perChatPermits - available;
            if (toRelease > 0) {
                semaphore.release(toRelease);
            }
        });
    }
}
