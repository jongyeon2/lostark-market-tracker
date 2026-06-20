package com.lostark.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

/**
 * One collected price point for a tracked item at a tick-normalized instant.
 * {@code UNIQUE(tracked_item_id, collected_at)} makes collection idempotent (DATA-01).
 * {@code avg_price} / {@code trade_count} are deferred to Task 0 (D-06).
 */
@Entity
@Table(
        name = "price_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_price_snapshot_item_time",
                columnNames = {"tracked_item_id", "collected_at"}
        )
)
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracked_item_id", nullable = false)
    private TrackedItem trackedItem;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    @Column(name = "min_price")
    private Long minPrice;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    protected PriceSnapshot() {
    }

    public PriceSnapshot(TrackedItem trackedItem, OffsetDateTime collectedAt, Long minPrice, OffsetDateTime fetchedAt) {
        this.trackedItem = trackedItem;
        this.collectedAt = collectedAt;
        this.minPrice = minPrice;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public TrackedItem getTrackedItem() {
        return trackedItem;
    }

    public OffsetDateTime getCollectedAt() {
        return collectedAt;
    }

    public Long getMinPrice() {
        return minPrice;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }
}
