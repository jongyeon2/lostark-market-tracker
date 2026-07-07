package com.lostark.tracker.collect;

import com.lostark.tracker.collect.dto.MarketItem;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.collect.error.AuthApiException;
import com.lostark.tracker.collect.error.NonRetryableApiException;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.collect.error.TransientApiException;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fetches one watchlisted item's current price on the dedicated collection executor (D-06). It
 * acquires a rate-limit token, issues the markets list call through the bounded {@link RetryPolicy}
 * (429 Retry-After + 5xx/timeout retried up to the cap — D-09/D-10), and resolves the item by
 * {@code Id == externalItemId} (D-05). It returns a DTO {@link ItemFetchResult} (no persistence —
 * the collector persists successes after the join).
 *
 * <p>Fatal auth (401/403) is NOT retried: it sets the shared {@code fatalAuth} flag so the tick
 * stops issuing NEW calls and marks the run AUTH_ERROR (D-08). Failure reasons are categorical
 * markers only — never the API key.
 */
@Service
public class ItemFetchService {

    private final RedisTokenBucket rateLimiter;
    private final LostarkApiClient apiClient;
    private final RetryPolicy retryPolicy;

    public ItemFetchService(RedisTokenBucket rateLimiter, LostarkApiClient apiClient, RetryPolicy retryPolicy) {
        this.rateLimiter = rateLimiter;
        this.apiClient = apiClient;
        this.retryPolicy = retryPolicy;
    }

    @Async(CollectionConfig.COLLECTION_EXECUTOR)
    public CompletableFuture<ItemFetchResult> fetch(Long trackedItemId, String externalItemId,
                                                    String categoryCode, String itemName,
                                                    AtomicBoolean fatalAuth) {
        // Stop NEW outbound calls once any item hit fatal auth (D-08 convergence).
        if (fatalAuth.get()) {
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "AUTH"));
        }
        if (!rateLimiter.tryAcquire()) {
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "RATE_LIMITED"));
        }
        try {
            MarketItemsResponse response = retryPolicy.execute(
                    () -> apiClient.searchMarketItems(categoryCode, itemName));
            MarketItem match = response.items().stream()
                    .filter(i -> String.valueOf(i.id()).equals(externalItemId))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "NOT_FOUND"));
            }
            return CompletableFuture.completedFuture(
                    ItemFetchResult.success(trackedItemId, match.currentMinPrice(), match.yDayAvgPrice(),
                            OffsetDateTime.now(ZoneOffset.UTC)));
        } catch (AuthApiException e) {
            // Fatal: stop new calls + mark the run. No key in the marker.
            fatalAuth.set(true);
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "AUTH"));
        } catch (RateLimitedApiException e) {
            // Retries exhausted (D-09) -> skip the item this tick.
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "RATE_LIMITED"));
        } catch (TransientApiException e) {
            // Retries exhausted (D-10) -> only this item fails.
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "TRANSIENT"));
        } catch (NonRetryableApiException e) {
            // Other 4xx -> skip-light (D-11).
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "CLIENT_ERROR"));
        }
    }
}
