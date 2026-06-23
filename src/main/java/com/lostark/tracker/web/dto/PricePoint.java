package com.lostark.tracker.web.dto;

import java.time.OffsetDateTime;

/**
 * One point on the (possibly downsampled) price line. In RAW mode {@code collectedAt}+{@code minPrice}
 * are the snapshot's own values and {@code sampleCount} is null. In DOWNSAMPLED mode {@code collectedAt}
 * is the bucket start, {@code minPrice} is {@code round(avg(min_price))} over the bucket, and
 * {@code sampleCount} is how many raw snapshots the bucket covers (D-07/D-08). UTC ISO-8601 (D-11).
 */
public record PricePoint(
        OffsetDateTime collectedAt,
        Long minPrice,
        Integer sampleCount
) {
}
