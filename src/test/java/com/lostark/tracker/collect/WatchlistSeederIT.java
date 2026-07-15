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
 * Proves the {@link WatchlistSeeder} replaces the watchlist with the Phase 12 + 17.1 + 21 + quick-260714
 * spike-verified curation of 49 (SEED-01, v1.5 MKT-02) and that {@link SyntheticDemoData} populates synthetic
 * history for the resulting new items WITHOUT a key or network (SEED-02) — all on Testcontainers.
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
    void seedsCurationOf49WithRoleDistributionAndEnrichment() {
        seeder.run(null);

        List<TrackedItem> items = trackedItemRepository.findByActiveTrue();
        assertThat(items).hasSize(49);

        Map<String, Long> byRole = items.stream()
                .collect(Collectors.groupingBy(TrackedItem::getRoleGroup, Collectors.counting()));
        // v1.5(MKT-02) + quick-260714: 장인의 야금술/재봉술 1~4단계 8종 추가 → MATERIAL 23→31. 각인서 불변.
        assertThat(byRole).containsEntry("MATERIAL", 31L)
                .containsEntry("DEALER", 11L)
                .containsEntry("SUPPORT", 7L);

        // Every entry carries CDN-prefixed enrichment from one of the item groups.
        assertThat(items).allSatisfy(item -> {
            assertThat(item.getIconUrl()).startsWith(ICON_BASE);
            assertThat(item.getItemGroup()).isIn("재련재료", "상급재련", "재련보조", "아크그리드젬", "각인서");
        });

        // quick-260715: 강화재료 그룹 폐지 — 융화 2종이 재련기본 9종과 합쳐져 재련재료 11. 대시보드의
        // 재료 leaf는 item_group에서 그대로 파생되므로, 이 카운트가 곧 사용자가 보는 카테고리다.
        Map<String, Long> byGroup = items.stream()
                .collect(Collectors.groupingBy(TrackedItem::getItemGroup, Collectors.counting()));
        assertThat(byGroup).doesNotContainKey("강화재료")
                .containsEntry("재련재료", 11L)
                .containsEntry("상급재련", 8L)
                .containsEntry("재련보조", 6L)
                .containsEntry("아크그리드젬", 6L)
                .containsEntry("각인서", 18L);

        Map<String, TrackedItem> byId = items.stream()
                .collect(Collectors.toMap(TrackedItem::getExternalItemId, Function.identity()));

        // Sample fusion material: distinct icon, MATERIAL/재련재료, category 50010. quick-260715: 융화재료는
        // 재련에 반드시 들어가므로 '강화재료' 그룹을 폐지하고 재련재료로 통합했다(기존 행은 V7이 이관).
        TrackedItem fusion = byId.get("6861012");
        assertThat(fusion).isNotNull();
        assertThat(fusion.getIconUrl()).isEqualTo(ICON_BASE + "use_12_86.png");
        assertThat(fusion.getItemGroup()).isEqualTo("재련재료");
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

        // Sample 아크그리드 젬 (v1.5 MKT-02): 거래소(230000)·MATERIAL·아크그리드젬, 영웅 등급.
        TrackedItem gem = byId.get("67400003");
        assertThat(gem).isNotNull();
        assertThat(gem.getDisplayName()).isEqualTo("질서의 젬 : 안정");
        assertThat(gem.getIconUrl()).isEqualTo(ICON_BASE + "use_13_110.png");
        assertThat(gem.getItemGroup()).isEqualTo("아크그리드젬");
        assertThat(gem.getRoleGroup()).isEqualTo("MATERIAL");
        assertThat(gem.getCategory()).isEqualTo("230000");

        // Sample 상급 재련 (quick-260714): 진짜 상급 재련 재료 = 장인의 야금술 4단계(고대), category 50020.
        TrackedItem master = byId.get("66112717");
        assertThat(master).isNotNull();
        assertThat(master.getDisplayName()).isEqualTo("장인의 야금술 : 4단계");
        assertThat(master.getIconUrl()).isEqualTo(ICON_BASE + "use_13_223.png");
        assertThat(master.getItemGroup()).isEqualTo("상급재련");
        assertThat(master.getRoleGroup()).isEqualTo("MATERIAL");
        assertThat(master.getCategory()).isEqualTo("50020");

        // Sample 재련 보조 (quick-260714 교정): "업화 [15-18]"는 일반 재련 보조 재료 → item_group=재련보조(상급재련 아님).
        TrackedItem refineAid = byId.get("66112551");
        assertThat(refineAid).isNotNull();
        assertThat(refineAid.getDisplayName()).isEqualTo("야금술 : 업화 [15-18]");
        assertThat(refineAid.getItemGroup()).isEqualTo("재련보조");
        assertThat(refineAid.getRoleGroup()).isEqualTo("MATERIAL");
        assertThat(refineAid.getCategory()).isEqualTo("50020");

        // Sample 숨결 (quick-260714 교정): 용암의 숨결은 상급 재련·일반 강화 겸용 → 상급재련 전용 아님 → item_group=재련보조.
        TrackedItem breath = byId.get("66111131");
        assertThat(breath).isNotNull();
        assertThat(breath.getDisplayName()).isEqualTo("용암의 숨결");
        assertThat(breath.getIconUrl()).isEqualTo(ICON_BASE + "use_12_171.png");
        assertThat(breath.getItemGroup()).isEqualTo("재련보조");
        assertThat(breath.getRoleGroup()).isEqualTo("MATERIAL");
        assertThat(breath.getCategory()).isEqualTo("50020");
    }

    @Test
    void seederIsIdempotentByExternalItemId() {
        seeder.run(null);
        assertThat(trackedItemRepository.count()).isEqualTo(49);

        // A second pass upserts by external_item_id — no duplicate inserts.
        seeder.run(null);
        assertThat(trackedItemRepository.count()).isEqualTo(49);
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
