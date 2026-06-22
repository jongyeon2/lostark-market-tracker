package com.lostark.tracker.collect;

import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.collect.error.TransientApiException;

import java.util.function.Supplier;

/**
 * Explicit, hand-rolled bounded retry (D-09/D-10) — deliberately NOT {@code @Retryable} AOP so the
 * policy is visible and unit-testable (consistent with the self-built token bucket).
 *
 * <ul>
 *   <li>Retries {@link RateLimitedApiException} (honors {@code Retry-After} first, else exponential
 *       backoff) and {@link TransientApiException} (exponential backoff), up to {@code maxAttempts}.</li>
 *   <li>Does NOT retry {@code AuthApiException} (fatal) or {@code NonRetryableApiException} — those
 *       propagate immediately on the first attempt.</li>
 * </ul>
 *
 * The {@link Sleeper} is injected so tests assert the backoff schedule without real waiting.
 */
public class RetryPolicy {

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis);
    }

    private final int maxAttempts;
    private final long baseBackoffMillis;
    private final Sleeper sleeper;

    public RetryPolicy(int maxAttempts, long baseBackoffMillis, Sleeper sleeper) {
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoffMillis;
        this.sleeper = sleeper;
    }

    public <T> T execute(Supplier<T> call) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return call.get();
            } catch (RateLimitedApiException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                Integer retryAfter = e.getRetryAfterSeconds();
                sleeper.sleep(retryAfter != null ? retryAfter * 1000L : backoffMillis(attempt));
            } catch (TransientApiException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                sleeper.sleep(backoffMillis(attempt));
            }
            // AuthApiException / NonRetryableApiException are not caught here -> propagate immediately.
        }
    }

    private long backoffMillis(int attempt) {
        return baseBackoffMillis * (1L << (attempt - 1)); // base, base*2, base*4, ...
    }
}
