package com.lostark.tracker.web.dto;

/**
 * The measurement a {@code change_rate} was computed from (Phase 25, IMPACT-V2-01).
 *
 * <p>The event-impact metric has two possible lenses and they are NOT interchangeable:
 * {@link #SNAPSHOT_MIN} reads {@code price_snapshot.min_price} (the cheapest 호가 at a 10-minute tick)
 * while {@link #DAILY_AVG} reads {@code item_daily_stats.avg_price} (the whole day's 체결 평균가). A
 * ratio built from one of each would be meaningless, so a rate is only ever computed within a single
 * source — and this enum reports which one, so the UI can label it instead of letting the reader
 * assume every row means the same thing.
 */
public enum AnchorSource {

    /** 10분 틱의 최저 호가 앵커 — the default whenever snapshots cover the event (Phase 5 semantics). */
    SNAPSHOT_MIN,

    /**
     * 백필 일별 체결 평균가 앵커 — the fallback for events with NO snapshot coverage, i.e. anything
     * before collection started. Coarser (one point per day) but real, measured data.
     */
    DAILY_AVG
}
