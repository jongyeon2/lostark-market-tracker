package com.lostark.tracker.web.error;

/**
 * Thrown when a read request targets an item that either does not exist or has no price snapshot
 * yet ("no price yet"). Both collapse to a 404 via {@link ApiExceptionHandler} (D-10 — the 404
 * half of the error contract). The message carries only the itemId — never any internal state or
 * secret (T-0301-04).
 */
public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(long itemId) {
        super("No price found for item " + itemId);
    }
}
