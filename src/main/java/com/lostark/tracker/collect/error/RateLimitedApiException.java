package com.lostark.tracker.collect.error;

/**
 * 429 — rate limited. Carries the parsed {@code Retry-After} hint (seconds, nullable when the
 * header is absent) so the retry layer (02-03) can honor it before exponential backoff (D-09).
 */
public class RateLimitedApiException extends LostarkApiException {

    private final Integer retryAfterSeconds;

    public RateLimitedApiException(String message, Integer retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Retry-After in seconds, or {@code null} when the header was absent/unparseable. */
    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
