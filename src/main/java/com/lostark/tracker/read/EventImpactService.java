package com.lostark.tracker.read;

import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.ItemDailyStat;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.web.dto.AnchorSource;
import com.lostark.tracker.web.dto.EventImpactItem;
import com.lostark.tracker.web.dto.EventImpactResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 * <p><b>Daily fallback (Phase 25, IMPACT-V2-01).</b> Snapshots only exist from the day collection
 * started, so every event before that read "데이터 부족" forever even though the backfilled daily
 * averages ({@code item_daily_stats}, Phase 17.4) covered it plainly — the timeline could see the
 * spike and this page could not. When snapshot anchors do NOT qualify, the metric falls back to
 * {@code pre} = the KST day BEFORE the event, {@code post} = the KST day OF the event, and reports
 * {@link AnchorSource#DAILY_AVG} so the client can say which lens was used.
 *
 * <p>The two lenses are never mixed inside one ratio: {@code min_price} is the cheapest 호가 at a
 * 10-minute tick, {@code avg_price} is a whole day's 체결 평균 — a ratio of one over the other would
 * mean nothing. Snapshots always win when they qualify, so no existing {@code ok} row changes value.
 * {@code DETAIL_STATS} beats {@code YDAY_AVG} on a day carrying both: it is the source that reaches
 * back before collection started (exactly the gap this fallback exists for), so it keeps {@code pre}
 * and {@code post} on one consistent basis.
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
     * purpose — externalizing this threshold to config is explicitly deferred to v2 (CFG-V2-01).
     */
    private static final Duration STALENESS_ALLOWANCE = Duration.ofMinutes(30);

    /**
     * Event days are resolved on the KST wall clock (Phase 25) — {@code item_daily_stats.stat_date} is
     * a KST calendar day, so a UTC-derived date would pick the wrong day for any event between
     * 15:00–24:00 UTC (D-11, the same off-by-9h guard the rest of the read path applies).
     */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GameEventRepository gameEventRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ItemDailyStatRepository itemDailyStatRepository;

    public EventImpactService(GameEventRepository gameEventRepository,
                              PriceSnapshotRepository priceSnapshotRepository,
                              ItemDailyStatRepository itemDailyStatRepository) {
        this.gameEventRepository = gameEventRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.itemDailyStatRepository = itemDailyStatRepository;
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

        // The ONE batch daily-stat read, same discipline: the fallback needs the KST day before the
        // earliest event through the day of the latest, so a single inclusive range covers every event.
        Map<LocalDate, BigDecimal> dailyAvg = dailyAveragesByDay(
                itemId, kstDay(min).minusDays(1), kstDay(max));

        List<EventImpactItem> items = new ArrayList<>(events.size());
        for (GameEvent event : events) {
            items.add(toImpactItem(event, window, snapshots, dailyAvg));
        }
        return new EventImpactResponse(itemId, windowHours, items);
    }

    /**
     * One daily average per KST day, DETAIL_STATS winning over YDAY_AVG when a day carries both (the
     * UNIQUE key includes source, so coexistence is normal). Read once for the whole event span, then
     * consulted in memory — the N+1 avoidance that governs the snapshot read governs this one too.
     */
    private Map<LocalDate, BigDecimal> dailyAveragesByDay(long itemId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        Map<LocalDate, DailyStatSource> chosen = new HashMap<>();
        for (ItemDailyStat stat : itemDailyStatRepository
                .findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(itemId, from, to)) {
            LocalDate day = stat.getStatDate();
            if (chosen.get(day) == DailyStatSource.DETAIL_STATS && stat.getSource() != DailyStatSource.DETAIL_STATS) {
                continue; // already hold the preferred source for this day
            }
            byDay.put(day, stat.getAvgPrice());
            chosen.put(day, stat.getSource());
        }
        return byDay;
    }

    private static LocalDate kstDay(OffsetDateTime at) {
        return at.atZoneSameInstant(KST).toLocalDate();
    }

    private EventImpactItem toImpactItem(GameEvent event, Duration window, List<PriceSnapshot> snapshots,
                                         Map<LocalDate, BigDecimal> dailyAvg) {
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
            BigDecimal changeRate = rate(
                    BigDecimal.valueOf(pre.getMinPrice()), BigDecimal.valueOf(post.getMinPrice()));
            return new EventImpactItem(
                    event.getId(), event.getEventType(), event.getTitle(), occurredAt,
                    STATUS_OK,
                    pre.getCollectedAt(), post.getCollectedAt(),
                    pre.getMinPrice(), post.getMinPrice(),
                    changeRate,
                    AnchorSource.SNAPSHOT_MIN);
        }

        // No usable snapshot pair — try the daily averages before giving up (Phase 25). Snapshots were
        // checked FIRST on purpose: they are the finer measurement, so a row that could be answered
        // with them never quietly switches lens.
        EventImpactItem fallback = dailyFallback(event, occurredAt, dailyAvg);
        if (fallback != null) {
            return fallback;
        }

        // Otherwise insufficient_data with NO change_rate. Always report the discovered in-window
        // anchor time on each side; leave a side null ONLY when it had zero in-window snapshots — so a
        // caller distinguishes "no data" (null) from "stale data" (time reported, gap > 30min) (D-05).
        return new EventImpactItem(
                event.getId(), event.getEventType(), event.getTitle(), occurredAt,
                STATUS_INSUFFICIENT,
                pre != null ? pre.getCollectedAt() : null,
                post != null ? post.getCollectedAt() : null,
                null, null, null, null);
    }

    /**
     * The daily-average reading of one event, or {@code null} when either KST day is missing.
     *
     * <p>{@code pre} is the day BEFORE the event and {@code post} the day OF it — the daily mirror of
     * the snapshot rule (last-before / first-at-or-after). A day's average already contains the whole
     * day's trades, so the event day IS the first post-event observation available at this resolution.
     *
     * <p>Anchor times are left null: a daily average belongs to a date, not an instant, and inventing a
     * timestamp (midnight?) would misrepresent when it was measured. The {@code DAILY_AVG} source plus
     * the event's own date already tell the reader everything true about the timing.
     */
    private EventImpactItem dailyFallback(GameEvent event, OffsetDateTime occurredAt,
                                          Map<LocalDate, BigDecimal> dailyAvg) {
        LocalDate eventDay = kstDay(occurredAt);
        BigDecimal preAvg = dailyAvg.get(eventDay.minusDays(1));
        BigDecimal postAvg = dailyAvg.get(eventDay);
        if (preAvg == null || postAvg == null || preAvg.signum() <= 0) {
            return null;
        }
        return new EventImpactItem(
                event.getId(), event.getEventType(), event.getTitle(), occurredAt,
                STATUS_OK,
                null, null,
                preAvg.setScale(0, RoundingMode.HALF_UP).longValueExact(),
                postAvg.setScale(0, RoundingMode.HALF_UP).longValueExact(),
                rate(preAvg, postAvg),
                AnchorSource.DAILY_AVG);
    }

    /** {@code post / pre - 1} at the stable 4-dp HALF_UP scale — one definition for both sources. */
    private static BigDecimal rate(BigDecimal pre, BigDecimal post) {
        return post.divide(pre, CHANGE_RATE_SCALE, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
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
