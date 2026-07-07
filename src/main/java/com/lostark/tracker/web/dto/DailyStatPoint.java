package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.ItemDailyStat;

/**
 * One backfilled daily-average point for the timeline (Phase 17.4, BACKFILL-03). {@code statDate} is
 * a calendar day (yyyy-MM-dd, KST), {@code avgPrice} is that day's traded AVG, and {@code source}
 * tags provenance ({@code YDAY_AVG} / {@code DETAIL_STATS}). Kept in a SEPARATE array from the
 * real-time min-ask {@code snapshots} so the two metrics are never mixed into one line (D-02).
 */
public record DailyStatPoint(String statDate, double avgPrice, String source) {

    public static DailyStatPoint from(ItemDailyStat s) {
        return new DailyStatPoint(s.getStatDate().toString(), s.getAvgPrice().doubleValue(), s.getSource().name());
    }
}
