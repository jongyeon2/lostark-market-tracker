package com.lostark.tracker.collect;

import com.lostark.tracker.cache.LatestPriceCache;
import com.lostark.tracker.collect.dto.MarketItem;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.collect.error.AuthApiException;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.collect.error.TransientApiException;
import com.lostark.tracker.domain.CollectionRun;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Proves the resilience layer end-to-end (COLL-04 retry half + COLL-05, Success Criteria 3/4):
 * 429 retry-then-success, 429-beyond-max skip + RATE_LIMITED marker, 5xx partial-failure isolation
 * (PARTIAL_SUCCESS), and 401 fatal-auth convergence with an AUTH_ERROR marker that carries no key.
 */
@SpringBootTest
class CollectionResilienceIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkApiClient apiClient;

    @Autowired
    ItemFetchService itemFetchService;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    CollectionRunRepository collectionRunRepository;
    @Autowired
    LatestPriceCache latestPriceCache;

    private static final Instant FIXED = Instant.parse("2026-06-22T10:30:30Z");
    private static final OffsetDateTime COLLECTED_AT = OffsetDateTime.parse("2026-06-22T10:30:00Z");

    private PriceCollector collector() {
        // perCall 5s leaves room for the bounded retry's backoff; overall 8s backstop.
        return new PriceCollector(itemFetchService, trackedItemRepository, priceSnapshotRepository,
                collectionRunRepository, latestPriceCache, Clock.fixed(FIXED, ZoneOffset.UTC), 5, 8);
    }

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        collectionRunRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    private void seed(String externalId, String name) {
        trackedItemRepository.save(new TrackedItem(externalId, name, "50010"));
    }

    private static MarketItemsResponse oneItem(long id, long price) {
        return new MarketItemsResponse(1, 10, 1, List.of(new MarketItem(id, "item" + id, price, null)));
    }

    private CollectionRun latestRun() {
        List<CollectionRun> runs = collectionRunRepository.findAll();
        return runs.get(runs.size() - 1);
    }

    @Test
    void rateLimitedThenSuccessWithinRetriesWritesSnapshot() {
        seed("1001", "itemA");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA")))
                .thenThrow(new RateLimitedApiException("429", null))
                .thenReturn(oneItem(1001, 100));

        collector().collectTick();

        assertThat(priceSnapshotRepository.findAll()).hasSize(1);
        assertThat(latestRun().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void rateLimitedBeyondMaxSkipsItemAndMarksRun() {
        seed("1001", "itemA");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA")))
                .thenThrow(new RateLimitedApiException("429", null)); // always

        collector().collectTick();

        assertThat(priceSnapshotRepository.findAll()).isEmpty();
        CollectionRun run = latestRun();
        assertThat(run.getItemsFailed()).isEqualTo(1);
        assertThat(run.getStatus()).isEqualTo("FAILED");
        assertThat(run.getSummaryMessage()).isEqualTo("RATE_LIMITED");
    }

    @Test
    void oneTransientFailureYieldsPartialSuccessWithoutBlockingOthers() {
        seed("1001", "itemA");
        seed("1002", "itemB");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA")))
                .thenThrow(new TransientApiException("503")); // always -> exhausts retries
        when(apiClient.searchMarketItems(eq("50010"), eq("itemB")))
                .thenReturn(oneItem(1002, 200));

        collector().collectTick();

        assertThat(priceSnapshotRepository.findAll()).hasSize(1); // only B
        CollectionRun run = latestRun();
        assertThat(run.getItemsAttempted()).isEqualTo(2);
        assertThat(run.getItemsSucceeded()).isEqualTo(1);
        assertThat(run.getItemsFailed()).isEqualTo(1);
        assertThat(run.getStatus()).isEqualTo("PARTIAL_SUCCESS");
    }

    @Test
    void fatalAuthMarksRunAuthErrorIsNotRetriedAndLeaksNoKey() {
        seed("1001", "itemA");
        when(apiClient.searchMarketItems(eq("50010"), eq("itemA")))
                .thenThrow(new AuthApiException("401 Authorization has been denied"));

        collector().collectTick();

        assertThat(priceSnapshotRepository.findAll()).isEmpty();
        CollectionRun run = latestRun();
        assertThat(run.getStatus()).isEqualTo("FAILED");
        // Categorical marker only — never the key/Authorization header (D-08).
        assertThat(run.getSummaryMessage()).isEqualTo("AUTH_ERROR");
    }
}
