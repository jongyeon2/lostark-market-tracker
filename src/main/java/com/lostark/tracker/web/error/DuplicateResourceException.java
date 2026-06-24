package com.lostark.tracker.web.error;

/**
 * Thrown when an admin create would duplicate a unique resource — specifically a {@code POST
 * /api/admin/items} for an {@code external_item_id} that already exists AND is active (D-05).
 * Mapped to a 409 Conflict by {@link ApiExceptionHandler} on the shared
 * {@code {timestamp,status,error,message}} contract. The message carries only the offending
 * identifier — never internal state or a secret.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}