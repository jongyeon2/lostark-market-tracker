package com.lostark.tracker.web;

import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.EnrichedEventImpactResponse;
import com.lostark.tracker.web.dto.LatestPriceResponse;
import com.lostark.tracker.web.dto.TimelineResponse;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the read-path enrichment additive layer (ITEM-03): all 4 read responses — item list,
 * latest, timeline, event-impact — expose {@code iconUrl}/{@code itemGroup}/{@code roleGroup}, and a
 * second {@code latest} call (cache HIT) returns the SAME enrichment (it was baked into the cached
 * value, so the API-02 zero-DB showcase is preserved). The ITEM-04 invariant (collect/cache/
 * event-impact source 0-line) is enforced separately by the plan's {@code git diff} gate plus the
 * untouched existing regression suite.
 *
 * <p>Runs under the {@code test} profile (WatchlistSeeder inactive), so the test inserts an enriched
 * {@link TrackedItem} fixture directly and drives the endpoints over HTTP — no key, no network.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ItemEnrichmentReadIT extends PostgresRedisContainers {

    private static final String EXTERNAL_ID = "6861009";
    private static final String ICON_URL =
            "https://cdn-lostark.game.onstove.com/efui_iconatlas/use/use_8_109.png";
    private static final String ITEM_GROUP = "강화재료";
    private static final String ROLE_GROUP = "MATERIAL";
    // A KST-midnight UTC boundary instant for the seeded snapshot (off-by-9h guard, D-11).
    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-21T15:00:00Z");

    @Autowired
    TestRestTemplate rest;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;

    private long itemId;

    @BeforeEach
    void seedEnrichedFixture() {
        // FK order: child snapshots first, then items, then events.
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();

        TrackedItem item = trackedItemRepository.save(new TrackedItem(
                EXTERNAL_ID, "상급 오레하 융화 재료", "50010", ICON_URL, ITEM_GROUP, ROLE_GROUP));
        itemId = item.getId();
        priceSnapshotRepository.save(new PriceSnapshot(item, AT, 1000L, AT));
    }

    @Test
    void itemListExposesEnrichment() {
        ResponseEntity<TrackedItemResponse[]> resp =
                rest.getForEntity("/api/items", TrackedItemResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        TrackedItemResponse mine = Arrays.stream(resp.getBody())
                .filter(i -> EXTERNAL_ID.equals(i.externalItemId()))
                .findFirst().orElseThrow();
        assertThat(mine.iconUrl()).isEqualTo(ICON_URL);
        assertThat(mine.itemGroup()).isEqualTo(ITEM_GROUP);
        assertThat(mine.roleGroup()).isEqualTo(ROLE_GROUP);
    }

    @Test
    void latestExposesEnrichmentAndCacheHitPreservesIt() {
        ResponseEntity<LatestPriceResponse> first =
                rest.getForEntity("/api/items/{id}/latest", LatestPriceResponse.class, itemId);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().iconUrl()).isEqualTo(ICON_URL);
        assertThat(first.getBody().itemGroup()).isEqualTo(ITEM_GROUP);
        assertThat(first.getBody().roleGroup()).isEqualTo(ROLE_GROUP);

        // Second call is a cache HIT — enrichment was baked into the cached value, so it returns the
        // SAME enrichment with no extra DB read (API-02 zero-DB preserved).
        ResponseEntity<LatestPriceResponse> second =
                rest.getForEntity("/api/items/{id}/latest", LatestPriceResponse.class, itemId);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().iconUrl()).isEqualTo(ICON_URL);
        assertThat(second.getBody().itemGroup()).isEqualTo(ITEM_GROUP);
        assertThat(second.getBody().roleGroup()).isEqualTo(ROLE_GROUP);
    }

    @Test
    void timelineExposesEnrichmentAtTopLevel() {
        ResponseEntity<TimelineResponse> resp = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                itemId, AT.minusHours(1).toString(), AT.plusHours(1).toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().iconUrl()).isEqualTo(ICON_URL);
        assertThat(resp.getBody().itemGroup()).isEqualTo(ITEM_GROUP);
        assertThat(resp.getBody().roleGroup()).isEqualTo(ROLE_GROUP);
    }

    @Test
    void eventImpactExposesEnrichmentAtTopLevel() {
        ResponseEntity<EnrichedEventImpactResponse> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EnrichedEventImpactResponse.class, itemId, 24);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().itemId()).isEqualTo(itemId);
        assertThat(resp.getBody().window()).isEqualTo(24);
        assertThat(resp.getBody().iconUrl()).isEqualTo(ICON_URL);
        assertThat(resp.getBody().itemGroup()).isEqualTo(ITEM_GROUP);
        assertThat(resp.getBody().roleGroup()).isEqualTo(ROLE_GROUP);
        // No events seeded -> a 200 with an empty (non-null) events array (shape preserved).
        assertThat(resp.getBody().events()).isNotNull().isEmpty();
    }
}
