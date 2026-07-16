package com.lostark.tracker.market.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The {@code GET /markets/options} envelope. We only need {@code Classes} — the 30 playable classes the
 * avatar page's 직업 드롭다운 offers (버서커…가디언나이트). Categories/ItemGrades/ItemTiers are ignored:
 * category codes are pinned constants in this feature and tier/grade filters are out of scope
 * (사용자 결정: 직업 드롭다운만).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketOptionsResponse(
        @JsonProperty("Classes") List<String> classes) {

    public MarketOptionsResponse {
        classes = classes == null ? List.of() : classes;
    }
}
