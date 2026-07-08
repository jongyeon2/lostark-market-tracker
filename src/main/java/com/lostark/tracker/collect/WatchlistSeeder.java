package com.lostark.tracker.collect;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.TrackedItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the curated watchlist into {@code tracked_item} on startup (D-01/D-02). Idempotent: each
 * entry is upserted by {@code external_item_id}, so re-running adds no duplicates. Active only
 * under the {@code dev}/{@code seed} profiles — never under {@code test} (integration tests insert
 * their own fixtures).
 *
 * <p>Each entry maps to a {@code POST /markets/items} call as (CategoryCode = {@code category},
 * ItemName = {@code displayName}); the response is matched back by {@code Id == externalItemId}
 * (D-05). So {@code category} holds the numeric leaf CategoryCode, NOT a label.
 *
 * <p>Beyond the collection identity, each entry carries the read-path enrichment locked by the
 * Phase 12 + 17.1 API spikes (12-/17.1-SPIKE-FINDINGS.md): {@code iconUrl} (CDN base + measured
 * filename), {@code itemGroup} (강화재료/재련재료/각인서), and {@code roleGroup} ({@code MATERIAL}/
 * {@code DEALER}/{@code SUPPORT}). All 22 entries below carry a real, spike-verified {@code Id} and
 * {@code Icon} — no placeholders. The 18 relic engraving recipes share {@code use_9_25.png}
 * (grade-single glyph, not per-engraving), so Phase 14 always labels them by name (D-06); the 4
 * materials (2 융화재료 + 2 재련 재료) each have a distinct icon.
 *
 * <p>This seeder writes only public metadata (Id / name / CategoryCode / iconUrl / group) — never a
 * key, account identifier, or price (SEED-04).
 *
 * <p>{@code prod} is included (Phase 18) so the live deployment seeds the 22-item watchlist too —
 * real collection needs targets. Unlike {@code seed}, {@code prod} does NOT run SeedDataRunner
 * ({@code @Profile("seed")}), so production accrues real prices only, never synthetic snapshots.
 */
@Component
@Profile({"dev", "seed", "prod"})
@Order(1) // Run BEFORE SeedDataRunner(@Order(2)) so active items exist when the synthetic seeder reads them.
public class WatchlistSeeder implements ApplicationRunner {

    /** Icon CDN base from the Phase 12 spike (12-SPIKE-FINDINGS.md (b)); each iconUrl = base + filename. */
    private static final String ICON_BASE = "https://cdn-lostark.game.onstove.com/efui_iconatlas/use/";

    /**
     * A watchlist entry: stable Id, display name (ItemName filter), leaf CategoryCode, plus the
     * spike-locked enrichment (icon URL, item group, role group).
     */
    private record SeedItem(String externalItemId, String displayName, String category,
                            String iconUrl, String itemGroup, String roleGroup) {
    }

    // Spike-verified curation of 22 (12- + 17.1-SPIKE-FINDINGS): 4 materials (2 융화재료 + 2 재련 재료)
    // + 11 dealer engravings + 7 supporter engravings. Current T4 meta — high-volatility, high-value
    // picks across all 3 role groups — a curation of judgment, not a dump.
    private static final List<SeedItem> WATCHLIST = List.of(
            // 재료 4 (role_group=MATERIAL, category=50010) — distinct icons. 융화재료 2 (아비도스) +
            // 재련 재료 2 (운명 결정 — 17.1-SPIKE-FINDINGS §2).
            new SeedItem("6861012", "아비도스 융화 재료", "50010", ICON_BASE + "use_12_86.png", "강화재료", "MATERIAL"),
            new SeedItem("6861013", "상급 아비도스 융화 재료", "50010", ICON_BASE + "use_13_252.png", "강화재료", "MATERIAL"),
            new SeedItem("66102007", "운명의 파괴석 결정", "50010", ICON_BASE + "use_13_249.png", "재련재료", "MATERIAL"),
            new SeedItem("66102107", "운명의 수호석 결정", "50010", ICON_BASE + "use_13_250.png", "재련재료", "MATERIAL"),
            // 딜러 각인서 11 (role_group=DEALER, item_group=각인서, category=40000, Grade=유물) — all use_9_25.png.
            new SeedItem("65200505", "유물 원한 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65201005", "유물 예리한 둔기 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65202805", "유물 저주받은 인형 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65203905", "유물 아드레날린 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65203705", "유물 타격의 대가 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65203005", "유물 기습의 대가 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65203305", "유물 돌격대장 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65201505", "유물 결투의 대가 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65203505", "유물 질량 증가 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65200605", "유물 슈퍼 차지 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            new SeedItem("65203205", "유물 바리케이드 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "DEALER"),
            // 서포터 각인서 7 (role_group=SUPPORT, item_group=각인서, category=40000, Grade=유물) — all use_9_25.png.
            new SeedItem("65203405", "유물 각성 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT"),
            new SeedItem("65204105", "유물 전문의 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT"),
            new SeedItem("65200805", "유물 구슬동자 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT"),
            new SeedItem("65203105", "유물 마나의 흐름 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT"),
            new SeedItem("65202205", "유물 폭발물 전문가 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT"),
            new SeedItem("65201705", "유물 분쇄의 주먹 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT"),
            new SeedItem("65202105", "유물 중갑 착용 각인서", "40000", ICON_BASE + "use_9_25.png", "각인서", "SUPPORT")
    );

    private final TrackedItemRepository trackedItemRepository;

    public WatchlistSeeder(TrackedItemRepository trackedItemRepository) {
        this.trackedItemRepository = trackedItemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedItem seed : WATCHLIST) {
            // Idempotent upsert by external_item_id: insert when absent, otherwise leave existing
            // (Phase 4 admin CRUD owns later edits — the seeder must not clobber manual changes).
            trackedItemRepository.findByExternalItemId(seed.externalItemId())
                    .orElseGet(() -> trackedItemRepository.save(new TrackedItem(
                            seed.externalItemId(), seed.displayName(), seed.category(),
                            seed.iconUrl(), seed.itemGroup(), seed.roleGroup())));
        }
    }
}
