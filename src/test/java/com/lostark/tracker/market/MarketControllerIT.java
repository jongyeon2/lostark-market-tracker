package com.lostark.tracker.market;

import com.lostark.tracker.collect.LostarkApiClient;
import com.lostark.tracker.market.dto.MarketSearchItem;
import com.lostark.tracker.market.dto.MarketSearchResponse;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Proves the {@code GET /api/market/**} read contract over HTTP. {@link LostarkApiClient} is mocked (no live
 * call/key). Pins the server-side guards a client cannot bypass: avatar {@code class} is required and only
 * whitelisted parts are accepted, and both are 400 with the shared error contract — never a 500 or a
 * silently-wrong category.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MarketControllerIT extends PostgresRedisContainers {

    @MockitoBean
    LostarkApiClient client;
    @MockitoBean
    RedisTokenBucket rateLimiter;

    @Autowired
    TestRestTemplate rest;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void reset() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(client.searchMarket(anyString(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(new MarketSearchResponse(1, 10, 1, List.of(
                        new MarketSearchItem(1L, "x", "영웅", "https://cdn/x.png", 100L, 101L, 1.0))));
    }

    @Test
    void adventureSearchReturns200WithItems() {
        ResponseEntity<MarketSearchResponse> res = rest.getForEntity(
                "/api/market/adventure?q=숨결&sort=min_price&dir=desc", MarketSearchResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().items()).hasSize(1);
    }

    @Test
    void avatarWithoutClassIs400() {
        ResponseEntity<String> res = rest.getForEntity("/api/market/avatar?q=하프", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void avatarWithBlankClassIs400() {
        ResponseEntity<String> res = rest.getForEntity("/api/market/avatar?class=", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void avatarWithClassReturns200() {
        ResponseEntity<MarketSearchResponse> res = rest.getForEntity(
                "/api/market/avatar?class=바드", MarketSearchResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    void avatarWithUnknownPartIs400() {
        ResponseEntity<String> res = rest.getForEntity(
                "/api/market/avatar?class=바드&part=99999", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void avatarWithWhitelistedPartReturns200() {
        ResponseEntity<MarketSearchResponse> res = rest.getForEntity(
                "/api/market/avatar?class=바드&part=20005", MarketSearchResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void badSortIs400() {
        ResponseEntity<String> res = rest.getForEntity(
                "/api/market/adventure?sort=GRADE", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * The frontend zod contract is camelCase. The DTOs deserialize the PascalCase upstream via
     * {@code @JsonAlias} but MUST serialize camelCase to the client — asserting the raw JSON keys here
     * pins that, since a round-trip through the same DTO (as the typed tests above do) would hide a
     * PascalCase leak. This is the bug live verification caught (zod rejected {@code PageNo}).
     */
    @Test
    void responseJsonUsesCamelCaseKeysNotUpstreamPascalCase() {
        String json = rest.getForObject("/api/market/adventure", String.class);

        assertThat(json).contains("\"pageNo\"", "\"totalCount\"", "\"items\"", "\"currentMinPrice\"");
        assertThat(json).doesNotContain("\"PageNo\"", "\"TotalCount\"", "\"CurrentMinPrice\"", "\"Icon\"");
    }

    @Test
    void classesReturns200() {
        when(client.getMarketOptions()).thenReturn(
                new com.lostark.tracker.market.dto.MarketOptionsResponse(List.of("바드", "버서커")));

        ResponseEntity<String[]> res = rest.getForEntity("/api/market/classes", String[].class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsExactly("바드", "버서커");
    }
}
