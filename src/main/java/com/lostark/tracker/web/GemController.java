package com.lostark.tracker.web;

import com.lostark.tracker.gem.GemDtos.GemsResponse;
import com.lostark.tracker.gem.GemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read endpoint for 보석 현재가 (GEM-02). {@code GET /api/gems} returns the 6 curated 티어4 보석
 * (겁화·작열 × 8/9/10) with their 최저 즉시구매가, served from {@link GemService}'s Redis snapshot.
 *
 * <p>The frontend consumes ONLY this endpoint and never calls 경매장 directly (D-06) — the API key
 * lives in server env alone. permitAll via SecurityConfig {@code anyRequest().permitAll()};
 * SecurityConfig is unchanged. Read-only: no write surface, no admin gate.
 *
 * <p>Rows carry a status ({@code OK}/{@code NO_BUYOUT}/{@code FETCH_FAILED}) with a nullable price
 * rather than a placeholder number — the page says 즉시구매 매물 없음 instead of showing 0골드.
 */
@RestController
@RequestMapping("/api/gems")
public class GemController {

    private final GemService gemService;

    public GemController(GemService gemService) {
        this.gemService = gemService;
    }

    @GetMapping
    public GemsResponse list() {
        return gemService.getAll();
    }
}
