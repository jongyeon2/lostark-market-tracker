package com.lostark.tracker.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a tracked item. Bound instead of the entity to avoid mass-assignment
 * of id/active (T-02-01).
 */
public record TrackedItemRequest(
        @NotBlank String externalItemId,
        @NotBlank String displayName,
        String category
) {
}
