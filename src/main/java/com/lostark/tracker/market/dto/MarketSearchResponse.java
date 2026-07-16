package com.lostark.tracker.market.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The {@code POST /markets/items} list envelope for the market-search feature: paged {@link MarketSearchItem}
 * rows plus paging metadata. PageSize is fixed at 10 by the upstream API; the frontend derives total pages
 * from {@code totalCount} for its 이전/다음 + 페이지 번호 control.
 *
 * <p>Camelcase fields + {@link JsonAlias} for the same input/output reason as {@link MarketSearchItem}:
 * this DTO is serialized to the frontend (which expects camelCase), so {@code @JsonProperty} would wrongly
 * emit PascalCase.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketSearchResponse(
        @JsonAlias("PageNo") int pageNo,
        @JsonAlias("PageSize") int pageSize,
        @JsonAlias("TotalCount") int totalCount,
        @JsonAlias("Items") List<MarketSearchItem> items) {

    public MarketSearchResponse {
        items = items == null ? List.of() : items;
    }
}
