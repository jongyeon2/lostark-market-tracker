package com.lostark.tracker.repository;

import com.lostark.tracker.domain.CollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    /** The most recent collection run (by started_at) — the source for the collection health endpoint. */
    Optional<CollectionRun> findTopByOrderByStartedAtDesc();
}
