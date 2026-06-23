package com.lostark.tracker.read;

import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The 4A shared window query (D-06, design decision 4A / Task T6).
 *
 * <p>For one {@code [from, to]} window it composes exactly TWO read queries — the item's snapshots
 * (ascending) and the events overlapping the window — and returns them bundled, doing NO
 * item-existence check and NO DTO mapping. Keeping it pure data-access (two queries, no per-row
 * N+1) is deliberate: the timeline endpoint maps the bundle here, and Phase 5 event-impact reuses
 * this exact method to gather pre/post-event snapshots, so the inclusive UTC boundary semantics are
 * defined and tested in one place.
 */
@Service
public class WindowQueryService {

    /** The window bundle: the item's window snapshots (ascending) and the events overlapping it. */
    public record WindowResult(List<PriceSnapshot> snapshots, List<GameEvent> events) {
    }

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final GameEventRepository gameEventRepository;

    public WindowQueryService(PriceSnapshotRepository priceSnapshotRepository,
                              GameEventRepository gameEventRepository) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.gameEventRepository = gameEventRepository;
    }

    public WindowResult fetchWindow(long itemId, OffsetDateTime from, OffsetDateTime to) {
        List<PriceSnapshot> snapshots = priceSnapshotRepository
                .findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc(itemId, from, to);
        List<GameEvent> events = gameEventRepository.findByOccurredAtBetween(from, to);
        return new WindowResult(snapshots, events);
    }
}
