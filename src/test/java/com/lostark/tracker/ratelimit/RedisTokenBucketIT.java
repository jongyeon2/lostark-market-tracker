package com.lostark.tracker.ratelimit;

import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the self-built Redis token bucket (D-03): atomic consume via Lua, lazy refill from a
 * stored timestamp, and — the headline (Success Criterion 2) — that the token count is restored
 * from Redis when a fresh bucket instance is constructed (i.e. survives an app restart).
 *
 * <p>The clock is injected so refill is asserted deterministically without real waiting.
 */
@SpringBootTest
class RedisTokenBucketIT extends PostgresRedisContainers {

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    RedisScript<Long> tokenBucketScript;

    private RedisTokenBucket bucket(String key, long capacity, double refillPerSec, LongSupplier clock) {
        return new RedisTokenBucket(redis, tokenBucketScript, key, capacity, refillPerSec, clock);
    }

    @Test
    void consumesUpToCapacityThenThrottles() {
        String key = "test:bucket:" + UUID.randomUUID();
        AtomicLong now = new AtomicLong(1_000_000L);
        // refillPerSec = 0 -> no refill during the test, so capacity is the hard ceiling.
        RedisTokenBucket b = bucket(key, 5, 0.0, now::get);

        for (int i = 0; i < 5; i++) {
            assertThat(b.tryAcquire()).as("acquire %d of capacity", i + 1).isTrue();
        }
        assertThat(b.tryAcquire()).as("6th acquire over capacity is throttled").isFalse();
    }

    @Test
    void lazilyRefillsProportionalToElapsedTime() {
        String key = "test:bucket:" + UUID.randomUUID();
        AtomicLong now = new AtomicLong(2_000_000L);
        // 10 tokens/sec. Exhaust 10, then advance 500ms -> exactly 5 tokens refill.
        RedisTokenBucket b = bucket(key, 10, 10.0, now::get);

        for (int i = 0; i < 10; i++) {
            assertThat(b.tryAcquire()).isTrue();
        }
        assertThat(b.tryAcquire()).as("empty before refill").isFalse();

        now.addAndGet(500); // +0.5s * 10/s = 5 tokens
        for (int i = 0; i < 5; i++) {
            assertThat(b.tryAcquire()).as("refilled token %d", i + 1).isTrue();
        }
        assertThat(b.tryAcquire()).as("only 5 tokens refilled").isFalse();
    }

    @Test
    void restoresPartialTokenCountAfterRestart() {
        String key = "test:bucket:" + UUID.randomUUID();
        AtomicLong now = new AtomicLong(3_000_000L);

        // Instance #1 consumes 3 of 5 tokens (2 remain), persisted in Redis.
        RedisTokenBucket first = bucket(key, 5, 0.0, now::get);
        for (int i = 0; i < 3; i++) {
            assertThat(first.tryAcquire()).isTrue();
        }

        // Instance #2 simulates an app restart: brand-new object, no in-memory count,
        // same Redis key + same (frozen) clock. It must see the persisted 2 tokens,
        // NOT reset back to full capacity.
        RedisTokenBucket afterRestart = bucket(key, 5, 0.0, now::get);
        assertThat(afterRestart.tryAcquire()).as("restored token 1").isTrue();
        assertThat(afterRestart.tryAcquire()).as("restored token 2").isTrue();
        assertThat(afterRestart.tryAcquire()).as("no reset-to-full after restart").isFalse();
    }
}
