package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The {@code GET /markets/items/{id}} detail envelope (Phase 17.4). Only {@code Stats[]} — the last
 * ~14 days of daily traded averages — is mapped; everything else is ignored. Used by the detail-Stats
 * backfill runner to fill materials' past gaps (D-01 source ②). {@code stats} is never null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemDetailResponse(
        @JsonProperty("Stats") List<MarketStat> stats) {

    public ItemDetailResponse {
        stats = stats == null ? List.of() : stats;
    }
}
