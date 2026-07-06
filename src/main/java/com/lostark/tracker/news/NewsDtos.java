package com.lostark.tracker.news;

import java.util.List;

/**
 * News read DTOs (D-02) — the {@code GET /api/news} contract consumed by the dashboard news panel
 * (17.2-03). Fields transcribe the live-measured Lostark {@code /news} schema locked in
 * {@code 17.2-NEWS-SPIKE-FINDINGS.md} (D-05): events carry title/link/기간(start~end)/thumbnail,
 * notices carry title/link/date/type. All fields are Strings — dates pass through as the source's
 * ISO-8601 local strings (no offset), so no server-side {@code LocalDateTime} parsing is needed and
 * the frontend owns display formatting. These records serialize to the client in camelCase (Spring's
 * default mapper); the PascalCase inbound mapping from Lostark lives in {@link LostarkNewsClient}.
 *
 * <p>Public metadata only — no price, key, or account data ever flows through these DTOs (D-06).
 */
public final class NewsDtos {

    private NewsDtos() {
    }

    /** One in-game event. {@code endDate} is the 종료임박 sort key (D-04). Public metadata only. */
    public record NewsEvent(String title, String link, String startDate, String endDate, String thumbnail) {
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
