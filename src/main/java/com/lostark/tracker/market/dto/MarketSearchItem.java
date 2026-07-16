package com.lostark.tracker.market.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One row of a {@code POST /markets/items} response as the market-search feature needs it (아바타·모험의 서
 * 실시간 조회). Distinct from {@link com.lostark.tracker.collect.dto.MarketItem}, which the collector uses
 * and which deliberately ignores Grade/RecentPrice/Icon — this feature SHOWS those, so it maps them.
 *
 * <p>This DTO is used BOTH ways: deserialized from the PascalCase upstream response AND serialized to the
 * frontend. So the field names are camelCase (the frontend zod contract) and {@link JsonAlias} accepts the
 * upstream PascalCase on the way IN — NOT {@code @JsonProperty}, which would rename the OUTPUT too and emit
 * PascalCase to the frontend (a real bug caught in live verification: zod .parse rejected {@code PageNo}).
 *
 * <p>{@code currentMinPrice} is nullable and stays null when 즉시구매 매물이 없을 때(입찰 전용): we never
 * invent a price (the {@code GemPrice} NO_BUYOUT precedent). The frontend renders "즉시구매 매물 없음".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketSearchItem(
        @JsonAlias("Id") long id,
        @JsonAlias("Name") String name,
        @JsonAlias("Grade") String grade,
        @JsonAlias("Icon") String iconUrl,
        @JsonAlias("CurrentMinPrice") Long currentMinPrice,
        @JsonAlias("RecentPrice") Long recentPrice,
        @JsonAlias("YDayAvgPrice") Double yDayAvgPrice) {
}
