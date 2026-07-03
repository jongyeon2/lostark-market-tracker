package com.lostark.tracker.admin;

import com.lostark.tracker.domain.TrackedItem;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full item admin matrix on Testcontainers (Postgres + Redis), ADMIN-02 / Phase Success Criterion 3:
 * create 201 + appears in GET /api/items; soft-delete 204 + disappears while the row survives
 * (active=false, history-preserving D-03); idempotent re-delete 204; reactivate-on-repost 200 + the
 * item reappears with no duplicate row (D-05); active-duplicate 409; missing id 404; @Valid 400 —
 * 409/404/400 bodies asserted on the {timestamp,status,error,message} contract. Every admin call
 * sends X-Admin-Secret (inert this wave, the gate credential once 04-02 lands).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminItemControllerIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;

    @Value("${admin.api.secret:test-admin-secret}")
    String adminSecret;

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
    }

    @Test
    void createReturns201AndItemAppearsInPublicList() {
        ResponseEntity<TrackedItemResponse> created = post(new TrackedItemRequest("66102101", "수호석 조각", "50010"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().id()).isNotNull();
        assertThat(created.getBody().active()).isTrue();
        assertThat(publicExternalIds()).contains("66102101");
    }

    @Test
    void postActiveDuplicateReturns409Contract() {
        post(new TrackedItemRequest("66102102", "파괴석 조각", "50010"));

        ResponseEntity<ApiErrorResponse> dup = rest.exchange(
                "/api/admin/items", HttpMethod.POST,
                AdminAuth.entity(new TrackedItemRequest("66102102", "파괴석 조각", "50010"), adminSecret),
                ApiErrorResponse.class);

        assertContract(dup, HttpStatus.CONFLICT);
        // No duplicate row was inserted.
        assertThat(trackedItemRepository.findByExternalItemId("66102102")).isPresent();
        assertThat(trackedItemRepository.findAll())
                .filteredOn(it -> it.getExternalItemId().equals("66102102"))
                .hasSize(1);
    }

    @Test
    void deleteIsSoftAndIdempotentAndPreservesRow() {
        Long id = post(new TrackedItemRequest("66102103", "정제된 파괴강석", "50010")).getBody().id();
        assertThat(publicExternalIds()).contains("66102103");

        assertThat(delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Disappears from the public read surface and the collector's findByActiveTrue...
        assertThat(publicExternalIds()).doesNotContain("66102103");
        // ...but the row survives with active=false (history-preserving soft delete, D-03).
        TrackedItem row = trackedItemRepository.findById(id).orElseThrow();
        assertThat(row.isActive()).isFalse();

        // Idempotent: deleting an already-inactive item is still a 204.
        assertThat(delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(trackedItemRepository.findById(id)).isPresent();
    }

    @Test
    void repostAfterSoftDeleteReactivatesSameRowWith200() {
        TrackedItemResponse first = post(new TrackedItemRequest("66102104", "정제된 수호강석", "50010")).getBody();
        delete(first.id());

        // Re-post the same external_item_id -> 200 reactivation (not 201), refreshing display fields (D-05).
        ResponseEntity<TrackedItemResponse> reactivated = post(
                new TrackedItemRequest("66102104", "정제된 수호강석 (갱신)", "50011"));

        assertThat(reactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reactivated.getBody()).isNotNull();
        assertThat(reactivated.getBody().id()).isEqualTo(first.id()); // same row, no duplicate
        assertThat(reactivated.getBody().active()).isTrue();
        assertThat(reactivated.getBody().displayName()).isEqualTo("정제된 수호강석 (갱신)");
        assertThat(reactivated.getBody().category()).isEqualTo("50011");
        assertThat(publicExternalIds()).contains("66102104");
        assertThat(trackedItemRepository.findAll())
                .filteredOn(it -> it.getExternalItemId().equals("66102104"))
                .hasSize(1);
    }

    @Test
    void deleteOnMissingIdReturns404Contract() {
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/items/999999", HttpMethod.DELETE, AdminAuth.entity(adminSecret), ApiErrorResponse.class);

        assertContract(resp, HttpStatus.NOT_FOUND);
    }

    @Test
    void postWithBlankExternalItemIdReturns400Contract() {
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/items", HttpMethod.POST,
                AdminAuth.entity(new TrackedItemRequest("  ", "이름", "50010"), adminSecret), ApiErrorResponse.class);

        assertContract(resp, HttpStatus.BAD_REQUEST);
    }

    @Test
    void getItemsWithSecretReturnsActiveAndInactive() {
        // Seed one active item...
        post(new TrackedItemRequest("66102201", "활성 품목", "50010"));
        // ...and one soft-deleted (inactive) item — the public GET /api/items would hide this one (D-12).
        Long inactiveId = post(new TrackedItemRequest("66102202", "비활성 품목", "50010")).getBody().id();
        delete(inactiveId);

        ResponseEntity<TrackedItemResponse[]> resp = rest.exchange(
                "/api/admin/items", HttpMethod.GET, AdminAuth.entity(adminSecret), TrackedItemResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // The admin list is the reactivation source of truth: it carries BOTH the active and the
        // soft-deleted row, each with its correct `active` flag (D-13, ADMINUI-04).
        java.util.Map<String, Boolean> activeByExternalId = java.util.Arrays.stream(resp.getBody())
                .collect(java.util.stream.Collectors.toMap(
                        TrackedItemResponse::externalItemId, TrackedItemResponse::active));
        assertThat(activeByExternalId).containsEntry("66102201", true);
        assertThat(activeByExternalId).containsEntry("66102202", false);
    }

    @Test
    void getItemsWithoutSecretReturns401Contract() {
        // No X-Admin-Secret header — the /api/admin/** gate 401s before any item leaks.
        ResponseEntity<ApiErrorResponse> resp =
                rest.getForEntity("/api/admin/items", ApiErrorResponse.class);

        assertContract(resp, HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<TrackedItemResponse> post(TrackedItemRequest request) {
        return rest.exchange(
                "/api/admin/items", HttpMethod.POST, AdminAuth.entity(request, adminSecret), TrackedItemResponse.class);
    }

    private ResponseEntity<Void> delete(long id) {
        return rest.exchange(
                "/api/admin/items/" + id, HttpMethod.DELETE, AdminAuth.entity(adminSecret), Void.class);
    }

    private java.util.List<String> publicExternalIds() {
        ResponseEntity<TrackedItemResponse[]> listed =
                rest.getForEntity("/api/items", TrackedItemResponse[].class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).isNotNull();
        return java.util.Arrays.stream(listed.getBody()).map(TrackedItemResponse::externalItemId).toList();
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