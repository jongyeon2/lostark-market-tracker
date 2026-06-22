package com.lostark.tracker.collect;

import com.lostark.tracker.collect.dto.MarketItem;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.collect.error.LostarkApiException;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches one watchlisted item's current price on the dedicated collection executor (D-06). Each
 * call acquires a rate-limit token, issues the markets list call, and resolves the item by
 * {@code Id == externalItemId} (D-05). It returns a DTO {@link ItemFetchResult} and performs NO
 * persistence — the collector persists only successful results after the join, which keeps a slow
 * item that finishes after the tick's timeout from writing a late snapshot.
 *
 * <p>In plan 02-02 a thrown {@link LostarkApiException} maps straight to a FAILED result. Plan
 * 02-03 wraps the call in the bounded retry policy (429/5xx) and the fatal-auth handling.
 */
@Service
public class ItemFetchService {

    private final RedisTokenBucket rateLimiter;
    private final LostarkApiClient apiClient;

    public ItemFetchService(RedisTokenBucket rateLimiter, LostarkApiClient apiClient) {
        this.rateLimiter = rateLimiter;
        this.apiClient = apiClient;
    }

    @Async(CollectionConfig.COLLECTION_EXECUTOR)
    public CompletableFuture<ItemFetchResult> fetch(Long trackedItemId, String externalItemId,
                                                    String categoryCode, String itemName) {
        if (!rateLimiter.tryAcquire()) {
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "RATE_LIMITED"));
        }
        try {
            MarketItemsResponse response = apiClient.searchMarketItems(categoryCode, itemName);
            MarketItem match = response.items().stream()
                    .filter(i -> String.valueOf(i.id()).equals(externalItemId))
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                // ItemName is a partial-match filter; no exact Id match -> skip-light (D-05).
                return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "NOT_FOUND"));
            }
            return CompletableFuture.completedFuture(
                    ItemFetchResult.success(trackedItemId, match.currentMinPrice(), OffsetDateTime.now(ZoneOffset.UTC)));
        } catch (LostarkApiException e) {
            // 02-02: classify-and-fail (no retry yet). 02-03 adds the bounded retry + fatal-auth path.
            return CompletableFuture.completedFuture(ItemFetchResult.failed(trackedItemId, "API_ERROR"));
        }
    }
}
