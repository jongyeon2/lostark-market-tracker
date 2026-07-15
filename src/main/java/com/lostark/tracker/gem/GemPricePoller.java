package com.lostark.tracker.gem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly gem price record trigger (Phase 27, GEM-03). Fires a boot-time record then every
 * {@code gem.poll-interval-ms} (default 1h, fixedDelay so ticks never overlap) — its own bean, its own
 * table ({@code gem_price_snapshot}), completely separate from {@code PriceCollector}'s 10분 schedule.
 * Scheduling is enabled app-wide by {@code CollectionConfig @EnableScheduling}.
 *
 * <p>6 requests/hour = 0.1/min against the shared 100/min budget (Phase 24 §H2), i.e. 2% of what the
 * collector already spends — and every one of them goes through the shared token bucket first
 * ({@link GemPriceFetcher}), so the collector is never starved by this.
 *
 * <p><b>Why a boot-time record is safe here</b> even though Phase 19 redeploys on every {@code main}
 * push: the record is keyed by hour slot with {@code ON CONFLICT DO NOTHING}, so a restart inside an
 * already-recorded hour writes nothing. Deploy storms cost at most a few no-op calls, never duplicate
 * history.
 *
 * <p>Like the collector and the news poller, tests push {@code gem.initial-delay-ms} far out
 * (application-test.yml) so this never auto-fires during a test boot; ITs drive
 * {@link GemPriceRecorder#record()} directly with a mocked client. The {@code seed} profile pushes it
 * out too, keeping the keyless demo boot free of live 경매장 calls.
 */
@Component
public class GemPricePoller {

    private static final Logger log = LoggerFactory.getLogger(GemPricePoller.class);

    private final GemPriceRecorder recorder;

    public GemPricePoller(GemPriceRecorder recorder) {
        this.recorder = recorder;
    }

    @Scheduled(fixedDelayString = "${gem.poll-interval-ms:3600000}",
            initialDelayString = "${gem.initial-delay-ms:0}")
    public void poll() {
        log.debug("gem price poll tick");
        recorder.record();
    }
}
