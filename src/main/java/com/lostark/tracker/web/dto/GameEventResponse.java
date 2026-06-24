package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;

import java.time.OffsetDateTime;

/**
 * Response body for a game event. Exposes the entity-stamped {@code createdAt}/{@code updatedAt}
 * (D-07) alongside the mutable fields so callers can confirm the timestamps were set by the entity,
 * never by the controller/service. All instants are UTC ISO-8601 ({@code ...Z}).
 */
public record GameEventResponse(
        Long id,
        EventType eventType,
        String title,
        OffsetDateTime occurredAt,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static GameEventResponse from(GameEvent event) {
        return new GameEventResponse(
                event.getId(),
                event.getEventType(),
                event.getTitle(),
                event.getOccurredAt(),
                event.getDescription(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}