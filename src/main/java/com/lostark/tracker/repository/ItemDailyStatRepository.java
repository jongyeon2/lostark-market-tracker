package com.lostark.tracker.repository;

import com.lostark.tracker.domain.ItemDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Storage contract for the backfill daily-average layer (Phase 17.4, plan 01). The idempotent
 * {@link #upsertDailyStat} is written by 17.4-02 (YDayAvgPrice capture) and 17.4-03 (detail-Stats
 * backfill); the window finder is read by 17.4-04 (timeline read merge).
 */
public interface ItemDailyStatRepository extends JpaRepository<ItemDailyStat, Long> {

    /**
     * Idempotent daily-average upsert (D-03): inserts one row per (item, day, source), or refreshes
     * {@code avg_price}/{@code updated_at} in place on conflict — so a server restart or the once-a-day
     * re-run never appends duplicates. PostgreSQL {@code ON CONFLICT} on the {@code uq_item_daily_stats}
     * unique key. {@code source} is bound as the {@link com.lostark.tracker.domain.DailyStatSource}
     * enum name; parameters are bound (no string assembly → no injection surface).
     *
     * <p>{@code @Transactional} makes the native modifying query self-sufficient — the write commits
     * whether the caller (backfill service / runner, or an IT calling directly) supplies an ambient
     * transaction or not. {@code clearAutomatically} flushes the persistence context so a subsequent
     * read sees the committed value.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO item_daily_stats (tracked_item_id, stat_date, avg_price, source, created_at, updated_at)
            VALUES (:trackedItemId, :statDate, :avgPrice, :source, now(), now())
            ON CONFLICT (tracked_item_id, stat_date, source)
            DO UPDATE SET avg_price = EXCLUDED.avg_price, updated_at = now()
            """)
    int upsertDailyStat(@Param("trackedItemId") long trackedItemId,
                        @Param("statDate") LocalDate statDate,
                        @Param("avgPrice") BigDecimal avgPrice,
                        @Param("source") String source);

    /**
     * Read-only window finder for the timeline read merge (17.4-04): an item's daily averages within
     * {@code from <= stat_date <= to} (inclusive {@code Between}), ascending by {@code stat_date} for
     * direct chart consumption. Returns rows of every source; the caller separates by
     * {@link com.lostark.tracker.domain.DailyStatSource} if needed.
     */
    List<ItemDailyStat> findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(
            Long trackedItemId, LocalDate from, LocalDate to);
}
