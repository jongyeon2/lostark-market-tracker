package com.lostark.tracker.market;

import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.market.dto.MarketSearchItem;
import com.lostark.tracker.market.dto.MarketSearchResponse;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.error.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the market-search serving contract on Testcontainers Redis. {@link LostarkApiClient} is a
 * {@link MockitoBean} so no live 거래소 call or API key is needed.
 *
 * <p>The load-bearing tests: (1) the sort/direction WHITELIST — the upstream API 200-ignores an unknown
 * Sort, so validation here is what makes "정렬했는데 안 바뀐다" impossible; (2) the cache actually prevents
 * calls, a Core Value guarantee since a search spends from the collector's shared bucket; (3) a throttle
 * yields WITHOUT calling the API.
 */
@SpringBootTest
@ActiveProfiles("test")
class MarketSearchServiceIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkApiClient client;
    @MockitoBean
    RedisTokenBucket rateLimiter;

    @Autowired
    MarketSearchService service;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    private static MarketSearchResponse oneItem(long price) {
        return new MarketSearchResponse(1, 10, 1, List.of(
                new MarketSearchItem(1L, "테스트 아바타", "영웅", "https://cdn/x.png", price, price + 1, 1.0)));
    }

    @BeforeEach
    void reset() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
        when(rateLimiter.tryAcquire()).thenReturn(true);
    }

    @Test
    void mapsFrontendVocabularyToUpstreamSortAndCondition() {
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(oneItem(1000L));

        service.search("100000", null, "숨결", 1, "min_price", "asc");

        ArgumentCaptor<String> sort = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dir = ArgumentCaptor.forClass(String.class);
        verify(client).searchMarket(eq("100000"), any(), eq("숨결"), eq(1), sort.capture(), dir.capture());
        assertThat(sort.getValue()).isEqualTo("CURRENT_MIN_PRICE");
        assertThat(dir.getValue()).isEqualTo("ASC");
    }

    @Test
    void recentPriceDescMapsToUpstreamRecentPriceDesc() {
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(oneItem(1000L));

        service.search("20000", "바드", null, 2, "recent_price", "desc");

        verify(client).searchMarket(eq("20000"), eq("바드"), any(), eq(2), eq("RECENT_PRICE"), eq("DESC"));
    }

    /** The upstream 200-ignores an unknown Sort — so an unvalidated value must be a 400 here, not a no-op. */
    @Test
    void unknownSortIsRejectedAndNeverReachesTheApi() {
        assertThatThrownBy(() -> service.search("100000", null, null, 1, "GRADE", "asc"))
                .isInstanceOf(InvalidRequestException.class);
        verify(client, never()).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    @Test
    void unknownDirectionIsRejected() {
        assertThatThrownBy(() -> service.search("100000", null, null, 1, "min_price", "sideways"))
                .isInstanceOf(InvalidRequestException.class);
        verify(client, never()).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    @Test
    void pageBelowOneIsRejected() {
        assertThatThrownBy(() -> service.search("100000", null, null, 0, "min_price", "asc"))
                .isInstanceOf(InvalidRequestException.class);
    }

    /** Core Value guard: the second identical search hits the cache and issues ZERO API calls. */
    @Test
    void cacheHitIssuesZeroApiCalls() {
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(oneItem(500L));

        service.search("100000", null, "숨결", 1, "min_price", "asc");
        service.search("100000", null, "숨결", 1, "min_price", "asc");

        verify(client, times(1)).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    /** A different page is a different cache key — it must reach the API, not serve page 1's rows. */
    @Test
    void differentPageIsADifferentCacheKey() {
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(oneItem(500L));

        service.search("100000", null, "숨결", 1, "min_price", "asc");
        service.search("100000", null, "숨결", 2, "min_price", "asc");

        verify(client, times(2)).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    /** Shared-bucket throttle: yield WITHOUT calling the API (never race the collector into a real 429). */
    @Test
    void throttledSearchDoesNotCallTheApi() {
        when(rateLimiter.tryAcquire()).thenReturn(false);

        assertThatThrownBy(() -> service.search("100000", null, null, 1, "min_price", "asc"))
                .isInstanceOf(RateLimitedApiException.class);
        verify(client, never()).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    // --- 모험의 서 전량 (getAdventureAll) ---------------------------------------------------------
    // The 대륙별 view needs a 대륙's items, which are scattered across every page, so this walks the
    // whole category and caches it. That makes the paging loop's termination load-bearing: it spends
    // one shared-bucket token PER PAGE, so a loop that fails to stop would drain the collector.

    /** A page of {@code size} rows starting at {@code from}, in a category of {@code totalCount}. */
    private static MarketSearchResponse page(int from, int size, int totalCount) {
        List<MarketSearchItem> rows = java.util.stream.IntStream.range(from, from + size)
                .mapToObj(i -> new MarketSearchItem((long) i, "수집품" + i, "일반", "https://cdn/x.png",
                        100L + i, 101L + i, 1.0))
                .toList();
        return new MarketSearchResponse(1, 10, totalCount, rows);
    }

    /** Walks every page and concatenates — the whole category comes back, not just page 1. */
    @Test
    void adventureAllWalksEveryPageUntilTotalCountIsReached() {
        when(client.searchMarket(anyString(), any(), any(), eq(1), anyString(), anyString()))
                .thenReturn(page(0, 10, 25));
        when(client.searchMarket(anyString(), any(), any(), eq(2), anyString(), anyString()))
                .thenReturn(page(10, 10, 25));
        when(client.searchMarket(anyString(), any(), any(), eq(3), anyString(), anyString()))
                .thenReturn(page(20, 5, 25));

        MarketSearchResponse all = service.getAdventureAll();

        assertThat(all.items()).hasSize(25);
        assertThat(all.totalCount()).isEqualTo(25);
        verify(client, times(3)).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    /**
     * Core Value guard, and the whole point of the 10분 TTL: a miss costs ~14 calls, so the second
     * request must cost ZERO. Browsing 대륙 must not scale API spend.
     */
    @Test
    void adventureAllSecondCallIsFullyCached() {
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(page(0, 3, 3));

        service.getAdventureAll();
        service.getAdventureAll();

        verify(client, times(1)).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    /**
     * Termination guard. If the upstream reports a totalCount we can never reach (here: always 10 rows
     * but claims 9999), the loop must stop at ADVENTURE_MAX_PAGES instead of spinning forever and
     * draining the collector's token budget one call at a time.
     */
    @Test
    void adventureAllStopsAtMaxPagesWhenTotalCountIsUnreachable() {
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(page(0, 10, 9999));

        MarketSearchResponse all = service.getAdventureAll();

        assertThat(all.items()).hasSize(10 * MarketSearchService.ADVENTURE_MAX_PAGES);
        verify(client, times(MarketSearchService.ADVENTURE_MAX_PAGES))
                .searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    /** An empty page ends the walk too — otherwise a short category would burn all 50 page slots. */
    @Test
    void adventureAllStopsOnAnEmptyPage() {
        when(client.searchMarket(anyString(), any(), any(), eq(1), anyString(), anyString()))
                .thenReturn(page(0, 10, 9999));
        when(client.searchMarket(anyString(), any(), any(), eq(2), anyString(), anyString()))
                .thenReturn(new MarketSearchResponse(2, 10, 9999, List.of()));

        MarketSearchResponse all = service.getAdventureAll();

        assertThat(all.items()).hasSize(10);
        verify(client, times(2)).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }

    /**
     * The bucket can run dry MID-walk (14 tokens is a lot to ask at once). We fail the request rather
     * than keep spending — Core Value outranks an on-demand search — and nothing partial is cached.
     */
    @Test
    void adventureAllYieldsWhenTheBucketRunsDryMidWalk() {
        when(rateLimiter.tryAcquire()).thenReturn(true, true, false);
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(page(0, 10, 9999));

        assertThatThrownBy(() -> service.getAdventureAll())
                .isInstanceOf(RateLimitedApiException.class);
        verify(client, times(2)).searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString());
    }
}
