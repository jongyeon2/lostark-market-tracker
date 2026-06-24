package com.lostark.tracker.web.dto;

import java.util.List;

/**
 * The event-impact read payload (IMPACT-01, D-07): the request echoed ({@code itemId} + {@code window}
 * hours) plus a per-event {@code events} list in {@code occurred_at} DESCENDING order. {@code game_event}
 * is global, so EVERY event is evaluated against the queried item; an existing item with zero events
 * yields an empty {@code events} array (a 200, not a 404).
 */
public record EventImpactResponse(
        long itemId,
        int window,
        List<EventImpactItem> events
) {
}
