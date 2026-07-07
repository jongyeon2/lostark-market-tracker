package com.lostark.tracker.web.dto;

import java.util.List;

/**
 * The timeline read payload. TWO independent arrays (D-04): {@code snapshots} is the window's price
 * line (raw points, or {@code avg(min_price)} buckets when the range is downsampled), and
 * {@code events} is the overlapping game events — never downsampled.
 *
 * <p>{@code downsampled} reports whether the server aggregated the snapshots (raw point count &gt; N),
 * and {@code bucketWidth} ({@code "hour"}/{@code "day"}, null when raw) is the chosen {@code date_trunc}
 * unit (D-08). Snapshots are {@link PricePoint}s so raw and bucketed points share one shape.
 *
 * <p>{@code iconUrl}/{@code itemGroup}/{@code roleGroup} are the item's static enrichment as top-level
 * metadata (read-path additive; range reads are not cached). The controller fills them via findById.
 *
 * <p>{@code backfill} (Phase 17.4, BACKFILL-03) is a THIRD independent array: the window's backfilled
 * daily traded AVERAGES ({@link DailyStatPoint}), kept separate from {@code snapshots} because they are
 * a different metric — a daily traded avg vs. a real-time min ask — and must not be joined into one
 * line (D-02). Empty (never null) when the window has no backfill.
 */
public record TimelineResponse(
        boolean downsampled,
        String bucketWidth,
        List<PricePoint> snapshots,
        List<EventPoint> events,
        String iconUrl,
        String itemGroup,
        String roleGroup,
        List<DailyStatPoint> backfill
) {
}
