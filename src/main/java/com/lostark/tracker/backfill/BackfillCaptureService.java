package com.lostark.tracker.backfill;

import com.lostark.tracker.collect.ItemFetchResult;
import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Rides the collection tick to capture the free previous-day traded average ({@code YDayAvgPrice})
 * carried on the already-fetched list response into {@code item_daily_stats} (source=YDAY_AVG, D-01
 * source ①, BACKFILL-01). Builds a going-forward daily-average series for ALL watchlist items
 * (engraving books included) at zero extra API cost.
 *
 * <p><b>Fail-open (BACKFILL-04):</b> capture runs AFTER the snapshot is persisted and swallows any
 * runtime error — a backfill hiccup must never break price collection. Nothing but the item id is
 * logged (no API key / request body), matching the collection logging convention.
 */
@Service
public class BackfillCaptureService {

    private static final Logger log = LoggerFactory.getLogger(BackfillCaptureService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ItemDailyStatRepository repository;
    private final Clock clock;

    public BackfillCaptureService(ItemDailyStatRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Idempotently upsert this item's {@code YDayAvgPrice} for the previous KST day. No-op when the
     * API omitted the value. Never throws — a failure is logged and swallowed (fail-open).
     */
    public void captureYDayAvg(TrackedItem item, ItemFetchResult result) {
        if (result.yDayAvgPrice() == null) {
            return;
        }
        // YDayAvgPrice is the PREVIOUS day's average → stamp it on yesterday's KST date.
        LocalDate statDate = LocalDate.ofInstant(clock.instant(), KST).minusDays(1);
        try {
            repository.upsertDailyStat(item.getId(), statDate,
                    BigDecimal.valueOf(result.yDayAvgPrice()), DailyStatSource.YDAY_AVG.name());
        } catch (RuntimeException e) {
            log.warn("YDayAvgPrice backfill capture failed for item {}", item.getId(), e);
        }
    }
}
