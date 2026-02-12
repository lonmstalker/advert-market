package com.advertmarket.shared.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.event.TopicNames;
import com.advertmarket.shared.metric.MetricNames;
import com.advertmarket.shared.metric.MetricsFacade;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("OutboxPoller")
@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private OutboxPublisher publisher;

    @Mock
    private OutboxProperties properties;

    @Mock
    private MetricsFacade metrics;

    @InjectMocks
    private OutboxPoller poller;

    @Test
    @DisplayName("Empty batch increments poll counter and returns")
    void poll_emptyBatch_incrementsCounter() {
        when(properties.batchSize()).thenReturn(50);
        when(repository.findPendingBatch(50))
                .thenReturn(List.of());

        poller.poll();

        verify(metrics).incrementCounter(
                MetricNames.OUTBOX_POLL_COUNT);
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("Successful publish marks entry as delivered")
    void poll_successfulPublish_marksDelivered() {
        OutboxEntry entry = testEntry(1L, 0);
        when(properties.batchSize()).thenReturn(50);
        when(repository.findPendingBatch(50))
                .thenReturn(List.of(entry));
        when(publisher.publish(entry))
                .thenReturn(CompletableFuture.completedFuture(null));

        poller.poll();

        verify(repository).markDelivered(1L);
        verify(metrics).incrementCounter(
                MetricNames.OUTBOX_PUBLISHED);
    }

    @Test
    @DisplayName("Failed publish increments retry when under max")
    void poll_failedPublish_incrementsRetry() {
        OutboxEntry entry = testEntry(2L, 0);
        when(properties.batchSize()).thenReturn(50);
        when(properties.maxRetries()).thenReturn(3);
        when(repository.findPendingBatch(50))
                .thenReturn(List.of(entry));
        when(publisher.publish(entry))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")));

        poller.poll();

        verify(repository).incrementRetry(2L);
        verify(repository, never()).markFailed(2L);
    }

    @Test
    @DisplayName("Failed publish marks as failed when max retries reached")
    void poll_maxRetriesReached_marksFailed() {
        OutboxEntry entry = testEntry(3L, 2);
        when(properties.batchSize()).thenReturn(50);
        when(properties.maxRetries()).thenReturn(3);
        when(repository.findPendingBatch(50))
                .thenReturn(List.of(entry));
        when(publisher.publish(entry))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")));

        poller.poll();

        verify(repository).markFailed(3L);
        verify(metrics).incrementCounter(
                MetricNames.OUTBOX_RECORDS_FAILED);
    }

    @Test
    @DisplayName("Multiple entries are processed independently")
    void poll_multipleEntries_processedIndependently() {
        OutboxEntry success = testEntry(1L, 0);
        OutboxEntry failure = testEntry(2L, 2);
        when(properties.batchSize()).thenReturn(50);
        when(properties.maxRetries()).thenReturn(3);
        when(repository.findPendingBatch(50))
                .thenReturn(List.of(success, failure));
        when(publisher.publish(success))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(publisher.publish(failure))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("fail")));

        poller.poll();

        verify(repository).markDelivered(1L);
        verify(repository).markFailed(2L);
    }

    private static OutboxEntry testEntry(
            long id, int retryCount) {
        return OutboxEntry.builder()
                .id(id)
                .topic(TopicNames.DEAL_STATE_CHANGED)
                .payload("{\"test\":true}")
                .status(OutboxStatus.PENDING)
                .retryCount(retryCount)
                .version(0)
                .createdAt(Instant.now())
                .build();
    }
}
