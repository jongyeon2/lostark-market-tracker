package com.lostark.tracker.collect;

import com.lostark.tracker.collect.dto.ItemDetailResponse;
import com.lostark.tracker.collect.dto.MarketItemsResponse;
import com.lostark.tracker.collect.error.AuthApiException;
import com.lostark.tracker.collect.error.NonRetryableApiException;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.collect.error.TransientApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Proves the productized client classifies every HTTP outcome into a distinct typed signal
 * (COLL-04 classification half — the retry orchestration is plan 02-03), and that the configured
 * key is normalized (bearer prefix + ALL whitespace stripped — the Task-0 401 gotcha) and sent as
 * {@code Authorization: bearer <normalized>}. No Docker — pure MockRestServiceServer.
 */
class LostarkApiClientTest {

    private static final String BASE = "https://lostark.test";
    // Raw key with a leading "bearer " and mid-token spaces — must normalize to "abcd".
    private static final String RAW_KEY = "  bearer  ab cd  ";

    private record Fixture(LostarkApiClient client, MockRestServiceServer server) {
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LostarkApiClient client = new LostarkApiClient(builder, BASE, RAW_KEY);
        return new Fixture(client, server);
    }

    @Test
    void parsesSuccessfulListResponse() {
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("authorization", "bearer abcd")) // normalized key
                .andRespond(withSuccess("""
                        {"PageNo":1,"PageSize":10,"TotalCount":1,
                         "Items":[{"Id":66102101,"Name":"수호석 조각","Grade":"일반",
                                   "CurrentMinPrice":1,"YDayAvgPrice":0.0,"RecentPrice":1}]}
                        """, MediaType.APPLICATION_JSON));

        MarketItemsResponse res = f.client().searchMarketItems("50010", "수호석 조각");

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).id()).isEqualTo(66102101L);
        assertThat(res.items().get(0).name()).isEqualTo("수호석 조각");
        assertThat(res.items().get(0).currentMinPrice()).isEqualTo(1L);
        f.server().verify();
    }

    @Test
    void parsesItemDetailTopLevelArray() {
        // The real GET /markets/items/{id} returns a TOP-LEVEL ARRAY of item-detail objects
        // (each with Stats[]), NOT a bare {Stats:[...]} object — verified against the live API.
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items/66102007"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("authorization", "bearer abcd"))
                .andRespond(withSuccess("""
                        [{"Name":"운명의 파괴석 결정","TradeRemainCount":null,"BundleCount":100,
                          "Stats":[{"Date":"2026-07-07","AvgPrice":1711.4,"TradeCount":75251},
                                   {"Date":"2026-07-06","AvgPrice":1711.8,"TradeCount":167290}]}]
                        """, MediaType.APPLICATION_JSON));

        ItemDetailResponse res = f.client().getItemDetail(66102007L);

        assertThat(res.stats()).hasSize(2);
        assertThat(res.stats().get(0).date()).isEqualTo("2026-07-07");
        assertThat(res.stats().get(0).avgPrice()).isEqualTo(1711.4);
        assertThat(res.stats().get(0).tradeCount()).isEqualTo(75251L);
        f.server().verify();
    }

    @Test
    void getItemDetailPicksMostTradedArrayElement() {
        // Engraving detail returns TWO elements for one id: a bound "trade-once" variant (Stats all 0)
        // AND the freely traded market variant (real 14-day series) — and element [0] is the bound one.
        // getItemDetail must NOT take [0]; it keeps the element with real trade activity (verified live).
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items/65200505"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"Name":"유물 원한 각인서","TradeRemainCount":1,"BundleCount":1,
                          "Stats":[{"Date":"2026-07-07","AvgPrice":0.0,"TradeCount":0},
                                   {"Date":"2026-07-06","AvgPrice":0.0,"TradeCount":0}]},
                         {"Name":"유물 원한 각인서","TradeRemainCount":0,"BundleCount":1,
                          "Stats":[{"Date":"2026-07-07","AvgPrice":145743.1,"TradeCount":912},
                                   {"Date":"2026-07-06","AvgPrice":147007.4,"TradeCount":1267}]}]
                        """, MediaType.APPLICATION_JSON));

        ItemDetailResponse res = f.client().getItemDetail(65200505L);

        // The traded element ([1]) is selected, not the bound-variant [0].
        assertThat(res.stats()).hasSize(2);
        assertThat(res.stats().get(0).avgPrice()).isEqualTo(145743.1);
        assertThat(res.stats().get(0).tradeCount()).isEqualTo(912L);
        assertThat(res.stats().get(1).avgPrice()).isEqualTo(147007.4);
        f.server().verify();
    }

    @Test
    void itemDetailEmptyArrayYieldsEmptyStats() {
        // A no-result detail (empty top-level array) must yield empty Stats, not blow up.
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items/999"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(f.client().getItemDetail(999L).stats()).isEmpty();
        f.server().verify();
    }

    @Test
    void classifies401AsFatalAuth() {
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> f.client().searchMarketItems("50010", "x"))
                .isInstanceOf(AuthApiException.class);
    }

    @Test
    void classifies403AsFatalAuth() {
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> f.client().searchMarketItems("50010", "x"))
                .isInstanceOf(AuthApiException.class);
    }

    @Test
    void classifies429AsRateLimitedCarryingRetryAfter() {
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "7"));
        assertThatThrownBy(() -> f.client().searchMarketItems("50010", "x"))
                .isInstanceOfSatisfying(RateLimitedApiException.class,
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(7));
    }

    @Test
    void classifies503AsTransient() {
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> f.client().searchMarketItems("50010", "x"))
                .isInstanceOf(TransientApiException.class);
    }

    @Test
    void classifiesOther4xxAsNonRetryable() {
        Fixture f = fixture();
        f.server().expect(requestTo(BASE + "/markets/items"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> f.client().searchMarketItems("50010", "x"))
                .isInstanceOf(NonRetryableApiException.class);
    }
}
