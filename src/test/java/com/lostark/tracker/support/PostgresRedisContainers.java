package com.lostark.tracker.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers base for every integration test in the project.
 *
 * <p>Starts a singleton PostgreSQL container (the same engine used in dev — no MySQL) and a
 * Redis container once per JVM, and exposes their ephemeral connection coordinates to Spring
 * via {@link DynamicPropertySource}. Tests extend this class instead of declaring their own
 * containers, so local and CI runs exercise the identical persistence + cache stack. This is
 * the contract reused by Phases 2-5.
 */
@Testcontainers
public abstract class PostgresRedisContainers {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("lostark")
                    .withUsername("lostark")
                    .withPassword("lostark");

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    static {
        // Singleton-container pattern: start once and reuse across all subclasses.
        // Not annotated @Container on purpose, so Testcontainers does not stop them per-class;
        // the Ryuk resource reaper tears them down when the JVM exits.
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
