package com.lostark.tracker.security;

import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.AdminAuth;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.ApiErrorResponse;
import com.lostark.tracker.web.dto.TrackedItemRequest;
import com.lostark.tracker.web.dto.TrackedItemResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves Phase 4 Success Criterion 1 / ADMIN-03 end-to-end on Testcontainers: {@code /api/admin/**}
 * is 401 (explicitly NOT 403) on the {@code {timestamp,status,error,message}} contract without or with
 * a wrong secret, and 201 with the correct secret — while the public read surface (GET
 * {@code /api/items}, {@code /api/health/collection}) stays 200 with no secret (no regression from
 * adding Spring Security). Reuses {@link AdminAuth} from 04-01.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminAuthGateIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;

    @Value("${admin.api.secret}")
    String adminSecret;

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
    }

    @Test
    void adminPostWithoutSecretReturns401Contract() {
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/items", HttpMethod.POST,
                new HttpEntity<>(new TrackedItemRequest("auth-1", "Gate item", "50010")),
                ApiErrorResponse.class);

        assertUnauthorizedContract(resp);
        assertThat(trackedItemRepository.findByExternalItemId("auth-1")).isEmpty();
    }

    @Test
    void adminPostWithWrongSecretReturns401Contract() {
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/items", HttpMethod.POST,
                AdminAuth.entity(new TrackedItemRequest("auth-2", "Gate item", "50010"), "wrong-secret"),
                ApiErrorResponse.class);

        assertUnauthorizedContract(resp);
        assertThat(trackedItemRepository.findByExternalItemId("auth-2")).isEmpty();
    }

    @Test
    void adminPostWithCorrectSecretReturns201() {
        ResponseEntity<TrackedItemResponse> resp = rest.exchange(
                "/api/admin/items", HttpMethod.POST,
                AdminAuth.entity(new TrackedItemRequest("auth-3", "Gate item", "50010"), adminSecret),
                TrackedItemResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isNotNull();
        assertThat(resp.getBody().externalItemId()).isEqualTo("auth-3");
    }

    @Test
    void publicItemListStays200WithoutSecret() {
        ResponseEntity<TrackedItemResponse[]> resp =
                rest.getForEntity("/api/items", TrackedItemResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void publicCollectionHealthStays200WithoutSecret() {
        ResponseEntity<String> resp = rest.getForEntity("/api/health/collection", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static void assertUnauthorizedContract(ResponseEntity<ApiErrorResponse> resp) {
        // Explicitly 401, never 403 (D-02).
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(401);
        assertThat(resp.getBody().error()).isEqualTo("Unauthorized");
        assertThat(resp.getBody().timestamp()).isNotNull();
        assertThat(resp.getBody().message()).isNotBlank();
        // The secret value must never be echoed back in the error body.
        assertThat(resp.getBody().message().toLowerCase()).doesNotContain("secret-");
    }
}