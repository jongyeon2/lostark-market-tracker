package com.lostark.tracker.read;

import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.web.dto.EventImpactItem;
import com.lostark.tracker.web.dto.EventImpactResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes the v1 event-impact metric for one item across every game event (IMPACT-01, design §79).
 *
 * <p>Per event the metric is the ANCHOR single-snapshot delta {@code changeRate = post / pre - 1} on
 * {@code min_price}, where {@code pre} is the LAST snapshot in {@code [occurredAt - Nh, occurredAt]}
 * and {@code post} the FIRST snapshot in {@code [occurredAt, occurredAt + Nh]} — anchor snapshot
 * prices, NOT a window average (D-01).
 *
 * <p>The headline engineering property is N+1 avoidance (D-09 / design T7): events are fetched ONCE
 * ({@code findAllByOrderByOccurredAtDesc}) and the item's snapshots for the whole
 * {@code [min(occurredAt) - Nh, max(occurredAt) + Nh]} span are fetched in a SINGLE range read; every
 * event's anchors are then derived in memory. The snapshot finder runs exactly once regardless of
 * event count — it is never called per event.
 *
 * <p>An event is {@code "ok"} (with {@code change_rate}) ONLY when both anchors are present AND both
 * are fresh — within the {@link #STALENESS_ALLOWANCE} of the event (IMPACT-02, D-02/D-03/D-04).
 * Otherwise it is {@code "insufficient_data"} with no {@code change_rate}; the discovered in-window
 * anchor time is still reported on each present side and is {@code null} only for a side with no
 * in-window snapshot, so a caller can tell "no data" (null) from "stale data" (time reported, D-05).
 * {@code changeRate} is a TEMPORAL CORRELATION, never a claim of causation (PROJECT premise 5).
 */
@Service
public class EventImpactService {

    /** Status strings shared with the 05-02 staleness layer — do NOT introduce new status values. */
    private static final String STATUS_OK = "ok";
    private static final String STATUS_INSUFFICIENT = "insufficient_data";

    /** change_rate representation (D-01, Claude's Discretion): a stable 4-dp HALF_UP ratio delta. */
    private static final int CHANGE_RATE_SCALE = 4;

    /**
     * Staleness allowance (IMPACT-02, D-03): a FIXED ABSOLUTE 30 minutes (~ the 10-min collection
     * cadence × 3 ticks — "tolerates ≤2 consecutive missed ticks"). An anchor is fresh when
     * {@code |occurred_at − collected_at| ≤ 30min} (boundary INCLUSIVE). Held as an MVP constant on
     * purpose — {@code @ConfigurationProperties} externalization is explicitly v2 (CFG-V2-01).
     */
    private static final Duration STALENESS_ALLOWANCE = Duration.ofMinutes(30);

    private final GameEventRepository gameEventRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;

    public EventImpactService(GameEventRepository gameEventRepository,
                              PriceSnapshotRepository priceSnapshotRepository) {
        this.gameEventRepository = gameEventRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    public EventImpactResponse eventImpact(long itemId, int windowHours) {
        List<GameEvent> events = gameEventRepository.findAllByOrderByOccurredAtDesc();
        if (events.isEmpty()) {
            return new EventImpactResponse(itemId, windowHours, List.of());
        }

        Duration window = Duration.ofHours(windowHours);
        OffsetDateTime min = events.stream().map(GameEvent::getOccurredAt).min(Comparator.naturalOrder()).orElseThrow();
        OffsetDateTime max = events.stream().map(GameEvent::getOccurredAt).max(Comparator.naturalOrder()).orElseThrow();

        // The ONE batch snapshot read for the whole event span (D-09 / design T7) — never per event.
        List<PriceSnapshot> snapshots = priceSnapshotRepository
                .findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc(itemId, min.minus(window), max.plus(window));

        List<EventImpactItem> items = new ArrayList<>(events.size());
        for (GameEvent event : events) {
            items.add(toImpactItem(event, window, snapshots));
        }
        return new EventImpactResponse(itemId, windowHours, items);
    }

    private EventImpactItem toImpactItem(GameEvent event, Duration window, List<PriceSnapshot> snapshots) {
        OffsetDateTime occurredAt = event.getOccurredAt();
        OffsetDateTime lo = occurredAt.minus(window);
        OffsetDateTime hi = occurredAt.plus(window);

        // pre = LAST snapshot in [lo, occurredAt]; post = FIRST snapshot in [occurredAt, hi].
        // A snapshot exactly at occurredAt (gap 0) is a valid anchor on BOTH sides (D-01 tie rule).
        PriceSnapshot pre = null;
        PriceSnapshot post = null;
        for (PriceSnapshot s : snapshots) { // ascending by collected_at
            OffsetDateTime t = s.getCollectedAt();
            if (!t.isBefore(lo) && !t.isAfter(occurredAt)) {
                pre = s; // keep overwriting as the list ascends -> last match wins
            }
            if (post == null && !t.isBefore(occurredAt) && !t.isAfter(hi)) {
                post = s; // first match wins
            }
        }

        // "ok" + change_rate ONLY when both anchors are present, priced, AND both fresh — a single
        // stale side would distort the ratio (D-02 sufficiency + D-03/D-04 both-fresh).
        if (isFreshAnchor(pre, occurredAt) && isFreshAnchor(post, occurredAt)) {
            BigDecimal changeRate = BigDecimal.valueOf(post.getMinPrice())
                    .divide(BigDecimal.valueOf(pre.getMinPrice()), CHANGE_RATE_SCALE, RoundingMode.HALF_UP)
                    .subtract(BigDecimal.ONE);
            return new EventImpactItem(
                    event.getId(), event.getEventType(), event.getTitle(), occurredAt,
                    STATUS_OK,
                    pre.getCollectedAt(), post.getCollectedAt(),
                    pre.getMinPrice(), post.getMinPrice(),
                    changeRate);
        }

        // Otherwise insufficient_data with NO change_rate. Always report the discovered in-window
        // anchor time on each side; leave a side null ONLY when it had zero in-window snapshots — so a
        // caller distinguishes "no data" (null) from "stale data" (time reported, gap > 30min) (D-05).
        return new EventImpactItem(
                event.getId(), event.getEventType(), event.getTitle(), occurredAt,
                STATUS_INSUFFICIENT,
                pre != null ? pre.getCollectedAt() : null,
                post != null ? post.getCollectedAt() : null,
                null, null, null);
    }

    /**
     * An anchor is fresh when it exists, is priced, and its {@code collected_at} is within the
     * {@link #STALENESS_ALLOWANCE} of the event instant (boundary INCLUSIVE: exactly 30min is fresh,
     * 30min+1s is stale) (D-03/D-04).
     */
    private static boolean isFreshAnchor(PriceSnapshot anchor, OffsetDateTime occurredAt) {
        return anchor != null
                && anchor.getMinPrice() != null
                && Duration.between(anchor.getCollectedAt(), occurredAt).abs().compareTo(STALENESS_ALLOWANCE) <= 0;
    }
}
