package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One daily entry in a {@code GET /markets/items/{id}} detail {@code Stats[]} array (Phase 17.4,
 * D-01 source ②): the traded {@code AvgPrice} and {@code TradeCount} for {@code Date}. Both materials
 * and engraving books fill all ~14 days on their real-trade array element — the all-zero entries seen
 * on an engraving's bound trade-once variant are why {@code getItemDetail} selects by TradeCount. The
 * backfill runner still skips any {@code AvgPrice <= 0} entry defensively. {@code Date} may carry a
 * time suffix — callers parse only the leading yyyy-MM-dd.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketStat(
        @JsonProperty("Date") String date,
        @JsonProperty("AvgPrice") Double avgPrice,
        @JsonProperty("TradeCount") Long tradeCount) {
}
