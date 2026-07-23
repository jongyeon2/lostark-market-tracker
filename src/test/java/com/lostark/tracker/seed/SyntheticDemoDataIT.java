package com.lostark.tracker.seed;

import com.lostark.tracker.domain.CollectionRun;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.seed.SyntheticDemoData.SeedSummary;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.EventImpactItem;
import com.lostark.tracker.web.dto.EventImpactResponse;
import com.lostark.tracker.web.dto.TimelineResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the {@code seed} synthetic data yields the headline demo (DIST-03, ROADMAP success criteria
 * 2 & 4) end-to-end on Testcontainers: after {@link SyntheticDemoData#seed()}, the timeline read is
 * non-empty AND event-impact returns ≥1 event with {@code status:"ok"} and a non-null (real)
 * {@code change_rate} — not all {@code insufficient_data}. Also proves a second {@code seed()} is
 * idempotent (snapshot count unchanged).
 *
 * <p>Runs under the {@code test} profile, where {@code WatchlistSeeder} is inactive
 * ({@code @Profile({dev,seed})}), so the test creates its own active item fixture and drives
 * {@code seed()} directly. The query window is derived from the SAME {@link Clock} the seeder
 * uses, so it always covers the synthetic span regardless of wall-clock time.
 *
 * <p>🔑 That clock is FROZEN per test ({@link #frozenClock}), not the application's
 * {@code Clock.systemUTC()} bean. {@link SyntheticDemoData#seed()} re-derives its 10-minute tick
 * grid from the clock on EVERY call, so two {@code seed()} calls that straddle a grid boundary
 * compute different grids and the second one writes a NEW tick — breaking the idempotence the two
 * double-seed tests below assert. That is not hypothetical: CI failed at exactly
 * {@code 05:30:00.785Z} (quick-260723-kcs). {@code WatchlistSeederIT} pins the clock for the same
 * reason (quick-260713-mur); this class was missed at the time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SyntheticDemoDataIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    CollectionRunRepository collectionRunRepository;
    /** The application clock ({@code Clock.systemUTC()}) — only used to source the frozen instant. */
    @Autowired
    Clock appClock;

    /**
     * The application clock frozen at the start of each test. Freezing (rather than moving the tests
     * to some fixed past date) keeps the seeded data where it has always been — around real "now" —
     * so the timeline window and the stale-run ordering below still mean what they meant before;
     * it only removes the grid-boundary race. See the class javadoc.
     */
    Clock frozenClock;

    /** The subject, rebuilt per test on {@link #frozenClock} instead of the autowired bean. */
    SyntheticDemoData syntheticDemoData;

    @BeforeEach
    void clean() {
        // FK order: child snapshots first, then items, then events (mirrors the read ITs).
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
        // collection_run has no FK to the above — clear it so each test starts from a known run history.
        collectionRunRepository.deleteAll();

        frozenClock = Clock.fixed(Instant.now(appClock), ZoneOffset.UTC);
        syntheticDemoData = new SyntheticDemoData(trackedItemRepository, priceSnapshotRepository,
                gameEventRepository, collectionRunRepository, frozenClock);
    }

    @Test
    void seedYieldsNonEmptyTimelineAndAnEventImpactOk() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("9001", "데모 아이템", "50010"));

        SeedSummary summary = syntheticDemoData.seed();
        assertThat(summary.snapshots()).isPositive();
        assertThat(summary.events()).isGreaterThanOrEqualTo(2);

        // Timeline: a window derived from the seeder's own clock, comfortably bracketing the 8-day span.
        OffsetDateTime now = OffsetDateTime.now(frozenClock);
        ResponseEntity<TimelineResponse> timeline = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                item.getId(), now.minusDays(9).toString(), now.plusHours(1).toString());
        assertThat(timeline.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(timeline.getBody()).isNotNull();
        assertThat(timeline.getBody().snapshots()).isNotEmpty();

        // Event-impact: ≥1 event with a real (non-null) change_rate and status "ok" — the §2 demo.
        ResponseEntity<EventImpactResponse> impact = rest.getForEntity(
                "/api/items/{id}/event-impact?window={w}", EventImpactResponse.class, item.getId(), 24);
        assertThat(impact.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(impact.getBody()).isNotNull();
        assertThat(impact.getBody().events()).isNotEmpty();
        assertThat(impact.getBody().events())
                .anySatisfy(e -> {
                    assertThat(e.status()).isEqualTo("ok");
                    assertThat(e.changeRate()).isNotNull();
                });
        // The "ok" event must carry a genuine, non-zero correlation (proves distinct pre/post anchors).
        EventImpactItem ok = impact.getBody().events().stream()
                .filter(e -> "ok".equals(e.status()))
                .findFirst().orElseThrow();
        assertThat(ok.changeRate().signum()).isNotZero();
        assertThat(ok.prePrice()).isNotNull();
        assertThat(ok.postPrice()).isNotNull();
    }

    @Test
    void seedIsIdempotent() {
        trackedItemRepository.save(new TrackedItem("9001", "데모 아이템", "50010"));

        syntheticDemoData.seed();
        long afterFirst = priceSnapshotRepository.count();
        assertThat(afterFirst).isPositive();

        syntheticDemoData.seed();
        long afterSecond = priceSnapshotRepository.count();

        // Per-tick existsBy guard + event count() guard make a second run a no-op — but only within
        // ONE tick, which is why the clock is frozen (class javadoc). seed() is a "top up the grid"
        // seeder: a later tick producing a new row is CORRECT behaviour, not a bug to assert against.
        assertThat(afterSecond).isEqualTo(afterFirst);
        assertThat(gameEventRepository.count()).isEqualTo(2);
    }

    @Test
    void seedWritesSuccessRunThatBeatsAStalePersistedAuthErrorRun() {
        trackedItemRepository.save(new TrackedItem("9001", "데모 아이템", "50010"));

        // A stale keyless AUTH_ERROR run left in a persisted volume — the very face seed must override.
        OffsetDateTime now = OffsetDateTime.now(frozenClock);
        CollectionRun stale = new CollectionRun(now.minusHours(1), now.minusHours(1), 12, 0, 12, "FAILED");
        stale.setSummaryMessage("AUTH_ERROR");
        collectionRunRepository.save(stale);

        syntheticDemoData.seed();

        // The newest run (startedAt=gridNow) must now be the synthetic SUCCESS — what the health card reads.
        CollectionRun latest = collectionRunRepository.findTopByOrderByStartedAtDesc().orElseThrow();
        assertThat(latest.getStatus()).isEqualTo("SUCCESS");
        assertThat(latest.getItemsAttempted()).isEqualTo(1);
        assertThat(latest.getItemsSucceeded()).isEqualTo(1);
        assertThat(latest.getItemsFailed()).isZero();
        assertThat(latest.getSummaryMessage()).isNull();

        // Idempotent on the same 10-minute grid: a second seed() adds no duplicate SUCCESS run.
        // "the same grid" is guaranteed by the frozen clock, not by luck (class javadoc).
        syntheticDemoData.seed();
        assertThat(collectionRunRepository.count()).isEqualTo(2); // stale FAILED + one synthetic SUCCESS
    }
}
