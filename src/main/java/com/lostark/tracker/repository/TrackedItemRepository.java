package com.lostark.tracker.repository;

import com.lostark.tracker.domain.TrackedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    Optional<TrackedItem> findByExternalItemId(String externalItemId);

    /**
     * Active watchlist items the collector polls each tick — also the Phase 17.4 detail-Stats backfill
     * target set (all items, materials and engraving books alike; {@code quick 260707-tzj} corrected the
     * earlier material-only scope once engravings' real detail element was found).
     */
    List<TrackedItem> findByActiveTrue();
}
