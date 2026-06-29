package com.lostark.tracker.web.dto;

import java.util.List;

/**
 * The controller-assembled event-impact payload: {@link EventImpactResponse}'s
 * {@code itemId}/{@code window}/{@code events} plus the item's static enrichment
 * ({@code iconUrl}/{@code itemGroup}/{@code roleGroup}) as top-level metadata.
 *
 * <p>This wrapper exists so the read-path enrichment is additive WITHOUT touching
 * {@code EventImpactService} or {@code EventImpactResponse} (ITEM-04: those build the correlation and
 * stay 0-line). The controller calls the service unchanged, then wraps the result with enrichment
 * read via findById. The {@code events} list is passed through verbatim.
 */
public record EnrichedEventImpactResponse(
        long itemId,
        int window,
        String iconUrl,
        String itemGroup,
        String roleGroup,
        List<EventImpactItem> events
) {
}
