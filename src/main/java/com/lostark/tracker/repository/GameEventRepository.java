package com.lostark.tracker.repository;

import com.lostark.tracker.domain.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read access to admin-curated {@code game_event}s (CRUD lands in Phase 4). The window finder backs
 * the timeline overlay and the Phase 5 event-impact correlation through the shared
 * {@link com.lostark.tracker.read.WindowQueryService}.
 */
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    /**
     * Events overlapping a window by inclusive containment on the single {@code occurred_at} instant:
     * {@code from <= occurred_at <= to} (Spring Data {@code Between} is inclusive on both bounds, D-05).
     * Boundaries are UTC instants — no timezone conversion.
     */
    List<GameEvent> findByOccurredAtBetween(OffsetDateTime from, OffsetDateTime to);

    /**
     * All events newest-first — the single batch source for Phase 5 event-impact (D-06, D-09).
     * {@code game_event} is global (not per-item), so event-impact reads every event ONCE here and
     * derives each one's anchors in memory, rather than issuing a per-event window query.
     */
    List<GameEvent> findAllByOrderByOccurredAtDesc();
}
