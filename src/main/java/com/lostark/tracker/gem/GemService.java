package com.lostark.tracker.gem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.tracker.gem.GemDtos.GemPriceStatus;
import com.lostark.tracker.gem.GemDtos.GemsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Cache-aside SERVING layer for 보석 현재가 (GEM-02). One JSON snapshot lives at {@code gem:latest}
 * (TTL {@link #CACHE_TTL}) — Redis only, never PostgreSQL.
 *
 * <p><b>Why serving stays on-demand and grows no poller</b> (unlike {@code NewsService}'s 6h schedule):
 * 경매장 shares the per-key rate-limit budget with the 10분 수집 tick (Phase 24 §H2), so every gem call is
 * taken straight out of the collector's allowance. Cache-aside costs ZERO when nobody is looking.
 *
 * <p><b>There IS a poller now — but not for this path</b> (Phase 27, GEM-03). {@link GemPriceRecorder}
 * records a sample hourly into {@code gem_price_snapshot}, because 경매장 offers no history endpoint and
 * gems have no {@code Id} (Phase 24 §H5), so the record is the ONLY way gem history can ever exist.
 * It deliberately does NOT warm {@code gem:latest}: an hourly refresh would make this screen serve a
 * value up to an hour old, when it currently serves one at most {@link #CACHE_TTL} old. Recording and
 * serving want opposite things — an hourly heartbeat vs. the freshest possible answer — so they stay
 * separate paths over one {@link GemPriceFetcher}.
 *
 * <p>Consequence for copy: this is NOT "5분마다 갱신" and it is NOT "1시간마다 갱신" either. It is
 * "a snapshot at most {@link #CACHE_TTL} old, built when someone last looked" — which is why the
 * response carries {@code updatedAt} and the screen shows 기준 시각 rather than promising a cadence.
 */
@Service
public class GemService {

    private static final Logger log = LoggerFactory.getLogger(GemService.class);

    static final String CACHE_KEY = "gem:latest";
    /** Short TTL: gem prices move, and a miss costs only 6 calls. Long enough to absorb a page refresh. */
    static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final GemPriceFetcher fetcher;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GemService(GemPriceFetcher fetcher,
                      StringRedisTemplate redis,
                      ObjectMapper objectMapper) {
        this.fetcher = fetcher;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Serve the cached snapshot, building it on a miss. A Redis failure is fail-open: the snapshot is
     * built and served without caching rather than turning a cache outage into a page outage
     * ({@code NewsService.getLatest} precedent).
     */
    public GemsResponse getAll() {
        GemsResponse cached = readCache();
        if (cached != null) {
            return cached;
        }
        GemsResponse fresh = new GemsResponse(fetcher.fetchAll(), Instant.now().toString());
        if (isCacheable(fresh)) {
            writeCache(fresh);
        }
        return fresh;
    }

    /**
     * Cache everything EXCEPT a snapshot degraded by our own throttling.
     *
     * <p>{@code OK}/{@code NO_BUYOUT}/{@code FETCH_FAILED} are answers — 경매장 responded (or genuinely
     * failed), so caching them is right and re-asking would just re-spend the shared budget.
     * {@code RATE_LIMITED} is not an answer: we never asked. Freezing it for {@link #CACHE_TTL} would
     * keep the page degraded for 5 minutes after the tokens refill seconds later — and retrying costs
     * ZERO API calls, since a throttled row is rejected by the bucket before any request goes out.
     */
    private static boolean isCacheable(GemsResponse snapshot) {
        return snapshot.gems().stream().noneMatch(g -> g.status() == GemPriceStatus.RATE_LIMITED);
    }

    private GemsResponse readCache() {
        try {
            String json = redis.opsForValue().get(CACHE_KEY);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, GemsResponse.class);
        } catch (Exception e) {
            log.debug("gem cache read failed ({}) — rebuilding", e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Cache the snapshot INCLUDING any failed/no-buyout rows. Caching only the successes would make the
     * next request re-query the failing gem, spending the shared budget on a repeat failure on every
     * page view; the TTL gives it a natural retry instead.
     */
    private void writeCache(GemsResponse snapshot) {
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(snapshot), CACHE_TTL);
        } catch (Exception e) {
            // fail-open: serving without a cache beats failing the page.
            log.debug("gem cache write failed ({}) — serving uncached", e.getClass().getSimpleName());
        }
    }
}
