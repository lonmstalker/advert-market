package com.advertmarket.app.outbox;

import com.advertmarket.shared.outbox.OutboxEntry;
import com.advertmarket.shared.outbox.OutboxPublisher;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed implementation of {@link OutboxPublisher}.
 */
@Component
@RequiredArgsConstructor
public class KafkaOutboxPublisher implements OutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public @NonNull CompletableFuture<Void> publish(
            @NonNull OutboxEntry entry) {
        return kafkaTemplate.send(
                        entry.topic(),
                        entry.partitionKey(),
                        entry.payload())
                .thenAccept(_ -> {});
    }
}
