package com.lostark.tracker.collect;

import com.lostark.tracker.backfill.BackfillCaptureService;
import com.lostark.tracker.cache.LatestPriceCache;
import com.lostark.tracker.collect.dto.MarketItem;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.domain.CollectionRun;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.health.CollectionHeartbeat;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the scheduled tick end-to-end (COLL-01/02, Success Criteria 1/4/5) on Testcontainers:
 * shared tick-normalized collected_at, idempotent re-run (UNIQUE), hanging-item timeout isolation,
 * and a recorded collection_run with counts. The clock is pinned so collected_at is deterministic;
 * LostarkApiClient is mocked so no real HTTP happens.
 */
@SpringBootTest
class PriceCollectionIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkApiClient apiClient;

    @Autowired
    ItemFetchService itemFetchService;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    CollectionRunRepository collectionRunRepository;
    @Autowired
    LatestPriceCache latestPriceCache;
    @Autowired
    BackfillCaptureService backfillCaptureService;
    // Mocked so the tick's dead-man ping is captured (and no real HTTP happens) — MONITORING §9.
    @MockitoBean
    CollectionHeartbeat heartbeat;

    // Pinned clock -> collected_at == 2026-06-22T09:15:00Z for every tick in this test.
    private static final Instant FIXED = Instant.parse("2026-06-22T09:15:30Z");
    private static final OffsetDateTime EXPECTED_COLLECTED_AT = OffsetDateTime.parse("2026-06-22T09:15:00Z");

    private PriceCollector collector(long perCallSeconds, long overallSeconds) {
        return new PriceCollector(itemFetchService, trackedItemRepository, priceSnapshotRepository,
                collectionRunRepository, latestPriceCache, backfillCaptureService, heartbeat,
                Clock.fixed(FIXED, ZoneOffset.UTC), perCallSeconds, overallSeconds);
    }

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        collectionRunRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    private TrackedItem seed(String externalId, String name) {
        return trackedItemRepository.save(new TrackedItem(externalId, name, "50010"));
    }

    private static MarketItemsResponse oneItem(long id, long price) {
        return new MarketItemsResponse(1, 10, 1, List.of(new MarketItem(id, "item" + id, price, null)));
    }

    @Test
    void tickWritesOneSnapshotPerItemAllSharingCollectedAtAndRecordsRun() {
        seed("1001", "itemA");
        seed("1002", "itemB");
        seed("1003", "itemC");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA"))).thenReturn(oneItem(1001, 100));
        when(apiClient.searchMarketItems(eq("50010"), eq("itemB"))).thenReturn(oneItem(1002, 200));
        when(apiClient.searchMarketItems(eq("50010"), eq("itemC"))).thenReturn(oneItem(1003, 300));

        collector(2, 3).collectTick();

        List<PriceSnapshot> snaps = priceSnapshotRepository.findAll();
        assertThat(snaps).hasSize(3);
        assertThat(snaps).allSatisfy(s ->
                assertThat(s.getCollectedAt().toInstant()).isEqualTo(EXPECTED_COLLECTED_AT.toInstant()));
        assertThat(snaps).allSatisfy(s -> assertThat(s.getFetchedAt()).isNotNull());

        CollectionRun run = latestRun();
        assertThat(run.getStartedAt()).isNotNull();
        assertThat(run.getFinishedAt()).isNotNull();
        assertThat(run.getItemsAttempted()).isEqualTo(3);
        assertThat(run.getItemsSucceeded()).isEqualTo(3);
        assertThat(run.getItemsFailed()).isEqualTo(0);
        assertThat(run.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void reRunningSameTickWritesNoDuplicateSnapshot() {
        seed("1001", "itemA");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA"))).thenReturn(oneItem(1001, 100));

        collector(2, 3).collectTick();
        collector(2, 3).collectTick(); // same pinned clock -> same collected_at -> UNIQUE blocks dup

        assertThat(priceSnapshotRepository.findAll()).hasSize(1);
    }

    @Test
    void hangingItemDoesNotBlockOthersAndIsCountedFailed() {
        seed("1001", "itemA");
        seed("1002", "itemB"); // this one hangs
        seed("1003", "itemC");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA"))).thenReturn(oneItem(1001, 100));
        when(apiClient.searchMarketItems(eq("50010"), eq("itemC"))).thenReturn(oneItem(1003, 300));
        when(apiClient.searchMarketItems(eq("50010"), eq("itemB"))).thenAnswer(inv -> {
            Thread.sleep(10_000); // far beyond the per-call timeout
            return oneItem(1002, 200);
        });

        collector(2, 4).collectTick(); // per-call 2s bounds the hang; overall 4s backstop

        assertThat(priceSnapshotRepository.findAll()).hasSize(2);
        Long idA = trackedItemRepository.findByExternalItemId("1001").orElseThrow().getId();
        Long idB = trackedItemRepository.findByExternalItemId("1002").orElseThrow().getId();
        Long idC = trackedItemRepository.findByExternalItemId("1003").orElseThrow().getId();
        assertThat(priceSnapshotRepository.existsByTrackedItem_IdAndCollectedAt(idA, EXPECTED_COLLECTED_AT)).isTrue();
        assertThat(priceSnapshotRepository.existsByTrackedItem_IdAndCollectedAt(idC, EXPECTED_COLLECTED_AT)).isTrue();
        assertThat(priceSnapshotRepository.existsByTrackedItem_IdAndCollectedAt(idB, EXPECTED_COLLECTED_AT)).isFalse();

        CollectionRun run = latestRun();
        assertThat(run.getItemsAttempted()).isEqualTo(3);
        assertThat(run.getItemsSucceeded()).isEqualTo(2);
        assertThat(run.getItemsFailed()).isEqualTo(1);
        assertThat(run.getStatus()).isEqualTo("PARTIAL_SUCCESS");
    }

    @Test
    void tickReportsSucceededCountToHeartbeat() {
        seed("1001", "itemA");
        seed("1002", "itemB");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA"))).thenReturn(oneItem(1001, 100));
        when(apiClient.searchMarketItems(eq("50010"), eq("itemB"))).thenReturn(oneItem(1002, 200));

        collector(2, 3).collectTick();

        // The dead-man switch is fed the succeeded count for this tick (MONITORING §4).
        verify(heartbeat).report(2);
    }

    @Test
    void emptyWatchlistReportsZeroToHeartbeatDespiteFalseSuccessStatus() {
        // No active items -> succeeded == items.size() == 0 records a *false* SUCCESS run (the known
        // application-prod.yml:6-8 edge). The heartbeat must still report 0 so it pings /fail, not the
        // healthy URL — this is exactly why the ping rule keys off `succeeded`, not `status` (§4).
        collector(2, 3).collectTick();

        CollectionRun run = latestRun();
        assertThat(run.getItemsAttempted()).isEqualTo(0);
        assertThat(run.getStatus()).isEqualTo("SUCCESS"); // the false-positive we refuse to trust
        verify(heartbeat).report(0);                       // ...reported as a failure regardless
    }

    private CollectionRun latestRun() {
        List<CollectionRun> runs = collectionRunRepository.findAll();
        return runs.get(runs.size() - 1);
    }
}
