package com.lostark.tracker.gem;

import com.lostark.tracker.domain.GemPriceSnapshot;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.repository.GemPriceSnapshotRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the 보석 recording contract (GEM-03) on Testcontainers Postgres. The external
 * {@link LostarkAuctionClient} is a {@link MockitoBean} so no live 경매장 call or API key is needed.
 *
 * <p>The load-bearing rule here is that the record NEVER guesses: a price and a definite
 * "즉시구매 매물 없음" are both answers and become rows, while a throttle or a failure records NOTHING —
 * because 경매장 has no history endpoint (Phase 24 §H5), a wrong row can never be corrected from source.
 */
@SpringBootTest
@ActiveProfiles("test")
class GemPriceRecorderIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkAuctionClient client;
    /** Mocked so the throttle is deterministic. Default: every token granted (collector idle). */
    @MockitoBean
    RedisTokenBucket rateLimiter;

    @Autowired
    GemPriceRecorder recorder;
    @Autowired
    GemPriceSnapshotRepository repository;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    private static final String LV8_GEOPHWA = "8레벨 겁화의 보석";
    private static final String LV10_JAKYEOL = "10레벨 작열의 보석";

    @BeforeEach
    void reset() {
        repository.deleteAll();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(1_000L));
    }

    /** The happy path: six gems answered → six rows, all sharing one hour slot. */
    @Test
    void recordsEveryGemAsOneRowPerHourSlot() {
        recorder.record();

        List<GemPriceSnapshot> rows = repository.findAll();
        assertThat(rows).hasSize(6);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getMinBuyPrice()).isEqualTo(1_000L);
            // hour_slot is recorded_at truncated — and recorded_at itself is NOT rounded.
            assertThat(row.getHourSlot()).isEqualTo(row.getRecordedAt().truncatedTo(ChronoUnit.HOURS));
        });
        // One batch = one slot, even if the calls straddled an hour boundary.
        assertThat(rows.stream().map(GemPriceSnapshot::getHourSlot).distinct()).hasSize(1);
        assertThat(rows.stream().map(r -> r.getSeries() + r.getLevel()).distinct()).hasSize(6);
    }

    /**
     * "즉시구매 매물이 없었다" is an ANSWER from 경매장 (bid-only 매물 are real — Phase 24), so it must be
     * recorded as a null price rather than dropped. Dropping it would make the gap indistinguishable
     * from an hour we never managed to ask about.
     */
    @Test
    void noBuyoutIsRecordedAsNullPriceNotAsMissingRow() {
        when(client.findLowestBuyPrice(LV10_JAKYEOL)).thenReturn(Optional.empty());

        recorder.record();

        assertThat(repository.findAll()).hasSize(6);
        GemPriceSnapshot noBuyout = repository.findAll().stream()
                .filter(r -> r.getSeries().equals("작열") && r.getLevel() == 10)
                .findFirst().orElseThrow();
        assertThat(noBuyout.getMinBuyPrice()).isNull();
    }

    /**
     * The Core Value guard: when the collector holds the budget, the recorder yields WITHOUT calling
     * 경매장 at all, and writes nothing. Recording a row here would invent a fact we never observed.
     */
    @Test
    void throttledRunLeavesNoRowsAndNeverCallsAuctionApi() {
        when(rateLimiter.tryAcquire()).thenReturn(false);

        recorder.record();

        assertThat(repository.findAll()).isEmpty();
        verify(client, never()).findLowestBuyPrice(anyString());
    }

    /**
     * A failure is not an answer — the failing gem leaves NO row (absence = "we could not ask"), while
     * the other five are recorded normally. Per-row isolation, same discipline as the serving path.
     */
    @Test
    void fetchFailureLeavesNoRowSoAbsenceMeansWeCouldNotAsk() {
        when(client.findLowestBuyPrice(LV8_GEOPHWA)).thenThrow(new RuntimeException("boom"));

        recorder.record();

        List<GemPriceSnapshot> rows = repository.findAll();
        assertThat(rows).hasSize(5);
        assertThat(rows).noneMatch(r -> r.getSeries().equals("겁화") && r.getLevel() == 8);
    }

    /**
     * Idempotency: a restart inside an already-recorded hour must not duplicate it. Phase 19 redeploys
     * on every main push, so boot-time records collide with existing slots as a matter of routine —
     * and the FIRST sample of the hour is kept, because a later sample is a different fact, not a
     * correction of the earlier one.
     */
    @Test
    void secondRunInSameHourDoesNotDuplicateAndKeepsTheFirstSample() {
        recorder.record();
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(9_999L));

        recorder.record();

        List<GemPriceSnapshot> rows = repository.findAll();
        assertThat(rows).hasSize(6);
        assertThat(rows).allMatch(r -> r.getMinBuyPrice() == 1_000L);
    }

    /**
     * Recording must not warm the serving cache: /api/gems promises a value at most 5 minutes old, and
     * an hourly write into gem:latest would silently make the screen serve an hour-old price.
     */
    @Test
    void recorderDoesNotTouchTheServingCache() {
        recorder.record();

        assertThat(redis.opsForValue().get(GemService.CACHE_KEY)).isNull();
    }

    /** Six gems = six calls, one per gem — 경매장 offers no batch lookup (Phase 24). */
    @Test
    void recordsSpendExactlyOneCallPerGem() {
        recorder.record();

        verify(client, times(6)).findLowestBuyPrice(anyString());
    }
}