package com.lostark.tracker.spike;

import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 0 API verification spike (D-04). Calls the real Lostark {@code markets/items} endpoint
 * ONCE and prints the raw response so the developer can capture fields, the item-matching
 * identifier, and rate-limit headers into TASK0-FINDINGS.md.
 *
 * <p>{@code @Disabled} by default so {@code ./gradlew test} never runs it and CI never makes a
 * network call or needs a key. To run locally: set {@code LOSTARK_API_KEY}, ensure
 * docker-compose Postgres/Redis are up, temporarily remove {@code @Disabled}, and run
 * {@code ./gradlew test --tests MarketsApiSpikeTest}. Extends the shared container base so the
 * full context boots; if the key is absent the test self-skips via {@code assumeTrue}.
 */
@Disabled("Task 0 manual API verification spike — run locally with LOSTARK_API_KEY set; never in CI")
@SpringBootTest
@ActiveProfiles("spike")
class MarketsApiSpikeTest extends PostgresRedisContainers {

    @Autowired
    private LostarkSpikeClient client;

    @Value("${lostark.api.key:}")
    private String apiKey;

    @Test
    void captureRealMarketsItemsResponse() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Task 0 spike");

        ResponseEntity<String> response = client.searchMarketItems("아비도스 융화 재료");

        System.out.println("=== Task 0 spike :: STATUS  = " + response.getStatusCode());
        System.out.println("=== Task 0 spike :: HEADERS = " + response.getHeaders());
        System.out.println("=== Task 0 spike :: BODY    = " + response.getBody());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotBlank();
    }
}
