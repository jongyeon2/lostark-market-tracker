package com.lostark.tracker.repository;

import com.lostark.tracker.domain.TrackedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    Optional<TrackedItem> findByExternalItemId(String externalItemId);

    /** Active watchlist items the collector polls each tick. */
    List<TrackedItem> findByActiveTrue();

    /**
     * Active items in one role group — the Phase 17.4 detail-Stats backfill targets materials
     * ({@code roleGroup="MATERIAL"}) only (engraving books return 0 in detail Stats, D-01 source ②).
     */
    List<TrackedItem> findByActiveTrueAndRoleGroup(String roleGroup);
}
