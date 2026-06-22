package com.lostark.tracker.collect.error;

/**
 * Any other 4xx (e.g. 400/404, excluding 401/403/429) — a request/matching problem. Not retried;
 * the item is skipped (skip-light) with no snapshot written (D-11).
 */
public class NonRetryableApiException extends LostarkApiException {

    public NonRetryableApiException(String message) {
        super(message);
    }
}
