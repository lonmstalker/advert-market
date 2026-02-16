package com.advertmarket.shared.outbox;

import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Polls the outbox table and publishes pending entries.
 *
 * <p>Activated only when both {@link OutboxRepository} and
 * {@link OutboxPublisher} beans are present (provided by the
 * app module).
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository repository;
    private final OutboxPublisher publisher;
    private final OutboxProperties properties;
    private final MetricsFacade metrics;

    private long lastRecoveryMillis;

    /**
     * Scheduled polling loop that fetches and publishes outbox entries.
     */
    @Scheduled(fixedDelayString =
            "${app.outbox.poll-interval:500ms}")
    public void poll() {
        metrics.incrementCounter(MetricNames.OUTBOX_POLL_COUNT);

        recoverStuckEntries();

        List<OutboxEntry> batch =
                repository.findPendingBatch(properties.batchSize());

        if (batch.isEmpty()) {
            return;
        }

        log.debug("Outbox poll: {} entries to publish",
                batch.size());

        for (OutboxEntry entry : batch) {
            publishEntry(entry);
        }
    }

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "id() is never null for entries fetched from DB")
    private void publishEntry(OutboxEntry entry) {
        try {
            publisher.publish(entry)
                    .get(properties.publishTimeout().toMillis(),
                            TimeUnit.MILLISECONDS);
            repository.markDelivered(entry.id());
            metrics.incrementCounter(MetricNames.OUTBOX_PUBLISHED);
        } catch (TimeoutException ex) {
            handleFailure(entry, ex);
        } catch (ExecutionException ex) {
            handleFailure(entry,
                    ex.getCause() != null ? ex.getCause() : ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            handleFailure(entry, ex);
        }
    }

    private static final long RECOVERY_INTERVAL_MS = 60_000;

    private void recoverStuckEntries() {
        long now = System.currentTimeMillis();
        if (now - lastRecoveryMillis < RECOVERY_INTERVAL_MS) {
            return;
        }
        lastRecoveryMillis = now;

        int reset = repository.resetStuckProcessing(
                properties.stuckThresholdSeconds());
        if (reset > 0) {
            log.warn("Reset {} stuck PROCESSING outbox entries", reset);
            metrics.incrementCounter(
                    MetricNames.OUTBOX_STUCK_RECOVERED);
        }
    }

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "id() is never null for entries fetched from DB")
    private void handleFailure(
            OutboxEntry entry, Throwable ex) {
        int newRetryCount = entry.retryCount() + 1;
        if (newRetryCount >= properties.maxRetries()) {
            log.error(
                    "Outbox entry {} permanently failed after {} retries: {}",
                    entry.id(), newRetryCount, ex.getMessage());
            repository.markFailed(entry.id());
            metrics.incrementCounter(MetricNames.OUTBOX_RECORDS_FAILED);
        } else {
            log.warn("Outbox entry {} failed (attempt {}/{}): {}",
                    entry.id(), newRetryCount, properties.maxRetries(),
                    ex.getMessage());
            repository.incrementRetry(entry.id());
        }
    }

}
