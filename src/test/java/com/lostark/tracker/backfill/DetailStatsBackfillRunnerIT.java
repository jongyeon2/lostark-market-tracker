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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the detail-Stats backfill (Phase 17.4, plan 03; scope corrected by quick 260707-tzj) on
 * Testcontainers: only AvgPrice&gt;0 days upsert as DETAIL_STATS, re-running stays idempotent, and
 * engraving books are now backfilled too (their real-trade detail element carries the series). The
 * runner is {@code @Profile("!test")} so it is built by hand here with mocked API + rate limiter;
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

        // Default: any other active item left in the shared Testcontainers DB returns empty Stats
        // (no rows, no NPE). Specific items below override this (Mockito: last matching stub wins).
        when(apiClient.getItemDetail(anyLong())).thenReturn(new ItemDetailResponse(List.of()));

        // Material: 14 daily stats, one of them AvgPrice=0 (an unfilled day -> skipped by the guard).
        List<MarketStat> materialStats = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            double avg = (i == 0) ? 0.0 : 1000.0 + i;
            materialStats.add(new MarketStat(BASE.minusDays(i).toString(), avg, 10L));
        }
        when(apiClient.getItemDetail(Long.parseLong(material.getExternalItemId())))
                .thenReturn(new ItemDetailResponse(materialStats));

        // Engraving: getItemDetail already collapsed to the real-trade element, so all 14 days are
        // positive here — the runner must now backfill them (previously engravings were excluded).
        List<MarketStat> engravingStats = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            engravingStats.add(new MarketStat(BASE.minusDays(i).toString(), 145000.0 + i, 900L));
        }
        when(apiClient.getItemDetail(Long.parseLong(engraving.getExternalItemId())))
                .thenReturn(new ItemDetailResponse(engravingStats));
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
    void engravingItemsAreAlsoBackfilled() {
        runner.backfill();

        // The engraving IS fetched (scope now includes all active items, quick 260707-tzj) and its
        // real-trade series (all 14 days positive) is upserted as DETAIL_STATS.
        verify(apiClient).getItemDetail(Long.parseLong(engraving.getExternalItemId()));
        List<ItemDailyStat> rows = itemDailyStatRepository
                .findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(engraving.getId(), BASE.minusDays(13), BASE);
        assertThat(rows).hasSize(14);
        assertThat(rows).allMatch(r -> r.getSource() == DailyStatSource.DETAIL_STATS);
    }
}
