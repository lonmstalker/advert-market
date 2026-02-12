package com.advertmarket.shared.outbox;

import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox table and publishes pending entries.
 *
 * <p>Activated only when both {@link OutboxRepository} and
 * {@link OutboxPublisher} beans are present (provided by the
 * app module).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean({OutboxRepository.class, OutboxPublisher.class})
public class OutboxPoller {

    private final OutboxRepository repository;
    private final OutboxPublisher publisher;
    private final OutboxProperties properties;
    private final MetricsFacade metrics;

    /**
     * Scheduled polling loop that fetches and publishes outbox entries.
     */
    @Scheduled(fixedDelayString =
            "#{@outboxProperties.pollInterval().toMillis()}")
    public void poll() {
        metrics.incrementCounter(MetricNames.OUTBOX_POLL_COUNT);

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

    @SuppressWarnings("IllegalCatch")
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "id() is never null for entries fetched from DB")
    private void publishEntry(OutboxEntry entry) {
        try {
            publisher.publish(entry).join();
            repository.markDelivered(entry.id());
            metrics.incrementCounter(MetricNames.OUTBOX_PUBLISHED);
        } catch (RuntimeException ex) {
            handleFailure(entry, ex);
        }
    }

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "id() is never null for entries fetched from DB")
    private void handleFailure(
            OutboxEntry entry, RuntimeException ex) {
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
