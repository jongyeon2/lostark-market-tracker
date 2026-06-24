package com.lostark.tracker.read;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.AdminAuth;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.EventImpactResponse;
import com.lostark.tracker.web.dto.GameEventRequest;
import com.lostark.tracker.web.dto.GameEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Proves the event-impact v1 metric (IMPACT-01, D-01/D-06/D-07/D-08/D-09) end-to-end on
 * Testcontainers: the §79 anchor single-snapshot delta {@code change_rate = post/pre - 1} from
 * {@code min_price}, occurred_at-desc ordering, the empty-events 200, the missing-item 404, the full
 * window-param 400 matrix, the N+1 batch guard (snapshot finder called exactly once for N>=2 events),
 * and the admin-write -> read-correlation E2E slice. Happy-path anchors sit within 30min of the event
 * so they survive 05-02's staleness gate with zero retrofit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventImpactIT extends PostgresRedisContainers {

    // 2026-06-22 00:00 KST == 2026-06-21T15:00:00Z (off-by-9h boundary guard).
    private static final OffsetDateTime E = OffsetDateTime.parse("2026-06-21T15:00:00Z");

    @Autowired
    TestRestTemplate rest;
    @MockitoSpyBean
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    @Value("${admin.api.secret:test-admin-secret}")
    String adminSecret;

    @BeforeEach
    void clean() {
        // FK order: child snapshots first, then items, then events (events have no FK but clean all three).
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
    }

    @Test
    void happyPathComputesAnchorDeltaChangeRate() {
        TrackedItem item = saveItem();
        saveEvent(EventType.MAJOR_UPDATE, "여름 업데이트", E);
        saveSnap(item, E.minusMinutes(10), 1000L); // pre anchor (within 30min -> stays ok after 05-02)
        saveSnap(item, E.plusMinutes(10), 1200L);  // post anchor

        ResponseEntity<EventImpactResponse> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EventImpactResponse.class, item.getId(), 24);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().itemId()).isEqualTo(item.getId());
        assertThat(resp.getBody().window()).isEqualTo(24);
        assertThat(resp.getBody().events()).hasSize(1);

        var event = resp.getBody().events().get(0);
        assertThat(event.status()).isEqualTo("ok");
        assertThat(event.eventType()).isEqualTo(EventType.MAJOR_UPDATE);
        assertThat(event.prePrice()).isEqualTo(1000L);
        assertThat(event.postPrice()).isEqualTo(1200L);
        assertThat(event.preAnchorAt().toInstant()).isEqualTo(E.minusMinutes(10).toInstant());
        assertThat(event.postAnchorAt().toInstant()).isEqualTo(E.plusMinutes(10).toInstant());
        // 1200 / 1000 - 1 = 0.2 (a temporal correlation, not a claimed cause).
        assertThat(event.changeRate()).isEqualByComparingTo("0.2");
    }

    @Test
    void eventsAreReturnedNewestFirst() {
        TrackedItem item = saveItem();
        saveEvent(EventType.LOA_ON, "older", E);
        saveEvent(EventType.SEASON_END, "newer", E.plusHours(48));

        ResponseEntity<EventImpactResponse> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EventImpactResponse.class, item.getId(), 24);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().events())
                .extracting(i -> i.occurredAt().toInstant())
                .containsExactly(E.plusHours(48).toInstant(), E.toInstant());
    }

    @Test
    void existingItemWithNoEventsReturns200EmptyArray() {
        TrackedItem item = saveItem();

        ResponseEntity<EventImpactResponse> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EventImpactResponse.class, item.getId(), 24);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().events()).isEmpty();
    }

    @Test
    void missingItemReturns404Contract() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", String.class, 999999L, 24);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("\"status\":404");
        assertThat(resp.getBody()).contains("\"error\":\"Not Found\"");
    }

    @Test
    void zeroWindowReturns400() {
        long id = saveItem().getId();
        assertThat(getRaw(id, "0").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getRaw(id, "0").getBody()).contains("\"status\":400");
    }

    @Test
    void negativeWindowReturns400() {
        long id = saveItem().getId();
        assertThat(getRaw(id, "-1").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getRaw(id, "-1").getBody()).contains("\"status\":400");
    }

    @Test
    void windowAboveCapReturns400() {
        long id = saveItem().getId();
        assertThat(getRaw(id, "200").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getRaw(id, "200").getBody()).contains("\"status\":400");
    }

    @Test
    void omittedWindowReturns400() {
        long id = saveItem().getId();
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/items/{id}/event-impact", String.class, id);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"status\":400");
    }

    @Test
    void nonIntegerWindowReturns400() {
        long id = saveItem().getId();
        assertThat(getRaw(id, "abc").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getRaw(id, "abc").getBody()).contains("\"status\":400");
    }

    @Test
    void snapshotRangeFinderIsCalledExactlyOnceForMultipleEvents() {
        TrackedItem item = saveItem();
        // Two events with their own bracketing snapshots; a single batch read must cover both.
        saveEvent(EventType.LOA_ON, "e1", E);
        saveSnap(item, E.minusMinutes(10), 1000L);
        saveSnap(item, E.plusMinutes(10), 1100L);
        saveEvent(EventType.SEASON_END, "e2", E.plusHours(48));
        saveSnap(item, E.plusHours(48).minusMinutes(10), 2000L);
        saveSnap(item, E.plusHours(48).plusMinutes(10), 2200L);

        rest.getForEntity("/api/items/{id}/event-impact?window={w}",
                EventImpactResponse.class, item.getId(), 24);

        // D-09 / design T7: N events -> ONE snapshot range read, never 2N.
        verify(priceSnapshotRepository, times(1))
                .findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc(anyLong(), any(), any());
    }

    @Test
    void adminRegisteredEventSurfacesInEventImpact() {
        // E2E (eng-review): admin registers an event through the authenticated write path, snapshots
        // bracket it, then event-impact returns the computed change_rate anchored on the admin occurred_at.
        TrackedItem item = saveItem();
        GameEventRequest req = new GameEventRequest(EventType.BALANCE_PATCH, "밸런스 패치", E, "5.0 패치");
        ResponseEntity<GameEventResponse> created = rest.exchange(
                "/api/admin/events", HttpMethod.POST, AdminAuth.entity(req, adminSecret), GameEventResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        saveSnap(item, E.minusMinutes(10), 500L);
        saveSnap(item, E.plusMinutes(10), 750L);

        ResponseEntity<EventImpactResponse> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EventImpactResponse.class, item.getId(), 24);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().events()).hasSize(1);
        var event = resp.getBody().events().get(0);
        assertThat(event.status()).isEqualTo("ok");
        assertThat(event.title()).isEqualTo("밸런스 패치");
        // 750 / 500 - 1 = 0.5
        assertThat(event.changeRate()).isEqualByComparingTo("0.5");
    }

    private ResponseEntity<String> getRaw(long itemId, String window) {
        return rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", String.class, itemId, window);
    }

    private TrackedItem saveItem() {
        return trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
    }

    private void saveEvent(EventType type, String title, OffsetDateTime occurredAt) {
        gameEventRepository.save(new GameEvent(type, title, occurredAt, null));
    }

    private void saveSnap(TrackedItem item, OffsetDateTime collectedAt, Long minPrice) {
        priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, minPrice, collectedAt));
    }
}
