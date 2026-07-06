package com.lostark.tracker.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Low-frequency independent news poller (D-01/D-06). Fires a boot-time refresh then every
 * {@code news.poll-interval-ms} (default 6h, fixedDelay so ticks never overlap) — completely
 * separate from the market collection schedule ({@code PriceCollector}): its own bean, its own
 * Redis key ({@code news:latest}), ~2 requests per 6h, so it never competes for the 100/min market
 * rate-limit budget. Scheduling is enabled app-wide by {@code CollectionConfig @EnableScheduling}.
 *
 * <p>Like the collector, integration tests push {@code news.initial-delay-ms} far out
 * (application-test.yml) so this never auto-fires during a test boot; ITs drive
 * {@link NewsService#refresh()} directly with a mocked client instead.
 */
@Component
public class NewsPoller {

    private static final Logger log = LoggerFactory.getLogger(NewsPoller.class);

    private final NewsService newsService;

    public NewsPoller(NewsService newsService) {
        this.newsService = newsService;
    }

    @Scheduled(fixedDelayString = "${news.poll-interval-ms:21600000}",
            initialDelayString = "${news.initial-delay-ms:0}")
    public void poll() {
        log.debug("news poll tick");
        newsService.refresh();
    }
}