package com.lostark.tracker.collect;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.seed.SyntheticDemoData;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the {@link WatchlistSeeder} replaces the watchlist with the Phase 12 + 17.1 spike-verified
 * curation of 22 (SEED-01) and that {@link SyntheticDemoData} populates synthetic history for the
 * resulting new items WITHOUT a key or network (SEED-02) — all on Testcontainers.
 *
 * <p>Runs under the {@code test} profile, where {@code WatchlistSeeder} is inactive
 * ({@code @Profile({dev,seed})}). The test therefore constructs the seeder directly against the
 * autowired repository and drives {@code run(null)} — fully controlled, no profile gymnastics, and
 * it never touches the external API (no key required). {@code SyntheticDemoData} is a plain
 * {@code @Component} (no profile), so it is autowired as-is; this IT is the evidence that its source
 * needs 0 changes to absorb the new items (it reads {@code findByActiveTrue()}).
 */
@SpringBootTest
@ActiveProfiles("test")
class WatchlistSeederIT extends PostgresRedisContainers {

    private static final String ICON_BASE = "https://cdn-lostark.game.onstove.com/efui_iconatlas/use/";

    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    CollectionRunRepository collectionRunRepository;

    // Constructed per-test in clean() with a FIXED clock so seed()'s 10-minute-grid timestamps are
    // deterministic. With a wall-clock now(), the first seed() (25k inserts, tens of seconds) and the
    // second could straddle a 10-min grid boundary → different timestamps → broken idempotency (flaky).
    // Prod calls seed() once at boot, so pinning the clock is a test-only seam, not a product change.
    private SyntheticDemoData syntheticDemoData;

    private WatchlistSeeder seeder;

    @BeforeEach
    void clean() {
        // FK order: child snapshots first, then items, then events (mirrors the read ITs).
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
        seeder = new WatchlistSeeder(trackedItemRepository);
        // Fixed instant (off the 10-min grid → floors to 12:00) shared by every seed() call in a test,
        // so a second seed() is a true no-op regardless of wall-clock time.
        syntheticDemoData = new SyntheticDemoData(trackedItemRepository, priceSnapshotRepository,
                gameEventRepository, collectionRunRepository,
                Clock.fixed(Instant.parse("2026-07-01T12:05:00Z"), ZoneOffset.UTC));
    }

    @Test
    void seedsCurationOf22WithRoleDistributionAndEnrichment() {
        seeder.run(null);

        List<TrackedItem> items = trackedItemRepository.findByActiveTrue();
        assertThat(items).hasSize(22);

        Map<String, Long> byRole = items.stream()
                .collect(Collectors.groupingBy(TrackedItem::getRoleGroup, Collectors.counting()));
        assertThat(byRole).containsEntry("MATERIAL", 4L)
                .containsEntry("DEALER", 11L)
                .containsEntry("SUPPORT", 7L);

        // Every entry carries CDN-prefixed enrichment from one of the item groups.
        assertThat(items).allSatisfy(item -> {
            assertThat(item.getIconUrl()).startsWith(ICON_BASE);
            assertThat(item.getItemGroup()).isIn("강화재료", "재련재료", "각인서");
        });

        Map<String, TrackedItem> byId = items.stream()
                .collect(Collectors.toMap(TrackedItem::getExternalItemId, Function.identity()));

        // Sample fusion material: distinct icon, MATERIAL/강화재료, category 50010.
        TrackedItem fusion = byId.get("6861012");
        assertThat(fusion).isNotNull();
        assertThat(fusion.getIconUrl()).isEqualTo(ICON_BASE + "use_12_86.png");
        assertThat(fusion.getItemGroup()).isEqualTo("강화재료");
        assertThat(fusion.getRoleGroup()).isEqualTo("MATERIAL");
        assertThat(fusion.getCategory()).isEqualTo("50010");

        // Sample NEW refining material (17.1): distinct icon, MATERIAL/재련재료, category 50010.
        TrackedItem refining = byId.get("66102007");
        assertThat(refining).isNotNull();
        assertThat(refining.getDisplayName()).isEqualTo("운명의 파괴석 결정");
        assertThat(refining.getIconUrl()).isEqualTo(ICON_BASE + "use_13_249.png");
        assertThat(refining.getItemGroup()).isEqualTo("재련재료");
        assertThat(refining.getRoleGroup()).isEqualTo("MATERIAL");
        assertThat(refining.getCategory()).isEqualTo("50010");

        // Sample supporter engraving: shared engraving glyph, SUPPORT/각인서, category 40000.
        TrackedItem supporter = byId.get("65203405");
        assertThat(supporter).isNotNull();
        assertThat(supporter.getIconUrl()).isEqualTo(ICON_BASE + "use_9_25.png");
        assertThat(supporter.getItemGroup()).isEqualTo("각인서");
        assertThat(supporter.getRoleGroup()).isEqualTo("SUPPORT");
        assertThat(supporter.getCategory()).isEqualTo("40000");
    }

    @Test
    void seederIsIdempotentByExternalItemId() {
        seeder.run(null);
        assertThat(trackedItemRepository.count()).isEqualTo(22);

        // A second pass upserts by external_item_id — no duplicate inserts.
        seeder.run(null);
        assertThat(trackedItemRepository.count()).isEqualTo(22);
    }

    @Test
    void syntheticDemoDataSnapshotsNewItemsKeylessAndIdempotent() {
        // SEED-02: the new curated items flow into SyntheticDemoData via findByActiveTrue() with
        // ZERO changes to its source — this test is that proof.
        seeder.run(null);

        syntheticDemoData.seed();
        long afterFirst = priceSnapshotRepository.count();
        assertThat(afterFirst).isPositive();

        // A brand-new curated item (17.1 — not in the old watchlist) has synthetic snapshots.
        TrackedItem newItem = trackedItemRepository.findByExternalItemId("66102007").orElseThrow();
        assertThat(priceSnapshotRepository.findTopByTrackedItem_IdOrderByCollectedAtDesc(newItem.getId()))
                .isPresent();

        // Second seed() is idempotent: per-tick existsBy guard makes it a no-op.
        syntheticDemoData.seed();
        assertThat(priceSnapshotRepository.count()).isEqualTo(afterFirst);
    }
}
