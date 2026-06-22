package com.lostark.tracker.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Self-built client-side rate limiter (D-03/D-04): a single global token bucket whose state
 * (token count + last-refill timestamp) lives entirely in Redis. There is intentionally NO
 * in-memory token field — every {@link #tryAcquire()} reads the count from Redis through an
 * atomic Lua script, so the bucket throttles correctly under concurrency AND restores its
 * token count after an app restart (Success Criterion 2). Bucket4j was rejected so this
 * restart-restore behaviour stays visible rather than hidden behind a library.
 *
 * <p>The clock is injected ({@link LongSupplier} of epoch millis) so refill can be tested
 * deterministically; production wires {@link System#currentTimeMillis()}.
 */
public class RedisTokenBucket {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> script;
    private final String key;
    private final long capacity;
    private final double refillPerSec;
    private final LongSupplier clockMillis;

    public RedisTokenBucket(StringRedisTemplate redis,
                            RedisScript<Long> script,
                            String key,
                            long capacity,
                            double refillPerSec,
                            LongSupplier clockMillis) {
        this.redis = redis;
        this.script = script;
        this.key = key;
        this.capacity = capacity;
        this.refillPerSec = refillPerSec;
        this.clockMillis = clockMillis;
    }

    /** Try to consume a single token. Returns {@code true} if granted, {@code false} if throttled. */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Try to consume {@code permits} tokens atomically. On any Redis failure this fails CLOSED
     * (returns {@code false}) so the caller throttles itself rather than bypassing the limiter
     * and flooding the rate-limited API.
     */
    public boolean tryAcquire(int permits) {
        try {
            Long granted = redis.execute(
                    script,
                    List.of(key),
                    Long.toString(clockMillis.getAsLong()),
                    Long.toString(capacity),
                    Double.toString(refillPerSec),
                    Integer.toString(permits));
            return granted != null && granted == 1L;
        } catch (RuntimeException ex) {
            // Fail closed: never grant a token when Redis is unreachable.
            return false;
        }
    }
}
