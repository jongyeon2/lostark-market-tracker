package com.lostark.tracker.news;

import com.lostark.tracker.news.NewsDtos.NewsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read surface for the dashboard news panel (D-02): {@code GET /api/news} returns the last
 * cached {@link NewsResponse}{events, notices, updatedAt}. No admin gate — SecurityConfig's
 * {@code anyRequest().permitAll()} already exposes it publicly (only {@code /api/admin/**} is
 * authenticated); the response carries public news metadata only, never a key or secret (D-06).
 * Serving is cache-only: this never calls the external API itself (the poller owns refresh, D-01).
 */
@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/api/news")
    public NewsResponse news() {
        return newsService.getLatest();
    }
}