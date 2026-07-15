package com.lostark.tracker.news;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * News read DTOs (D-02) — the {@code GET /api/news} contract consumed by the dashboard news panel
 * (17.2-03). Fields transcribe the live-measured Lostark {@code /news} schema locked in
 * {@code 17.2-NEWS-SPIKE-FINDINGS.md} (D-05): events carry title/link/기간(start~end)/thumbnail,
 * notices carry title/link/date/type. All fields are Strings — dates pass through as the source's
 * ISO-8601 local strings (no offset), so the frontend owns display formatting. These records
 * serialize to the client in camelCase (Spring's default mapper); the PascalCase inbound mapping from
 * Lostark lives in {@link LostarkNewsClient}.
 *
 * <p>Public metadata only — no price, key, or account data ever flows through these DTOs (D-06).
 */
public final class NewsDtos {

    private NewsDtos() {
    }

    /** One in-game event. {@code endDate} is the 종료임박 sort key (D-04). Public metadata only. */
    public record NewsEvent(String title, String link, String startDate, String endDate, String thumbnail) {

        /**
         * Whether this event is still running at {@code nowKst} — the 진행중 판정 shared by the poll-time
         * filter ({@link LostarkNewsClient#fetchEvents()}) and the serve-time filter
         * ({@link NewsService#getLatest()}), so both paths can never disagree.
         *
         * <p>{@code nowKst} is a parameter, not {@code LocalDateTime.now()}, because {@code endDate} is
         * the source's KST WALL-CLOCK string with NO offset ({@code "2026-07-08T06:00:00"}): comparing it
         * against a UTC instant would shift the verdict by 9 hours. Callers pass KST explicitly.
         *
         * <p>Boundary is INCLUSIVE (an event ending exactly now is still 진행중), and an {@code endDate}
         * we cannot read — null, blank, or unparseable (e.g. a 상시 event) — KEEPS the event: the filter
         * removes only events it can PROVE are over, never ones it merely fails to understand.
         */
        public boolean isOngoingAt(LocalDateTime nowKst) {
            if (endDate == null || endDate.isBlank()) {
                return true;
            }
            try {
                return !LocalDateTime.parse(endDate).isBefore(nowKst);
            } catch (DateTimeParseException e) {
                return true;
            }
        }
    }

    /** One official notice. {@code date} is the 최신순 sort key; {@code type} is an opaque badge string (D-04). */
    public record NewsNotice(String title, String link, String date, String type) {
    }

    /**
     * The {@code GET /api/news} envelope. {@code updatedAt} is the last successful refresh instant
     * (ISO-8601) or {@code null} when the cache is empty / never filled (D-07 honesty signal).
     */
    public record NewsResponse(List<NewsEvent> events, List<NewsNotice> notices, String updatedAt) {
    }
}
