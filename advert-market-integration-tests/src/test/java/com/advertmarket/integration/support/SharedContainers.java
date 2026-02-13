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

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName
                    .parse("paradedb/paradedb:latest")
                    .asCompatibleSubstituteFor("postgres"));

    @SuppressWarnings("resource")
    public static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:8.4-alpine")
                    .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    private SharedContainers() {
    }

    public static String pgJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    public static String pgUsername() {
        return POSTGRES.getUsername();
    }

    public static String pgPassword() {
        return POSTGRES.getPassword();
    }

    public static String redisHost() {
        return REDIS.getHost();
    }

    public static int redisPort() {
        return REDIS.getMappedPort(6379);
    }
}
