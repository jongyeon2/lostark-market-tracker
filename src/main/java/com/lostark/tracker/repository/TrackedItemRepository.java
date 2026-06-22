package com.lostark.tracker.repository;

import com.lostark.tracker.domain.TrackedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    Optional<TrackedItem> findByExternalItemId(String externalItemId);

    /** Active watchlist items the collector polls each tick. */
    List<TrackedItem> findByActiveTrue();
}
