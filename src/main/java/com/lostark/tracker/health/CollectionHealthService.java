package com.lostark.tracker.health;

import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.web.dto.CollectionHealthResponse;
import org.springframework.stereotype.Service;

/**
 * Reads the latest {@code collection_run} (produced by the Phase 2 scheduler) and maps it to the
 * {@link CollectionHealthResponse} ops snapshot (OPS-01, 3A). It passes counts/timestamps/status and
 * the categorical {@code summary_message} marker straight through — the marker is already
 * secret-free by Phase 2 construction, so nothing is reconstructed and no API key or auth header is
 * ever read on this path (D-12).
 */
@Service
public class CollectionHealthService {

    private final CollectionRunRepository collectionRunRepository;

    public CollectionHealthService(CollectionRunRepository collectionRunRepository) {
        this.collectionRunRepository = collectionRunRepository;
    }

    public CollectionHealthResponse latestHealth() {
        return collectionRunRepository.findTopByOrderByStartedAtDesc()
                .map(CollectionHealthResponse::from)
                .orElseGet(CollectionHealthResponse::noRuns);
    }
}
