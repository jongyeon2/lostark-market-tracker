package com.lostark.tracker.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.tracker.news.NewsDtos.NewsEvent;
import com.lostark.tracker.news.NewsDtos.NewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Redis-cached serving layer for the news panel (D-01/D-02/D-07). A single JSON snapshot lives at
 * {@code news:latest} (TTL 12h) — Redis cache-only, never persisted to PostgreSQL (news is volatile
 * display data). {@link #refresh()} polls both {@code /news} endpoints and overwrites the snapshot
 * ON SUCCESS ONLY; any failure (HTTP/parse) keeps the last cache untouched and leaves {@code updatedAt}
 * as it was — the read path keeps serving the last good snapshot instead of blanking the panel (D-07,
 * honesty). {@link #getLatest()} serves that snapshot, or an empty response ({@code updatedAt=null})
 * on a cold/expired cache so the panel shows an honest empty state. Independent of the market price
 * cache — its own key, its own {@link StringRedisTemplate} usage, no shared state (D-06).
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    static final String CACHE_KEY = "news:latest";
    /** Fixed 12h TTL backstop; the 6h poller refreshes well within it (D-01). */
    static final Duration CACHE_TTL = Duration.ofHours(12);

    /** 진행중 판정 zone — event {@code endDate} is a KST wall-clock string, so "now" must be KST too. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final LostarkNewsClient client;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public NewsService(LostarkNewsClient client, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.client = client;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Poll {@code /news/events} + {@code /news/notices} and overwrite {@code news:latest} on success.
     * On ANY failure the cache is left untouched (keep-on-failure) and only a warning is logged —
     * without the API key or any secret (D-06/D-07).
     */
    public void refresh() {
        try {
            NewsResponse snapshot = new NewsResponse(
                    client.fetchEvents(), client.fetchNotices(), Instant.now().toString());
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(snapshot), CACHE_TTL);
        } catch (Exception e) {
            // keep-on-failure: never overwrite the last good cache; log class only (no key/secret leak).
            log.warn("news refresh failed ({}) — keeping last cache", e.getClass().getSimpleName());
        }
    }

    /**
     * Serve the last cached snapshot, re-filtered so only 진행중 events go out. A miss (cold or expired
     * cache) — or any Redis error (fail-open) — returns empty lists + {@code updatedAt=null} so the
     * panel renders an honest empty state (D-07).
     *
     * <p>The serve-time 만료 filter is a safety net over the poll-time one in
     * {@link LostarkNewsClient#fetchEvents()}: the snapshot is cached for up to {@link #CACHE_TTL} and
     * refreshed every 6h, so an event that was running when it was cached can end well before the next
     * poll. Without this, the panel would keep advertising it as 진행중 for up to 6 hours. Filtering here
     * (not re-polling) costs nothing and never touches the keep-on-failure guarantee — the cached JSON
     * itself is left exactly as it is.
     */
    public NewsResponse getLatest() {
        try {
            String json = redis.opsForValue().get(CACHE_KEY);
            if (json == null || json.isBlank()) {
                return empty();
            }
            NewsResponse cached = objectMapper.readValue(json, NewsResponse.class);
            LocalDateTime nowKst = LocalDateTime.now(KST);
            List<NewsEvent> ongoing = cached.events().stream()
                    .filter(e -> e.isOngoingAt(nowKst))
                    .toList();
            return new NewsResponse(ongoing, cached.notices(), cached.updatedAt());
        } catch (Exception e) {
            log.debug("news cache read failed ({}) — serving empty", e.getClass().getSimpleName());
            return empty();
        }
    }

    private static NewsResponse empty() {
        return new NewsResponse(List.of(), List.of(), null);
    }
}