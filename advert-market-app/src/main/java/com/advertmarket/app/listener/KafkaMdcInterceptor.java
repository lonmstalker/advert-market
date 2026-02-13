package com.advertmarket.app.listener;

import com.advertmarket.shared.logging.MdcKeys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Kafka interceptor that propagates correlation ID from record headers
 * into MDC for structured logging.
 *
 * <p>If no {@code X-Correlation-Id} header is present, a new UUID is generated.
 */
public class KafkaMdcInterceptor
        implements RecordInterceptor<String, String> {

    /** Kafka header name for the correlation ID. */
    public static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    public ConsumerRecord<String, String> intercept(
            @NonNull ConsumerRecord<String, String> record,
            @NonNull Consumer<String, String> consumer) {
        Header header = record.headers().lastHeader(HEADER_NAME);
        String correlationId;
        if (header != null && header.value() != null) {
            correlationId = new String(
                    header.value(), StandardCharsets.UTF_8);
        } else {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        return record;
    }

    @Override
    public void success(
            @NonNull ConsumerRecord<String, String> record,
            @NonNull Consumer<String, String> consumer) {
        MDC.clear();
    }

    @Override
    public void failure(
            @NonNull ConsumerRecord<String, String> record,
            @NonNull Exception exception,
            @NonNull Consumer<String, String> consumer) {
        MDC.clear();
    }

    @Override
    public void afterRecord(
            @NonNull ConsumerRecord<String, String> record,
            @NonNull Consumer<String, String> consumer) {
        MDC.clear();
    }
}
