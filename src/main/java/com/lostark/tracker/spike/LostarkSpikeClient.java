package com.lostark.tracker.spike;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Read-only spike client (D-03 RestClient) for the Lostark Open API. Returns the FULL response
 * (status + headers + raw body) from each endpoint so a spike can inspect the actual fields and
 * rate-limit headers before any data model is locked. No persistence, no parsing.
 *
 * <p>Grown one endpoint at a time by successive spikes, each answering a question that had to be
 * measured rather than assumed: {@code markets/*} (Task 0 fields, Phase 12 categories, Phase 17.4
 * daily Stats), {@code news/*} (Phase 17.2 DTO mapping), and {@code auctions/*} (Phase 24 — 보석
 * catalog and what "현재가" means on a bid-based market).
 *
 * <p>Active only under the {@code spike} profile, exercised by {@code @Disabled} spike tests
 * ({@link MarketsApiSpikeTest} and friends); never wired in normal runs or CI.
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
     * GET {@code /auctions/options} — the auction house's category tree + the search parameter set
     * (grades, tiers, quality, sort conditions). The Phase 24 spike reads this to find the 보석 leaf
     * {@code CategoryCode} EMPIRICALLY rather than trusting a remembered constant, and to see which
     * request fields {@link #searchAuctionItems} may legitimately send.
     *
     * <p>The auction house is a DIFFERENT API surface from {@code /markets/*} — this project had
     * never called it before Phase 24 (zero {@code auctions} references). Read-only GET, no body;
     * returns status + headers + raw body so the spike can read rate-limit headers too. No persistence.
     */
    public ResponseEntity<String> getAuctionOptions() {
        return restClient.get()
                .uri("/auctions/options")
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * POST {@code /auctions/items} — auction search. Deliberately NOT reusing
     * {@link #searchMarketItems}: the endpoint, the request body schema, and the response shape all
     * differ from the 거래소. An auction listing is a bid, not a fixed ask, so it carries an
     * {@code AuctionInfo} block instead of 거래소's flat {@code CurrentMinPrice} — which is exactly
     * why Phase 24 must measure what "현재가" even means here before anything is built on top.
     *
     * <p>Null/blank parameters are OMITTED from the body rather than sent empty: an empty
     * {@code "ItemName": ""} risks being read as a filter for the empty string (the
     * {@link #searchMarketItems} {@code nameField} precedent). Returns status + headers + raw body.
     *
     * @param categoryCode leaf category read from {@link #getAuctionOptions()} — never a guessed constant
     * @param itemName     optional name filter; omitted when null/blank
     * @param itemTier     optional tier filter (4 = 티어4); omitted when null
     * @param pageNo       page index — the spike records whether this API is 0-based or 1-based
     * @param sort         sort field, e.g. BUY_PRICE / BIDSTART_PRICE — valid values come from options
     * @param sortCondition ASC or DESC
     */
    public ResponseEntity<String> searchAuctionItems(int categoryCode, String itemName, Integer itemTier,
                                                     int pageNo, String sort, String sortCondition) {
        StringBuilder fields = new StringBuilder();
        if (itemName != null && !itemName.isBlank()) {
            fields.append("\"ItemName\": \"%s\",%n".formatted(itemName));
        }
        if (itemTier != null) {
            fields.append("\"ItemTier\": %d,%n".formatted(itemTier));
        }
        String requestBody = """
                {
                  %s"CategoryCode": %d,
                  "Sort": "%s",
                  "PageNo": %d,
                  "SortCondition": "%s"
                }
                """.formatted(fields, categoryCode, sort, pageNo, sortCondition);

        return restClient.post()
                .uri("/auctions/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
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
