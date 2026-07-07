package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One element of the {@code GET /markets/items/{id}} detail array (Phase 17.4). Only {@code Stats[]} —
 * the last ~14 days of daily traded averages — is mapped; everything else is ignored. Used by the
 * detail-Stats backfill runner to fill past gaps (D-01 source ②). Note the endpoint may return several
 * of these per id (e.g. an engraving's bound trade-once variant vs. its freely traded variant);
 * {@code LostarkApiClient.getItemDetail} selects the element with real trade activity. {@code stats}
 * is never null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemDetailResponse(
        @JsonProperty("Stats") List<MarketStat> stats) {

    public ItemDetailResponse {
        stats = stats == null ? List.of() : stats;
    }
}
