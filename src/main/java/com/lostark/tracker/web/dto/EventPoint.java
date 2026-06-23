package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;

import java.time.OffsetDateTime;

/**
 * One event marker for the chart overlay: {@code occurredAt} (UTC ISO-8601, D-11) + {@code eventType}
 * + {@code title}. The timeline's {@code events} array is independent of {@code snapshots} so
 * downsampling (03-03) can shrink the price line without touching event markers (D-04 orthogonality).
 */
public record EventPoint(
        OffsetDateTime occurredAt,
        EventType eventType,
        String title
) {
    public static EventPoint from(GameEvent event) {
        return new EventPoint(event.getOccurredAt(), event.getEventType(), event.getTitle());
    }
}
