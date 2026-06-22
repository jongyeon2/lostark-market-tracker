package com.lostark.tracker.collect;

import com.lostark.tracker.collect.error.AuthApiException;
import com.lostark.tracker.collect.error.NonRetryableApiException;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.collect.error.TransientApiException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the hand-rolled bounded retry (COLL-04, D-09/D-10): max-3 attempts, exponential backoff,
 * Retry-After honored before backoff, and NO retry for fatal-auth / non-retryable. A recording
 * sleeper makes timing assertions exact without real waiting.
 */
class RetryPolicyTest {

    private static final class RecordingSleeper implements RetryPolicy.Sleeper {
        final List<Long> sleeps = new ArrayList<>();

        @Override
        public void sleep(long millis) {
            sleeps.add(millis);
        }
    }

    @Test
    void retriesRateLimitedUpToMaxThenThrows() {
        RecordingSleeper sleeper = new RecordingSleeper();
        RetryPolicy policy = new RetryPolicy(3, 100, sleeper);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> policy.execute(() -> {
            calls.incrementAndGet();
            throw new RateLimitedApiException("429", null);
        })).isInstanceOf(RateLimitedApiException.class);

        assertThat(calls.get()).isEqualTo(3);                  // 3 attempts (max)
        assertThat(sleeper.sleeps).containsExactly(100L, 200L); // exponential backoff between attempts
    }

    @Test
    void honorsRetryAfterBeforeExponentialBackoff() {
        RecordingSleeper sleeper = new RecordingSleeper();
        RetryPolicy policy = new RetryPolicy(3, 100, sleeper);
        AtomicInteger calls = new AtomicInteger();

        String result = policy.execute(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new RateLimitedApiException("429", 5); // Retry-After: 5s
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
        assertThat(sleeper.sleeps).containsExactly(5000L); // honored Retry-After, NOT the 100ms backoff
    }

    @Test
    void retriesTransientThenSucceeds() {
        RecordingSleeper sleeper = new RecordingSleeper();
        RetryPolicy policy = new RetryPolicy(3, 100, sleeper);
        AtomicInteger calls = new AtomicInteger();

        String result = policy.execute(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new TransientApiException("503");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
        assertThat(sleeper.sleeps).containsExactly(100L);
    }

    @Test
    void doesNotRetryFatalAuth() {
        RecordingSleeper sleeper = new RecordingSleeper();
        RetryPolicy policy = new RetryPolicy(3, 100, sleeper);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> policy.execute(() -> {
            calls.incrementAndGet();
            throw new AuthApiException("401");
        })).isInstanceOf(AuthApiException.class);

        assertThat(calls.get()).isEqualTo(1);   // no retry
        assertThat(sleeper.sleeps).isEmpty();
    }

    @Test
    void doesNotRetryNonRetryable() {
        RecordingSleeper sleeper = new RecordingSleeper();
        RetryPolicy policy = new RetryPolicy(3, 100, sleeper);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> policy.execute(() -> {
            calls.incrementAndGet();
            throw new NonRetryableApiException("400");
        })).isInstanceOf(NonRetryableApiException.class);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(sleeper.sleeps).isEmpty();
    }
}
