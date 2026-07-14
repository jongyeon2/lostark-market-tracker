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
@Disabled("Manual API verification spike (Task 0 + Phase 12) — run locally with LOSTARK_API_KEY set; never in CI")
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

        // 2b) 딜러/서포터 균형 각인서 (D-02) — query each curated engraving by name within 40000 so the
        //     유물-grade Id/Icon can be locked. ItemName is a substring filter; pick the Grade=유물 row
        //     from each response when building the findings table.
        for (String engraving : ENGRAVING_NAME_CANDIDATES) {
            ResponseEntity<String> named = client.searchMarketItems(ENGRAVING_CATEGORY, engraving);
            System.out.println("=== SPIKE engraving name='" + engraving + "' STATUS = " + named.getStatusCode());
            printItemFields("engraving/" + engraving, named.getBody());
            assertThat(named.getStatusCode().is2xxSuccessful()).isTrue();
        }

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

    /**
     * Phase 17.1 spike (D-02/D-03/D-04): live-measure the 10 NEW curation items — 8 유물 각인서
     * (딜러 3 + 서포터 5) and 2 재련 재료 (운명의 파괴석/수호석) — so 17.1-SPIKE-FINDINGS.md can lock their real
     * Id/Icon/CategoryCode before 17.1-02 transcribes them into WatchlistSeeder. Prints ONLY public
     * metadata (Id/Name/Grade/Icon) — no price fields — so no 가격原文 lands in the console dump. 유물
     * 각인서 share the use_9_25 glyph (Phase 14 D-06) → identify by label, not icon.
     */
    @Test
    void captureNewCurationItems() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 17.1 spike");

        // 1) 유물 각인서 (40000): query each NEW engraving by ItemName; pick the Grade=유물 row when building
        //    the findings table. ItemName is a substring filter.
        for (String engraving : NEW_ENGRAVING_NAME_CANDIDATES) {
            try {
                ResponseEntity<String> named = client.searchMarketItems(ENGRAVING_CATEGORY, engraving);
                System.out.println("=== SPIKE 17.1 engraving name='" + engraving + "' STATUS = " + named.getStatusCode());
                printItemFields("engraving/" + engraving, named.getBody());
                printIconDistinctness("engraving/" + engraving, named.getBody());
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                System.out.println("=== SPIKE 17.1 engraving name='" + engraving + "' HTTP " + e.getStatusCode() + " (skipped)");
            }
        }

        // 2) /markets/options — read the 재련 재료 leaf CategoryCode for 운명의 파괴석/수호석 결정 (≠ 50010
        //    융화재료). Category tree only (no price fields).
        ResponseEntity<String> options = client.getMarketOptions();
        System.out.println("=== SPIKE 17.1 options STATUS = " + options.getStatusCode());
        System.out.println("=== SPIKE 17.1 options BODY   = " + options.getBody());
        assertThat(options.getStatusCode().is2xxSuccessful()).isTrue();

        // 3) 운명의 파괴석/수호석 결정 (재련 재료): search each NEW material across candidate leaf codes so the
        //    real CategoryCode (≠ 50010) surfaces. printItemFields shows which category returns them.
        for (int categoryCode : NEW_MATERIAL_CATEGORY_CANDIDATES) {
            for (String material : NEW_MATERIAL_NAME_CANDIDATES) {
                try {
                    ResponseEntity<String> mat = client.searchMarketItems(categoryCode, material);
                    System.out.println("=== SPIKE 17.1 material cat=" + categoryCode + " name='" + material
                            + "' STATUS = " + mat.getStatusCode());
                    printItemFields("material/" + categoryCode + "/" + material, mat.getBody());
                } catch (org.springframework.web.client.HttpClientErrorException e) {
                    System.out.println("=== SPIKE 17.1 material cat=" + categoryCode + " name='" + material
                            + "' HTTP " + e.getStatusCode() + " (skipped)");
                }
            }
        }
    }

    /**
     * Phase 17.2 spike (D-05): live-measure the real field names of {@code /news/events} and
     * {@code /news/notices} ONCE so 17.2-NEWS-SPIKE-FINDINGS.md can lock the EventDTO/NoticeDTO
     * mapping before 17.2-02 writes the parser. News payloads are fully public metadata — titles,
     * links, dates, event periods — with no price or key data, so the raw body is safe to print;
     * {@link #printTopLevelFields} additionally lists the distinct field keys of the first array
     * element for a clean lock table. Two GET requests only (6h-poll parity, no market budget impact).
     */
    @Test
    void captureNewsFields() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 17.2 news spike");

        // 1) /news/events — currently-running events. Lock EventDTO (title/link/startDate/endDate[/thumbnail]).
        ResponseEntity<String> events = client.getNewsEvents();
        System.out.println("=== SPIKE 17.2 /news/events STATUS = " + events.getStatusCode());
        System.out.println("=== SPIKE 17.2 /news/events BODY   = " + events.getBody());
        printTopLevelFields("events", events.getBody());
        assertThat(events.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(events.getBody()).isNotBlank();

        // 2) /news/notices — official notices. Lock NoticeDTO (title/link/date/type).
        ResponseEntity<String> notices = client.getNewsNotices();
        System.out.println("=== SPIKE 17.2 /news/notices STATUS = " + notices.getStatusCode());
        System.out.println("=== SPIKE 17.2 /news/notices BODY   = " + notices.getBody());
        printTopLevelFields("notices", notices.getBody());
        assertThat(notices.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(notices.getBody()).isNotBlank();
    }

    /**
     * Phase 21 spike (MKT-01): live-measure the 스펙업 재련 재료 catalog on the 거래소(MARKETS) so
     * 21-SPIKE-FINDINGS.md can lock the curated set (Id/Name/Grade/Icon/CategoryCode) before Phase 22
     * transcribes it into WatchlistSeeder. Also probes whether 아크그리드 젬 live on MARKETS(230000)
     * or must come from the 경매장(AUCTIONS) API (Phase 24). Prints ONLY public metadata
     * (Id/Name/Grade/Icon) — no price fields — so no 가격原文 lands in the console dump.
     */
    @Test
    void captureHoningMaterialCatalog() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 21 spike");

        // 1) /markets/options — full category tree so every 재련 재료 leaf code surfaces
        //    (재련재료/추가재료/기타재료/무기진화/아크그리드재료 + any 상급재련 leaf).
        ResponseEntity<String> options = client.getMarketOptions();
        System.out.println("=== SPIKE 21 options STATUS = " + options.getStatusCode());
        System.out.println("=== SPIKE 21 options BODY   = " + options.getBody());
        assertThat(options.getStatusCode().is2xxSuccessful()).isTrue();

        // 2) 스펙업 재련 재료 — search each candidate name across the 강화재료 leaf codes so the real
        //    CategoryCode surfaces (파괴석/수호석/돌파석/파편/융화재료/숨결/야금술·재봉술 상급재련).
        for (int categoryCode : HONING_CATEGORY_CANDIDATES) {
            for (String material : HONING_MATERIAL_NAME_CANDIDATES) {
                try {
                    ResponseEntity<String> mat = client.searchMarketItems(categoryCode, material);
                    System.out.println("=== SPIKE 21 material cat=" + categoryCode + " name='" + material
                            + "' STATUS = " + mat.getStatusCode());
                    printItemFields("material/" + categoryCode + "/" + material, mat.getBody());
                } catch (org.springframework.web.client.HttpClientErrorException e) {
                    System.out.println("=== SPIKE 21 material cat=" + categoryCode + " name='" + material
                            + "' HTTP " + e.getStatusCode() + " (skipped)");
                }
            }
        }

        // 3) 아크그리드 젬 probe — search '젬' in 아크그리드재료(230000). 0 rows => 젬은 경매장(AUCTIONS,
        //    Phase 24)에 있고 MARKETS엔 없음을 의미. Metadata only.
        try {
            ResponseEntity<String> gem = client.searchMarketItems(ARKGRID_CATEGORY, "젬");
            System.out.println("=== SPIKE 21 arkgrid gem cat=" + ARKGRID_CATEGORY + " name='젬' STATUS = " + gem.getStatusCode());
            printItemFields("arkgrid-gem/" + ARKGRID_CATEGORY, gem.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("=== SPIKE 21 arkgrid gem HTTP " + e.getStatusCode() + " (skipped)");
        }
    }

    /**
     * Phase 21b (MKT-01): enumerate the grade tiers of the 6 아크그리드 젬 by ItemName so the ratified
     * curation can lock the chosen grade's Id (the 젬-only search returns 고급/희귀 first, price ASC —
     * higher grades need a per-name query). Metadata only (Id/Name/Grade/Icon), no prices.
     */
    @Test
    void captureArkgridGemGrades() {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 21b spike");
        for (String gem : ARKGRID_GEM_NAMES) {
            try {
                ResponseEntity<String> g = client.searchMarketItems(ARKGRID_CATEGORY, gem);
                System.out.println("=== SPIKE 21b gem name='" + gem + "' STATUS = " + g.getStatusCode());
                printItemFields("gem/" + gem, g.getBody());
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                System.out.println("=== SPIKE 21b gem name='" + gem + "' HTTP " + e.getStatusCode() + " (skipped)");
            }
        }
    }

    /** Phase 21: 강화 재료 leaf CategoryCodes to probe for 스펙업 재련 재료. */
    private static final int[] HONING_CATEGORY_CANDIDATES = {50010, 50020, 51000};

    /** 아크 그리드 재료 leaf — probe whether 젬 live here (MARKETS) vs 경매장(AUCTIONS). */
    private static final int ARKGRID_CATEGORY = 230000;

    /** Phase 21b: 6 아크그리드 젬 (질서 3 + 혼돈 3) queried by ItemName to enumerate grade tiers. */
    private static final java.util.List<String> ARKGRID_GEM_NAMES = java.util.List.of(
            "질서의 젬 : 안정", "질서의 젬 : 견고", "질서의 젬 : 불변",
            "혼돈의 젬 : 침식", "혼돈의 젬 : 왜곡", "혼돈의 젬 : 붕괴");

    /** Phase 21 스펙업 재련 재료 candidate names (substring ItemName filter). */
    private static final java.util.List<String> HONING_MATERIAL_NAME_CANDIDATES = java.util.List.of(
            "운명의 파괴석", "운명의 수호석", "운명의 돌파석", "운명의 파편",
            "아비도스 융화 재료", "상급 아비도스 융화 재료", "오레하 융화 재료",
            "용암의 숨결", "빙하의 숨결", "야금술", "재봉술");

    /** 유물 각인서 leaf CategoryCode (community-confirmed; re-verified from the options dump). */
    private static final int ENGRAVING_CATEGORY = 40000;

    /** 융화재료 leaf CategoryCode — confirmed empirically from the /markets/options dump (재련 재료). */
    private static final int[] MATERIAL_CATEGORY_CANDIDATES = {50010};

    /** D-01 융화재료 4종 candidate names queried by ItemName filter. */
    private static final java.util.List<String> MATERIAL_NAME_CANDIDATES = java.util.List.of(
            "상급 오레하 융화 재료", "최상급 오레하 융화 재료", "아비도스 융화 재료", "상급 아비도스 융화 재료");

    /** D-02 딜러/서포터 균형 각인서 큐레이션 (딜러 9 + 서포터 4) queried by ItemName within 40000. */
    private static final java.util.List<String> ENGRAVING_NAME_CANDIDATES = java.util.List.of(
            "원한", "예리한 둔기", "저주받은 인형", "아드레날린", "정밀 단도",
            "타격의 대가", "기습의 대가", "돌격대장", "결투의 대가",
            "각성", "만개", "전문의", "구원");

    /**
     * Phase 17.1 D-01/D-02 신규 각인 8종 (딜러 3 + 서포터 5) queried by ItemName within 40000.
     * "중갑" is a substring for the "중갑 착용" 각인서 (the "중갑착용" spelling returns 0 rows).
     */
    private static final java.util.List<String> NEW_ENGRAVING_NAME_CANDIDATES = java.util.List.of(
            "질량 증가", "슈퍼 차지", "바리케이드",
            "구슬동자", "마나의 흐름", "폭발물 전문가", "분쇄의 주먹", "중갑");

    /** Phase 17.1 D-01 신규 재련 재료 2종 (운명의 파괴석/수호석 결정) queried by ItemName substring. */
    private static final java.util.List<String> NEW_MATERIAL_NAME_CANDIDATES = java.util.List.of(
            "운명의 파괴석", "운명의 수호석");

    /** 강화 재료 leaf CategoryCodes to probe: 50010 재련 재료 (운명 결정 live here) + 50020 재련 추가 재료. */
    private static final int[] NEW_MATERIAL_CATEGORY_CANDIDATES = {50010, 50020};

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

    /**
     * Print the distinct top-level field names of the first JSON object in a (news) array body so
     * the 17.2 findings lock table can be hand-built. News bodies are public metadata only — this
     * lists field KEYS (e.g. Title/Link/StartDate/EndDate), never price or key data.
     */
    private static void printTopLevelFields(String label, String body) {
        if (body == null || body.isBlank()) {
            System.out.println("--- " + label + ": <empty body>");
            return;
        }
        // Grab the first {...} object (the array's first element) and list its top-level keys.
        var obj = java.util.regex.Pattern.compile("\\{(.*?)\\}", java.util.regex.Pattern.DOTALL).matcher(body);
        if (!obj.find()) {
            System.out.println("--- " + label + ": no JSON object found in body");
            return;
        }
        var key = java.util.regex.Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:").matcher(obj.group(1));
        var keys = new java.util.LinkedHashSet<String>();
        while (key.find()) {
            keys.add(key.group(1));
        }
        System.out.println("--- " + label + " fields = " + keys);
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
