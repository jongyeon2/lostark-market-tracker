package com.lostark.tracker.collect;

import java.time.OffsetDateTime;

/**
 * Outcome of one item's fetch. A SUCCESS carries the {@code minPrice} (from CurrentMinPrice) and
 * the real call-completion instant ({@code fetchedAt}); a FAILED carries a categorical
 * {@code failureReason} marker (e.g. RATE_LIMITED / TRANSIENT / NOT_FOUND / CLIENT_ERROR / AUTH —
 * NEVER the API key). The collector persists only SUCCESS results (after the join), so a slow or
 * failed item writes no snapshot and is reflected only in the collection_run counts.
 */
public record ItemFetchResult(
        Long trackedItemId,
        Outcome outcome,
        Long minPrice,
        OffsetDateTime fetchedAt,
        String failureReason) {

    public enum Outcome {SUCCESS, FAILED}

    public static ItemFetchResult success(Long trackedItemId, Long minPrice, OffsetDateTime fetchedAt) {
        return new ItemFetchResult(trackedItemId, Outcome.SUCCESS, minPrice, fetchedAt, null);
    }

    public static ItemFetchResult failed(Long trackedItemId, String failureReason) {
        return new ItemFetchResult(trackedItemId, Outcome.FAILED, null, null, failureReason);
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }
}
