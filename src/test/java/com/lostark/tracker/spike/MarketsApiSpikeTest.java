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

        // 1) Search a leaf category — confirms request shape, item fields, and rate-limit headers.
        ResponseEntity<String> search = client.searchMarketItems("");
        System.out.println("=== SPIKE search STATUS    = " + search.getStatusCode());
        System.out.println("=== SPIKE rate-limit limit = " + search.getHeaders().getFirst("x-ratelimit-limit")
                + " remaining=" + search.getHeaders().getFirst("x-ratelimit-remaining"));
        System.out.println("=== SPIKE search BODY      = " + search.getBody());
        assertThat(search.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(search.getBody()).isNotBlank();

        // 2) Detail of the first item — its Stats[] carries daily {Date, AvgPrice, TradeCount} (D-06).
        var matcher = java.util.regex.Pattern.compile("\"Id\":(\\d+)").matcher(search.getBody());
        if (matcher.find()) {
            long itemId = Long.parseLong(matcher.group(1));
            ResponseEntity<String> detail = client.getItemDetail(itemId);
            System.out.println("=== SPIKE detail id=" + itemId + " STATUS = " + detail.getStatusCode());
            System.out.println("=== SPIKE detail BODY = " + detail.getBody());
            assertThat(detail.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    /**
     * Phase 12 spike (D-09): confirm the 유물 각인서 (40000) and 융화재료 leaf CategoryCodes, the
     * icon URL field, and whether engraving icons are distinct (Pitfall 7) — the inputs the curation
     * lock in 12-SPIKE-FINDINGS.md depends on. Prints only public metadata (Id/Name/Grade/Icon);
     * prices are intentionally omitted from output so no price原文 lands in a captured console dump.
     */
    @Test
    void captureEngravingAndMaterialCategories() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 12 spike");

        // 1) /markets/options — dump the category tree. Read the 유물 각인서 (40000) and 융화재료
        //    leaf codes from Categories[].Subs[].Code (a parent code returns TotalCount:0).
        ResponseEntity<String> options = client.getMarketOptions();
        System.out.println("=== SPIKE options STATUS = " + options.getStatusCode());
        System.out.println("=== SPIKE options BODY   = " + options.getBody());
        assertThat(options.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(options.getBody()).isNotBlank();

        // 2) 유물 각인서 (CategoryCode 40000, community-confirmed; re-verify from the options dump).
        //    Print Id/Name/Grade/Icon per item and report whether the engraving icons are distinct
        //    or identical — Phase 14 falls back to label-병기 when identical (D-06; Pitfall 7).
        ResponseEntity<String> engravings = client.searchMarketItems(ENGRAVING_CATEGORY, "");
        System.out.println("=== SPIKE engraving(" + ENGRAVING_CATEGORY + ") STATUS = " + engravings.getStatusCode());
        printItemFields("engraving", engravings.getBody());
        printIconDistinctness("engraving", engravings.getBody());
        assertThat(engravings.getStatusCode().is2xxSuccessful()).isTrue();

        // 3) 융화재료 (D-01: 상급/최상급 오레하 + 아비도스 + 상급 아비도스). The leaf CategoryCode is
        //    read from the options dump above; until confirmed, query candidate names across the
        //    강화재료 leaf candidates so the developer can see which category actually returns them.
        for (int categoryCode : MATERIAL_CATEGORY_CANDIDATES) {
            for (String material : MATERIAL_NAME_CANDIDATES) {
                ResponseEntity<String> mat = client.searchMarketItems(categoryCode, material);
                System.out.println("=== SPIKE material cat=" + categoryCode + " name='" + material
                        + "' STATUS = " + mat.getStatusCode());
                printItemFields("material/" + categoryCode + "/" + material, mat.getBody());
                assertThat(mat.getStatusCode().is2xxSuccessful()).isTrue();
            }
        }
    }

    /** 유물 각인서 leaf CategoryCode (community-confirmed; re-verified from the options dump). */
    private static final int ENGRAVING_CATEGORY = 40000;

    /** 융화재료 leaf CategoryCode candidates — confirm the real one from the /markets/options dump. */
    private static final int[] MATERIAL_CATEGORY_CANDIDATES = {50010, 50020};

    /** D-01 융화재료 4종 candidate names queried by ItemName filter. */
    private static final java.util.List<String> MATERIAL_NAME_CANDIDATES = java.util.List.of(
            "상급 오레하 융화 재료", "최상급 오레하 융화 재료", "아비도스 융화 재료", "상급 아비도스 융화 재료");

    /** Print Id/Name/Grade/Icon per item (no price fields) so the findings table can be hand-built. */
    private static void printItemFields(String label, String body) {
        if (body == null || body.isBlank()) {
            System.out.println("--- " + label + ": <empty body>");
            return;
        }
        var item = java.util.regex.Pattern.compile(
                "\"Id\":(\\d+).*?\"Name\":\"([^\"]*)\".*?\"Grade\":\"([^\"]*)\".*?\"Icon\":\"([^\"]*)\"",
                java.util.regex.Pattern.DOTALL).matcher(body);
        int count = 0;
        while (item.find()) {
            System.out.printf("--- %s | Id=%s | Name=%s | Grade=%s | Icon=%s%n",
                    label, item.group(1), item.group(2), item.group(3), item.group(4));
            count++;
        }
        if (count == 0) {
            System.out.println("--- " + label + ": no Id/Name/Grade/Icon tuples matched (check category)");
        }
    }

    /** Report whether the Icon URLs in a response are all distinct or contain duplicates (Pitfall 7). */
    private static void printIconDistinctness(String label, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        var icon = java.util.regex.Pattern.compile("\"Icon\":\"([^\"]*)\"").matcher(body);
        var all = new java.util.ArrayList<String>();
        while (icon.find()) {
            all.add(icon.group(1));
        }
        long distinct = all.stream().distinct().count();
        System.out.printf("=== SPIKE %s icons: total=%d distinct=%d -> %s%n",
                label, all.size(), distinct, distinct == all.size() ? "ALL DISTINCT" : "HAS DUPLICATES");
    }
}
