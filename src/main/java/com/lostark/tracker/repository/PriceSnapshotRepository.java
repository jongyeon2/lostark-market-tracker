package com.lostark.tracker.repository;

import com.lostark.tracker.domain.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    /** Idempotency guard: has this item already been snapshotted at this tick's collected_at? (D-16) */
    boolean existsByTrackedItem_IdAndCollectedAt(Long trackedItemId, OffsetDateTime collectedAt);
}
