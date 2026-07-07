package com.lostark.tracker.web;

import com.lostark.tracker.domain.Coupon;
import com.lostark.tracker.repository.CouponRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.CouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Public coupon read filter on Testcontainers (Postgres + Redis), COUPON-02/03. Seeds two valid
 * coupons (expiring today and today+10) plus one expired coupon (yesterday) against the server's KST
 * date, then asserts {@code GET /api/coupons} returns only the two valid coupons soonest-expiry-first
 * and excludes the expired one. The expiry day itself stays valid — a coupon expiring today is still
 * served through end of that day (D-01). No secret required (public surface); no real API call.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CouponControllerIT extends PostgresRedisContainers {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    TestRestTemplate rest;
    @Autowired
    CouponRepository couponRepository;

    @BeforeEach
    void clean() {
        couponRepository.deleteAll();
    }

    @Test
    void publicListReturnsOnlyUnexpiredSoonestFirst() {
        LocalDate today = LocalDate.now(KST);
        // Expiry-day itself is still valid (D-01): today is included; yesterday is excluded.
        couponRepository.save(new Coupon("VALID-TODAY", "오늘 만료", today));
        couponRepository.save(new Coupon("VALID-LATER", "10일 후 만료", today.plusDays(10)));
        couponRepository.save(new Coupon("EXPIRED", "어제 만료", today.minusDays(1)));

        ResponseEntity<CouponResponse[]> resp =
                rest.getForEntity("/api/coupons", CouponResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // Only the two valid coupons, soonest-expiry-first ([today, today+10]).
        assertThat(resp.getBody())
                .extracting(CouponResponse::expiresAt)
                .containsExactly(today, today.plusDays(10));
        // The expired coupon must not appear.
        assertThat(resp.getBody())
                .extracting(CouponResponse::code)
                .doesNotContain("EXPIRED");
    }
}
