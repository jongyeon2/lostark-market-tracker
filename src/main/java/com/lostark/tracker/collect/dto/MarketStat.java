package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One daily entry in a {@code GET /markets/items/{id}} detail {@code Stats[]} array (Phase 17.4,
 * D-01 source ②): the traded {@code AvgPrice} for {@code Date}. Spike-measured: materials fill all
 * ~14 days (AvgPrice &gt; 0); engraving books return 0 (API limitation) so they are skipped by the
 * backfill runner. {@code Date} may carry a time suffix — callers parse only the leading yyyy-MM-dd.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketStat(
        @JsonProperty("Date") String date,
        @JsonProperty("AvgPrice") Double avgPrice,
        @JsonProperty("TradeCount") Long tradeCount) {
}
