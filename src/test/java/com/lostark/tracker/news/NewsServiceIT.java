package com.lostark.tracker.news;

import com.lostark.tracker.news.NewsDtos.NewsEvent;
import com.lostark.tracker.news.NewsDtos.NewsNotice;
import com.lostark.tracker.news.NewsDtos.NewsResponse;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Proves the news cache-aside + honesty contract on Testcontainers Redis (D-01/D-07). The external
 * {@link LostarkNewsClient} is a {@link MockitoBean} so no live {@code /news} call or API key is ever
 * needed — the test drives {@link NewsService#refresh()} with fixture events/notices and asserts:
 * <ul>
 *   <li>refresh() caches a snapshot and getLatest() serves it (events/notices/updatedAt);</li>
 *   <li>a failing poll KEEPS the last cache untouched (D-07 keep-on-failure);</li>
 *   <li>a cold cache returns empty lists + {@code updatedAt=null}.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class NewsServiceIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkNewsClient client;

    @Autowired
    NewsService newsService;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    private static final NewsEvent EVENT =
            new NewsEvent("이벤트A", "https://lostark.game.onstove.com/e", "2026-07-01T06:00:00",
                    "2026-07-20T06:00:00", "https://cdn-lostark.game.onstove.com/t.jpg");
    private static final NewsNotice NOTICE =
            new NewsNotice("공지A", "https://lostark.game.onstove.com/n", "2026-07-01T15:10:18.527", "공지");

    @BeforeEach
    void flush() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushDb(); // isolate news:latest between tests
        }
    }

    @Test
    void refreshCachesSnapshotAndGetLatestServesIt() {
        when(client.fetchEvents()).thenReturn(List.of(EVENT));
        when(client.fetchNotices()).thenReturn(List.of(NOTICE));

        newsService.refresh();
        NewsResponse latest = newsService.getLatest();

        assertThat(latest.events()).extracting(NewsEvent::title).containsExactly("이벤트A");
        assertThat(latest.events()).extracting(NewsEvent::endDate).containsExactly("2026-07-20T06:00:00");
        assertThat(latest.notices()).extracting(NewsNotice::type).containsExactly("공지");
        assertThat(latest.updatedAt()).isNotNull();
    }

    @Test
    void keepsLastCacheWhenPollFails() {
        // 1) seed a good snapshot
        when(client.fetchEvents()).thenReturn(List.of(EVENT));
        when(client.fetchNotices()).thenReturn(List.of(NOTICE));
        newsService.refresh();
        String firstUpdatedAt = newsService.getLatest().updatedAt();

        // 2) next poll fails — the cache must remain the previous good snapshot (D-07)
        when(client.fetchEvents()).thenThrow(new IllegalStateException("simulated /news outage"));
        newsService.refresh();

        NewsResponse afterFailure = newsService.getLatest();
        assertThat(afterFailure.events()).extracting(NewsEvent::title).containsExactly("이벤트A");
        assertThat(afterFailure.updatedAt()).isEqualTo(firstUpdatedAt); // not re-stamped
    }

    @Test
    void getLatestOnColdCacheReturnsEmpty() {
        NewsResponse latest = newsService.getLatest();

        assertThat(latest.events()).isEmpty();
        assertThat(latest.notices()).isEmpty();
        assertThat(latest.updatedAt()).isNull();
    }
}