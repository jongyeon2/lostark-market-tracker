package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.PriceSnapshot;

import java.time.OffsetDateTime;

/**
 * One point on the price line: {@code collectedAt} (UTC ISO-8601, D-11) + {@code minPrice}. The
 * timeline's {@code snapshots} array carries these ascending by {@code collectedAt} (D-04).
 */
public record SnapshotPoint(
        OffsetDateTime collectedAt,
        Long minPrice
) {
    public static SnapshotPoint from(PriceSnapshot snapshot) {
        return new SnapshotPoint(snapshot.getCollectedAt(), snapshot.getMinPrice());
    }
}
