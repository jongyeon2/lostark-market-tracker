package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One game event's effect on the queried item's {@code min_price} (IMPACT-01, D-01/D-07). The v1
 * metric is the §79 ANCHOR single-snapshot delta: {@code changeRate = postPrice / prePrice - 1},
 * where {@code prePrice} is the {@code min_price} of the last snapshot in {@code [occurredAt - Nh,
 * occurredAt]} and {@code postPrice} the first in {@code [occurredAt, occurredAt + Nh]} — anchor
 * snapshot prices, NOT window averages.
 *
 * <p>{@code status} is {@code "ok"} (both anchors present — and, from 05-02, both fresh) or
 * {@code "insufficient_data"}. For an insufficient event the impact fields ({@code prePrice}/
 * {@code postPrice}/{@code changeRate}) are {@code null}; {@code preAnchorAt}/{@code postAnchorAt}
 * still carry the discovered in-window anchor time on each side and are {@code null} only when that
 * side had no snapshot in the window (so a caller can tell "no data" from "stale data", 05-02 D-05).
 *
 * <p>{@code changeRate} is a TEMPORAL CORRELATION, not a claim that the event caused the move
 * (PROJECT premise 5).
 */
public record EventImpactItem(
        Long id,
        EventType eventType,
        String title,
        OffsetDateTime occurredAt,
        String status,
        OffsetDateTime preAnchorAt,
        OffsetDateTime postAnchorAt,
        Long prePrice,
        Long postPrice,
        BigDecimal changeRate,
        /**
         * Which measurement {@code changeRate} was computed from, or {@code null} when there is no rate
         * (Phase 25). {@code SNAPSHOT_MIN} = 10-minute 최저 호가 anchors (the default, unchanged since
         * Phase 5); {@code DAILY_AVG} = the backfilled daily 체결 평균가, used ONLY when snapshot anchors
         * are absent. The two are never mixed inside one rate — this field tells the client which lens
         * a row was measured through so it can say so out loud rather than implying they are the same.
         */
        AnchorSource anchorSource
) {
}
