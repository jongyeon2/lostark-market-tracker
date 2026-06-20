package com.lostark.tracker;

import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Walking-skeleton smoke test: boots the full Spring context against real Postgres + Redis
 * (Testcontainers) and proves the infrastructure beans are wired. If this passes, the app
 * stands up and connects before any schema or domain code exists.
 */
@SpringBootTest
@ActiveProfiles("test")
class SmokeContextTest extends PostgresRedisContainers {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoadsAndConnectsToPostgresAndRedis() {
        assertThat(dataSource).isNotNull();
        assertThat(redisConnectionFactory).isNotNull();
    }
}
