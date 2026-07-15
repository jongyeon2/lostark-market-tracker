package com.lostark.tracker.spike;

import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 24 spike (GEM-01): the auction house (AUCTIONS) API, which this project had NEVER called
 * before — every client here spoke only {@code /markets/*} and {@code /news/*}.
 *
 * <p>Answers the questions 24-01-PLAN lists as hypotheses (H1–H7), the load-bearing ones being:
 * <ul>
 *   <li><b>H2 rate-limit bucket</b> — does an auction call spend the SAME budget as 10분 수집?
 *       If it does, gem browsing can starve the collector, and that outranks the feature
 *       (Core Value: 수집 신뢰성).</li>
 *   <li><b>H6 "현재가"</b> — an auction listing is a bid, not a fixed ask, so 거래소's flat
 *       {@code CurrentMinPrice} has no obvious counterpart. Which field answers "이 보석 얼마야?"
 *       must be READ, not remembered.</li>
 * </ul>
 *
 * <p>Unlike {@link MarketsApiSpikeTest}'s helpers, this captures the RAW body: those helpers require
 * {@code "Id":(\d+)} to match, and if auction items carry no {@code Id} (H5) the regex would silently
 * yield zero rows — an absent field would masquerade as an empty catalog. A spike must not be able
 * to fail that quietly.
 *
 * <p>{@code @Disabled} by default so {@code ./gradlew test} never runs it and CI never makes a
 * network call or needs a key. To run locally: set {@code LOSTARK_API_KEY}, ensure Docker is up
 * (Testcontainers), temporarily remove {@code @Disabled}, and run
 * {@code ./gradlew test --tests AuctionsApiSpikeTest}. Self-skips via {@code assumeTrue} without a key.
 */
@Disabled("Manual API verification spike (Phase 24 GEM-01) — run locally with LOSTARK_API_KEY set; never in CI")
@SpringBootTest
@ActiveProfiles("spike")
class AuctionsApiSpikeTest extends PostgresRedisContainers {

    @Autowired
    private LostarkSpikeClient client;

    @Value("${lostark.api.key:}")
    private String apiKey;

    /** H3 — remembered as the 보석 leaf, treated as a GUESS until the options tree confirms it. */
    private static final int GEM_CATEGORY_HYPOTHESIS = 210000;

    /** A known-good 거래소 leaf, used only as the rate-limit bucket probe's first call (H2). */
    private static final int MARKETS_PROBE_CATEGORY = 50010;

    /**
     * (a)+(b) — rate-limit bucket sharing (H1/H2) and the auction options tree (H3/H4).
     *
     * <p>The bucket probe is the point of the ordering: call 거래소 first, read
     * {@code x-ratelimit-remaining}, then call 경매장 and read it again. A remaining that keeps
     * counting DOWN across the two means one shared per-key budget; a reset means separate buckets.
     * That single number decides whether GEM-02 can query on demand or must cache hard.
     */
    @Test
    void captureAuctionOptionsAndRateLimitBucket() throws IOException {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 24 spike");

        StringBuilder sb = new StringBuilder();

        // 1) 거래소 call first — the baseline for the shared-bucket comparison.
        ResponseEntity<String> marketProbe = client.searchMarketItems(MARKETS_PROBE_CATEGORY, "");
        sb.append("## H2 rate-limit bucket probe\n");
        appendRateLimit(sb, "1. MARKETS /markets/items", marketProbe);

        // 2) 경매장 call second — same key, different surface. Does remaining keep descending?
        ResponseEntity<String> options = client.getAuctionOptions();
        appendRateLimit(sb, "2. AUCTIONS /auctions/options", options);
        sb.append("\n=> shared bucket if #2 remaining == #1 remaining - 1; separate if it reset.\n\n");

        // 3) Raw options tree — the 보석 CategoryCode and the legal request fields are READ from here,
        //    never assumed. Written whole because we do not yet know its shape.
        sb.append("## H1/H3/H4 — GET /auctions/options\n");
        sb.append("STATUS=").append(options.getStatusCode()).append('\n');
        sb.append("BODY=").append(options.getBody()).append("\n\n");

        Path out = Path.of("build", "spike-auction-options.txt");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("=== SPIKE auctions/options wrote " + out.toAbsolutePath());

        assertThat(options.getStatusCode().is2xxSuccessful())
                .as("H1: the existing MARKETS key should authenticate against AUCTIONS too")
                .isTrue();
    }

    /**
     * (c)+(d) — the 티어4 보석 catalog (H5/H6/H7) and the display fields the findings must lock.
     *
     * <p>Scope is 사용자 확정: 티어4, levels 8·9·10 only. The level filter is NOT assumed to exist —
     * this probes an unfiltered tier-4 page first to see what the response actually offers, then
     * narrows by name/sort. Whatever narrowing works is what GEM-02 will have to use, so the file
     * records the attempt either way.
     */
    @Test
    void captureTier4GemCatalog() throws IOException {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 24 spike");

        StringBuilder sb = new StringBuilder();

        // 1) Unfiltered tier-4 page 1, both sort directions. The FULL body goes to file: field names
        //    (Id present? AuctionInfo shape? which price fields are null?) are the deliverable here.
        for (String sortCondition : List.of("ASC", "DESC")) {
            ResponseEntity<String> page = client.searchAuctionItems(
                    GEM_CATEGORY_HYPOTHESIS, null, 4, 1, "BUY_PRICE", sortCondition);
            sb.append("## H5/H6 — POST /auctions/items tier=4 sort=BUY_PRICE ")
                    .append(sortCondition).append(" page=1\n");
            appendRateLimit(sb, "rate-limit", page);
            sb.append("STATUS=").append(page.getStatusCode()).append('\n');
            sb.append("BODY=").append(page.getBody()).append("\n\n");
        }

        // 2) Name-narrowed probes. These names are HYPOTHESES (H7) — a zero-count row is a finding,
        //    not a bug, and gets recorded as "가설 → 실측" the way Phase 21 recorded 아크그리드젬.
        for (String gemName : GEM_NAME_HYPOTHESES) {
            ResponseEntity<String> named = client.searchAuctionItems(
                    GEM_CATEGORY_HYPOTHESIS, gemName, 4, 1, "BUY_PRICE", "ASC");
            sb.append("## H7 — ItemName=\"").append(gemName).append("\" tier=4\n");
            sb.append("STATUS=").append(named.getStatusCode()).append('\n');
            sb.append("BODY=").append(named.getBody()).append("\n\n");
        }

        Path out = Path.of("build", "spike-auction-gems.txt");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("=== SPIKE auction gems wrote " + out.toAbsolutePath());
    }

    /**
     * The 사용자-확정 scope, measured: 티어4 보석 levels 8·9·10 only.
     *
     * <p>The first run overturned the obvious approach — the response's {@code Level} field is 1640
     * for EVERY gem (that is 아이템 레벨, the equip requirement), while the gem's own level lives in
     * the {@code Name} ("8레벨 겁화의 보석"). There is no numeric level filter to send, so the level
     * has to be spelled into {@code ItemName}. This records whether that exact-name narrowing works,
     * because GEM-02 has no other lever.
     *
     * <p>Also records, per name, how many listings have a null {@code BuyPrice} — the 즉시구매가
     * missing case that decides what "현재가" can honestly mean.
     */
    @Test
    void captureGemLevels8to10() throws IOException {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 24 spike");

        StringBuilder sb = new StringBuilder();
        for (String gem : List.of("겁화의 보석", "작열의 보석")) {
            for (int level : new int[]{8, 9, 10}) {
                String name = level + "레벨 " + gem;
                ResponseEntity<String> r = client.searchAuctionItems(
                        GEM_CATEGORY_HYPOTHESIS, name, 4, 1, "BUY_PRICE", "ASC");
                sb.append("## SCOPE — ItemName=\"").append(name).append("\" tier=4 sort=BUY_PRICE ASC\n");
                appendRateLimit(sb, "rate-limit", r);
                sb.append("BODY=").append(r.getBody()).append("\n\n");
            }
        }
        Path out = Path.of("build", "spike-auction-gems-8to10.txt");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("=== SPIKE gems 8~10 wrote " + out.toAbsolutePath());
    }

    /**
     * H2 again, controlled. The first probe was NOISE: remaining fell 85 -> 53 across a single call of
     * mine, so ~32 requests were spent by someone else on this key mid-measurement — almost certainly
     * the production collector (10분 틱 × ~49 품목) sharing the same JWT. A number that moves on its
     * own cannot answer "does 경매장 spend 거래소's budget?".
     *
     * <p>So: four calls back-to-back with no work between them, and an INTERFERENCE CHECK. Two
     * consecutive auction calls must fall by exactly 1; if they don't, a concurrent consumer was
     * active and the run is inconclusive by construction — retry in a quieter window rather than
     * reading a conclusion out of noise.
     */
    @Test
    void probeRateLimitBucketTightly() throws IOException {
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LOSTARK_API_KEY not set — skipping live Phase 24 spike");

        StringBuilder sb = new StringBuilder("## H2 controlled bucket probe (tight loop)\n");

        ResponseEntity<String> a1 = client.getAuctionOptions();
        ResponseEntity<String> a2 = client.getAuctionOptions();
        ResponseEntity<String> m1 = client.searchMarketItems(MARKETS_PROBE_CATEGORY, "");
        ResponseEntity<String> a3 = client.getAuctionOptions();

        appendRateLimit(sb, "a1 AUCTIONS", a1);
        appendRateLimit(sb, "a2 AUCTIONS", a2);
        appendRateLimit(sb, "m1 MARKETS ", m1);
        appendRateLimit(sb, "a3 AUCTIONS", a3);

        Integer r1 = remaining(a1), r2 = remaining(a2), rm = remaining(m1), r3 = remaining(a3);
        sb.append("\ninterference check: a1-a2 == 1? ")
                .append(r1 != null && r2 != null ? (r1 - r2) : "n/a").append('\n');
        sb.append("shared bucket => m1 == a2-1 and a3 == m1-1\n");
        sb.append("separate bucket => m1 tracks its own counter and a3 == a2-1\n");
        sb.append("a1=%s a2=%s m1=%s a3=%s%n".formatted(r1, r2, rm, r3));

        Path out = Path.of("build", "spike-auction-ratelimit.txt");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("=== SPIKE ratelimit wrote " + out.toAbsolutePath());
    }

    private static Integer remaining(ResponseEntity<String> response) {
        String v = response.getHeaders().getFirst("x-ratelimit-remaining");
        return v == null ? null : Integer.valueOf(v.trim());
    }

    /**
     * H7 candidate 보석 names for 티어4. Deliberately a hypothesis list: the spike exists BECAUSE the
     * tier-4 gem lineup cannot be asserted from memory (Phase 21 precedent — the 아크그리드젬 guess
     * was wrong). If these return nothing, the unfiltered tier-4 dump above still shows the truth.
     */
    private static final List<String> GEM_NAME_HYPOTHESES = List.of("겁화", "작열", "멸화", "홍염");

    /**
     * Append the rate-limit headers for one response. Prints ONLY the quota headers — never the
     * Authorization header or the key (which would leak the real JWT into a captured file).
     */
    private static void appendRateLimit(StringBuilder sb, String label, ResponseEntity<String> response) {
        HttpHeaders headers = response.getHeaders();
        sb.append(label)
                .append(" | status=").append(response.getStatusCode())
                .append(" | limit=").append(headers.getFirst("x-ratelimit-limit"))
                .append(" | remaining=").append(headers.getFirst("x-ratelimit-remaining"))
                .append(" | reset=").append(headers.getFirst("x-ratelimit-reset"))
                .append('\n');
    }
}
