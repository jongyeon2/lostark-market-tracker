package com.lostark.tracker.cache;

import com.lostark.tracker.web.dto.LatestPriceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Hand-rolled cache-aside primitive for the latest price of one item (D-01/D-03).
 *
 * <p>Explicit {@link #get}/{@link #put}/{@link #evict} against the key {@code item:{id}:latest} —
 * no Spring cache abstraction — so both halves of cache-aside are visible: the reader fills on a
 * miss ({@code get} -> DB -> {@code put}) and the writer invalidates on a snapshot write
 * ({@code evict}). The {@link #SAFETY_TTL} is a backstop that bounds staleness to at most a
 * couple of collection cycles if an evict is ever missed; the PRIMARY invalidation signal is the
 * evict, not the TTL (D-01, evict-on-write — not TTL-only).
 *
 * <p>All operations FAIL OPEN: a Redis error is swallowed (logged at debug) and surfaces as a
 * miss / no-op so the caller falls through to a direct DB read. A cache outage therefore degrades
 * latency, not availability (T-0301-02).
 */
@Component
public class LatestPriceCache {

    private static final Logger log = LoggerFactory.getLogger(LatestPriceCache.class);

    /** Safety-net TTL: 2 collection ticks (10-min fixedDelay) — the evict is the primary signal (D-01). */
    static final Duration SAFETY_TTL = Duration.ofMinutes(20);

    private final RedisTemplate<String, LatestPriceResponse> redis;

    public LatestPriceCache(RedisTemplate<String, LatestPriceResponse> latestPriceRedisTemplate) {
        this.redis = latestPriceRedisTemplate;
    }

    static String keyFor(long itemId) {
        return "item:" + itemId + ":latest";
    }

    /** Cache read. A miss — or any Redis failure (fail-open) — returns an empty Optional. */
    public Optional<LatestPriceResponse> get(long itemId) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(keyFor(itemId)));
        } catch (RuntimeException ex) {
            log.debug("latest-price cache get failed for item {} — falling through to DB", itemId);
            return Optional.empty();
        }
    }

    /** Cache fill with the safety-net TTL. A Redis failure is a swallowed no-op (fail-open). */
    public void put(long itemId, LatestPriceResponse value) {
        try {
            redis.opsForValue().set(keyFor(itemId), value, SAFETY_TTL);
        } catch (RuntimeException ex) {
            log.debug("latest-price cache put failed for item {} — leaving cache empty", itemId);
        }
    }

    /** Evict-on-write invalidation. A Redis failure is a swallowed no-op (fail-open). */
    public void evict(long itemId) {
        try {
            redis.delete(keyFor(itemId));
        } catch (RuntimeException ex) {
            log.debug("latest-price cache evict failed for item {} — TTL backstop will bound staleness", itemId);
        }
    }
}
