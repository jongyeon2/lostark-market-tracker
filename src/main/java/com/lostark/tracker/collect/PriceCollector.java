package com.lostark.tracker.collect;

import com.lostark.tracker.domain.CollectionRun;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The scheduled collection tick (COLL-01/02). Every {@code fixedDelay} (10 min — fixedDelay so a
 * tick never overlaps the previous one) it: opens a {@code collection_run} (RUNNING), computes one
 * tick-normalized {@code collected_at} shared by the whole tick (D-15), fans out one async fetch
 * per active item through {@link ItemFetchService}, and awaits them with {@code allOf().get(...)}
 * under a per-call timeout AND an overall timeout below the tick interval (D-07 — the one CRITICAL
 * failure mode). Only successful results are persisted (idempotently, UNIQUE — D-16); a slow or
 * failed item writes no snapshot and is reflected only in the run counts. Finally it finalizes the
 * run with counts + a count-based status (refined further in 02-03).
 */
@Component
public class PriceCollector {

    private record ItemFuture(TrackedItem item, CompletableFuture<ItemFetchResult> future) {
    }

    private final ItemFetchService fetchService;
    private final TrackedItemRepository trackedItemRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final CollectionRunRepository collectionRunRepository;
    private final Clock clock;
    private final long perCallTimeoutSeconds;
    private final long overallTimeoutSeconds;

    public PriceCollector(ItemFetchService fetchService,
                          TrackedItemRepository trackedItemRepository,
                          PriceSnapshotRepository priceSnapshotRepository,
                          CollectionRunRepository collectionRunRepository,
                          Clock clock,
                          @Value("${collection.per-call-timeout-seconds:5}") long perCallTimeoutSeconds,
                          @Value("${collection.overall-timeout-seconds:90}") long overallTimeoutSeconds) {
        this.fetchService = fetchService;
        this.trackedItemRepository = trackedItemRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.collectionRunRepository = collectionRunRepository;
        this.clock = clock;
        this.perCallTimeoutSeconds = perCallTimeoutSeconds;
        this.overallTimeoutSeconds = overallTimeoutSeconds;
    }

    /**
     * The run's logical timestamp: run-start instant in UTC truncated to the minute (D-15). Every
     * snapshot in the tick shares it; a retried tick within the same minute collides idempotently.
     */
    public static OffsetDateTime normalizeCollectedAt(OffsetDateTime startedAt) {
        return startedAt.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
    }

    @Scheduled(fixedDelayString = "${collection.fixed-delay-ms:600000}",
            initialDelayString = "${collection.initial-delay-ms:0}")
    public void collectTick() {
        OffsetDateTime startedAt = OffsetDateTime.now(clock);
        OffsetDateTime collectedAt = normalizeCollectedAt(startedAt);

        List<TrackedItem> items = trackedItemRepository.findByActiveTrue();
        CollectionRun run = collectionRunRepository.save(
                new CollectionRun(startedAt, null, items.size(), 0, 0, "RUNNING"));

        // Fan out: one async fetch per item, each bounded by the per-call timeout (D-07).
        List<ItemFuture> futures = new ArrayList<>();
        for (TrackedItem item : items) {
            CompletableFuture<ItemFetchResult> future = fetchService
                    .fetch(item.getId(), item.getExternalItemId(), item.getCategory(), item.getDisplayName())
                    .orTimeout(perCallTimeoutSeconds, TimeUnit.SECONDS);
            futures.add(new ItemFuture(item, future));
        }

        // Wait for all to settle, bounded by the overall timeout (< tick interval, D-07).
        awaitAll(futures);

        int succeeded = 0;
        int failed = 0;
        for (ItemFuture itf : futures) {
            ItemFetchResult result = settledResult(itf.future());
            if (result != null && result.isSuccess()) {
                persistSnapshot(itf.item(), collectedAt, result);
                succeeded++;
            } else {
                failed++;
            }
        }

        String status = succeeded == items.size() ? "SUCCESS"
                : (succeeded == 0 ? "FAILED" : "PARTIAL_SUCCESS");
        run.finish(OffsetDateTime.now(clock), succeeded, failed, status);
        collectionRunRepository.save(run);
    }

    private void awaitAll(List<ItemFuture> futures) {
        CompletableFuture<?>[] all = futures.stream().map(ItemFuture::future).toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(all).get(overallTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Overall backstop tripped — unfinished items are counted failed below; never block the tick.
        } catch (ExecutionException e) {
            // Individual completions (incl. per-call timeouts) are inspected per-future below.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Returns the result only when the future completed normally; otherwise null (timed out/failed). */
    private ItemFetchResult settledResult(CompletableFuture<ItemFetchResult> future) {
        if (future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
            try {
                return future.getNow(null);
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private void persistSnapshot(TrackedItem item, OffsetDateTime collectedAt, ItemFetchResult result) {
        // Idempotent: UNIQUE(tracked_item_id, collected_at) makes a retried tick a no-op (D-16).
        if (priceSnapshotRepository.existsByTrackedItem_IdAndCollectedAt(item.getId(), collectedAt)) {
            return;
        }
        try {
            priceSnapshotRepository.save(
                    new PriceSnapshot(item, collectedAt, result.minPrice(), result.fetchedAt()));
        } catch (DataIntegrityViolationException duplicate) {
            // Lost a race on the UNIQUE constraint — treat as an idempotent skip, not a failure.
        }
    }
}
