package com.lostark.tracker.news;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.lostark.tracker.news.NewsDtos.NewsEvent;
import com.lostark.tracker.news.NewsDtos.NewsNotice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * Reads the Lostark official news feed for the dashboard panel (D-01/D-02). Two read-only GETs —
 * {@code /news/events} and {@code /news/notices} — reusing the same {@code lostark.api.*} base-url +
 * {@code bearer} key as the collection client (key normalization copied from {@code LostarkApiClient}:
 * drop a {@code bearer } prefix, strip all whitespace — the Task-0 401 gotcha). The public news schema
 * is the one live-measured in {@code 17.2-NEWS-SPIKE-FINDINGS.md}: both endpoints return a top-level
 * JSON array of PascalCase objects, so a dedicated {@link ObjectMapper} with an
 * {@code UPPER_CAMEL_CASE} naming strategy maps {@code Title/Link/StartDate/…} onto the camelCase
 * record components — while the records still serialize back to the frontend in camelCase.
 *
 * <p>Events are returned 종료임박순 ({@code endDate} ascending), notices 최신순 ({@code date} descending),
 * each capped at {@link #MAX_ITEMS} (D-04). HTTP and parse failures propagate to {@link NewsService},
 * which keeps the last cache (D-07). No price/key data is read or logged (D-06); unknown extra source
 * fields (e.g. {@code RewardDate}) are ignored so a schema addition never breaks the read path.
 */
@Component
public class LostarkNewsClient {

    /** Display cap per section (D-04) — the panel shows the most relevant handful, not the full feed. */
    static final int MAX_ITEMS = 6;

    /** 진행중 판정 zone — {@code endDate} is a KST wall-clock string, so "now" must be KST too. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RestClient restClient;

    /**
     * Inbound-only mapper: {@code UPPER_CAMEL_CASE} maps Lostark's {@code Title/StartDate/…} onto the
     * camelCase record components; {@code FAIL_ON_UNKNOWN_PROPERTIES=false} tolerates extra source
     * fields (e.g. {@code RewardDate}, which the display DTO intentionally omits).
     */
    private final ObjectMapper newsMapper = JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public LostarkNewsClient(RestClient.Builder lostarkRestClientBuilder,
                             @Value("${lostark.api.base-url}") String baseUrl,
                             @Value("${lostark.api.key:}") String apiKey) {
        // Same normalization as LostarkApiClient: drop optional "bearer "/"Bearer " prefix + strip ALL
        // whitespace (JWTs have none; a wrapped copy/paste injects spaces -> 401, the Task-0 gotcha).
        String token = apiKey == null ? "" :
                apiKey.strip().replaceFirst("(?i)^bearer\\s+", "").replaceAll("\\s", "");
        // clone() so mutating base-url here never leaks onto the shared collection-client builder.
        this.restClient = lostarkRestClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("accept", "application/json")
                .defaultHeader("authorization", "bearer " + token)
                .build();
    }

    /**
     * GET {@code /news/events} -> 진행중인 events only, 종료임박순(endDate asc), capped at
     * {@link #MAX_ITEMS}.
     *
     * <p>The 만료 filter MUST run BEFORE the sort and the cap. The sort is 종료임박순, so ended events
     * sort FIRST — filtering after the cap would let them occupy every one of the {@link #MAX_ITEMS}
     * slots and push genuinely running events out of the snapshot entirely (the live 2026-07-15 bug:
     * two events that ended 7/8 held the top of the panel).
     */
    public List<NewsEvent> fetchEvents() {
        List<NewsEvent> events = readArray("/news/events", new TypeReference<List<NewsEvent>>() {
        });
        LocalDateTime nowKst = LocalDateTime.now(KST);
        return events.stream()
                .filter(e -> e.isOngoingAt(nowKst))
                .sorted(Comparator.comparing(NewsEvent::endDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_ITEMS)
                .toList();
    }

    /** GET {@code /news/notices} -> notices 최신순(date desc), capped at {@link #MAX_ITEMS}. */
    public List<NewsNotice> fetchNotices() {
        List<NewsNotice> notices = readArray("/news/notices", new TypeReference<List<NewsNotice>>() {
        });
        return notices.stream()
                .sorted(Comparator.comparing(NewsNotice::date, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(MAX_ITEMS)
                .toList();
    }

    /**
     * GET {@code path} and parse the top-level JSON array with the inbound PascalCase mapper. A
     * non-array body (e.g. an error envelope that slips past the status handler) yields an empty list
     * rather than a crash; HTTP 4xx/5xx already throw before reaching here and propagate to the caller.
     */
    private <T> List<T> readArray(String path, TypeReference<List<T>> type) {
        String body = restClient.get().uri(path).retrieve().body(String.class);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = newsMapper.readTree(body);
            if (!root.isArray()) {
                return List.of();
            }
            return newsMapper.convertValue(root, type);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Malformed body — surface as a fetch failure so NewsService keeps the last cache (D-07).
            throw new IllegalStateException("Lostark /news parse failure for " + path, e);
        }
    }
}
