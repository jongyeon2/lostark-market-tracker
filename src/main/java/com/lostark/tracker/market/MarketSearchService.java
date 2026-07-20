package com.lostark.tracker.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.market.dto.MarketSearchItem;
import com.lostark.tracker.market.dto.MarketSearchResponse;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.web.error.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * On-demand SERVING layer for 아바타·모험의 서 실시간 시세 검색. Nothing is persisted — this is a cached,
 * rate-limited passthrough to {@code POST /markets/items}. Modeled on {@link com.lostark.tracker.gem.GemService}:
 * shared token bucket + Redis cache-aside (fail-open) so a search never spends unmetered budget nor turns a
 * cache outage into a page outage.
 *
 * <p><b>Why on-demand, not stored</b>: unlike the tracked 거래소 items, avatars/adventure books are not
 * event-correlation subjects (the headline is engravings/materials/gems) and there are ~9k avatars per class —
 * storing them would be pure load for no analysis. A search costs at most one API call (on a cache miss).
 *
 * <p><b>Sort whitelist is load-bearing, not defensive</b>: the upstream API returns 200 and SILENTLY IGNORES
 * an unknown Sort/SortCondition, so an unvalidated value would sort by default order while the UI claims it
 * sorted. We map a small closed vocabulary here and 400 anything else, so "정렬했는데 안 바뀐다" cannot happen.
 */
@Service
public class MarketSearchService {

    private static final Logger log = LoggerFactory.getLogger(MarketSearchService.class);

    /** Short TTL — market prices move; a miss costs one call. Long enough to absorb paging back and forth. */
    static final Duration CACHE_TTL = Duration.ofMinutes(5);
    /** The class list changes only when 로스트아크 ships a new class — cache it long. */
    static final Duration CLASSES_TTL = Duration.ofHours(6);
    static final String CLASSES_CACHE_KEY = "market:classes";

    /** 모험의 서 (a single leaf category, no class/part). */
    static final String ADVENTURE_CATEGORY = "100000";
    static final String ADVENTURE_CACHE_KEY = "market:adventure:all";
    /**
     * Longer than {@link #CACHE_TTL} because a miss costs ~14 calls, not one. This is the knob that caps
     * 모험의 서 at 14 calls per 10 minutes no matter how much the page is used.
     */
    static final Duration ADVENTURE_TTL = Duration.ofMinutes(10);
    /**
     * Hard stop on the paging loop. 모험의 서 is ~140 items = 14 pages at the API's fixed PageSize 10;
     * 50 leaves room for the category to grow while guaranteeing termination if the upstream ever
     * returns a totalCount we can never reach (a non-decreasing page that always yields items would
     * otherwise spin forever, burning the collector's token budget).
     */
    static final int ADVENTURE_MAX_PAGES = 50;

    /** Frontend sort key -> upstream Sort. A closed vocabulary; anything else is a 400 (see class doc). */
    private static final Map<String, String> SORT = Map.of(
            "min_price", "CURRENT_MIN_PRICE",
            "recent_price", "RECENT_PRICE");
    /** Frontend direction -> upstream SortCondition. ASC = 낮은가격순, DESC = 높은가격순. */
    private static final Map<String, String> DIRECTION = Map.of(
            "asc", "ASC",
            "desc", "DESC");

    private final LostarkApiClient client;
    private final RedisTokenBucket rateLimiter;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public MarketSearchService(LostarkApiClient client,
                               RedisTokenBucket rateLimiter,
                               StringRedisTemplate redis,
                               ObjectMapper objectMapper) {
        this.client = client;
        this.rateLimiter = rateLimiter;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Search one category, optionally class-filtered, sorted and paged. {@code sortKey}/{@code dirKey} are the
     * frontend vocabulary (min_price/recent_price, asc/desc) and are whitelisted here — an unknown value is a
     * 400, never a silently-ignored upstream default. A cache hit spends zero API budget.
     */
    public MarketSearchResponse search(String categoryCode, String characterClass, String itemName,
                                       int page, String sortKey, String dirKey) {
        String apiSort = SORT.get(sortKey);
        String apiDir = DIRECTION.get(dirKey);
        if (apiSort == null) {
            throw new InvalidRequestException("정렬 기준이 올바르지 않습니다 (min_price | recent_price)");
        }
        if (apiDir == null) {
            throw new InvalidRequestException("정렬 방향이 올바르지 않습니다 (asc | desc)");
        }
        if (page < 1) {
            throw new InvalidRequestException("페이지는 1 이상이어야 합니다");
        }

        String cacheKey = cacheKey(categoryCode, characterClass, itemName, page, sortKey, dirKey);
        MarketSearchResponse cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        // The SAME shared bucket the collector spends from — "one API key, one bucket" (D-03). A search that
        // skipped this could race the 10분 수집 틱 into a real 429 (the Phase 26 gem bug). Core Value outranks
        // an on-demand search, so we fail the search rather than steal the collector's budget silently.
        if (!rateLimiter.tryAcquire()) {
            throw new RateLimitedApiException("검색 요청이 많습니다. 잠시 후 다시 시도해 주세요.", null);
        }

        MarketSearchResponse fresh = client.searchMarket(
                categoryCode, characterClass, itemName, page, apiSort, apiDir);
        writeCache(cacheKey, fresh, CACHE_TTL);
        return fresh;
    }

    /**
     * EVERY 모험의 서 item in one response (~140), for the 대륙별 분류 view.
     *
     * <p><b>Why the whole category instead of one page</b>: a 대륙's 7 collectibles are scattered across
     * all ~14 pages — the upstream API has no continent filter and 카테고리 100000 has no sub-categories
     * (실측), so no single paged request can produce "루테란 서부 7개". Searching by name 7 times would
     * cost 7 calls AND drag in partial-name matches.
     *
     * <p>The trade is a ~14-call cache miss, spent from the SAME shared bucket as the collector (D-03).
     * Mid-loop the bucket can run dry; we fail the request rather than steal the collector's budget —
     * Core Value outranks an on-demand search (same rule as {@link #search}). The 10분 TTL is what makes
     * this cheap in aggregate: it caps 모험의 서 at 14 calls per 10 minutes however heavily it is browsed,
     * whereas the old per-page passthrough spent one call on every page turn.
     *
     * <p>Sorting/filtering is NOT done here. The caller (frontend) holds the 대륙 map and applies 검색·정렬
     * over these 140 rows locally, so switching 대륙 costs zero calls.
     */
    public MarketSearchResponse getAdventureAll() {
        MarketSearchResponse cached = readCache(ADVENTURE_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        List<MarketSearchItem> all = new ArrayList<>();
        int totalCount = 0;
        for (int page = 1; page <= ADVENTURE_MAX_PAGES; page++) {
            if (!rateLimiter.tryAcquire()) {
                throw new RateLimitedApiException("검색 요청이 많습니다. 잠시 후 다시 시도해 주세요.", null);
            }
            MarketSearchResponse chunk = client.searchMarket(
                    ADVENTURE_CATEGORY, null, null, page, "CURRENT_MIN_PRICE", "ASC");
            totalCount = chunk.totalCount();
            all.addAll(chunk.items());
            // An empty page also ends the loop: a totalCount we can never reach must not spin forever.
            if (chunk.items().isEmpty() || all.size() >= totalCount) {
                break;
            }
        }

        MarketSearchResponse fresh = new MarketSearchResponse(1, all.size(), totalCount, all);
        writeCache(ADVENTURE_CACHE_KEY, fresh, ADVENTURE_TTL);
        return fresh;
    }

    /** The 30 playable classes for the avatar 직업 드롭다운, cached long (see {@link #CLASSES_TTL}). */
    public List<String> getClasses() {
        List<String> cached = readClassesCache();
        if (cached != null) {
            return cached;
        }
        if (!rateLimiter.tryAcquire()) {
            throw new RateLimitedApiException("잠시 후 다시 시도해 주세요.", null);
        }
        List<String> classes = client.getMarketOptions().classes();
        writeClassesCache(classes);
        return classes;
    }

    private static String cacheKey(String categoryCode, String characterClass, String itemName,
                                   int page, String sortKey, String dirKey) {
        return String.join(":", "market",
                categoryCode,
                characterClass == null ? "" : characterClass,
                itemName == null ? "" : itemName,
                sortKey, dirKey, String.valueOf(page));
    }

    private MarketSearchResponse readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, MarketSearchResponse.class);
        } catch (Exception e) {
            log.debug("market cache read failed ({}) — rebuilding", e.getClass().getSimpleName());
            return null;
        }
    }

    private void writeCache(String key, MarketSearchResponse value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            // fail-open: serving without a cache beats failing the page (GemService precedent).
            log.debug("market cache write failed ({}) — serving uncached", e.getClass().getSimpleName());
        }
    }

    private List<String> readClassesCache() {
        try {
            String json = redis.opsForValue().get(CLASSES_CACHE_KEY);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.debug("market classes cache read failed ({}) — rebuilding", e.getClass().getSimpleName());
            return null;
        }
    }

    private void writeClassesCache(List<String> classes) {
        try {
            redis.opsForValue().set(CLASSES_CACHE_KEY, objectMapper.writeValueAsString(classes), CLASSES_TTL);
        } catch (Exception e) {
            log.debug("market classes cache write failed ({}) — serving uncached", e.getClass().getSimpleName());
        }
    }
}
