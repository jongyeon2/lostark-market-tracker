package com.lostark.tracker.backfill;

import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.collect.dto.ItemDetailResponse;
import com.lostark.tracker.collect.dto.MarketStat;
import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.ItemDailyStat;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the detail-Stats material backfill (Phase 17.4, plan 03) on Testcontainers: only AvgPrice&gt;0
 * days upsert as DETAIL_STATS, re-running stays idempotent, and non-material items are never fetched.
 * The runner is {@code @Profile("!test")} so it is built by hand here with mocked API + rate limiter;
 * no real API is called.
 */
@SpringBootTest
@ActiveProfiles("test")
class DetailStatsBackfillRunnerIT extends PostgresRedisContainers {

    @Autowired
    ItemDailyStatRepository itemDailyStatRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    private static final LocalDate BASE = LocalDate.of(2026, 7, 7);

    private LostarkApiClient apiClient;
    private RedisTokenBucket rateLimiter;
    private DetailStatsBackfillRunner runner;

    private TrackedItem material;
    private TrackedItem engraving;

    @BeforeEach
    void setUp() {
        itemDailyStatRepository.deleteAll();
        // Numeric external ids (the runner parses them via Long.parseLong); per-run-unique to dodge V3.
        material = trackedItemRepository.save(new TrackedItem(
                "9" + System.nanoTime(), "운명의 파괴석 결정", "50010", null, "재련재료", "MATERIAL"));
        engraving = trackedItemRepository.save(new TrackedItem(
                "8" + System.nanoTime(), "유물 원한 각인서", "40000", null, "각인서", "DEALER"));

        apiClient = mock(LostarkApiClient.class);
        rateLimiter = mock(RedisTokenBucket.class);
        when(rateLimiter.tryAcquire()).thenReturn(true);
        runner = new DetailStatsBackfillRunner(apiClient, trackedItemRepository, itemDailyStatRepository, rateLimiter);

        // 14 daily stats, one of them AvgPrice=0 (the engraving-style unfilled entry -> skipped).
        List<MarketStat> stats = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            double avg = (i == 0) ? 0.0 : 1000.0 + i;
            stats.add(new MarketStat(BASE.minusDays(i).toString(), avg, 10L));
        }
        long materialId = Long.parseLong(material.getExternalItemId());
        when(apiClient.getItemDetail(materialId)).thenReturn(new ItemDetailResponse(stats));
    }

    @AfterEach
    void tidy() {
        itemDailyStatRepository.deleteAll();
        trackedItemRepository.deleteById(material.getId());
        trackedItemRepository.deleteById(engraving.getId());
    }

    @Test
    void backfillsOnlyPositiveAvgDaysAsDetailStats() {
        runner.backfill();

        List<ItemDailyStat> rows = itemDailyStatRepository
                .findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(material.getId(), BASE.minusDays(13), BASE);
        assertThat(rows).hasSize(13); // 14 days minus the single AvgPrice=0 entry
        assertThat(rows).allMatch(r -> r.getSource() == DailyStatSource.DETAIL_STATS);
        assertThat(rows).noneMatch(r -> r.getStatDate().equals(BASE)); // the AvgPrice=0 day (i==0) skipped
    }

    @Test
    void reRunIsIdempotent() {
        runner.backfill();
        runner.backfill();

        List<ItemDailyStat> rows = itemDailyStatRepository
                .findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(material.getId(), BASE.minusDays(13), BASE);
        assertThat(rows).hasSize(13);
    }

    @Test
    void nonMaterialItemsAreNeverFetched() {
        runner.backfill();

        long engravingId = Long.parseLong(engraving.getExternalItemId());
        verify(apiClient, never()).getItemDetail(engravingId);
        // The engraving never contributes rows.
        assertThat(itemDailyStatRepository
                .findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(engraving.getId(), BASE.minusDays(13), BASE))
                .isEmpty();
    }
}
