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
 * {@code DEALER}/{@code SUPPORT}). All 39 entries below carry a real, spike-verified {@code Id} and
 * {@code Icon} — no placeholders. The 18 relic engraving recipes share {@code use_9_25.png}
 * (grade-single glyph, not per-engraving), so Phase 14 always labels them by name (D-06); the 21
 * materials (융화재료 + 재련재료 + 상급재련 + 아크그리드젬) each have a distinct icon.
 *
 * <p>This seeder writes only public metadata (Id / name / CategoryCode / iconUrl / group) — never a
 * key, account identifier, or price (SEED-04).
 *
 * <p>{@code prod} is included (Phase 18) so the live deployment seeds the 39-item watchlist too —
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

    // Spike-verified curation of 39 (12-/17.1-/21-SPIKE-FINDINGS): 21 materials (융화 2 + 재련기본 9 +
    // 상급재련 4 + 아크그리드젬 6) + 11 dealer + 7 supporter engravings. Current T4 meta — high-volatility,
    // high-value 스펙업 picks across all 3 role groups — a curation of judgment, not a dump.
    private static final List<SeedItem> WATCHLIST = List.of(
            // 재료 4 (role_group=MATERIAL, category=50010) — distinct icons. 융화재료 2 (아비도스) +
            // 재련 재료 2 (운명 결정 — 17.1-SPIKE-FINDINGS §2).
            new SeedItem("6861012", "아비도스 융화 재료", "50010", ICON_BASE + "use_12_86.png", "강화재료", "MATERIAL"),
            new SeedItem("6861013", "상급 아비도스 융화 재료", "50010", ICON_BASE + "use_13_252.png", "강화재료", "MATERIAL"),
            new SeedItem("66102007", "운명의 파괴석 결정", "50010", ICON_BASE + "use_13_249.png", "재련재료", "MATERIAL"),
            new SeedItem("66102107", "운명의 수호석 결정", "50010", ICON_BASE + "use_13_250.png", "재련재료", "MATERIAL"),
            // ── v1.5(MKT-02) 신규 스펙업 재련 재료 17 (21-SPIKE-FINDINGS 비준). 전부 role_group=MATERIAL,
            //    실측 Id·icon. 파괴/수호석 결정(위 2)은 이미 있어 재추가하지 않는다(멱등).
            // 재련 기본 7 (category=50010, item_group=재련재료) — 파괴/수호석 base + 돌파석 둘 다 + 파편 소중대.
            new SeedItem("66102006", "운명의 파괴석", "50010", ICON_BASE + "use_12_88.png", "재련재료", "MATERIAL"),
            new SeedItem("66102106", "운명의 수호석", "50010", ICON_BASE + "use_12_89.png", "재련재료", "MATERIAL"),
            new SeedItem("66110225", "운명의 돌파석", "50010", ICON_BASE + "use_12_85.png", "재련재료", "MATERIAL"),
            new SeedItem("66110226", "위대한 운명의 돌파석", "50010", ICON_BASE + "use_13_251.png", "재련재료", "MATERIAL"),
            new SeedItem("66130141", "운명의 파편 주머니(소)", "50010", ICON_BASE + "use_12_91.png", "재련재료", "MATERIAL"),
            new SeedItem("66130142", "운명의 파편 주머니(중)", "50010", ICON_BASE + "use_12_92.png", "재련재료", "MATERIAL"),
            new SeedItem("66130143", "운명의 파편 주머니(대)", "50010", ICON_BASE + "use_12_93.png", "재련재료", "MATERIAL"),
            // 상급 재련 4 (category=50020, item_group=상급재련) — 숨결 2 + 야금술/재봉술 업화[15-18](유물).
            new SeedItem("66111131", "용암의 숨결", "50020", ICON_BASE + "use_12_171.png", "상급재련", "MATERIAL"),
            new SeedItem("66111132", "빙하의 숨결", "50020", ICON_BASE + "use_12_172.png", "상급재련", "MATERIAL"),
            new SeedItem("66112551", "야금술 : 업화 [15-18]", "50020", ICON_BASE + "use_12_218.png", "상급재련", "MATERIAL"),
            new SeedItem("66112552", "재봉술 : 업화 [15-18]", "50020", ICON_BASE + "use_12_219.png", "상급재련", "MATERIAL"),
            // 아크그리드 젬 6 (category=230000, item_group=아크그리드젬, 영웅 등급) — 질서 3 + 혼돈 3. 거래소에서 거래(경매장 아님).
            new SeedItem("67400003", "질서의 젬 : 안정", "230000", ICON_BASE + "use_13_110.png", "아크그리드젬", "MATERIAL"),
            new SeedItem("67400103", "질서의 젬 : 견고", "230000", ICON_BASE + "use_13_111.png", "아크그리드젬", "MATERIAL"),
            new SeedItem("67400203", "질서의 젬 : 불변", "230000", ICON_BASE + "use_13_112.png", "아크그리드젬", "MATERIAL"),
            new SeedItem("67410303", "혼돈의 젬 : 침식", "230000", ICON_BASE + "use_13_113.png", "아크그리드젬", "MATERIAL"),
            new SeedItem("67410403", "혼돈의 젬 : 왜곡", "230000", ICON_BASE + "use_13_114.png", "아크그리드젬", "MATERIAL"),
            new SeedItem("67410503", "혼돈의 젬 : 붕괴", "230000", ICON_BASE + "use_13_115.png", "아크그리드젬", "MATERIAL"),
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
