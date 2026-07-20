package com.lostark.tracker.web.dto;

import java.util.List;

/**
 * The event-impact read payload (IMPACT-01, D-07): the request echoed ({@code itemId} + {@code window}
 * hours) plus a per-event {@code events} list in the requested {@code occurred_at} order.
 * {@code game_event} is global, so every event matching the filter is evaluated against the queried
 * item; an existing item with zero matching events yields an empty {@code events} array (a 200, not a 404).
 *
 * <p>{@code totalCount} is how many events matched the TYPE FILTER, before {@code limit} — not
 * {@code events.size()}. The client needs the distinction to say "전체 N건 중 M건" without lying and
 * to know when 더 보기 has nothing left to fetch. Reporting only the returned size would silently
 * hide that results were cut.
 */
public record EventImpactResponse(
        long itemId,
        int window,
        long totalCount,
        List<EventImpactItem> events
) {
}
