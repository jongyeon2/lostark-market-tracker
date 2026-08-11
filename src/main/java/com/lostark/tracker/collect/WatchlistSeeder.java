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
 * Phase 12 + 17.1 + 21 + quick-260714 API spikes (12-/17.1-/21-SPIKE-FINDINGS.md): {@code iconUrl}
 * (CDN base + measured filename), {@code itemGroup} (재련재료/상급재련/재련보조/아크그리드젬/각인서),
 * and {@code roleGroup} ({@code MATERIAL}/{@code DEALER}/{@code SUPPORT}). All 53 entries below carry
 * a real, spike-verified {@code Id} and {@code Icon} — no placeholders. The 18 relic engraving recipes
 * share {@code use_9_25.png} (grade-single glyph, not per-engraving), so Phase 14 always labels them by
 * name (D-06); the 35 materials carry distinct icons except the 재련보조 업화·전율 groups (야금술/재봉술 업화
 * [15-18]·[19-20]·전율 [12-15]·[16-19] share use_12_218/219 — labeled by name). NOTE(domain, quick-260714): item_group=상급재련은 상급 재련
 * 전용 재료(장인의 야금술/재봉술 1~4단계)만; 업화 계열은 일반 재련 성공률 보조, 숨결은 상급·일반 겸용이라 둘 다
 * item_group=재련보조로 분류한다.
 *
 * <p>This seeder writes only public metadata (Id / name / CategoryCode / iconUrl / group) — never a
 * key, account identifier, or price (SEED-04).
 *
 * <p>{@code prod} is included (Phase 18) so the live deployment seeds the 49-item watchlist too —
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

    // Spike-verified curation of 53 (12-/17.1-/21-/22b-/quick-260714-/quick-260811-SPIKE-FINDINGS): 35 materials
    // (재련재료 11 [융화 2 + 재련기본 9] + 상급재련 8 + 재련보조 10 + 아크그리드젬 6) + 11 dealer + 7 supporter
    // engravings. Current T4 meta — high-volatility, high-value 스펙업 picks across all 3 role groups —
    // judgment, not a dump.
    private static final List<SeedItem> WATCHLIST = List.of(
            // 재료 4 (role_group=MATERIAL, category=50010) — distinct icons. 융화재료 2 (아비도스) +
            // 재련 재료 2 (운명 결정 — 17.1-SPIKE-FINDINGS §2). NOTE(domain, quick-260715): 융화재료는 재련에
            // 반드시 들어가므로 별도 '강화재료' 그룹을 폐지하고 재련재료에 통합했다(V7이 기존 행을 이관 —
            // 이 시더는 insert-only라 소스 수정만으론 기존 DB가 바뀌지 않는다).
            new SeedItem("6861012", "아비도스 융화 재료", "50010", ICON_BASE + "use_12_86.png", "재련재료", "MATERIAL"),
            new SeedItem("6861013", "상급 아비도스 융화 재료", "50010", ICON_BASE + "use_13_252.png", "재련재료", "MATERIAL"),
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
            // 상급 재련 8 (category=50020, item_group=상급재련) — 장인의 야금술/재봉술 1~4단계. 상급 재련 전용 추가 재료.
            //   무기=야금술, 방어구=재봉술; 단계별 등급 영웅→전설→유물→고대. 3·4단계 Id·등급은 quick-260714
            //   스파이크(captureRefineMasterBooks, DESC/ASC)로 실측·잠금. 아이콘 전부 distinct.
            new SeedItem("66112711", "장인의 야금술 : 1단계", "50020", ICON_BASE + "use_12_242.png", "상급재련", "MATERIAL"),
            new SeedItem("66112713", "장인의 야금술 : 2단계", "50020", ICON_BASE + "use_12_244.png", "상급재련", "MATERIAL"),
            new SeedItem("66112715", "장인의 야금술 : 3단계", "50020", ICON_BASE + "use_13_221.png", "상급재련", "MATERIAL"),
            new SeedItem("66112717", "장인의 야금술 : 4단계", "50020", ICON_BASE + "use_13_223.png", "상급재련", "MATERIAL"),
            new SeedItem("66112712", "장인의 재봉술 : 1단계", "50020", ICON_BASE + "use_12_243.png", "상급재련", "MATERIAL"),
            new SeedItem("66112714", "장인의 재봉술 : 2단계", "50020", ICON_BASE + "use_12_245.png", "상급재련", "MATERIAL"),
            new SeedItem("66112716", "장인의 재봉술 : 3단계", "50020", ICON_BASE + "use_13_222.png", "상급재련", "MATERIAL"),
            new SeedItem("66112718", "장인의 재봉술 : 4단계", "50020", ICON_BASE + "use_13_224.png", "상급재련", "MATERIAL"),
            // 재련 보조 10 (category=50020, item_group=재련보조) — 재련에 넣는 보조 재료. 무기=야금술, 방어구=재봉술.
            //   숨결 2(용암/빙하)는 상급 재련·일반 강화 겸용이라 상급재련 전용 아님 → 재련보조로 분류(도메인 교정, quick-260714).
            //   업화 [15-18]·[19-20] 4는 일반 재련 성공률 보조 재료(아이콘은 레벨구간 공유 use_12_218/219 — 라벨로 구분).
            new SeedItem("66111131", "용암의 숨결", "50020", ICON_BASE + "use_12_171.png", "재련보조", "MATERIAL"),
            new SeedItem("66111132", "빙하의 숨결", "50020", ICON_BASE + "use_12_172.png", "재련보조", "MATERIAL"),
            new SeedItem("66112551", "야금술 : 업화 [15-18]", "50020", ICON_BASE + "use_12_218.png", "재련보조", "MATERIAL"),
            new SeedItem("66112552", "재봉술 : 업화 [15-18]", "50020", ICON_BASE + "use_12_219.png", "재련보조", "MATERIAL"),
            new SeedItem("66112553", "야금술 : 업화 [19-20]", "50020", ICON_BASE + "use_12_218.png", "재련보조", "MATERIAL"),
            new SeedItem("66112554", "재봉술 : 업화 [19-20]", "50020", ICON_BASE + "use_12_219.png", "재련보조", "MATERIAL"),
            // v1.6 신규 재련보조 '전율' 4종 (벨가르딘 그림자 레이드 2026-08-05 — quick-260811 스파이크 실측). 일반 재련
            //   성공률 보조 재료(업화 계열과 동일 성격), 전부 Grade=고대. 아이콘은 업화와 공유(야금술 use_12_218 ·
            //   재봉술 use_12_219) → 라벨로 구분(업화 선례). 강화 업화[19-20](66112555/66112556)는 기존 미편입이라 제외.
            new SeedItem("66112561", "야금술 : 전율 [12-15]", "50020", ICON_BASE + "use_12_218.png", "재련보조", "MATERIAL"),
            new SeedItem("66112562", "야금술 : 전율 [16-19]", "50020", ICON_BASE + "use_12_218.png", "재련보조", "MATERIAL"),
            new SeedItem("66112564", "재봉술 : 전율 [12-15]", "50020", ICON_BASE + "use_12_219.png", "재련보조", "MATERIAL"),
            new SeedItem("66112565", "재봉술 : 전율 [16-19]", "50020", ICON_BASE + "use_12_219.png", "재련보조", "MATERIAL"),
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
