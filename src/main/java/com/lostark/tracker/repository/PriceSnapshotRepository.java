package com.lostark.tracker.repository;

import com.lostark.tracker.domain.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    /** Idempotency guard: has this item already been snapshotted at this tick's collected_at? (D-16) */
    boolean existsByTrackedItem_IdAndCollectedAt(Long trackedItemId, OffsetDateTime collectedAt);

    /**
     * Read-only: the newest snapshot for an item (max collected_at) — the latest-price source on a
     * cache miss. Added alongside the insert-path guard above; the Phase 2 write path is untouched.
     */
    Optional<PriceSnapshot> findTopByTrackedItem_IdOrderByCollectedAtDesc(Long trackedItemId);

    /**
     * Read-only window finder (4A): an item's snapshots within {@code from <= collected_at <= to}
     * (inclusive {@code Between}), ascending by {@code collected_at} for direct chart consumption
     * (D-04). Boundaries are UTC instants. Backs {@link com.lostark.tracker.read.WindowQueryService}.
     */
    List<PriceSnapshot> findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc(
            Long trackedItemId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Server-side downsample (D-07): bucket an item's window by {@code date_trunc} and return
     * {@code avg(min_price)} + {@code count(*)} per bucket, ascending. Aggregation runs in
     * PostgreSQL, not Java. {@code unit} is a server-chosen whitelist value ({@code 'hour'}/{@code 'day'},
     * D-09) — never client input — so binding it as a parameter has no injection surface. Read-only;
     * the Phase 2 insert path is untouched.
     */
    @Query(value = """
            SELECT date_trunc(:unit, collected_at) AS "bucketStart",
                   avg(min_price)                  AS "avgMinPrice",
                   count(*)                        AS "sampleCount"
            FROM price_snapshot
            WHERE tracked_item_id = :id AND collected_at BETWEEN :from AND :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<PriceBucketView> aggregateByBucket(@Param("id") long trackedItemId,
                                            @Param("unit") String unit,
                                            @Param("from") OffsetDateTime from,
                                            @Param("to") OffsetDateTime to);
}
