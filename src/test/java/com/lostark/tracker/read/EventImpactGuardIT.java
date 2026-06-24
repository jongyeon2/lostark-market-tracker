package com.lostark.tracker.read;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.EventImpactItem;
import com.lostark.tracker.web.dto.EventImpactResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the IMPACT-02 data-quality guards on top of the 05-01 event-impact metric (D-02/D-03/D-04/D-05).
 * The window is deliberately generous (24h) so the sparse/stale cases are about anchor DISTANCE from
 * the event, not window bounds:
 *
 * <ul>
 *   <li>Sufficiency (D-02): a side with zero in-window snapshots -> insufficient_data, that anchor null.</li>
 *   <li>Staleness (D-03/D-04): an anchor &gt;30min from the event -> insufficient_data, no change_rate.</li>
 *   <li>Two distinguishable reasons (D-05): a stale-but-present anchor still REPORTS its time
 *       (non-null), while a sparse side reports null — "stale data" vs. "no data".</li>
 *   <li>The 30-minute boundary is INCLUSIVE: exactly 30min is fresh, 30min+1s is stale (D-03).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventImpactGuardIT extends PostgresRedisContainers {

    // 2026-06-22 00:00 KST == 2026-06-21T15:00:00Z (off-by-9h boundary guard).
    private static final OffsetDateTime E = OffsetDateTime.parse("2026-06-21T15:00:00Z");

    @Autowired
    TestRestTemplate rest;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
    }

    @Test
    void preOnlyWindowIsInsufficientWithNullPostAnchor() {
        TrackedItem item = saveItem();
        saveEvent(E);
        saveSnap(item, E.minusMinutes(10), 1000L); // pre only; nothing at-or-after E

        EventImpactItem event = single(item);

        assertThat(event.status()).isEqualTo("insufficient_data");
        assertThat(event.preAnchorAt()).isNotNull();
        assertThat(event.preAnchorAt().toInstant()).isEqualTo(E.minusMinutes(10).toInstant());
        assertThat(event.postAnchorAt()).isNull(); // sparse side -> null (D-05 "no data")
        assertThat(event.changeRate()).isNull();
    }

    @Test
    void postOnlyWindowIsInsufficientWithNullPreAnchor() {
        TrackedItem item = saveItem();
        saveEvent(E);
        saveSnap(item, E.plusMinutes(10), 1200L); // post only; nothing at-or-before E

        EventImpactItem event = single(item);

        assertThat(event.status()).isEqualTo("insufficient_data");
        assertThat(event.postAnchorAt()).isNotNull();
        assertThat(event.postAnchorAt().toInstant()).isEqualTo(E.plusMinutes(10).toInstant());
        assertThat(event.preAnchorAt()).isNull();
        assertThat(event.changeRate()).isNull();
    }

    @Test
    void stalePreAnchorIsInsufficientButReportsTheAnchorTime() {
        TrackedItem item = saveItem();
        saveEvent(E);
        saveSnap(item, E.minusMinutes(45), 1000L); // in-window but stale (>30min)
        saveSnap(item, E.plusMinutes(10), 1200L);  // post fresh

        EventImpactItem event = single(item);

        assertThat(event.status()).isEqualTo("insufficient_data");
        // D-05: stale != sparse — the discovered anchor time IS reported (non-null).
        assertThat(event.preAnchorAt()).isNotNull();
        assertThat(event.preAnchorAt().toInstant()).isEqualTo(E.minusMinutes(45).toInstant());
        assertThat(event.changeRate()).isNull();
    }

    @Test
    void stalePostAnchorIsInsufficientButReportsTheAnchorTime() {
        TrackedItem item = saveItem();
        saveEvent(E);
        saveSnap(item, E.minusMinutes(10), 1000L); // pre fresh
        saveSnap(item, E.plusMinutes(45), 1200L);  // in-window but stale

        EventImpactItem event = single(item);

        assertThat(event.status()).isEqualTo("insufficient_data");
        assertThat(event.postAnchorAt()).isNotNull();
        assertThat(event.postAnchorAt().toInstant()).isEqualTo(E.plusMinutes(45).toInstant());
        assertThat(event.changeRate()).isNull();
    }

    @Test
    void anchorsExactlyThirtyMinutesAreFreshAndComputeChangeRate() {
        TrackedItem item = saveItem();
        saveEvent(E);
        saveSnap(item, E.minusMinutes(30), 1000L); // exactly 30min -> inclusive, fresh
        saveSnap(item, E.plusMinutes(30), 1200L);

        EventImpactItem event = single(item);

        assertThat(event.status()).isEqualTo("ok");
        assertThat(event.changeRate()).isEqualByComparingTo("0.2"); // 1200/1000 - 1
    }

    @Test
    void anchorOneSecondPastThirtyMinutesIsStale() {
        TrackedItem item = saveItem();
        saveEvent(E);
        saveSnap(item, E.minusMinutes(30).minusSeconds(1), 1000L); // 30min+1s -> stale
        saveSnap(item, E.plusMinutes(10), 1200L);                  // post fresh

        EventImpactItem event = single(item);

        assertThat(event.status()).isEqualTo("insufficient_data");
        assertThat(event.preAnchorAt()).isNotNull(); // reported (stale, not sparse)
        assertThat(event.changeRate()).isNull();
    }

    private EventImpactItem single(TrackedItem item) {
        ResponseEntity<EventImpactResponse> resp = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EventImpactResponse.class, item.getId(), 24);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().events()).hasSize(1);
        return resp.getBody().events().get(0);
    }

    private TrackedItem saveItem() {
        return trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
    }

    private void saveEvent(OffsetDateTime occurredAt) {
        gameEventRepository.save(new GameEvent(EventType.MAJOR_UPDATE, "이벤트", occurredAt, null));
    }

    private void saveSnap(TrackedItem item, OffsetDateTime collectedAt, Long minPrice) {
        priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, minPrice, collectedAt));
    }
}
