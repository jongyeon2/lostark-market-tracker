package com.lostark.tracker.read;

import com.lostark.tracker.cache.LatestPriceCache;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.LatestPriceResponse;
import com.lostark.tracker.web.error.ItemNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The latest-price read model, orchestrating the hand-rolled cache-aside (D-01/D-03):
 *
 * <ol>
 *   <li>{@code cache.get} — on a HIT, return immediately with ZERO DB queries (the office-hours
 *       showcase, API-02 / Success Criterion 2).</li>
 *   <li>on a MISS, validate the item exists (else 404), read the newest snapshot from the DB (else
 *       404 "no price yet"), fill the cache, and return.</li>
 * </ol>
 *
 * The cache is filled but never the authority — a Redis outage just makes every read a miss that
 * falls through to the DB (fail-open via {@link LatestPriceCache}).
 */
@Service
public class LatestPriceService {

    private final TrackedItemRepository trackedItemRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final LatestPriceCache cache;

    public LatestPriceService(TrackedItemRepository trackedItemRepository,
                              PriceSnapshotRepository priceSnapshotRepository,
                              LatestPriceCache cache) {
        this.trackedItemRepository = trackedItemRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.cache = cache;
    }

    public LatestPriceResponse latest(long itemId) {
        Optional<LatestPriceResponse> cached = cache.get(itemId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Miss: distinguish "no such item" from "item exists but has no price yet" — both are 404 (D-10).
        if (!trackedItemRepository.existsById(itemId)) {
            throw new ItemNotFoundException(itemId);
        }
        PriceSnapshot newest = priceSnapshotRepository
                .findTopByTrackedItem_IdOrderByCollectedAtDesc(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        LatestPriceResponse response =
                new LatestPriceResponse(itemId, newest.getMinPrice(), newest.getCollectedAt());
        cache.put(itemId, response);
        return response;
    }
}
