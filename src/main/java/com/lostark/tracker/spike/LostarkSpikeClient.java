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
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("accept", "application/json")
                // Auth header per portal docs: "Authorization: bearer {token}". Key from env only.
                .defaultHeader("authorization", "bearer " + apiKey)
                .build();
    }

    /**
     * Single POST to {@code /markets/items} searching by item name. CategoryCode/Sort are a
     * best-guess starting point per the portal docs; the spike confirms the real request shape.
     */
    public ResponseEntity<String> searchMarketItems(String itemName) {
        String requestBody = """
                {
                  "Sort": "GRADE",
                  "CategoryCode": 50000,
                  "ItemName": "%s",
                  "PageNo": 0,
                  "SortCondition": "ASC"
                }
                """.formatted(itemName);

        return restClient.post()
                .uri("/markets/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toEntity(String.class);
    }
}
