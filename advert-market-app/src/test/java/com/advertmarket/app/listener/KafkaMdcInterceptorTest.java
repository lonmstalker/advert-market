package com.advertmarket.app.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.advertmarket.shared.logging.MdcKeys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("KafkaMdcInterceptor tests")
class KafkaMdcInterceptorTest {

    private KafkaMdcInterceptor interceptor;

    @SuppressWarnings("unchecked")
    private final Consumer<String, String> consumer =
            mock(Consumer.class);

    @BeforeEach
    void setUp() {
        interceptor = new KafkaMdcInterceptor();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Propagates correlation ID from Kafka header")
    void propagatesCorrelationIdFromHeader() {
        String expected = "test-correlation-id";
        RecordHeaders headers = new RecordHeaders();
        headers.add(KafkaMdcInterceptor.HEADER_NAME,
                expected.getBytes(StandardCharsets.UTF_8));
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L,
                        0L, null, 0, 0, "key", "value",
                        headers, java.util.Optional.empty());

        interceptor.intercept(record, consumer);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Generates UUID when no header present")
    void generatesUuidWhenNoHeader() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L,
                        "key", "value");

        interceptor.intercept(record, consumer);

        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        assertThat(correlationId).isNotNull().isNotBlank();
        assertThat(UUID.fromString(correlationId)).isNotNull();
    }

    @Test
    @DisplayName("success() clears MDC")
    void successClearsMdc() {
        MDC.put(MdcKeys.CORRELATION_ID, "some-id");
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L,
                        "key", "value");

        interceptor.success(record, consumer);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("failure() clears MDC")
    void failureClearsMdc() {
        MDC.put(MdcKeys.CORRELATION_ID, "some-id");
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L,
                        "key", "value");

        interceptor.failure(record,
                new RuntimeException("test"), consumer);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }

    @Test
    @DisplayName("afterRecord() clears MDC")
    void afterRecordClearsMdc() {
        MDC.put(MdcKeys.CORRELATION_ID, "some-id");
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L,
                        "key", "value");

        interceptor.afterRecord(record, consumer);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }
}
