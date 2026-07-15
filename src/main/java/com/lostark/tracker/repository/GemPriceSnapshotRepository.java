package com.lostark.tracker.repository;

import com.lostark.tracker.domain.GemPriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Storage contract for the hourly gem price record (Phase 27, GEM-03).
 */
public interface GemPriceSnapshotRepository extends JpaRepository<GemPriceSnapshot, Long> {

    /**
     * Idempotent hourly insert: one row per (series, level, hour_slot), or NOTHING on conflict.
     *
     * <p><b>Why DO NOTHING and not DO UPDATE</b> (the opposite of {@code ItemDailyStatRepository}):
     * a daily average is a running estimate that gets more accurate as the day fills, so refreshing it
     * in place is right. A gem price is a MEASUREMENT — the 10:05 sample is not made wrong by a 10:40
     * one, it is simply a different fact about a different instant. First sample of the hour wins, and
     * the record stays immutable.
     *
     * <p>This is also what makes the poller safe under Phase 19's CI/CD, which redeploys on every
     * {@code main} push: a boot-time record right after a restart collides with the hour already
     * written and is silently dropped, instead of stacking duplicate rows for that hour.
     *
     * <p>Parameters are bound (no string assembly → no injection surface). {@code @Transactional} makes
     * the native modifying query self-sufficient whether or not the caller supplies a transaction.
     *
     * @return 1 when the row was inserted, 0 when the hour was already recorded
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO gem_price_snapshot (series, level, min_buy_price, recorded_at, hour_slot, created_at)
            VALUES (:series, :level, :minBuyPrice, :recordedAt, :hourSlot, now())
            ON CONFLICT (series, level, hour_slot)
            DO NOTHING
            """)
    int insertIfAbsent(@Param("series") String series,
                       @Param("level") short level,
                       @Param("minBuyPrice") Long minBuyPrice,
                       @Param("recordedAt") OffsetDateTime recordedAt,
                       @Param("hourSlot") OffsetDateTime hourSlot);

    /**
     * Read-only window finder — a gem's samples within {@code from <= recorded_at <= to} (inclusive),
     * ascending for direct chart/analysis consumption. Rows with a null {@code minBuyPrice} are
     * included: "즉시구매 매물이 없었다" is part of the history, not a hole in it.
     */
    List<GemPriceSnapshot> findBySeriesAndLevelAndRecordedAtBetweenOrderByRecordedAtAsc(
            String series, short level, OffsetDateTime from, OffsetDateTime to);
}
