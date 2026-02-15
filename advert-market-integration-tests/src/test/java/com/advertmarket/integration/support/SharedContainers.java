package com.advertmarket.integration.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers for integration tests.
 *
 * <p>Provides shared PostgreSQL and Redis containers started once per JVM.
 * Never use {@code @Container} or {@code @Testcontainers} — always access
 * containers through this class.
 */
public final class SharedContainers {

    private static final int REDIS_PORT = 6379;

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName
                    // Pinned by digest to keep CI reproducible (avoid :latest flakiness).
                    .parse("paradedb/paradedb@sha256:6279f4c4540507f8c96d53ac0c1e202d21f9f47f3c14fed24a02a7cc2da0730c")
                    .asCompatibleSubstituteFor("postgres"));

    @SuppressWarnings("resource")
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse(
                    // Pinned by digest to keep CI reproducible.
                    "redis:8.4-alpine@sha256:e55195a3afbb812a14306909caad6583a1ab6c79f393f53490e42cb2111084a9"))
                    .withExposedPorts(REDIS_PORT);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    private SharedContainers() {
    }

    /** JDBC URL for the shared Postgres container. */
    public static String pgJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    /** Username for the shared Postgres container. */
    public static String pgUsername() {
        return POSTGRES.getUsername();
    }

    /** Password for the shared Postgres container. */
    public static String pgPassword() {
        return POSTGRES.getPassword();
    }

    /** Host for the shared Redis container. */
    public static String redisHost() {
        return REDIS.getHost();
    }

    /** Mapped port for the shared Redis container. */
    public static int redisPort() {
        return REDIS.getMappedPort(REDIS_PORT);
    }
}
