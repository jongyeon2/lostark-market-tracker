package com.lostark.tracker.admin;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.AdminAuth;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.ApiErrorResponse;
import com.lostark.tracker.web.dto.GameEventRequest;
import com.lostark.tracker.web.dto.GameEventResponse;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full event admin CRUD matrix on Testcontainers (Postgres + Redis), ADMIN-01 / Phase Success
 * Criterion 2: create 201 with entity-stamped created_at/updated_at (D-07); GET list occurred_at
 * desc; PUT full-replace incl. an occurred_at change with updated_at advancing (D-06); DELETE 204;
 * PUT/DELETE on a missing id 404; @Valid violations 400 — all 404/400 bodies asserted against the
 * {timestamp,status,error,message} contract (D-09, D-10). Every admin call sends X-Admin-Secret so
 * the suite is unchanged once 04-02 adds the gate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminEventControllerIT extends PostgresRedisContainers {

    // A KST-midnight boundary (2026-01-01T15:00Z == 2026-01-02 00:00 KST) guards an off-by-9h bug.
    private static final OffsetDateTime JAN = OffsetDateTime.of(2026, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime FEB = OffsetDateTime.of(2026, 2, 1, 15, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime MAR = OffsetDateTime.of(2026, 3, 1, 15, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    TestRestTemplate rest;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;

    @Value("${admin.api.secret:test-admin-secret}")
    String adminSecret;

    @BeforeEach
    void clean() {
        // Child first (FK), then parents. Not @Transactional (RANDOM_PORT server thread).
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        gameEventRepository.deleteAll();
    }

    @Test
    void createReturns201AndEntityStampsTimestamps() {
        GameEventRequest request = new GameEventRequest(EventType.MAJOR_UPDATE, "여름 업데이트", JAN, "시즌 패치");

        ResponseEntity<GameEventResponse> created = rest.exchange(
                "/api/admin/events", HttpMethod.POST, AdminAuth.entity(request, adminSecret), GameEventResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().id()).isNotNull();
        assertThat(created.getBody().occurredAt().toInstant()).isEqualTo(JAN.toInstant());

        // The DB row carries entity-stamped created_at AND updated_at (D-07) — never set by the controller.
        GameEvent persisted = gameEventRepository.findById(created.getBody().id()).orElseThrow();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getEventType()).isEqualTo(EventType.MAJOR_UPDATE);
    }

    @Test
    void createNewClassEventPersistsAdditiveEnumValue() {
        // v1.5(EVT-01): additive event types(NEW_CLASS/NEW_RAID/GENERAL_PATCH)가 바인딩·영속을 왕복한다.
        // 차원술사 출시발 각인서 시세 변동을 NEW_CLASS로 상관 기록하는 시나리오.
        GameEventRequest request = new GameEventRequest(EventType.NEW_CLASS, "차원술사 출시", JAN, "신규 직업");

        ResponseEntity<GameEventResponse> created = rest.exchange(
                "/api/admin/events", HttpMethod.POST, AdminAuth.entity(request, adminSecret), GameEventResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().eventType()).isEqualTo(EventType.NEW_CLASS);

        GameEvent persisted = gameEventRepository.findById(created.getBody().id()).orElseThrow();
        assertThat(persisted.getEventType()).isEqualTo(EventType.NEW_CLASS);
    }

    @Test
    void listReturnsEventsOrderedByOccurredAtDesc() {
        create(new GameEventRequest(EventType.LOA_ON, "1월", JAN, null));
        create(new GameEventRequest(EventType.SEASON_END, "3월", MAR, null));
        create(new GameEventRequest(EventType.BALANCE_PATCH, "2월", FEB, null));

        ResponseEntity<GameEventResponse[]> listed = rest.exchange(
                "/api/admin/events", HttpMethod.GET, AdminAuth.entity(adminSecret), GameEventResponse[].class);

        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).isNotNull();
        assertThat(listed.getBody())
                .extracting(GameEventResponse::occurredAt)
                .containsExactly(MAR, FEB, JAN);
    }

    @Test
    void putFullReplaceChangesOccurredAtAndAdvancesUpdatedAt() {
        Long id = create(new GameEventRequest(EventType.LOA_ON, "원래 제목", JAN, "원래")).id();
        OffsetDateTime createdAt = gameEventRepository.findById(id).orElseThrow().getCreatedAt();

        GameEventRequest replacement = new GameEventRequest(EventType.SEASON_END, "정정된 제목", MAR, "정정");
        ResponseEntity<GameEventResponse> replaced = rest.exchange(
                "/api/admin/events/" + id, HttpMethod.PUT, AdminAuth.entity(replacement, adminSecret),
                GameEventResponse.class);

        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replaced.getBody()).isNotNull();
        // Full replace: every field overwritten, occurred_at INCLUDED (D-06).
        assertThat(replaced.getBody().eventType()).isEqualTo(EventType.SEASON_END);
        assertThat(replaced.getBody().title()).isEqualTo("정정된 제목");
        assertThat(replaced.getBody().occurredAt().toInstant()).isEqualTo(MAR.toInstant());

        GameEvent persisted = gameEventRepository.findById(id).orElseThrow();
        assertThat(persisted.getOccurredAt().toInstant()).isEqualTo(MAR.toInstant());
        // updated_at advanced past created_at via @PreUpdate (D-07); created_at unchanged.
        assertThat(persisted.getCreatedAt().toInstant()).isEqualTo(createdAt.toInstant());
        assertThat(persisted.getUpdatedAt()).isAfter(persisted.getCreatedAt());
    }

    @Test
    void deleteReturns204AndRemovesFromList() {
        Long id = create(new GameEventRequest(EventType.LOA_ON, "삭제 대상", JAN, null)).id();

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/admin/events/" + id, HttpMethod.DELETE, AdminAuth.entity(adminSecret), Void.class);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleted.getBody()).isNull();
        assertThat(gameEventRepository.findById(id)).isEmpty();
    }

    @Test
    void putOnMissingIdReturns404Contract() {
        GameEventRequest request = new GameEventRequest(EventType.LOA_ON, "없는 이벤트", JAN, null);

        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/events/999999", HttpMethod.PUT, AdminAuth.entity(request, adminSecret),
                ApiErrorResponse.class);

        assertContract(resp, HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteOnMissingIdReturns404Contract() {
        ResponseEntity<ApiErrorResponse> resp = rest.exchange(
                "/api/admin/events/999999", HttpMethod.DELETE, AdminAuth.entity(adminSecret), ApiErrorResponse.class);

        assertContract(resp, HttpStatus.NOT_FOUND);
    }

    @Test
    void postWithBlankTitleReturns400Contract() {
        assertContract(post(new GameEventRequest(EventType.LOA_ON, "  ", JAN, null)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void postWithNullEventTypeReturns400Contract() {
        assertContract(post(new GameEventRequest(null, "제목", JAN, null)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void postWithNullOccurredAtReturns400Contract() {
        assertContract(post(new GameEventRequest(EventType.LOA_ON, "제목", null, null)), HttpStatus.BAD_REQUEST);
    }

    private GameEventResponse create(GameEventRequest request) {
        ResponseEntity<GameEventResponse> resp = rest.exchange(
                "/api/admin/events", HttpMethod.POST, AdminAuth.entity(request, adminSecret), GameEventResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private ResponseEntity<ApiErrorResponse> post(GameEventRequest request) {
        return rest.exchange(
                "/api/admin/events", HttpMethod.POST, AdminAuth.entity(request, adminSecret), ApiErrorResponse.class);
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