package com.lostark.tracker.gem;

import com.lostark.tracker.gem.GemDtos.GemPrice;
import com.lostark.tracker.gem.GemDtos.GemPriceStatus;
import com.lostark.tracker.gem.GemDtos.GemsResponse;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import com.lostark.tracker.support.PostgresRedisContainers;
import org.springframework.web.client.HttpClientErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the 보석 cache-aside + honesty contract on Testcontainers Redis (GEM-02). The external
 * {@link LostarkAuctionClient} is a {@link MockitoBean} so no live 경매장 call or API key is ever needed.
 *
 * <p>The load-bearing test here is {@link #cacheHitIssuesZeroAuctionCalls()}: 경매장 shares the 10분
 * 수집 rate-limit budget (Phase 24 §H2), so "the cache actually prevents calls" is a Core Value
 * guarantee, not a performance nicety. The rest pin the honesty rules — a missing price must never
 * render as a number, and one gem's failure must not take the other five down.
 */
@SpringBootTest
@ActiveProfiles("test")
class GemServiceIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkAuctionClient client;
    /**
     * The rate limiter is mocked so these tests drive the throttle deterministically. Default: every
     * token granted (the collector is idle) — the throttled paths override it per test.
     */
    @MockitoBean
    RedisTokenBucket rateLimiter;

    @Autowired
    GemService gemService;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    private static final String LV8_DEALER = "8레벨 겁화의 보석";
    private static final String LV10_SUPPORT = "10레벨 작열의 보석";

    @BeforeEach
    void flush() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
        when(rateLimiter.tryAcquire()).thenReturn(true);
    }

    /**
     * The Core Value guard, second half. 경매장 and 거래소 share ONE server-side per-key quota
     * (Phase 24 §H2), so gems must spend from the SAME token bucket as the collector — "one API key,
     * one bucket" (D-03). When the collector holds the budget, gems must yield WITHOUT issuing a call:
     * the first live run proved what bypassing this costs (겁화 3 OK, 작열 3 × HTTP 429).
     */
    @Test
    void throttledGemYieldsWithoutCallingAuctionApi() {
        when(rateLimiter.tryAcquire()).thenReturn(false);

        GemsResponse response = gemService.getAll();

        verify(client, never()).findLowestBuyPrice(anyString());
        assertThat(response.gems()).hasSize(6);
        assertThat(response.gems()).allSatisfy(g -> {
            assertThat(g.status()).isEqualTo(GemPriceStatus.RATE_LIMITED);
            assertThat(g.minBuyPrice()).isNull();
        });
    }

    /**
     * A throttled snapshot must NOT be cached: the tokens refill within seconds, and freezing the
     * degraded page for the full TTL would outlast the throttle. Retrying is free — a throttled row
     * never reaches the network.
     */
    @Test
    void throttledSnapshotIsNotCachedSoItRecoversAsSoonAsTokensReturn() {
        when(rateLimiter.tryAcquire()).thenReturn(false);
        gemService.getAll();

        // Collector finished; tokens are available again.
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(347_000L));
        GemsResponse recovered = gemService.getAll();

        assertThat(recovered.gems()).allSatisfy(g -> assertThat(g.status()).isEqualTo(GemPriceStatus.OK));
        verify(client, times(6)).findLowestBuyPrice(anyString());
    }

    @Test
    void cacheMissFetchesEveryGemAndServesSixRows() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(347_000L));

        GemsResponse response = gemService.getAll();

        assertThat(response.gems()).hasSize(6);
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.gems()).allSatisfy(g -> {
            assertThat(g.status()).isEqualTo(GemPriceStatus.OK);
            assertThat(g.minBuyPrice()).isEqualTo(347_000L);
        });
        // 6종 = 6콜, 1보석당 정확히 1콜 (경매장엔 배치 조회가 없다 — Phase 24)
        verify(client, times(6)).findLowestBuyPrice(anyString());
    }

    /**
     * The Core Value guard. A page view must not spend the collector's rate-limit budget: once warm,
     * the second read issues ZERO auction calls and returns the SAME snapshot instant.
     */
    @Test
    void cacheHitIssuesZeroAuctionCalls() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(1_000L));
        GemsResponse first = gemService.getAll();

        org.mockito.Mockito.clearInvocations(client);
        GemsResponse second = gemService.getAll();

        verify(client, never()).findLowestBuyPrice(anyString());
        assertThat(second.updatedAt()).isEqualTo(first.updatedAt());
        assertThat(second.gems()).hasSize(6);
    }

    /** 즉시구매를 건 매물이 하나도 없으면 값을 지어내지 않는다 — 0골드도, 마지막 값도 아니다. */
    @Test
    void gemWithNoBuyoutListingReportsNoBuyoutAndNullPrice() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(500L));
        when(client.findLowestBuyPrice(LV10_SUPPORT)).thenReturn(Optional.empty());

        GemsResponse response = gemService.getAll();

        GemPrice noBuyout = findByName(response, 10, GemCatalog.SERIES_SUPPORT);
        assertThat(noBuyout.status()).isEqualTo(GemPriceStatus.NO_BUYOUT);
        assertThat(noBuyout.minBuyPrice()).isNull();
        // 나머지 5행은 멀쩡하다
        assertThat(response.gems()).filteredOn(g -> g.status() == GemPriceStatus.OK).hasSize(5);
    }

    /**
     * A server-side 429 means the same thing as a local throttle — 곧 회복될 양보 — so it must NOT be
     * reported as 고장. Seen live on the first run: the shared 90-token bucket can outrun the real
     * 100/min window, so this path is reachable even with the limiter respected.
     */
    @Test
    void serverSideTooManyRequestsIsReportedAsRateLimitedNotFailure() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(500L));
        when(client.findLowestBuyPrice(LV8_DEALER))
                .thenThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests", org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0], null));

        GemsResponse response = gemService.getAll();

        assertThat(findByName(response, 8, GemCatalog.SERIES_DEALER).status())
                .isEqualTo(GemPriceStatus.RATE_LIMITED);
        assertThat(response.gems()).filteredOn(g -> g.status() == GemPriceStatus.OK).hasSize(5);
    }

    /** 한 보석의 실패가 나머지 다섯을 죽이지 않는다 (ItemCard per-card fan-out 정신). */
    @Test
    void oneGemFailureIsIsolatedToItsOwnRow() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(500L));
        when(client.findLowestBuyPrice(LV8_DEALER))
                .thenThrow(new org.springframework.web.client.RestClientException("boom"));

        GemsResponse response = gemService.getAll();

        assertThat(response.gems()).hasSize(6);
        GemPrice failed = findByName(response, 8, GemCatalog.SERIES_DEALER);
        assertThat(failed.status()).isEqualTo(GemPriceStatus.FETCH_FAILED);
        assertThat(failed.minBuyPrice()).isNull();
        assertThat(response.gems()).filteredOn(g -> g.status() == GemPriceStatus.OK).hasSize(5);
    }

    /**
     * A partial result is cached too. Caching only the successes would make every subsequent request
     * re-query the failing gem — burning the shared budget on a repeat failure. It retries when the
     * TTL lapses, not on every page view.
     */
    @Test
    void partialFailureIsStillCachedSoItDoesNotRefetchOnEveryRequest() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(500L));
        when(client.findLowestBuyPrice(LV8_DEALER))
                .thenThrow(new org.springframework.web.client.RestClientException("boom"));
        gemService.getAll();

        org.mockito.Mockito.clearInvocations(client);
        GemsResponse second = gemService.getAll();

        verify(client, never()).findLowestBuyPrice(anyString());
        assertThat(findByName(second, 8, GemCatalog.SERIES_DEALER).status())
                .isEqualTo(GemPriceStatus.FETCH_FAILED);
    }

    /** 카탈로그 순서·메타데이터가 findings 잠금과 일치한다(계열별·레벨 오름차순, 아이콘 distinct). */
    @Test
    void servesLockedCatalogOrderAndDistinctIcons() {
        when(client.findLowestBuyPrice(anyString())).thenReturn(Optional.of(1L));

        GemsResponse response = gemService.getAll();

        assertThat(response.gems()).extracting(GemPrice::series)
                .containsExactly("겁화", "겁화", "겁화", "작열", "작열", "작열");
        assertThat(response.gems()).extracting(GemPrice::level)
                .containsExactly(8, 9, 10, 8, 9, 10);
        assertThat(response.gems()).extracting(GemPrice::displayName)
                .containsExactly("8레벨", "9레벨", "10레벨", "8레벨", "9레벨", "10레벨");
        assertThat(response.gems()).extracting(GemPrice::iconUrl).doesNotHaveDuplicates();
    }

    private static GemPrice findByName(GemsResponse response, int level, String series) {
        return response.gems().stream()
                .filter(g -> g.level() == level && g.series().equals(series))
                .findFirst()
                .orElseThrow();
    }
}
