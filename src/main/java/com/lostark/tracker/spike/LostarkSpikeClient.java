package com.lostark.tracker.spike;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Task 0 spike client (D-03 RestClient). Issues a single read-only POST to the Lostark
 * {@code markets/items} endpoint and returns the full response (status + headers + raw body)
 * so the spike can inspect the actual fields and rate-limit headers. No persistence.
 *
 * <p>Active only under the {@code spike} profile, exercised by the {@code @Disabled}
 * {@link MarketsApiSpikeTest}; never wired in normal runs or CI.
 */
@Component
@Profile("spike")
public class LostarkSpikeClient {

    private final RestClient restClient;

    public LostarkSpikeClient(@Value("${lostark.api.base-url}") String baseUrl,
                              @Value("${lostark.api.key:}") String apiKey) {
        // Normalize the configured key: drop an optional "bearer "/"Bearer " prefix, then strip ALL
        // whitespace. JWTs contain no spaces, but copy/paste from a wrapped display injects them
        // (verified during the Task 0 spike) — that corrupts the signature and yields 401.
        String token = apiKey == null ? "" :
                apiKey.strip().replaceFirst("(?i)^bearer\\s+", "").replaceAll("\\s", "");
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("accept", "application/json")
                // Auth header per portal docs: "Authorization: bearer {token}". Key from env only.
                .defaultHeader("authorization", "bearer " + token)
                .build();
    }

    /**
     * GET {@code /markets/options} — returns the full category tree. Leaf {@code CategoryCode}s
     * live at {@code Categories[].Subs[].Code} (a parent code returns {@code TotalCount:0}; only a
     * leaf returns items, per the Task 0 finding). The Phase 12 spike reads this to confirm the
     * 유물 각인서 (40000) and 융화재료 leaf codes empirically before locking the curation list.
     * Returns status + headers + raw body; no persistence.
     */
    public ResponseEntity<String> getMarketOptions() {
        return restClient.get()
                .uri("/markets/options")
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * Single POST to {@code /markets/items}. CategoryCode 50010 is "재련 재료" (refining materials),
     * a leaf category confirmed during the Task 0 spike to return items; {@code ItemName} is added
     * only when provided (a blank name lists the category). Returns status + headers + raw body.
     *
     * <p>Backward-compatible overload: delegates to {@link #searchMarketItems(int, String)} with the
     * Task 0 leaf code so existing callers (and the original spike case) keep working unchanged.
     */
    public ResponseEntity<String> searchMarketItems(String itemName) {
        return searchMarketItems(50010, itemName);
    }

    /**
     * POST {@code /markets/items} with a parameterized {@code CategoryCode} so the Phase 12 spike can
     * query the 유물 각인서 category (40000) and 융화재료 leaf categories with the same request shape.
     * {@code CategoryCode} must be a leaf (a parent code yields {@code TotalCount:0}); {@code ItemName}
     * is an optional filter added only when provided. Returns status + headers + raw body.
     */
    public ResponseEntity<String> searchMarketItems(int categoryCode, String itemName) {
        return searchMarketItems(categoryCode, itemName, 1, "ASC");
    }

    /**
     * Overload with {@code PageNo} + {@code SortCondition} so a spike can page/sort — e.g. sort
     * {@code DESC} (most-expensive first) to surface the highest-tier items that a cheapest-first
     * page 1 hides (Phase 22b: confirm 상급재련 [19-20] tier). Same request shape otherwise.
     */
    public ResponseEntity<String> searchMarketItems(int categoryCode, String itemName, int pageNo, String sortCondition) {
        String nameField = (itemName == null || itemName.isBlank())
                ? ""
                : "\"ItemName\": \"%s\",%n".formatted(itemName);
        String requestBody = """
                {
                  %s"CategoryCode": %d,
                  "Sort": "CURRENT_MIN_PRICE",
                  "PageNo": %d,
                  "SortCondition": "%s"
                }
                """.formatted(nameField, categoryCode, pageNo, sortCondition);

        return restClient.post()
                .uri("/markets/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * GET {@code /markets/items/{id}} — item detail whose {@code Stats[]} carries daily
     * {@code {Date, AvgPrice, TradeCount}} (the source proving avg_price/trade_count availability).
     */
    public ResponseEntity<String> getItemDetail(long itemId) {
        return restClient.get()
                .uri("/markets/items/{id}", itemId)
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * GET {@code /news/events} — currently-running in-game events. Public metadata only
     * (제목/링크/시작·종료일/썸네일/보상일 per portal docs); no price or key data. The Phase 17.2
     * spike (D-05) reads this to lock the EventDTO field mapping before 17.2-02 implements the
     * parser. Same request shape as {@link #getMarketOptions()} — read-only GET, no body.
     * Returns status + headers + raw body; no persistence.
     */
    public ResponseEntity<String> getNewsEvents() {
        return restClient.get()
                .uri("/news/events")
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * GET {@code /news/notices} — official notices/announcements. Public metadata only
     * (제목/날짜/링크/타입 per portal docs); no price or key data. The Phase 17.2 spike (D-05) reads
     * this to lock the NoticeDTO field mapping before 17.2-02. Read-only GET, no body.
     * Returns status + headers + raw body; no persistence.
     */
    public ResponseEntity<String> getNewsNotices() {
        return restClient.get()
                .uri("/news/notices")
                .retrieve()
                .toEntity(String.class);
    }
}
