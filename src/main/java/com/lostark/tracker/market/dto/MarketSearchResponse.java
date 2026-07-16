package com.lostark.tracker.market.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The {@code POST /markets/items} list envelope for the market-search feature: paged {@link MarketSearchItem}
 * rows plus paging metadata. PageSize is fixed at 10 by the upstream API; the frontend derives total pages
 * from {@code totalCount} for its 이전/다음 + 페이지 번호 control.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketSearchResponse(
        @JsonProperty("PageNo") int pageNo,
        @JsonProperty("PageSize") int pageSize,
        @JsonProperty("TotalCount") int totalCount,
        @JsonProperty("Items") List<MarketSearchItem> items) {

    public MarketSearchResponse {
        items = items == null ? List.of() : items;
    }
}
