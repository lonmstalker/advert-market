package com.advertmarket.integration.shared;

import static com.advertmarket.db.generated.tables.NotificationOutbox.NOTIFICATION_OUTBOX;
import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.app.outbox.JooqOutboxRepository;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.metric.MetricsFacade;
import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxPoller;
import com.advertmarket.shared.outbox.OutboxProperties;
import com.advertmarket.shared.outbox.OutboxPublisher;
import com.advertmarket.shared.outbox.OutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the outbox poller pipeline with real PostgreSQL.
 */
@DisplayName("OutboxPoller — PostgreSQL integration")
class OutboxPollerIntegrationTest {

    private static DSLContext dsl;
    private JooqOutboxRepository repository;
    private MetricsFacade metrics;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        dsl.deleteFrom(NOTIFICATION_OUTBOX).execute();
        repository = new JooqOutboxRepository(dsl);
        metrics = new MetricsFacade(new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Save and fetch pending entries")
    void saveAndFetchPending() {
        OutboxEntry entry = testEntry();
        repository.save(entry);

        sleep(1200);

        List<OutboxEntry> pending =
                repository.findPendingBatch(10);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().topic())
                .isEqualTo(TopicNames.DEAL_STATE_CHANGED);
        assertThat(pending.getFirst().payload())
                .contains("\"test\"")
                .contains("true");
    }

    @Test
    @DisplayName("Mark delivered updates status and processed_at")
    void markDelivered_updatesStatus() {
        repository.save(testEntry());
        sleep(1200);

        List<OutboxEntry> batch =
                repository.findPendingBatch(10);
        assertThat(batch).isNotEmpty();

        repository.markDelivered(batch.getFirst().id());

        List<OutboxEntry> afterDeliver =
                repository.findPendingBatch(10);
        assertThat(afterDeliver).isEmpty();
    }

    @Test
    @DisplayName("Mark failed updates status")
    void markFailed_updatesStatus() {
        repository.save(testEntry());
        sleep(1200);

        List<OutboxEntry> batch =
                repository.findPendingBatch(10);
        repository.markFailed(batch.getFirst().id());

        List<OutboxEntry> afterFail =
                repository.findPendingBatch(10);
        assertThat(afterFail).isEmpty();
    }

    @Test
    @DisplayName("Increment retry count")
    void incrementRetry_updatesCount() {
        repository.save(testEntry());
        sleep(1200);

        List<OutboxEntry> batch =
                repository.findPendingBatch(10);
        long id = batch.getFirst().id();
        assertThat(batch.getFirst().retryCount()).isZero();

        repository.incrementRetry(id);

        List<OutboxEntry> after =
                repository.findPendingBatch(10);
        assertThat(after).hasSize(1);
        assertThat(after.getFirst().retryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Full poller cycle: save → poll → delivered")
    void pollerCycle_endToEnd() {
        OutboxPublisher publisher = entry ->
                CompletableFuture.completedFuture(null);
        OutboxProperties properties = new OutboxProperties(
                Duration.ofMillis(100), 50, 3,
                Duration.ofSeconds(1), Duration.ofSeconds(5));
        OutboxPoller poller = new OutboxPoller(
                repository, publisher, properties, metrics);

        repository.save(testEntry());
        sleep(1200);

        poller.poll();

        List<OutboxEntry> after =
                repository.findPendingBatch(10);
        assertThat(after).isEmpty();
    }

    private static OutboxEntry testEntry() {
        return OutboxEntry.builder()
                .topic(TopicNames.DEAL_STATE_CHANGED)
                .payload("{\"test\":true}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .version(0)
                .createdAt(Instant.now())
                .build();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
