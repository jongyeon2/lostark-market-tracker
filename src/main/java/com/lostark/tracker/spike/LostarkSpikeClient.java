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
     * Single POST to {@code /markets/items}. CategoryCode 50010 is "재련 재료" (refining materials),
     * a leaf category confirmed during the Task 0 spike to return items; {@code ItemName} is added
     * only when provided (a blank name lists the category). Returns status + headers + raw body.
     */
    public ResponseEntity<String> searchMarketItems(String itemName) {
        String nameField = (itemName == null || itemName.isBlank())
                ? ""
                : "\"ItemName\": \"%s\",%n".formatted(itemName);
        String requestBody = """
                {
                  %s"CategoryCode": 50010,
                  "Sort": "CURRENT_MIN_PRICE",
                  "PageNo": 1,
                  "SortCondition": "ASC"
                }
                """.formatted(nameField);

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
}
