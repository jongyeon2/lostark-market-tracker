package com.lostark.tracker.read;

import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.AnchorSource;
import com.lostark.tracker.web.dto.EventImpactItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the Phase 25 daily-average FALLBACK anchor (IMPACT-V2-01).
 *
 * <p>The bug this closes, reproduced from live data: 차원술사 출시 fired 2026-07-08T01:00Z, but price
 * collection only started 2026-07-10 — so the event had zero snapshot anchors and read
 * "데이터 부족" forever, even though the backfilled daily averages showed the +40% spike plainly. The
 * fixture below is those real numbers (7/7 32628.5 → 7/8 45877.9).
 *
 * <p>The contract has two halves and both matter: snapshots WIN when present (a rate must not silently
 * change lens), and a rate is only ever computed from ONE source — min(호가) and avg(체결) are never
 * mixed inside a single ratio.
 */
@SpringBootTest
@ActiveProfiles("test")
class EventImpactBackfillAnchorIT extends PostgresRedisContainers {

    /** 2026-07-08T01:00Z == 2026-07-08 10:00 KST — the live 차원술사 출시 instant. */
    private static final OffsetDateTime EVENT_AT = OffsetDateTime.parse("2026-07-08T01:00:00Z");
    private static final LocalDate EVENT_DAY_KST = LocalDate.parse("2026-07-08");
    private static final LocalDate DAY_BEFORE_KST = LocalDate.parse("2026-07-07");

    @Autowired
    EventImpactService eventImpactService;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    ItemDailyStatRepository itemDailyStatRepository;

    private Long itemId;

    @BeforeEach
    void seed() {
        itemDailyStatRepository.deleteAll();
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();

        TrackedItem item = trackedItemRepository.save(new TrackedItem(
                "65203705", "유물 타격의 대가 각인서", "40000",
                "https://cdn-lostark.game.onstove.com/efui_iconatlas/use/use_9_25.png", "각인서", "DEALER"));
        itemId = item.getId();
        gameEventRepository.save(new GameEvent(EventType.NEW_CLASS, "차원술사 출시", EVENT_AT, null));
    }

    private void daily(LocalDate day, String avg, DailyStatSource source) {
        itemDailyStatRepository.upsertDailyStat(itemId, day, new BigDecimal(avg), source.name());
    }

    /*
      이 IT는 백필 앵커 계산만 본다 — 필터/정렬/limit은 관심사가 아니므로 "전부 · 최신순 · 넉넉한 상한"
      으로 고정한다(2026-07-20 시그니처 확장). 이 클래스는 이벤트를 하나만 심으므로 어떤 값을 줘도
      같은 한 건이 나오고, 그래서 이 테스트가 검증하는 성질은 그대로다.
    */
    private EventImpactItem only(int windowHours) {
        return eventImpactService
                .eventImpact(itemId, windowHours, EnumSet.allOf(EventType.class), Sort.Direction.DESC, 50)
                .events()
                .get(0);
    }

    @Test
    void fallsBackToDailyAverageWhenNoSnapshotCoversTheEvent() {
        daily(DAY_BEFORE_KST, "32628.5", DailyStatSource.DETAIL_STATS);
        daily(EVENT_DAY_KST, "45877.9", DailyStatSource.DETAIL_STATS);

        EventImpactItem item = only(24);

        assertThat(item.status()).isEqualTo("ok");
        assertThat(item.anchorSource()).isEqualTo(AnchorSource.DAILY_AVG);
        assertThat(item.prePrice()).isEqualTo(32629L);  // HALF_UP of 32628.5
        assertThat(item.postPrice()).isEqualTo(45878L);
        // 45877.9 / 32628.5 - 1 = 0.40607... -> 4dp HALF_UP. 차원술사 출시 후 +40.6%.
        assertThat(item.changeRate()).isEqualByComparingTo("0.4061");
    }

    @Test
    void snapshotAnchorsWinWhenPresentAndKeepMinSemantics() {
        // Fresh snapshots on both sides (within the 30-min allowance) — plus daily stats that would
        // give a WILDLY different answer. The snapshot answer must be the one reported.
        priceSnapshotRepository.save(new PriceSnapshot(
                trackedItemRepository.findById(itemId).orElseThrow(),
                EVENT_AT.minusMinutes(10), 1000L, EVENT_AT.minusMinutes(10)));
        priceSnapshotRepository.save(new PriceSnapshot(
                trackedItemRepository.findById(itemId).orElseThrow(),
                EVENT_AT.plusMinutes(10), 1100L, EVENT_AT.plusMinutes(10)));
        daily(DAY_BEFORE_KST, "32628.5", DailyStatSource.DETAIL_STATS);
        daily(EVENT_DAY_KST, "45877.9", DailyStatSource.DETAIL_STATS);

        EventImpactItem item = only(24);

        assertThat(item.status()).isEqualTo("ok");
        assertThat(item.anchorSource()).isEqualTo(AnchorSource.SNAPSHOT_MIN);
        assertThat(item.prePrice()).isEqualTo(1000L);
        assertThat(item.postPrice()).isEqualTo(1100L);
        assertThat(item.changeRate()).isEqualByComparingTo("0.1000");
    }

    @Test
    void detailStatsWinsOverYdayAvgOnTheSameDay() {
        // Both sources land on the same date (UNIQUE includes source, so they coexist). DETAIL_STATS is
        // the one that reaches back before collection started, so it is the consistent basis.
        daily(DAY_BEFORE_KST, "32628.5", DailyStatSource.DETAIL_STATS);
        daily(DAY_BEFORE_KST, "99999.0", DailyStatSource.YDAY_AVG);
        daily(EVENT_DAY_KST, "45877.9", DailyStatSource.DETAIL_STATS);
        daily(EVENT_DAY_KST, "11111.0", DailyStatSource.YDAY_AVG);

        EventImpactItem item = only(24);

        assertThat(item.prePrice()).isEqualTo(32629L);
        assertThat(item.postPrice()).isEqualTo(45878L);
    }

    @Test
    void ydayAvgIsUsedWhenDetailStatsIsMissingForThatDay() {
        daily(DAY_BEFORE_KST, "32628.5", DailyStatSource.YDAY_AVG);
        daily(EVENT_DAY_KST, "45877.9", DailyStatSource.DETAIL_STATS);

        EventImpactItem item = only(24);

        assertThat(item.status()).isEqualTo("ok");
        assertThat(item.anchorSource()).isEqualTo(AnchorSource.DAILY_AVG);
    }

    @Test
    void stillInsufficientWhenOneDayIsMissing() {
        daily(EVENT_DAY_KST, "45877.9", DailyStatSource.DETAIL_STATS); // 전일 없음

        EventImpactItem item = only(24);

        assertThat(item.status()).isEqualTo("insufficient_data");
        assertThat(item.changeRate()).isNull();
        assertThat(item.anchorSource()).isNull();
    }

    @Test
    void insufficientWhenNeitherSnapshotsNorDailyStatsExist() {
        EventImpactItem item = only(24);

        assertThat(item.status()).isEqualTo("insufficient_data");
        assertThat(item.anchorSource()).isNull();
    }

    @Test
    void eventDayIsJudgedInKstNotUtc() {
        // EVENT_AT is 2026-07-08T01:00Z, which is 2026-07-08 10:00 KST — the same calendar day in KST
        // but the fixture proves we resolve the day in KST: were it read as UTC-date it would still be
        // 07-08 here, so use an instant where the two DIFFER — 2026-07-08T16:00Z == 07-09 01:00 KST.
        gameEventRepository.deleteAll();
        gameEventRepository.save(new GameEvent(
                EventType.GENERAL_PATCH, "심야 패치", OffsetDateTime.parse("2026-07-08T16:00:00Z"), null));
        daily(LocalDate.parse("2026-07-08"), "32628.5", DailyStatSource.DETAIL_STATS); // KST 전일
        daily(LocalDate.parse("2026-07-09"), "45877.9", DailyStatSource.DETAIL_STATS); // KST 당일

        EventImpactItem item = only(24);

        assertThat(item.status()).isEqualTo("ok");
        assertThat(item.prePrice()).isEqualTo(32629L);
        assertThat(item.postPrice()).isEqualTo(45878L);
    }
}
