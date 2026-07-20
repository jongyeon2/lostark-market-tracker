package com.lostark.tracker.repository;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
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
     * The single batch source for Phase 5 event-impact (D-06, D-09), filtered by type and PAGED.
     * {@code game_event} is global (not per-item), so event-impact reads its slice ONCE here and
     * derives each event's anchors in memory, rather than issuing a per-event window query.
     *
     * <p>Replaced {@code findAllByOrderByOccurredAtDesc()} (2026-07-20). That method returned EVERY
     * event, and the endpoint returned every result — at 10k events the response is 3~4MB and the
     * client renders 10k rows. The unbounded finder is deleted rather than left beside this one: a
     * live "read them all" path invites the same mistake again.
     *
     * <p>Callers with no type filter pass ALL {@link EventType} values rather than null, so there is
     * one query path instead of a nullable branch. {@code Page.getTotalElements()} yields the
     * filtered total for free, which the response reports as {@code totalCount} — the client needs it
     * to say "전체 N건 중 M건" honestly and to know when 더 보기 is exhausted.
     *
     * <p>Sort direction on {@code occurredAt} comes from the {@link Pageable}, so newest-first and
     * oldest-first share this one finder.
     */
    Page<GameEvent> findByEventTypeIn(Collection<EventType> types, Pageable pageable);
}
