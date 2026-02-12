package com.advertmarket.shared;

/**
 * Fenum group name constants for use with
 * {@link org.checkerframework.checker.fenum.qual.Fenum @Fenum}.
 *
 * <p>Centralizes group names so they are not hardcoded
 * across annotation sites.
 */
public final class FenumGroup {

    /** Kafka topic name group. */
    public static final String TOPIC_NAME = "TopicName";

    /** Kafka consumer group ID group. */
    public static final String CONSUMER_GROUP = "ConsumerGroup";

    /** Event type discriminator group. */
    public static final String EVENT_TYPE = "EventType";

    /** Domain exception error code group. */
    public static final String ERROR_CODE = "ErrorCode";

    /** Micrometer metric name group. */
    public static final String METRIC_NAME = "MetricName";

    private FenumGroup() {
    }
}
