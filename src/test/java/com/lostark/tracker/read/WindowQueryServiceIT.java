package com.lostark.tracker.read;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.read.WindowQueryService.WindowResult;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the 4A shared window query (D-06) on Testcontainers: inclusive UTC boundary semantics for
 * BOTH snapshots and events (on-{@code from} and on-{@code to} included, just-outside excluded),
 * ascending snapshot order, and per-item snapshot scoping. These are the exact semantics Phase 5
 * event-impact reuses, so they are pinned here.
 */
@SpringBootTest
class WindowQueryServiceIT extends PostgresRedisContainers {

    @Autowired
    WindowQueryService windowQueryService;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    // 2026-06-22 00:00 KST == 2026-06-21T15:00:00Z (off-by-9h boundary); a 3-hour window.
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-06-21T15:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-06-21T18:00:00Z");

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        gameEventRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    @Test
    void fetchWindowReturnsInclusiveAscendingSnapshotsAndOverlappingEvents() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
        TrackedItem other = trackedItemRepository.save(new TrackedItem("1002", "itemB", "50010"));

        // Snapshots inserted OUT of order to prove the ORDER BY collected_at ASC, not insertion order.
        snap(item, TO, 40);                                          // on `to` -> included
        snap(item, FROM, 20);                                        // on `from` -> included
        snap(item, OffsetDateTime.parse("2026-06-21T16:30:00Z"), 30);// mid -> included
        snap(item, OffsetDateTime.parse("2026-06-21T14:59:00Z"), 10);// before `from` -> excluded
        snap(item, OffsetDateTime.parse("2026-06-21T18:01:00Z"), 50);// after `to` -> excluded
        snap(other, OffsetDateTime.parse("2026-06-21T16:00:00Z"), 99); // different item -> excluded

        // Events: on-from / mid / on-to included; just-outside excluded (inclusive containment, D-05).
        evt(OffsetDateTime.parse("2026-06-21T14:59:00Z"), "before");
        evt(FROM, "onFrom");
        evt(OffsetDateTime.parse("2026-06-21T16:30:00Z"), "mid");
        evt(TO, "onTo");
        evt(OffsetDateTime.parse("2026-06-21T18:01:00Z"), "after");

        WindowResult result = windowQueryService.fetchWindow(item.getId(), FROM, TO);

        // Ascending min_price proves ascending collected_at order (seeded 20<30<40 by time).
        assertThat(result.snapshots())
                .extracting(PriceSnapshot::getMinPrice)
                .containsExactly(20L, 30L, 40L);
        assertThat(result.snapshots())
                .extracting(s -> s.getCollectedAt().toInstant())
                .isSorted();
        assertThat(result.events())
                .extracting(GameEvent::getTitle)
                .containsExactlyInAnyOrder("onFrom", "mid", "onTo");
    }

    private void snap(TrackedItem item, OffsetDateTime collectedAt, long minPrice) {
        priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, minPrice, collectedAt));
    }

    private void evt(OffsetDateTime occurredAt, String title) {
        gameEventRepository.save(new GameEvent(EventType.LOA_ON, title, occurredAt, null));
    }
}
