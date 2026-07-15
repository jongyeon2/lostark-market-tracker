package com.lostark.tracker.news;

import com.lostark.tracker.news.NewsDtos.NewsEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the 진행중 판정 rule behind the news panel's expired-event filter (요청 4번).
 *
 * <p>{@code endDate} arrives as the source's ISO-8601 LOCAL (KST wall-clock) string WITHOUT an offset,
 * so {@link NewsEvent#isOngoingAt(LocalDateTime)} takes the KST "now" as a parameter — the 9-hour
 * off-by-one that an {@code Instant}-based comparison would introduce is impossible to write here, and
 * every case below is time-independent (fixed dates, explicit now).
 *
 * <p>The honesty rule: a missing/unparseable {@code endDate} KEEPS the event. Dropping an event we
 * cannot judge would silently hide 상시 이벤트 (no end date) — the filter only ever removes events it
 * can PROVE are over.
 */
class NewsEventOngoingTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-07-15T10:00:00");

    private static NewsEvent endingAt(String endDate) {
        return new NewsEvent("이벤트", "https://lostark.game.onstove.com/e", "2026-07-01T06:00:00",
                endDate, "https://cdn-lostark.game.onstove.com/t.jpg");
    }

    @Test
    void endedEventIsNotOngoing() {
        // The live 7/8-종료 case that leaked onto the 7/15 dashboard.
        assertThat(endingAt("2026-07-08T06:00:00").isOngoingAt(NOW)).isFalse();
    }

    @Test
    void futureEndDateIsOngoing() {
        assertThat(endingAt("2026-07-22T05:59:00").isOngoingAt(NOW)).isTrue();
    }

    @Test
    void endDateExactlyNowIsStillOngoing() {
        // Boundary INCLUSIVE — an event is over only once its endDate is in the past.
        assertThat(endingAt("2026-07-15T10:00:00").isOngoingAt(NOW)).isTrue();
    }

    @Test
    void nullOrBlankEndDateIsKept() {
        assertThat(endingAt(null).isOngoingAt(NOW)).isTrue();
        assertThat(endingAt("").isOngoingAt(NOW)).isTrue();
    }

    @Test
    void unparseableEndDateIsKept() {
        assertThat(endingAt("상시").isOngoingAt(NOW)).isTrue();
    }

    @Test
    void endDateIsReadAsKstWallClockNotUtc() {
        // "2026-07-15T06:00:00" is 06:00 KST — already past at 10:00 KST. Were it (wrongly) read as
        // UTC it would resolve to 15:00 KST and wrongly survive as 진행중.
        assertThat(endingAt("2026-07-15T06:00:00").isOngoingAt(NOW)).isFalse();
    }
}
