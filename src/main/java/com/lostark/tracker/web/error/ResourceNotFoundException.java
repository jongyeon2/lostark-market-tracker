package com.lostark.tracker.web.error;

/**
 * Thrown when an admin write request targets a resource (game event or tracked item) that does not
 * exist — the generalized admin-layer 404, mapped to the shared {@code {timestamp,status,error,message}}
 * contract by {@link ApiExceptionHandler} (D-09). Kept SEPARATE from {@link ItemNotFoundException}
 * (the read-path "no price yet" 404) so admin messages read naturally ("Event 7 not found" /
 * "Item 7 not found") without regressing the read contract. The message carries only the resource id —
 * never internal state or a secret.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
