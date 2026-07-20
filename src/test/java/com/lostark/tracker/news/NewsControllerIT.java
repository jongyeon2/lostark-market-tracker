package com.lostark.tracker.news;

import com.lostark.tracker.news.NewsDtos.NewsEvent;
import com.lostark.tracker.news.NewsDtos.NewsNotice;
import com.lostark.tracker.news.NewsDtos.NewsResponse;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Proves the {@code GET /api/news} public read contract end-to-end (D-02/D-07). The external
 * {@link LostarkNewsClient} is mocked (no live call/key); the cache is seeded via
 * {@link NewsService#refresh()} and the endpoint asserted over HTTP:
 * <ul>
 *   <li>200 + camelCase {events, notices, updatedAt} JSON (the frontend contract, 17.2-03);</li>
 *   <li>public access with NO admin secret (only /api/admin/** is gated);</li>
 *   <li>cold cache -> 200 with empty arrays + updatedAt=null (honest empty state, D-07).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NewsControllerIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkNewsClient client;

    @Autowired
    NewsService newsService;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    /*
      진행 기간은 실행 시각 기준으로 만든다 — 하드코딩하면 안 된다.

      원래 "2026-07-01 ~ 2026-07-20"으로 박혀 있었고, 2026-07-20 06:00 KST를 지나는 순간 무관한 커밋의
      빌드가 빨갛게 됐다. NewsService는 서빙할 때마다 isOngoingAt(nowKst)로 종료된 이벤트를 걸러내므로
      (그게 이 기능의 사양이다) 고정 날짜는 반드시 만료된다. 날짜를 뒤로 미루는 건 폭탄의 타이머를
      다시 감는 것일 뿐이라 상대 시각으로 바꾼다.

      zone은 프로덕션 필터와 같은 KST여야 한다 — endDate가 KST 벽시계 문자열이기 때문(LostarkNewsClient).
    */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final NewsEvent EVENT =
            new NewsEvent("이벤트A", "https://lostark.game.onstove.com/e",
                    LocalDateTime.now(KST).minusDays(1).toString(),
                    LocalDateTime.now(KST).plusDays(1).toString(),
                    "https://cdn-lostark.game.onstove.com/t.jpg");
    private static final NewsNotice NOTICE =
            new NewsNotice("공지A", "https://lostark.game.onstove.com/n", "2026-07-01T15:10:18.527", "공지");

    @BeforeEach
    void flush() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushDb();
        }
    }

    @Test
    void getNewsReturnsCachedSnapshotAsCamelCaseJson() {
        when(client.fetchEvents()).thenReturn(List.of(EVENT));
        when(client.fetchNotices()).thenReturn(List.of(NOTICE));
        newsService.refresh();

        ResponseEntity<String> resp = rest.getForEntity("/api/news", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"events\"", "\"notices\"", "\"updatedAt\"");
        // camelCase field contract consumed by the frontend NewsPanel (17.2-03).
        assertThat(resp.getBody()).contains("\"title\":", "\"link\":", "\"startDate\":", "\"endDate\":");
        assertThat(resp.getBody()).contains("\"date\":", "\"type\":");
    }

    @Test
    void getNewsIsPublicWithoutAdminSecret() {
        // No X-Admin-Secret header — the public read surface must still answer 200 (not 401).
        ResponseEntity<NewsResponse> resp = rest.getForEntity("/api/news", NewsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getNewsOnColdCacheReturnsEmptyArrays() {
        ResponseEntity<NewsResponse> resp = rest.getForEntity("/api/news", NewsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().events()).isEmpty();
        assertThat(resp.getBody().notices()).isEmpty();
        assertThat(resp.getBody().updatedAt()).isNull();
    }
}