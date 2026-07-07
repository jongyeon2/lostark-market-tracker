package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One item in a {@code POST /markets/items} list response. Collection persists {@code Id} ->
 * external_item_id, {@code Name} -> display_name, {@code CurrentMinPrice} -> price_snapshot.min_price
 * (real-time min ASK price, D-06). {@code YDayAvgPrice} (previous-day traded AVG, present for ALL
 * items incl. engraving books — spike: 유물 원한 각인서 147007.4) is additionally mapped for the
 * Phase 17.4 backfill capture (item_daily_stats, source=YDAY_AVG, D-01) — it rides the already-fetched
 * list response, so reading it costs zero extra API calls. Other unknown fields (Grade, RecentPrice,
 * ...) stay ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketItem(
        @JsonProperty("Id") long id,
        @JsonProperty("Name") String name,
        @JsonProperty("CurrentMinPrice") Long currentMinPrice,
        @JsonProperty("YDayAvgPrice") Double yDayAvgPrice) {
}
