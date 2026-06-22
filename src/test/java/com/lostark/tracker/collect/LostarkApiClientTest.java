package com.lostark.tracker.collect;

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
