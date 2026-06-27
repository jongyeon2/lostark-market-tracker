package com.lostark.tracker.repository;

import com.lostark.tracker.domain.CollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    /** The most recent collection run (by started_at) — the source for the collection health endpoint. */
    Optional<CollectionRun> findTopByOrderByStartedAtDesc();

    /**
     * Idempotency guard for the {@code seed} demo: true when a run with this {@code started_at} and
     * {@code status} already exists, so re-seeding the same 10-minute grid never inserts a duplicate
     * synthetic SUCCESS run.
     */
    boolean existsByStartedAtAndStatus(OffsetDateTime startedAt, String status);
}
