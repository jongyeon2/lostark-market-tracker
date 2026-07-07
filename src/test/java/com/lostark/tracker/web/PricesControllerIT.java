package com.lostark.tracker.web;

import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.DailyStatPoint;
import com.lostark.tracker.web.dto.TimelineResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 17.4 backfill read merge (BACKFILL-03, D-02): {@code GET /api/items/{id}/prices}
 * returns the window's backfilled daily averages in a SEPARATE {@code backfill} array, distinct from
 * the real-time {@code snapshots}, ascending by stat_date and window-scoped; and an empty window still
 * 200s with {@code backfill: []}. HTTP round-trip on Testcontainers, no key, no network.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PricesControllerIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    ItemDailyStatRepository itemDailyStatRepository;

    // Window: 2026-06-20..2026-06-25 (UTC). KST calendar-day window is 2026-06-20..2026-06-25.
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-06-20T00:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-06-25T00:00:00Z");
    private static final OffsetDateTime SNAP_1 = OffsetDateTime.parse("2026-06-21T15:00:00Z");
    private static final OffsetDateTime SNAP_2 = OffsetDateTime.parse("2026-06-22T15:00:00Z");

    private long itemId;

    @BeforeEach
    void seed() {
        // FK order: clear both children before the parent.
        itemDailyStatRepository.deleteAll();
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();

        TrackedItem item = trackedItemRepository.save(
                new TrackedItem("6861099", "운명의 파괴석 결정", "50010", null, "재련재료", "MATERIAL"));
        itemId = item.getId();

        // Real min-ask snapshots (inside the window).
        priceSnapshotRepository.save(new PriceSnapshot(item, SNAP_1, 1000L, SNAP_1));
        priceSnapshotRepository.save(new PriceSnapshot(item, SNAP_2, 1100L, SNAP_2));

        // Backfill daily averages: two inside the window (mixed sources) + one outside it.
        itemDailyStatRepository.upsertDailyStat(itemId, LocalDate.of(2026, 6, 21),
                new BigDecimal("1050.5"), DailyStatSource.YDAY_AVG.name());
        itemDailyStatRepository.upsertDailyStat(itemId, LocalDate.of(2026, 6, 22),
                new BigDecimal("1075.0"), DailyStatSource.DETAIL_STATS.name());
        itemDailyStatRepository.upsertDailyStat(itemId, LocalDate.of(2026, 6, 30),
                new BigDecimal("9999.0"), DailyStatSource.YDAY_AVG.name());
    }

    @AfterEach
    void tidy() {
        itemDailyStatRepository.deleteAll();
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    @Test
    void backfillIsReturnedAsSeparateWindowScopedAscendingArray() {
        ResponseEntity<TimelineResponse> resp = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                itemId, FROM.toString(), TO.toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        TimelineResponse body = resp.getBody();
        assertThat(body).isNotNull();

        // (a) snapshots hold ONLY the real min-ask points — backfill is not merged in.
        assertThat(body.snapshots()).hasSize(2);
        assertThat(body.snapshots()).extracting(p -> p.minPrice()).containsExactly(1000L, 1100L);

        // (b) backfill holds only the in-window daily averages, ascending by stat_date, sources tagged.
        assertThat(body.backfill()).extracting(DailyStatPoint::statDate).containsExactly("2026-06-21", "2026-06-22");
        assertThat(body.backfill()).extracting(DailyStatPoint::source).containsExactly("YDAY_AVG", "DETAIL_STATS");
        assertThat(body.backfill()).extracting(DailyStatPoint::avgPrice).containsExactly(1050.5, 1075.0);
    }

    @Test
    void emptyBackfillWindowStill200sWithEmptyArray() {
        ResponseEntity<TimelineResponse> resp = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                itemId, "2027-01-01T00:00:00Z", "2027-01-05T00:00:00Z");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().backfill()).isEmpty();
        assertThat(resp.getBody().snapshots()).isEmpty();
    }
}
