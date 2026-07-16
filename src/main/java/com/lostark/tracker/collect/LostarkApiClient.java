package com.lostark.tracker.collect;

import com.lostark.tracker.collect.dto.ItemDetailResponse;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.collect.dto.MarketStat;
import com.lostark.tracker.collect.error.AuthApiException;
import com.lostark.tracker.collect.error.NonRetryableApiException;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.collect.error.TransientApiException;
import com.lostark.tracker.market.dto.MarketOptionsResponse;
import com.lostark.tracker.market.dto.MarketSearchResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Productized markets client (replaces the Task-0 spike for real collection). It:
 * <ul>
 *   <li>normalizes the configured key (drop a {@code bearer } prefix + strip ALL whitespace —
 *       the Task-0 401 gotcha) and sends {@code Authorization: bearer <token>};</li>
 *   <li>issues {@code POST /markets/items} (CategoryCode + ItemName) per item (D-05);</li>
 *   <li>maps every HTTP outcome to a distinct typed exception (COLL-04 classification):
 *       401/403 -&gt; {@link AuthApiException}, 429 -&gt; {@link RateLimitedApiException}
 *       (with Retry-After), 5xx / I-O timeout -&gt; {@link TransientApiException},
 *       other 4xx -&gt; {@link NonRetryableApiException}.</li>
 * </ul>
 * The key is never logged or placed in any exception message (D-08). Per-call connect/read
 * timeouts are configured on the injected builder by {@link ApiClientConfig} (D-07).
 */
public class LostarkApiClient {

    private final RestClient restClient;

    public LostarkApiClient(RestClient.Builder builder, String baseUrl, String apiKey) {
        // Normalize: drop optional "bearer "/"Bearer " prefix, then strip ALL whitespace.
        // JWTs contain no spaces; copy/paste from a wrapped display injects them -> 401 (Task 0).
        String token = apiKey == null ? "" :
                apiKey.strip().replaceFirst("(?i)^bearer\\s+", "").replaceAll("\\s", "");
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("accept", "application/json")
                .defaultHeader("authorization", "bearer " + token)
                .build();
    }

    /**
     * List the markets for {@code categoryCode} (a leaf code) filtered by {@code itemName}.
     * Returns the parsed envelope; the caller matches the desired item by Id (D-05).
     *
     * @throws AuthApiException 401/403 (fatal — do not retry)
     * @throws RateLimitedApiException 429 (carries Retry-After when present)
     * @throws TransientApiException 5xx or an I/O / read timeout (retryable)
     * @throws NonRetryableApiException any other 4xx (skip-light)
     */
    public MarketItemsResponse searchMarketItems(String categoryCode, String itemName) {
        String requestBody = """
                {
                  "Sort": "CURRENT_MIN_PRICE",
                  "CategoryCode": %s,
                  "ItemName": "%s",
                  "PageNo": 1,
                  "SortCondition": "ASC"
                }
                """.formatted(categoryCode, itemName == null ? "" : itemName.replace("\"", "\\\""));

        try {
            return restClient.post()
                    .uri("/markets/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(s -> s.value() == 401 || s.value() == 403,
                            (req, res) -> {
                                throw new AuthApiException("Lostark auth failed (HTTP " + res.getStatusCode().value() + ")");
                            })
                    .onStatus(s -> s.value() == 429,
                            (req, res) -> {
                                Integer retryAfter = parseRetryAfter(res.getHeaders().getFirst("Retry-After"));
                                throw new RateLimitedApiException("Lostark rate limited (HTTP 429)", retryAfter);
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                throw new TransientApiException("Lostark server error (HTTP " + res.getStatusCode().value() + ")");
                            })
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (req, res) -> {
                                throw new NonRetryableApiException("Lostark client error (HTTP " + res.getStatusCode().value() + ")");
                            })
                    .body(MarketItemsResponse.class);
        } catch (ResourceAccessException e) {
            // Connect/read timeout or other I/O — transient (D-10). No key in the message.
            throw new TransientApiException("Lostark I/O or timeout", e);
        }
    }

    /**
     * Fetch one item's detail ({@code Stats[]} = last ~14 days of daily traded averages) for the
     * Phase 17.4 backfill (D-01 source ②). A read-only GET that reuses {@link #searchMarketItems}'s
     * exact error taxonomy — 401/403 fatal auth, 429 rate-limited (with Retry-After), 5xx / I-O
     * transient, other 4xx non-retryable. The key is never logged or placed in a message.
     *
     * <p>The endpoint returns a TOP-LEVEL JSON ARRAY of item-detail objects (each with {@code Stats}),
     * not a bare object — verified against the live API. It can hold MORE THAN ONE element for a single
     * id: engraving books return a bound "trade-once" variant with all-zero {@code Stats} AND the freely
     * traded market variant with the real ~14-day series, and {@code details[0]} is the bound one. So we
     * do not blindly take the first element — {@link #selectTradedDetail} keeps the element whose Stats
     * carry the most actual trade activity (materials return a single element, so it is a no-op there).
     *
     * @throws AuthApiException 401/403 (fatal — do not retry)
     * @throws RateLimitedApiException 429 (carries Retry-After when present)
     * @throws TransientApiException 5xx or an I/O / read timeout (retryable)
     * @throws NonRetryableApiException any other 4xx (skip-light)
     */
    public ItemDetailResponse getItemDetail(long itemId) {
        try {
            ItemDetailResponse[] details = restClient.get()
                    .uri("/markets/items/{id}", itemId)
                    .retrieve()
                    .onStatus(s -> s.value() == 401 || s.value() == 403,
                            (req, res) -> {
                                throw new AuthApiException("Lostark auth failed (HTTP " + res.getStatusCode().value() + ")");
                            })
                    .onStatus(s -> s.value() == 429,
                            (req, res) -> {
                                Integer retryAfter = parseRetryAfter(res.getHeaders().getFirst("Retry-After"));
                                throw new RateLimitedApiException("Lostark rate limited (HTTP 429)", retryAfter);
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                throw new TransientApiException("Lostark server error (HTTP " + res.getStatusCode().value() + ")");
                            })
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (req, res) -> {
                                throw new NonRetryableApiException("Lostark client error (HTTP " + res.getStatusCode().value() + ")");
                            })
                    .body(ItemDetailResponse[].class);
            return selectTradedDetail(details);
        } catch (ResourceAccessException e) {
            // Connect/read timeout or other I/O — transient (D-10). No key in the message.
            throw new TransientApiException("Lostark I/O or timeout", e);
        }
    }

    /**
     * Search 거래소 for the market-search feature (아바타·모험의 서 실시간 조회) — a paged, sorted,
     * optionally class-filtered {@code POST /markets/items}. Unlike {@link #searchMarketItems} (which
     * hardcodes CURRENT_MIN_PRICE/ASC/PageNo1 for the collector and drops Grade/RecentPrice/Icon), this
     * passes every knob through and returns {@link MarketSearchResponse} with those fields kept.
     *
     * <p>The body is a {@link Map} serialized by RestClient's Jackson converter — NOT hand-built text —
     * so a 한글 {@code characterClass}/{@code itemName} is emitted as correct UTF-8 and JSON-escaped
     * safely (the text-block path's manual {@code \"} escaping does neither reliably). Blank
     * class/name fields are OMITTED, since an empty {@code CharacterClass} is not the same request as
     * no filter (모험의 서 has no class at all).
     *
     * <p>Caller (MarketSearchService) is responsible for whitelisting {@code sort}/{@code sortCondition}
     * BEFORE calling — the API silently ignores unknown values (returns 200 with default order), so an
     * unvalidated value would look like it worked while doing nothing.
     *
     * <p>Same error taxonomy as the collector paths (see {@link #applyErrorTaxonomy}).
     */
    public MarketSearchResponse searchMarket(String categoryCode, String characterClass, String itemName,
                                             int pageNo, String sort, String sortCondition) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Sort", sort);
        body.put("CategoryCode", Integer.parseInt(categoryCode));
        body.put("PageNo", pageNo);
        body.put("SortCondition", sortCondition);
        if (itemName != null && !itemName.isBlank()) {
            body.put("ItemName", itemName);
        }
        if (characterClass != null && !characterClass.isBlank()) {
            body.put("CharacterClass", characterClass);
        }
        try {
            return applyErrorTaxonomy(restClient.post()
                    .uri("/markets/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve())
                    .body(MarketSearchResponse.class);
        } catch (ResourceAccessException e) {
            throw new TransientApiException("Lostark I/O or timeout", e);
        }
    }

    /**
     * Fetch {@code GET /markets/options} for the 30 playable classes the avatar 직업 드롭다운 offers.
     * The list changes only when 로스트아크 ships a new class, so the caller caches it long-term.
     * Same error taxonomy as the other paths.
     */
    public MarketOptionsResponse getMarketOptions() {
        try {
            return applyErrorTaxonomy(restClient.get()
                    .uri("/markets/options")
                    .retrieve())
                    .body(MarketOptionsResponse.class);
        } catch (ResourceAccessException e) {
            throw new TransientApiException("Lostark I/O or timeout", e);
        }
    }

    /**
     * The shared HTTP-status → typed-exception mapping (COLL-04). Identical to the inline taxonomy in
     * {@link #searchMarketItems}/{@link #getItemDetail}; the market-search paths route through here so
     * the mapping lives in one place going forward. 401/403 → {@link AuthApiException}, 429 →
     * {@link RateLimitedApiException} (with Retry-After), 5xx / I-O → {@link TransientApiException},
     * other 4xx → {@link NonRetryableApiException}. No key/secret ever reaches a message (D-08).
     */
    private static RestClient.ResponseSpec applyErrorTaxonomy(RestClient.ResponseSpec spec) {
        return spec
                .onStatus(s -> s.value() == 401 || s.value() == 403,
                        (req, res) -> {
                            throw new AuthApiException("Lostark auth failed (HTTP " + res.getStatusCode().value() + ")");
                        })
                .onStatus(s -> s.value() == 429,
                        (req, res) -> {
                            Integer retryAfter = parseRetryAfter(res.getHeaders().getFirst("Retry-After"));
                            throw new RateLimitedApiException("Lostark rate limited (HTTP 429)", retryAfter);
                        })
                .onStatus(HttpStatusCode::is5xxServerError,
                        (req, res) -> {
                            throw new TransientApiException("Lostark server error (HTTP " + res.getStatusCode().value() + ")");
                        })
                .onStatus(HttpStatusCode::is4xxClientError,
                        (req, res) -> {
                            throw new NonRetryableApiException("Lostark client error (HTTP " + res.getStatusCode().value() + ")");
                        });
    }

    /**
     * Collapse the detail array to the element carrying the real market series. The endpoint can
     * return multiple elements for one id (engraving books: a bound trade-once variant with all-zero
     * Stats plus the freely traded variant with the real series — {@code details[0]} is the bound one).
     * We keep the element whose Stats hold the most total {@code TradeCount}, which is order- and
     * TradeRemainCount-independent. Materials return a single element (a no-op); an empty or all-zero
     * array yields the first element's (empty) Stats.
     */
    private static ItemDetailResponse selectTradedDetail(ItemDetailResponse[] details) {
        if (details == null || details.length == 0) {
            return new ItemDetailResponse(List.of());
        }
        ItemDetailResponse best = details[0];
        long bestTrades = totalTradeCount(best);
        for (ItemDetailResponse candidate : details) {
            long trades = totalTradeCount(candidate);
            if (trades > bestTrades) {
                best = candidate;
                bestTrades = trades;
            }
        }
        return best;
    }

    private static long totalTradeCount(ItemDetailResponse detail) {
        long sum = 0;
        for (MarketStat stat : detail.stats()) {
            if (stat.tradeCount() != null) {
                sum += stat.tradeCount();
            }
        }
        return sum;
    }

    private static Integer parseRetryAfter(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(headerValue.trim());
        } catch (NumberFormatException e) {
            // HTTP-date form of Retry-After is not honored in v1 (D-09 keeps it simple).
            return null;
        }
    }
}
