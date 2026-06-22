package com.lostark.tracker.collect.error;

/**
 * Base for every typed Lostark markets API failure. The four concrete subclasses let the
 * collector and the retry layer (02-03) react differently to auth vs rate-limit vs transient
 * vs non-retryable outcomes. Messages NEVER carry the API key or Authorization header (D-08).
 */
public abstract class LostarkApiException extends RuntimeException {

    protected LostarkApiException(String message) {
        super(message);
    }

    protected LostarkApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
