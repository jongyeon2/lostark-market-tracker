package com.lostark.tracker.seed;

import com.lostark.tracker.seed.SyntheticDemoData.SeedSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Boots the synthetic demo seed under the {@code seed} profile ONLY (DIST-03). Ordered AFTER
 * {@link com.lostark.tracker.collect.WatchlistSeeder} ({@code @Order(1)}) so the watchlist items
 * exist before {@link SyntheticDemoData#seed()} reads {@code findByActiveTrue()}.
 *
 * <p>Logs only the returned counts — never a request body, header, or secret. The live
 * {@code @Scheduled} collector stays quiet under {@code seed} (long {@code collection.initial-delay-ms}
 * in {@code application-seed.yml}), so the demo needs no {@code LOSTARK_API_KEY}.
 */
@Component
@Profile("seed")
@Order(2)
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final SyntheticDemoData syntheticDemoData;

    public SeedDataRunner(SyntheticDemoData syntheticDemoData) {
        this.syntheticDemoData = syntheticDemoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        SeedSummary summary = syntheticDemoData.seed();
        log.info("[seed] synthetic demo data ready: {} price snapshots, {} game events, {} collection runs",
                summary.snapshots(), summary.events(), summary.runs());
    }
}
