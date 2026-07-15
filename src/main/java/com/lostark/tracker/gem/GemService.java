package com.lostark.tracker.gem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.tracker.gem.GemDtos.GemPrice;
import com.lostark.tracker.gem.GemDtos.GemPriceStatus;
import com.lostark.tracker.gem.GemDtos.GemsResponse;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cache-aside serving layer for 보석 현재가 (GEM-02). One JSON snapshot lives at {@code gem:latest}
 * (TTL {@link #CACHE_TTL}) — Redis only, never PostgreSQL: gems are a browse-only view and are
 * deliberately NOT recorded as a time series (scope boundary — 거래소 시계열 수집 stays uncontaminated).
 *
 * <p><b>Why on-demand and not a poller (unlike {@code NewsService}'s 6h schedule):</b> 경매장 shares the
 * per-key rate-limit budget with the 10분 수집 tick (Phase 24 §H2), so every gem call is taken straight
 * out of the collector's allowance. News is on the dashboard, so every visitor sees it and a poller
 * pays for itself; gems live on their own page. A 5분 poller would spend ~1.2 calls/min forever —
 * including all the hours nobody is looking. Cache-aside costs ZERO when the page is unvisited.
 *
 * <p>Consequence for copy: this is NOT "5분마다 갱신". It is "a snapshot at most {@link #CACHE_TTL} old,
 * built when someone last looked" — which is why the response carries {@code updatedAt} and the page
 * shows 기준 시각 instead of promising a refresh cadence (UI-SPEC §갱신 시각).
 */
@Service
public class GemService {

    private static final Logger log = LoggerFactory.getLogger(GemService.class);

    static final String CACHE_KEY = "gem:latest";
    /** Short TTL: gem prices move, and a miss costs only 6 calls. Long enough to absorb a page refresh. */
    static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final LostarkAuctionClient client;
    private final RedisTokenBucket rateLimiter;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GemService(LostarkAuctionClient client,
                      RedisTokenBucket rateLimiter,
                      StringRedisTemplate redis,
                      ObjectMapper objectMapper) {
        this.client = client;
        this.rateLimiter = rateLimiter;
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
        GemsResponse fresh = new GemsResponse(fetchAll(), Instant.now().toString());
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

    /**
     * One call per gem — 경매장 offers no batch lookup, and each gem is isolated by an exact ItemName
     * (Phase 24: the level exists only in the name, there is no numeric level filter).
     *
     * <p>A single gem's failure is swallowed into a {@code FETCH_FAILED} row so it can never blank the
     * other five — the same per-row isolation {@code ItemCard} applies to a 404 latest-price.
     */
    private List<GemPrice> fetchAll() {
        List<GemPrice> gems = new ArrayList<>(GemCatalog.ENTRIES.size());
        for (GemCatalog.Entry entry : GemCatalog.ENTRIES) {
            gems.add(fetchOne(entry));
        }
        return gems;
    }

    private GemPrice fetchOne(GemCatalog.Entry entry) {
        // The SAME global bucket the collector spends from — "one API key, one bucket" (D-03). 경매장 and
        // 거래소 share one server-side per-key quota (Phase 24 §H2), so a gem call that skipped this
        // limiter would spend budget the app never accounted for and race the 10분 틱 into a real 429 —
        // which is exactly what happened on the first live run (겁화 3 OK, 작열 3 × TooManyRequests).
        // Yielding here keeps the collector whole: Core Value outranks this page.
        if (!rateLimiter.tryAcquire()) {
            return row(entry, null, GemPriceStatus.RATE_LIMITED);
        }
        try {
            Optional<Long> lowest = client.findLowestBuyPrice(entry.searchName());
            // empty = 매물은 있으나 즉시구매를 건 것이 없음(입찰 전용) → 값을 지어내지 않고 상태로 말한다.
            return lowest
                    .map(price -> row(entry, price, GemPriceStatus.OK))
                    .orElseGet(() -> row(entry, null, GemPriceStatus.NO_BUYOUT));
        } catch (HttpClientErrorException.TooManyRequests e) {
            // The bucket granted a token but the server refused anyway — the app's 90-token bucket can
            // legitimately outrun the real 100/min window (a full bucket plus a minute of refill is up to
            // 180 calls), and dev startup fires the 49-item tick and the backfill runner at once. This is
            // pre-existing shared-limiter behaviour (Phase 2), NOT something gems can fix without editing
            // Core Value code — the collector already absorbs 429 via Retry-After. Gems just tell the truth:
            // same meaning as a local throttle (설계된 양보, 곧 회복), so same status and same no-cache rule.
            log.warn("gem price throttled by API (429) for level {} {} — row marked RATE_LIMITED",
                    entry.level(), entry.series());
            return row(entry, null, GemPriceStatus.RATE_LIMITED);
        } catch (Exception e) {
            // Log the class only — never the key/secret (NewsService precedent).
            log.warn("gem price fetch failed for level {} {} ({}) — row marked FETCH_FAILED",
                    entry.level(), entry.series(), e.getClass().getSimpleName());
            return row(entry, null, GemPriceStatus.FETCH_FAILED);
        }
    }

    private static GemPrice row(GemCatalog.Entry entry, Long price, GemPriceStatus status) {
        return new GemPrice(entry.series(), entry.level(), entry.displayName(), entry.iconUrl(), price, status);
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
