package com.lostark.tracker.gem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * 경매장(AUCTIONS) read client for 보석 현재가 (GEM-02). A SEPARATE surface from
 * {@code LostarkApiClient}/{@code /markets/*}: different endpoint, different request body, different
 * response shape (Phase 24 spike). Reusing the 거래소 client here would have been wrong.
 *
 * <p><b>레이트리밋 경고(Phase 24 §H2):</b> this endpoint shares the SAME per-key budget as the 10분
 * 수집 tick — a 경매장 call decrements the very counter the collector spends. That is why every caller
 * must go through {@link GemService}'s cache and never call this per page view. Core Value(수집 신뢰성)
 * outranks this feature.
 */
@Component
public class LostarkAuctionClient {

    /** 보석 leaf, read from the live {@code /auctions/options} tree (Phase 24 §H3) — not a guess. */
    private static final int GEM_CATEGORY_CODE = 210000;
    private static final int TIER_4 = 4;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LostarkAuctionClient(RestClient.Builder lostarkRestClientBuilder,
                                @Value("${lostark.api.base-url}") String baseUrl,
                                @Value("${lostark.api.key:}") String apiKey,
                                ObjectMapper objectMapper) {
        // Same normalization as LostarkApiClient/LostarkNewsClient: drop an optional "bearer " prefix +
        // strip ALL whitespace. JWTs contain none, but a wrapped copy/paste injects them -> 401.
        String token = apiKey == null ? "" :
                apiKey.strip().replaceFirst("(?i)^bearer\\s+", "").replaceAll("\\s", "");
        // clone() so setting base-url here never leaks onto the shared collection-client builder.
        this.restClient = lostarkRestClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("accept", "application/json")
                .defaultHeader("authorization", "bearer " + token)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * The 최저 즉시구매가 for one gem, or empty when nothing is instantly buyable.
     *
     * <p><b>Why {@code BuyPrice} is the price (Phase 24 lock):</b> an auction listing exposes four
     * numbers and only this one answers "지금 이거 사려면 얼마?". {@code StartPrice} is a bid floor and
     * real listings set it to 1골드 as bait; {@code BidPrice} is 0 whenever nobody has bid;
     * {@code BidStartPrice} is the NEXT minimum bid, not a price you can pay today. {@code BuyPrice}
     * corresponds to 거래소's {@code CurrentMinPrice} in meaning, which is what lets the UI honestly
     * reuse 최저가 vocabulary.
     *
     * <p><b>Why ASC and never DESC:</b> {@code BuyPrice} is nullable (bid-only listings). ASC sorts
     * those nulls to the BACK, so {@code Items[0]} is always a real lowest buyout; DESC brings them to
     * the FRONT and the page-1 order stops being monotonic (measured in the spike). Sorting DESC to
     * find a maximum would return nulls.
     *
     * @return the lowest buyout in gold, or empty when there are no listings or none has a buyout
     * @throws org.springframework.web.client.RestClientException on HTTP/transport failure — the caller
     *         isolates it to a single row rather than failing the whole page
     */
    public Optional<Long> findLowestBuyPrice(String itemName) {
        String requestBody = """
                {
                  "ItemName": "%s",
                  "CategoryCode": %d,
                  "ItemTier": %d,
                  "Sort": "BUY_PRICE",
                  "PageNo": 1,
                  "SortCondition": "ASC"
                }
                """.formatted(itemName, GEM_CATEGORY_CODE, TIER_4);

        String body = restClient.post()
                .uri("/auctions/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return firstBuyPrice(body);
    }

    /**
     * Read {@code Items[0].AuctionInfo.BuyPrice}. Any shape surprise (empty Items, absent AuctionInfo,
     * explicit null BuyPrice) yields empty rather than a crash — the page then says 즉시구매 매물 없음
     * instead of inventing a number.
     */
    private Optional<Long> firstBuyPrice(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode items = objectMapper.readTree(body).path("Items");
            if (!items.isArray() || items.isEmpty()) {
                return Optional.empty();
            }
            JsonNode buyPrice = items.get(0).path("AuctionInfo").path("BuyPrice");
            if (!buyPrice.isNumber()) {
                return Optional.empty();
            }
            return Optional.of(buyPrice.asLong());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
