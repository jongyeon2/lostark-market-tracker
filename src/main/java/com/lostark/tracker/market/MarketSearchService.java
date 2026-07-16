package com.lostark.tracker.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.market.dto.MarketSearchResponse;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.web.error.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
        writeCache(cacheKey, fresh);
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

    private void writeCache(String key, MarketSearchResponse value) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
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
