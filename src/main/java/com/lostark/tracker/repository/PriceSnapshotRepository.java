package com.lostark.tracker.repository;

import com.lostark.tracker.domain.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    /** Idempotency guard: has this item already been snapshotted at this tick's collected_at? (D-16) */
    boolean existsByTrackedItem_IdAndCollectedAt(Long trackedItemId, OffsetDateTime collectedAt);

    /**
     * Read-only: the newest snapshot for an item (max collected_at) — the latest-price source on a
     * cache miss. Added alongside the insert-path guard above; the Phase 2 write path is untouched.
     */
    Optional<PriceSnapshot> findTopByTrackedItem_IdOrderByCollectedAtDesc(Long trackedItemId);
}
