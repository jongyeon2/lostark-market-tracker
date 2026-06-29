package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.TrackedItem;

public record TrackedItemResponse(
        Long id,
        String externalItemId,
        String displayName,
        String category,
        boolean active,
        String iconUrl,
        String itemGroup,
        String roleGroup
) {
    public static TrackedItemResponse from(TrackedItem item) {
        return new TrackedItemResponse(
                item.getId(),
                item.getExternalItemId(),
                item.getDisplayName(),
                item.getCategory(),
                item.isActive(),
                item.getIconUrl(),
                item.getItemGroup(),
                item.getRoleGroup()
        );
    }
}
