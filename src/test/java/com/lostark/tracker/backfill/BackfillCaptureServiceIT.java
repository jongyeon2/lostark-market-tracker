package com.lostark.tracker.backfill;

import com.lostark.tracker.collect.ItemFetchResult;
import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.ItemDailyStat;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Pins the YDayAvgPrice backfill capture (Phase 17.4, plan 02) on Testcontainers: previous-KST-day
 * stamping with decimal preservation, idempotent re-capture, null no-op, and the fail-open contract
 * (a repo error never propagates back into the collection tick). The clock is fixed so stat_date is
 * deterministic; no real API is called (ItemFetchResult is built directly).
 */
@SpringBootTest
@ActiveProfiles("test")
class BackfillCaptureServiceIT extends PostgresRedisContainers {

    @Autowired
    ItemDailyStatRepository itemDailyStatRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    // 2026-07-07 14:00 KST -> today KST 2026-07-07; YDayAvgPrice is the PREVIOUS day (2026-07-06).
    private static final Instant FIXED = Instant.parse("2026-07-07T05:00:00Z");
    private static final LocalDate EXPECTED_STAT_DATE = LocalDate.of(2026, 7, 6);
    private static final OffsetDateTime FETCHED_AT = OffsetDateTime.parse("2026-07-07T05:00:00Z");

    private BackfillCaptureService service;
    private TrackedItem item;

    @BeforeEach
    void setUp() {
        itemDailyStatRepository.deleteAll();
        item = trackedItemRepository.save(new TrackedItem("BCS-IT-" + System.nanoTime(), "유물 원한 각인서", "40000"));
        service = new BackfillCaptureService(itemDailyStatRepository, Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    @AfterEach
    void tidy() {
        // Keep the shared Testcontainers DB clean so item_daily_stats never FK-blocks other ITs.
        itemDailyStatRepository.deleteAll();
        trackedItemRepository.deleteById(item.getId());
    }

    @Test
    void capturesYDayAvgForPreviousKstDayPreservingDecimal() {
        service.captureYDayAvg(item, ItemFetchResult.success(item.getId(), 145500L, 147007.4, FETCHED_AT));

        List<ItemDailyStat> rows = itemDailyStatRepository.findAll();
        assertThat(rows).hasSize(1);
        ItemDailyStat row = rows.get(0);
        assertThat(row.getTrackedItemId()).isEqualTo(item.getId());
        assertThat(row.getStatDate()).isEqualTo(EXPECTED_STAT_DATE);
        assertThat(row.getSource()).isEqualTo(DailyStatSource.YDAY_AVG);
        assertThat(row.getAvgPrice()).isEqualByComparingTo(new BigDecimal("147007.4"));
    }

    @Test
    void repeatedCaptureIsIdempotent() {
        ItemFetchResult result = ItemFetchResult.success(item.getId(), 145500L, 147007.4, FETCHED_AT);
        service.captureYDayAvg(item, result);
        service.captureYDayAvg(item, result);
        assertThat(itemDailyStatRepository.findAll()).hasSize(1);
    }

    @Test
    void nullYDayAvgPriceIsNoOp() {
        service.captureYDayAvg(item, ItemFetchResult.success(item.getId(), 145500L, null, FETCHED_AT));
        assertThat(itemDailyStatRepository.findAll()).isEmpty();
    }

    @Test
    void captureIsFailOpen_repoErrorNeverPropagates() {
        // An unsaved item (id == null) makes the upsert bind a null tracked_item_id and blow up inside
        // the service; fail-open must swallow it so the collection tick is never broken. No row written.
        TrackedItem unsaved = new TrackedItem("BCS-IT-unsaved-" + System.nanoTime(), "x", "40000");
        assertThatNoException().isThrownBy(() ->
                service.captureYDayAvg(unsaved, ItemFetchResult.success(999L, 1L, 500.0, FETCHED_AT)));
        assertThat(itemDailyStatRepository.findAll()).isEmpty();
    }
}
