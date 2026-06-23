package com.lostark.tracker.web.dto;

import java.time.OffsetDateTime;

/**
 * The minimal latest-price payload: "what" ({@code minPrice}) plus "when" ({@code collectedAt}),
 * keyed by {@code itemId} (D-02). Deliberately NOT the full snapshot entity — caching the whole
 * row would be over-fetching, and a small DTO keeps the Redis value (and JSON) tight.
 *
 * <p>{@code collectedAt} is an {@link OffsetDateTime} serialized as UTC ISO-8601 ({@code ...Z},
 * D-11) so the cached value round-trips through Redis as the exact same instant it was stored at —
 * no server-side timezone conversion, which is what guards against the off-by-9h KST/UTC bug.
 * camelCase fields match {@code TrackedItemResponse} for a consistent API surface.
 */
public record LatestPriceResponse(
        Long itemId,
        Long minPrice,
        OffsetDateTime collectedAt
) {
}
