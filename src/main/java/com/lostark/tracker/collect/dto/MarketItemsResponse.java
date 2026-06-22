package com.lostark.tracker.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The {@code POST /markets/items} list envelope (Task 0): paged {@code Items} plus paging
 * metadata. The collector resolves a watchlisted item by matching {@code Items[].Id} against the
 * stored external_item_id (D-05) — {@code ItemName} is only a partial-match filter.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketItemsResponse(
        @JsonProperty("PageNo") int pageNo,
        @JsonProperty("PageSize") int pageSize,
        @JsonProperty("TotalCount") int totalCount,
        @JsonProperty("Items") List<MarketItem> items) {

    public MarketItemsResponse {
        items = items == null ? List.of() : items;
    }
}
