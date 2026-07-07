package com.lostark.tracker.admin;

import com.lostark.tracker.domain.Coupon;
import com.lostark.tracker.repository.CouponRepository;
import com.lostark.tracker.support.AdminAuth;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.ApiErrorResponse;
import com.lostark.tracker.web.dto.CouponRequest;
import com.lostark.tracker.web.dto.CouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full coupon admin CRUD matrix on Testcontainers (Postgres + Redis), COUPON-01, mirroring
 * {@link AdminEventControllerIT}: create 201 with entity-stamped created_at/updated_at; GET list
 * expires_at asc (includes expired); PUT full-replace of code/reward/expiresAt with updated_at
 * advancing; DELETE 204; PUT/DELETE on a missing id 404; @Valid violations 400 — all 404/400 bodies
 * asserted against the {timestamp,status,error,message} contract. Adds the auth-gate case: a POST
 * WITHOUT X-Admin-Secret returns the 401 contract (SPEC: secret-less requests are 401). Every
 * authenticated call sends X-Admin-Secret.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminCouponControllerIT extends PostgresRedisContainers {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final LocalDate FEB = LocalDate.of(2026, 2, 1);
    private static final LocalDate MAR = LocalDate.of(2026, 3, 1);

    @Autowired
    TestRestTemplate rest;
    @Autowired
    CouponRepository couponRepository;

    @Value("${admin.api.secret:test-admin-secret}")
    String adminSecret;

    @BeforeEach
    void clean() {
        couponRepository.deleteAll();
    }

    @Test
    void createReturns201AndEntityStampsTimestamps() {
        CouponRequest request = new CouponRequest("SUMMER2026", "골드 10000", JAN);

        ResponseEntity<CouponResponse> created = rest.exchange(
                "/api/admin/coupons", HttpMethod.POST, AdminAuth.entity(request, adminSecret), CouponResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().id()).isNotNull();
        assertThat(created.getBody().code()).isEqualTo("SUMMER2026");
        assertThat(created.getBody().expiresAt()).isEqualTo(JAN);

        // The DB row carries entity-stamped created_at AND updated_at — never set by the controller.
        Coupon persisted = couponRepository.findById(created.getBody().id()).orElseThrow();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getReward()).isEqualTo("골드 10000");
    }

    @Test
    void listReturnsCouponsOrderedByExpiresAtAsc() {
        create(new CouponRequest("C-JAN", "보상1", JAN));
        create(new CouponRequest("C-MAR", "보상3", MAR));
        create(new CouponRequest("C-FEB", "보상2", FEB));

        ResponseEntity<CouponResponse[]> listed = rest.exchange(
                "/api/admin/coupons", HttpMethod.GET, AdminAuth.entity(adminSecret), CouponResponse[].class);

        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).isNotNull();
        // Soonest-expiry-first (expires_at asc); admin listing includes every coupon.
        assertThat(listed.getBody())
                .extracting(CouponResponse::expiresAt)
                .containsExactly(JAN, FEB, MAR);
    }

    @Test
    void putFullReplaceChangesAllFieldsAndAdvancesUpdatedAt() {
        Long id = create(new CouponRequest("OLD", "원래 보상", JAN)).id();
        OffsetDateTime createdAt = couponRepository.findById(id).orElseThrow().getCreatedAt();

        CouponRequest replacement = new CouponRequest("NEW", "정정 보상", MAR);
        ResponseEntity<CouponResponse> replaced = rest.exchange(
                "/api/admin/coupons/" + id, HttpMethod.PUT, AdminAuth.entity(replacement, adminSecret),
                CouponResponse.class);

        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replaced.getBody()).isNotNull();
        // Full replace: code, reward, and expiresAt all overwritten.
        assertThat(replaced.getBody().code()).isEqualTo("NEW");
        assertThat(replaced.getBody().reward()).isEqualTo("정정 보상");
        assertThat(replaced.getBody().expiresAt()).isEqualTo(MAR);

        Coupon persisted = couponRepository.findById(id).orElseThrow();
        assertThat(persisted.getExpiresAt()).isEqualTo(MAR);
        // updated_at advanced past created_at via @PreUpdate; created_at unchanged.
        assertThat(persisted.getCreatedAt().toInstant()).isEqualTo(createdAt.toInstant());
        assertThat(persisted.getUpdatedAt()).isAfter(persisted.getCreatedAt());
    }

    @Test
    void deleteReturns204AndRemovesFromList() {
        Long id = create(new CouponRequest("DEL", "삭제 대상", JAN)).id();

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/admin/coupons/" + id, HttpMethod.DELETE, AdminAuth.entity(adminSecret), Void.class);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleted.getBody()).isNull();
        assertThat(couponRepository.findById(id)).isEmpty();
    }

    @Test
    void putOnMissingIdReturns404Contract() {
        CouponRequest request = new CouponRequest("NOPE", "없음", JAN);

        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/coupons/999999", HttpMethod.PUT, AdminAuth.entity(request, adminSecret),
                ApiErrorResponse.class);

        assertContract(resp, HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteOnMissingIdReturns404Contract() {
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/coupons/999999", HttpMethod.DELETE, AdminAuth.entity(adminSecret), ApiErrorResponse.class);

        assertContract(resp, HttpStatus.NOT_FOUND);
    }

    @Test
    void postWithBlankCodeReturns400Contract() {
        assertContract(post(new CouponRequest("  ", "보상", JAN)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void postWithNullExpiresAtReturns400Contract() {
        assertContract(post(new CouponRequest("CODE", "보상", null)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void postWithoutSecretReturns401Contract() {
        // No X-Admin-Secret header at all — the /api/admin/** gate must reject with the 401 contract.
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/coupons", HttpMethod.POST,
                new HttpEntity<>(new CouponRequest("NOSECRET", "보상", JAN)),
                ApiErrorResponse.class);

        // Explicitly 401, never 403.
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(401);
        assertThat(resp.getBody().error()).isEqualTo("Unauthorized");
        assertThat(resp.getBody().timestamp()).isNotNull();
        assertThat(resp.getBody().message()).isNotBlank();
        // Nothing was persisted by the rejected request.
        assertThat(couponRepository.findAll()).isEmpty();
    }

    private CouponResponse create(CouponRequest request) {
        ResponseEntity<CouponResponse> resp = rest.exchange(
                "/api/admin/coupons", HttpMethod.POST, AdminAuth.entity(request, adminSecret), CouponResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private ResponseEntity<ApiErrorResponse> post(CouponRequest request) {
        return rest.exchange(
                "/api/admin/coupons", HttpMethod.POST, AdminAuth.entity(request, adminSecret), ApiErrorResponse.class);
    }

    private static void assertContract(ResponseEntity<ApiErrorResponse> resp, HttpStatus expected) {
        assertThat(resp.getStatusCode()).isEqualTo(expected);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(expected.value());
        assertThat(resp.getBody().error()).isEqualTo(expected.getReasonPhrase());
        assertThat(resp.getBody().timestamp()).isNotNull();
        assertThat(resp.getBody().message()).isNotBlank();
    }
}
