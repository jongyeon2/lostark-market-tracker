package com.lostark.tracker.repository;

import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.ItemDailyStat;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the storage contract of the backfill daily-average layer (Phase 17.4, plan 01) on
 * Testcontainers PostgreSQL: idempotent upsert (refresh, not duplicate), per-source coexistence,
 * and the inclusive ascending window finder. No real API / API key is used.
 */
@SpringBootTest
@ActiveProfiles("test")
class ItemDailyStatRepositoryIT extends PostgresRedisContainers {

    @Autowired
    ItemDailyStatRepository itemDailyStatRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    private Long itemId;

    @BeforeEach
    void clean() {
        // Only clear this plan's own additive table; never touch price_snapshot (Core Value guard) or
        // do a global tracked_item wipe (other tests' price_snapshot rows FK-block it on the shared
        // Testcontainers DB). A per-run-unique external_item_id sidesteps the V3 uniqueness constraint.
        itemDailyStatRepository.deleteAll();
        String externalId = "IDS-IT-" + System.nanoTime();
        itemId = trackedItemRepository.save(new TrackedItem(externalId, "융화 재료", "50010")).getId();
    }

    @AfterEach
    void tidy() {
        // item_daily_stats is a NEW child of tracked_item on the shared Testcontainers DB; leaving rows
        // behind would FK-block every other IT's tracked_item.deleteAll(). Clear our table and drop the
        // per-test seed by id (safe: no children remain after the deleteAll above).
        itemDailyStatRepository.deleteAll();
        trackedItemRepository.deleteById(itemId);
    }

    @Test
    void upsertIsIdempotent_sameKeyRefreshesInPlaceInsteadOfDuplicating() {
        // Same (item, date, source), two different avg_prices -> exactly one row, holding the LAST value.
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY, new BigDecimal("100.0000"),
                DailyStatSource.YDAY_AVG.name());
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY, new BigDecimal("200.0000"),
                DailyStatSource.YDAY_AVG.name());

        assertThat(itemDailyStatRepository.findAll()).hasSize(1);
        assertThat(itemDailyStatRepository.findAll().get(0).getAvgPrice())
                .isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    void sameItemAndDate_differentSource_coexistAsTwoRows() {
        // The source column disambiguates provenance: YDAY_AVG and DETAIL_STATS are distinct rows.
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY, new BigDecimal("147007.4000"),
                DailyStatSource.YDAY_AVG.name());
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY, new BigDecimal("150000.0000"),
                DailyStatSource.DETAIL_STATS.name());

        assertThat(itemDailyStatRepository.findAll())
                .hasSize(2)
                .extracting(ItemDailyStat::getSource)
                .containsExactlyInAnyOrder(DailyStatSource.YDAY_AVG, DailyStatSource.DETAIL_STATS);
    }

    @Test
    void windowFinderReturnsInclusiveAscendingSubset() {
        // Seed three days; query [today-2, today-1] -> exactly the first two, ascending, today excluded.
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY.minusDays(2), new BigDecimal("10"),
                DailyStatSource.YDAY_AVG.name());
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY.minusDays(1), new BigDecimal("20"),
                DailyStatSource.YDAY_AVG.name());
        itemDailyStatRepository.upsertDailyStat(itemId, TODAY, new BigDecimal("30"),
                DailyStatSource.YDAY_AVG.name());

        var window = itemDailyStatRepository.findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(
                itemId, TODAY.minusDays(2), TODAY.minusDays(1));

        assertThat(window)
                .extracting(ItemDailyStat::getStatDate)
                .containsExactly(TODAY.minusDays(2), TODAY.minusDays(1));
    }
}
