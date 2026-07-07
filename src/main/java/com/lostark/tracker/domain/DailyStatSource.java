package com.lostark.tracker.domain;

/**
 * Provenance of a backfilled daily-average row in {@code item_daily_stats} (Phase 17.4, D-01).
 * Stored as {@code @Enumerated(STRING)} so the display layer can honestly label where a data point
 * came from, and an unknown string in the column fails loudly at the mapping boundary.
 */
public enum DailyStatSource {

    /**
     * Source ①: the free {@code YDayAvgPrice} (previous-day average) carried on the market list
     * response the collector already reads. Captured every collection tick for ALL watchlist items,
     * building a going-forward daily-average series (BACKFILL-01).
     */
    YDAY_AVG,

    /**
     * Source ②: the detail API {@code Stats[]} (last ~14 days of daily averages), fetched on startup
     * and once a day to backfill PAST gaps. Only fills materials — the detail endpoint returns 0 for
     * engraving books, so those rely on {@link #YDAY_AVG} going forward (BACKFILL-02).
     */
    DETAIL_STATS
}
