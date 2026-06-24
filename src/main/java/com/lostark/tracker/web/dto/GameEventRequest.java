package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Request body for creating ({@code POST}) and full-replacing ({@code PUT}) a game event (D-06).
 * Bound instead of the {@code GameEvent} entity so {@code id}/{@code createdAt}/{@code updatedAt}
 * cannot be mass-assigned from the body (D-07, D-10). The three core fields are required; a
 * {@code @Valid} violation returns 400 on the shared error contract. {@code description} is optional.
 */
public record GameEventRequest(
        @NotNull EventType eventType,
        @NotBlank String title,
        @NotNull OffsetDateTime occurredAt,
        String description
) {
}