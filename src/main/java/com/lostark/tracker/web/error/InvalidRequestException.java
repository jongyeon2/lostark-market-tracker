package com.lostark.tracker.web.error;

/**
 * Thrown for a semantically invalid read request — chiefly a bad time window ({@code from > to} or a
 * non-positive {@code window <= 0}). Mapped to a 400 by {@link ApiExceptionHandler} with the same
 * {@code {timestamp,status,error,message}} contract as the 404 (D-10, D-13). The message describes
 * the rule (e.g. "from must be before to") — never internal state or a secret.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
