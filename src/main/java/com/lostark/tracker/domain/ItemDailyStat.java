package com.lostark.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * One backfilled daily-average price for an item on a given calendar day, tagged with its
 * {@link DailyStatSource} (Phase 17.4, D-05). A purely additive layer alongside — never inside —
 * {@code price_snapshot}: collect/cache/event-impact never touch this table.
 *
 * <p>This entity backs read/validation ({@code ddl-auto=validate} against V6). The actual write is
 * an idempotent native upsert in {@link com.lostark.tracker.repository.ItemDailyStatRepository}, so
 * a re-run refreshes {@code avgPrice} in place instead of appending duplicates (D-03). Timestamps
 * follow the {@code GameEvent} @PrePersist/@PreUpdate pattern.
 */
@Entity
@Table(name = "item_daily_stats")
public class ItemDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Scalar FK column (no @ManyToOne): the backfill layer references tracked_item by id only,
    // keeping this table decoupled from the price_snapshot association graph.
    @Column(name = "tracked_item_id", nullable = false)
    private Long trackedItemId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal avgPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private DailyStatSource source;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ItemDailyStat() {
    }

    public ItemDailyStat(Long trackedItemId, LocalDate statDate, BigDecimal avgPrice, DailyStatSource source) {
        this.trackedItemId = trackedItemId;
        this.statDate = statDate;
        this.avgPrice = avgPrice;
        this.source = source;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getTrackedItemId() {
        return trackedItemId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public DailyStatSource getSource() {
        return source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
