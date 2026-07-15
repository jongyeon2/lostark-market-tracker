package com.lostark.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * One hourly measurement of a gem's 최저 즉시구매가 (Phase 27, GEM-03). A purely additive layer
 * alongside — never inside — {@code price_snapshot}: collect/cache/event-impact never touch this table.
 *
 * <p><b>No {@code tracked_item} FK.</b> A gem is not a tracked item: the auction response carries no
 * {@code Id} (Phase 24 §H5), so identity here IS {@code (series, level)}.
 *
 * <p><b>{@code minBuyPrice == null} is an answer, not a gap.</b> It means 경매장 responded and no
 * 즉시구매 listing existed (bid-only 매물 are real — Phase 24). "We could not ask" is expressed by the
 * ABSENCE of a row instead, which is why this entity carries no status field.
 *
 * <p><b>{@code recordedAt} vs {@code hourSlot}.</b> {@code recordedAt} is the real measurement instant;
 * rounding it to the hour would make a 10:05 sample claim it was taken at 10:00 (the same refusal
 * {@code EventImpactService} applies to inventing a timestamp for a daily average). {@code hourSlot} is
 * the truncated copy and exists only to carry the UNIQUE idempotency key.
 *
 * <p>This entity backs {@code ddl-auto=validate} against V9 and reads. The write is an idempotent
 * native insert in {@link com.lostark.tracker.repository.GemPriceSnapshotRepository}.
 */
@Entity
@Table(name = "gem_price_snapshot")
public class GemPriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series", nullable = false, length = 20)
    private String series;

    @Column(name = "level", nullable = false)
    private short level;

    /** NULL = 물어봤고 즉시구매 매물이 없었다. 행 자체가 없으면 = 못 물어봤다. */
    @Column(name = "min_buy_price")
    private Long minBuyPrice;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "hour_slot", nullable = false)
    private OffsetDateTime hourSlot;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected GemPriceSnapshot() {
    }

    public GemPriceSnapshot(String series, short level, Long minBuyPrice,
                           OffsetDateTime recordedAt, OffsetDateTime hourSlot) {
        this.series = series;
        this.level = level;
        this.minBuyPrice = minBuyPrice;
        this.recordedAt = recordedAt;
        this.hourSlot = hourSlot;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() {
        return id;
    }

    public String getSeries() {
        return series;
    }

    public short getLevel() {
        return level;
    }

    public Long getMinBuyPrice() {
        return minBuyPrice;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public OffsetDateTime getHourSlot() {
        return hourSlot;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
