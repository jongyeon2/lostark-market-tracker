package com.lostark.tracker.read;

import com.lostark.tracker.cache.LatestPriceCache;
import com.lostark.tracker.collect.ItemFetchService;
import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.collect.PriceCollector;
import com.lostark.tracker.collect.dto.MarketItem;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.LatestPriceResponse;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the latest-price cache-aside end-to-end on Testcontainers (Postgres + Redis), API-02 /
 * Success Criterion 2 and D-01:
 *
 * <ul>
 *   <li><b>Cache hit = 0 DB reads</b> — two consecutive {@code GET /latest} call the newest-snapshot
 *       finder exactly ONCE (a {@link MockitoSpyBean} on the repository counts the reads). The
 *       office-hours showcase.</li>
 *   <li><b>Evict-on-write</b> — after a read caches a price, a collection tick that persists a newer
 *       snapshot evicts the key, so the next read reflects the new price, not the stale cache.</li>
 *   <li><b>404 contract</b> — a missing item and an existing item with no snapshot both 404.</li>
 * </ul>
 *
 * The snapshot instant is a KST-midnight UTC boundary (2026-06-22 00:00 KST == 2026-06-21T15:00:00Z)
 * so the round-trip through Redis and JSON is asserted as the SAME UTC instant (off-by-9h guard, D-11).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LatestPriceCacheIT extends PostgresRedisContainers {

    @MockitoSpyBean
    PriceSnapshotRepository priceSnapshotRepository;
    @MockitoBean
    LostarkApiClient apiClient;

    @Autowired
    TestRestTemplate rest;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    CollectionRunRepository collectionRunRepository;
    @Autowired
    LatestPriceCache latestPriceCache;
    @Autowired
    ItemFetchService itemFetchService;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    // KST-midnight as the snapshot instant guards off-by-9h: 2026-06-22 00:00 KST == 2026-06-21T15:00:00Z.
    private static final OffsetDateTime COLLECTED_AT_1 = OffsetDateTime.parse("2026-06-21T15:00:00Z");
    // A later tick (the collector truncates collected_at to the minute) -> 2026-06-21T15:10:00Z.
    private static final Instant T2 = Instant.parse("2026-06-21T15:10:30Z");
    private static final OffsetDateTime COLLECTED_AT_2 = OffsetDateTime.parse("2026-06-21T15:10:00Z");

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        collectionRunRepository.deleteAll();
        trackedItemRepository.deleteAll();
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushDb(); // drop any latest-price keys + token-bucket state between tests
        }
        Mockito.clearInvocations(priceSnapshotRepository);
    }

    @Test
    void cacheHitServesSecondReadWithZeroDbReads() {
        TrackedItem item = seedItem("1001", "itemA");
        seedSnapshot(item, COLLECTED_AT_1, 100);
        Mockito.clearInvocations(priceSnapshotRepository); // ignore the seed save — count only read-path calls

        ResponseEntity<LatestPriceResponse> first = getLatest(item.getId());
        ResponseEntity<LatestPriceResponse> second = getLatest(item.getId());

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().itemId()).isEqualTo(item.getId());
        assertThat(second.getBody().minPrice()).isEqualTo(100L);
        // Same UTC instant survives the Redis round-trip (off-by-9h guard, D-11).
        assertThat(second.getBody().collectedAt().toInstant()).isEqualTo(COLLECTED_AT_1.toInstant());
        // Cache hit = 0 second DB read: the newest-snapshot finder runs exactly ONCE across two reads.
        verify(priceSnapshotRepository, times(1))
                .findTopByTrackedItem_IdOrderByCollectedAtDesc(item.getId());
    }

    @Test
    void evictOnWriteMakesNextReadReflectTheNewSnapshot() {
        TrackedItem item = seedItem("1001", "itemA");
        seedSnapshot(item, COLLECTED_AT_1, 100);

        // First read caches price 100.
        assertThat(getLatest(item.getId()).getBody().minPrice()).isEqualTo(100L);

        // A collection tick persists a NEWER snapshot (price 250) -> evict-on-write drops the cache key.
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA"))).thenReturn(oneItem(1001, 250));
        newCollectorAt(T2).collectTick();

        // Next read is now a miss -> reflects the new price, not the stale cached 100.
        LatestPriceResponse after = getLatest(item.getId()).getBody();
        assertThat(after).isNotNull();
        assertThat(after.minPrice()).isEqualTo(250L);
        assertThat(after.collectedAt().toInstant()).isEqualTo(COLLECTED_AT_2.toInstant());
    }

    @Test
    void missingItemReturns404() {
        ResponseEntity<String> resp = rest.getForEntity("/api/items/999999/latest", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("\"status\":404");
    }

    @Test
    void existingItemWithoutSnapshotReturns404() {
        TrackedItem item = seedItem("1002", "itemB");

        ResponseEntity<String> resp =
                rest.getForEntity("/api/items/" + item.getId() + "/latest", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("\"status\":404");
    }

    @Test
    void listReturnsActiveItemsOnlyWithPublicFields() {
        TrackedItem active = seedItem("1001", "itemA");
        TrackedItem inactive = seedItem("1002", "itemB");
        inactive.setActive(false);
        trackedItemRepository.save(inactive);

        ResponseEntity<TrackedItemResponse[]> resp =
                rest.getForEntity("/api/items", TrackedItemResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // Only the active item is listed (API-01, Success Criterion 1) — the inactive item is excluded.
        assertThat(resp.getBody()).extracting(TrackedItemResponse::externalItemId).containsExactly("1001");
        TrackedItemResponse only = resp.getBody()[0];
        assertThat(only.id()).isEqualTo(active.getId());
        assertThat(only.displayName()).isEqualTo("itemA");
    }

    private TrackedItem seedItem(String externalId, String name) {
        return trackedItemRepository.save(new TrackedItem(externalId, name, "50010"));
    }

    private PriceSnapshot seedSnapshot(TrackedItem item, OffsetDateTime collectedAt, long minPrice) {
        return priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, minPrice, collectedAt));
    }

    private ResponseEntity<LatestPriceResponse> getLatest(long itemId) {
        return rest.getForEntity("/api/items/" + itemId + "/latest", LatestPriceResponse.class);
    }

    private PriceCollector newCollectorAt(Instant instant) {
        return new PriceCollector(itemFetchService, trackedItemRepository, priceSnapshotRepository,
                collectionRunRepository, latestPriceCache, Clock.fixed(instant, ZoneOffset.UTC), 2, 3);
    }

    private static MarketItemsResponse oneItem(long id, long price) {
        return new MarketItemsResponse(1, 10, 1, List.of(new MarketItem(id, "item" + id, price)));
    }
}
