package com.lostark.tracker.collect;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the tick-normalized collected_at rule (D-15): a run's collected_at is the run-start
 * instant in UTC truncated to the minute, so every snapshot in one tick shares it and a retried
 * tick within the same minute collides idempotently.
 */
class TickInstantNormalizationTest {

    @Test
    void truncatesToTheMinuteSoSameMinuteInstantsShareCollectedAt() {
        OffsetDateTime early = OffsetDateTime.parse("2026-06-22T09:15:10.500Z");
        OffsetDateTime late = OffsetDateTime.parse("2026-06-22T09:15:55.999Z");

        OffsetDateTime na = PriceCollector.normalizeCollectedAt(early);
        OffsetDateTime nb = PriceCollector.normalizeCollectedAt(late);

        assertThat(na).isEqualTo(nb);
        assertThat(na).isEqualTo(OffsetDateTime.parse("2026-06-22T09:15:00Z"));
    }

    @Test
    void normalizesToUtcRegardlessOfInputOffset() {
        OffsetDateTime kst = OffsetDateTime.parse("2026-06-22T18:15:30+09:00"); // == 09:15:30Z

        OffsetDateTime n = PriceCollector.normalizeCollectedAt(kst);

        assertThat(n).isEqualTo(OffsetDateTime.parse("2026-06-22T09:15:00Z"));
        assertThat(n.getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
