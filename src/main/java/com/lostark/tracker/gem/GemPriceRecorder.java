package com.lostark.tracker.gem;

import com.lostark.tracker.gem.GemDtos.GemPrice;
import com.lostark.tracker.gem.GemDtos.GemPriceStatus;
import com.lostark.tracker.repository.GemPriceSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Records one hourly sample of every gem's 최저 즉시구매가 into {@code gem_price_snapshot}
 * (Phase 27, GEM-03).
 *
 * <p><b>Why record at all, when Phase 26 said 보석은 시계열 미기록:</b> 경매장 has no history endpoint and
 * gems have no {@code Id} (Phase 24 §H5), so unlike 거래소 items — whose past two weeks can be
 * backfilled from {@code Stats[]} (Phase 17.4) — a gem's past can NEVER be recovered. Whatever is not
 * recorded as it happens is gone for good. The old constraint was aimed at a 5분 poller
 * (1,728 calls/day, 1.2/min off the shared budget); at 1시간 this costs 144 calls/day = 0.1/min, i.e.
 * 2% of the collector's ~7,056. The rule was about frequency, not about recording.
 *
 * <p><b>Only answers are recorded.</b> {@code OK} and {@code NO_BUYOUT} are both answers from 경매장, so
 * both become rows (the latter with a null price). {@code RATE_LIMITED}/{@code FETCH_FAILED} mean we
 * never got an answer — writing anything for them would put a guess in the history. Their absence is
 * the honest record, and it is exactly what lets a future reader tell "매물이 없었다" (null price) from
 * "그 시각 우리가 못 물어봤다" (no row) without a status column.
 *
 * <p>Never touches {@code gem:latest}: see {@link GemService} for why recording must not warm the
 * serving cache.
 */
@Service
public class GemPriceRecorder {

    private static final Logger log = LoggerFactory.getLogger(GemPriceRecorder.class);

    private final GemPriceFetcher fetcher;
    private final GemPriceSnapshotRepository repository;
    private final Clock clock;

    public GemPriceRecorder(GemPriceFetcher fetcher, GemPriceSnapshotRepository repository, Clock clock) {
        this.fetcher = fetcher;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Fetch all six gems and record the ones that answered.
     *
     * <p>{@code recordedAt} is stamped ONCE for the whole batch so the six rows share one hour slot even
     * if the calls straddle the hour boundary — otherwise a batch starting at 10:59:58 would scatter
     * across two slots and record a partial hour twice.
     */
    public void record() {
        OffsetDateTime recordedAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime hourSlot = recordedAt.truncatedTo(ChronoUnit.HOURS);

        List<GemPrice> gems = fetcher.fetchAll();
        int recorded = 0;
        int skipped = 0;
        for (GemPrice gem : gems) {
            if (!isAnswer(gem.status())) {
                skipped++;
                continue;
            }
            // DO NOTHING on conflict → returns 0 when this hour was already recorded (restart/redeploy).
            recorded += repository.insertIfAbsent(
                    gem.series(), (short) gem.level(), gem.minBuyPrice(), recordedAt, hourSlot);
        }
        log.info("gem price record: {} rows inserted, {} unanswered (skipped), slot={}",
                recorded, skipped, hourSlot);
    }

    /** An answer from 경매장 — a price, or a definite "즉시구매 매물 없음". Not-asked is never recorded. */
    private static boolean isAnswer(GemPriceStatus status) {
        return status == GemPriceStatus.OK || status == GemPriceStatus.NO_BUYOUT;
    }
}
