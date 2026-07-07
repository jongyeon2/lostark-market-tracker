package com.lostark.tracker.backfill;

import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.collect.dto.ItemDetailResponse;
import com.lostark.tracker.collect.dto.MarketStat;
import com.lostark.tracker.collect.error.AuthApiException;
import com.lostark.tracker.domain.DailyStatSource;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Backfills materials' past daily-average gaps from the detail API {@code Stats[]} (Phase 17.4,
 * D-01 source ②, BACKFILL-02). Runs on startup ({@link ApplicationRunner}) AND once a day
 * ({@link Scheduled} cron) so a restart re-fills the last ~14 days and long-running servers refresh
 * daily — both idempotent (D-03). Materials only ({@code roleGroup=MATERIAL}); engraving books return
 * 0 in detail Stats so they are excluded (their backfill is 17.4-02's going-forward YDayAvgPrice).
 *
 * <p>Respects the shared {@link RedisTokenBucket} budget ({@code tryAcquire} — skip on exhaustion) and
 * is per-item fail-open, with a fatal-auth short-circuit. Only the item id is logged (never the key).
 * Excluded from the {@code test} profile (like {@code WatchlistSeeder}) so it never auto-fires real API
 * calls during integration tests; {@code DetailStatsBackfillRunnerIT} drives {@link #backfill()} directly.
 */
@Component
@Profile("!test")
public class DetailStatsBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DetailStatsBackfillRunner.class);
    private static final String MATERIAL = "MATERIAL";

    private final LostarkApiClient apiClient;
    private final TrackedItemRepository trackedItemRepo;
    private final ItemDailyStatRepository dailyStatRepo;
    private final RedisTokenBucket rateLimiter;

    public DetailStatsBackfillRunner(LostarkApiClient apiClient,
                                     TrackedItemRepository trackedItemRepo,
                                     ItemDailyStatRepository dailyStatRepo,
                                     RedisTokenBucket rateLimiter) {
        this.apiClient = apiClient;
        this.trackedItemRepo = trackedItemRepo;
        this.dailyStatRepo = dailyStatRepo;
        this.rateLimiter = rateLimiter;
    }

    /** Startup trigger (D-03): re-fill the last ~14 days on every boot. */
    @Override
    public void run(ApplicationArguments args) {
        backfill();
    }

    /** Once-a-day trigger (D-03): @EnableScheduling is app-wide (CollectionConfig). */
    @Scheduled(cron = "${backfill.detail-cron:0 30 4 * * *}")
    public void scheduledBackfill() {
        backfill();
    }

    void backfill() {
        for (TrackedItem item : trackedItemRepo.findByActiveTrueAndRoleGroup(MATERIAL)) {
            if (!rateLimiter.tryAcquire()) {
                log.info("detail backfill skipped item {} — rate-limit budget exhausted", item.getId());
                continue;
            }
            try {
                ItemDetailResponse detail = apiClient.getItemDetail(Long.parseLong(item.getExternalItemId()));
                for (MarketStat stat : detail.stats()) {
                    // Skip engraving/unfilled entries (AvgPrice 0) — only real material averages backfill.
                    if (stat.avgPrice() == null || stat.avgPrice() <= 0) {
                        continue;
                    }
                    LocalDate day = LocalDate.parse(stat.date().substring(0, 10));
                    dailyStatRepo.upsertDailyStat(item.getId(), day,
                            BigDecimal.valueOf(stat.avgPrice()), DailyStatSource.DETAIL_STATS.name());
                }
            } catch (AuthApiException e) {
                // Fatal auth: stop the whole run (no key in the message).
                log.warn("detail backfill stopped — fatal auth");
                break;
            } catch (RuntimeException e) {
                // Per-item fail-open: one bad item never aborts the run; next trigger retries.
                log.warn("detail backfill failed for item {}", item.getId(), e);
            }
        }
    }
}
