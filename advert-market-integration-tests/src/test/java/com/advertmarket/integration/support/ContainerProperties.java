package com.advertmarket.integration.support;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Registers shared container properties for {@code @DynamicPropertySource}.
 */
public final class ContainerProperties {

    private ContainerProperties() {
    }

    /**
     * Registers PostgreSQL + Redis properties.
     */
    public static void registerAll(DynamicPropertyRegistry registry) {
        registerPostgres(registry);
        registerRedis(registry);
    }

    /**
     * Registers PostgreSQL + Redis + Kafka properties.
     */
    public static void registerAllWithKafka(
            DynamicPropertyRegistry registry) {
        registerAll(registry);
        registerKafka(registry);
    }

    /**
     * Registers PostgreSQL datasource properties.
     */
    public static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                SharedContainers::pgJdbcUrl);
        registry.add("spring.datasource.username",
                SharedContainers::pgUsername);
        registry.add("spring.datasource.password",
                SharedContainers::pgPassword);
    }

    /**
     * Registers Redis connection properties.
     */
    public static void registerRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host",
                SharedContainers::redisHost);
        registry.add("spring.data.redis.port",
                SharedContainers::redisPort);
    }

    /**
     * Registers Kafka bootstrap servers.
     */
    public static void registerKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                SharedContainers::kafkaBootstrapServers);
    }
}
