package com.lostark.tracker.market.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of a {@code POST /markets/items} response as the market-search feature needs it (아바타·모험의 서
 * 실시간 조회). Distinct from {@link com.lostark.tracker.collect.dto.MarketItem}, which the collector uses
 * and which deliberately ignores Grade/RecentPrice/Icon — this feature SHOWS those, so it maps them.
 *
 * <p>{@code currentMinPrice} is nullable and stays null when 즉시구매 매물이 없을 때(입찰 전용): we never
 * invent a price (the {@code GemPrice} NO_BUYOUT precedent). The frontend renders "즉시구매 매물 없음".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketSearchItem(
        @JsonProperty("Id") long id,
        @JsonProperty("Name") String name,
        @JsonProperty("Grade") String grade,
        @JsonProperty("Icon") String iconUrl,
        @JsonProperty("CurrentMinPrice") Long currentMinPrice,
        @JsonProperty("RecentPrice") Long recentPrice,
        @JsonProperty("YDayAvgPrice") Double yDayAvgPrice) {
}
