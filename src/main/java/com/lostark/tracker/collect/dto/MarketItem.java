package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One item in a {@code POST /markets/items} list response. Only the fields collection persists
 * are mapped (Task 0): {@code Id} -> external_item_id, {@code Name} -> display_name,
 * {@code CurrentMinPrice} -> price_snapshot.min_price. avg_price/trade_count are NOT collected
 * per tick (D-06). Unknown fields (Grade, YDayAvgPrice, RecentPrice, ...) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketItem(
        @JsonProperty("Id") long id,
        @JsonProperty("Name") String name,
        @JsonProperty("CurrentMinPrice") Long currentMinPrice) {
}
