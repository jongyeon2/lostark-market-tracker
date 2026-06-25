package com.lostark.tracker.seed;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic demo history so a freshly cloned reviewer sees a non-empty timeline and a
 * working event-impact WITHOUT a {@code LOSTARK_API_KEY} (DIST-03). This is a plain
 * {@code @Component} with NO {@code @Profile} — the generation logic is therefore unit-testable on
 * Testcontainers without profile gymnastics, and it produces NO behavior on its own. The thin
 * {@link SeedDataRunner} ({@code @Profile("seed")}) is the only thing that calls {@link #seed()} at
 * boot, so dev/test/prod-default profiles are untouched.
 *
 * <p>For every active watchlist item it writes {@code min_price} snapshots on a 10-minute cadence
 * spanning {@value #SEED_DAYS} days (≥ 7) ending at the current tick, then seeds 2 demo
 * {@code game_event}s. Both layers are idempotent: snapshots insert only when the DATA-01 unique key
 * {@code (tracked_item_id, collected_at)} is absent, and events seed only when {@code game_event} is
 * empty — so re-running the {@code seed} profile adds no duplicates and never clobbers admin data.
 *
 * <p><b>Event placement & the staleness contract.</b> Events are placed 5 minutes OFF the 10-minute
 * grid (midway between two seeded ticks) so the {@link com.lostark.tracker.read.EventImpactService}
 * anchor selection picks the tick 5 min BEFORE as {@code pre} and the tick 5 min AFTER as
 * {@code post} — two DISTINCT snapshots, both inside the 30-minute {@code STALENESS_ALLOWANCE}. That
 * yields {@code status:"ok"} with a REAL (non-zero) {@code change_rate}. (Placing an event exactly ON
 * a tick would make {@code pre} and {@code post} the same gap-0 snapshot and the rate 0.) The price
 * walk additionally guarantees adjacent ticks differ, so the two anchors never coincide in value.
 */
@Component
public class SyntheticDemoData {

    /** Synthetic span in days — > 7 so the demo timeline is comfortably non-trivial (DIST-03). */
    private static final int SEED_DAYS = 8;
    /** Collection cadence mirrored from the live collector: one snapshot every 10 minutes. */
    private static final Duration TICK = Duration.ofMinutes(10);
    /** 8 days × 24h × 6 ticks/h = 1152 ticks per item. */
    private static final int TICK_COUNT = SEED_DAYS * 24 * 6;

    /** A summary of what one {@link #seed()} run wrote — logged by {@link SeedDataRunner}. */
    public record SeedSummary(int snapshots, int events) {
    }

    private final TrackedItemRepository trackedItemRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final GameEventRepository gameEventRepository;
    private final Clock clock;

    public SyntheticDemoData(TrackedItemRepository trackedItemRepository,
                             PriceSnapshotRepository priceSnapshotRepository,
                             GameEventRepository gameEventRepository,
                             Clock clock) {
        this.trackedItemRepository = trackedItemRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.gameEventRepository = gameEventRepository;
        this.clock = clock;
    }

    /**
     * Populates idempotent synthetic history for every active item plus 2 demo events. Safe to call
     * repeatedly: per-tick and event-count guards make a second run a no-op.
     */
    @Transactional
    public SeedSummary seed() {
        OffsetDateTime gridNow = currentTickFloor();
        List<TrackedItem> items = trackedItemRepository.findByActiveTrue();

        int snapshots = 0;
        for (TrackedItem item : items) {
            snapshots += seedSnapshots(item, gridNow);
        }

        // Events are global (not per-item); seed them once when none exist so admin-entered events
        // are never duplicated or clobbered. Needs ≥1 item so the demo events have snapshots to anchor.
        int events = 0;
        if (!items.isEmpty() && gameEventRepository.count() == 0) {
            events = seedEvents(gridNow);
        }
        return new SeedSummary(snapshots, events);
    }

    private int seedSnapshots(TrackedItem item, OffsetDateTime gridNow) {
        long base = priceBase(item);
        int inserted = 0;
        long prevPrice = Long.MIN_VALUE;
        // Walk oldest -> newest so "previous" is the temporally adjacent earlier tick.
        for (int i = TICK_COUNT - 1; i >= 0; i--) {
            OffsetDateTime at = gridNow.minus(TICK.multipliedBy(i));
            long price = priceAt(base, item.getId(), i);
            if (price == prevPrice) {
                // Guarantee adjacent ticks differ so an event's pre/post anchors yield a non-zero rate.
                price += 1;
            }
            prevPrice = price;
            // Idempotent per-tick insert honoring the DATA-01 unique key (re-seed adds nothing).
            if (!priceSnapshotRepository.existsByTrackedItem_IdAndCollectedAt(item.getId(), at)) {
                priceSnapshotRepository.save(new PriceSnapshot(item, at, price, at));
                inserted++;
            }
        }
        return inserted;
    }

    private int seedEvents(OffsetDateTime gridNow) {
        // 5 minutes off the grid -> pre = tick 5 min before, post = tick 5 min after (both fresh).
        OffsetDateTime e1 = gridNow.minusDays(3).plusMinutes(5);
        OffsetDateTime e2 = gridNow.minusDays(5).plusMinutes(5);
        gameEventRepository.save(new GameEvent(EventType.LOA_ON, "로아ON 쇼케이스", e1, "데모용 합성 이벤트"));
        gameEventRepository.save(new GameEvent(EventType.MAJOR_UPDATE, "대규모 업데이트", e2, "데모용 합성 이벤트"));
        return 2;
    }

    /** The current instant floored to the 10-minute grid (seconds/nanos zeroed), in UTC. */
    private OffsetDateTime currentTickFloor() {
        OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        return now.withMinute((now.getMinute() / 10) * 10);
    }

    /** A stable per-item base price in [1000, 5000], derived deterministically from the external id. */
    private static long priceBase(TrackedItem item) {
        int h = Math.abs(item.getExternalItemId().hashCode());
        return 1000L + (h % 9) * 500L;
    }

    /**
     * A deterministic pseudo-random price for one tick: a gentle sine swing (±15%, ~one cycle per 6h)
     * plus reproducible jitter (±2%) seeded off {@code itemId + tickIndex} — so the demo series is
     * identical run-to-run. Floored at 1.
     */
    private static long priceAt(long base, long itemId, int tickIndex) {
        double wave = Math.sin(tickIndex / 36.0) * 0.15;
        double jitter = (new Random(itemId * 1_000_003L + tickIndex).nextDouble() - 0.5) * 0.04;
        return Math.max(1L, Math.round(base * (1.0 + wave + jitter)));
    }
}
