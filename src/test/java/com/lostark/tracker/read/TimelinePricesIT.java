package com.lostark.tracker.read;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.TimelineResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves {@code GET /api/items/{id}/prices?from=&to=} end-to-end (API-03, Success Criterion 3) on
 * Testcontainers: two distinct arrays (D-04), inclusive event overlap on the {@code to} boundary
 * with a just-after-{@code to} event excluded (D-05), ascending snapshots, UTC boundaries that
 * round-trip without a 9h shift (D-11), a 404 for a missing item, and a 200 with empty arrays for a
 * valid-but-empty window (D-13 empty-range half).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimelinePricesIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    // 2026-06-22 00:00 KST == 2026-06-21T15:00:00Z; a 3-hour window.
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-06-21T15:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-06-21T18:00:00Z");

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        gameEventRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    @Test
    void timelineReturnsTwoArraysWithInclusiveOverlapAndAscendingSnapshots() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
        // Out-of-order inserts to prove ascending output ordering.
        snap(item, TO, 40);
        snap(item, FROM, 20);
        snap(item, OffsetDateTime.parse("2026-06-21T16:30:00Z"), 30);
        snap(item, OffsetDateTime.parse("2026-06-21T18:01:00Z"), 50); // outside -> excluded
        // Event exactly on `to` included; one just after `to` excluded (inclusive, D-05).
        evt(TO, "onTo");
        evt(OffsetDateTime.parse("2026-06-21T18:01:00Z"), "after");

        ResponseEntity<TimelineResponse> resp = getTimeline(item.getId(), FROM, TO);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        TimelineResponse body = resp.getBody();
        assertThat(body).isNotNull();
        // Snapshots: in-window only, ascending by collected_at (20 -> 30 -> 40).
        assertThat(body.snapshots()).extracting(p -> p.minPrice()).containsExactly(20L, 30L, 40L);
        // The on-`from` boundary snapshot round-trips as the SAME UTC instant (off-by-9h guard, D-11).
        assertThat(body.snapshots().get(0).collectedAt().toInstant()).isEqualTo(FROM.toInstant());
        // Events: independent array, inclusive on `to`, excludes just-after.
        assertThat(body.events()).extracting(e -> e.title()).containsExactly("onTo");
        assertThat(body.events().get(0).occurredAt().toInstant()).isEqualTo(TO.toInstant());
    }

    @Test
    void missingItemReturns404() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", String.class,
                999999L, FROM.toString(), TO.toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("\"status\":404");
    }

    @Test
    void validButEmptyWindowReturns200WithEmptyArrays() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
        // Item exists but has no snapshots/events in (or out of) the window.

        ResponseEntity<TimelineResponse> resp = getTimeline(item.getId(), FROM, TO);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().snapshots()).isEmpty();
        assertThat(resp.getBody().events()).isEmpty();
    }

    private ResponseEntity<TimelineResponse> getTimeline(long itemId, OffsetDateTime from, OffsetDateTime to) {
        return rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                itemId, from.toString(), to.toString());
    }

    private void snap(TrackedItem item, OffsetDateTime collectedAt, long minPrice) {
        priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, minPrice, collectedAt));
    }

    private void evt(OffsetDateTime occurredAt, String title) {
        gameEventRepository.save(new GameEvent(EventType.MAJOR_UPDATE, title, occurredAt, null));
    }
}
