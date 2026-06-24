package com.lostark.tracker;

import com.lostark.tracker.domain.CollectionRun;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.AdminAuth;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.TrackedItemRequest;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the locked 4-table model end-to-end on real Postgres (Testcontainers):
 * DATA-01 (UNIQUE idempotency), DATA-02 (TIMESTAMPTZ UTC round-trip), DATA-03 (item insert/read
 * via real HTTP), DATA-04 (collection_run record).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SchemaRoundTripIT extends PostgresRedisContainers {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TrackedItemRepository trackedItemRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private CollectionRunRepository collectionRunRepository;

    @Value("${admin.api.secret:test-admin-secret}")
    private String adminSecret;

    @BeforeEach
    void clean() {
        // Child first (FK), then parents. Tests are not @Transactional (RANDOM_PORT server thread).
        priceSnapshotRepository.deleteAll();
        trackedItemRepository.deleteAll();
        collectionRunRepository.deleteAll();
    }

    @Test
    void itemInsertedAndReadBackViaHttp() {
        TrackedItemRequest request = new TrackedItemRequest("66130141", "아비도스 융화 재료", "ENHANCEMENT");

        // Item create moved behind /api/admin/items (D-04); send the X-Admin-Secret header so this
        // round-trip survives the 04-02 gate unchanged. The GET read-back stays on the public surface.
        ResponseEntity<TrackedItemResponse> created = restTemplate.exchange(
                "/api/admin/items", HttpMethod.POST,
                AdminAuth.entity(request, adminSecret), TrackedItemResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().id()).isNotNull();
        assertThat(created.getBody().externalItemId()).isEqualTo("66130141");
        assertThat(created.getBody().displayName()).isEqualTo("아비도스 융화 재료");

        ResponseEntity<TrackedItemResponse[]> listed =
                restTemplate.getForEntity("/api/items", TrackedItemResponse[].class);

        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody())
                .extracting(TrackedItemResponse::externalItemId)
                .contains("66130141");
    }

    @Test
    void timestamptzRoundTripsAsUtcInstant() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("utc-1", "UTC item", "ENHANCEMENT"));
        // 2026-01-01T15:00Z == 2026-01-02 00:00 KST — a KST-midnight boundary that would expose an off-by-9h bug.
        OffsetDateTime collectedAt = OffsetDateTime.of(2026, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fetchedAt = OffsetDateTime.of(2026, 1, 1, 15, 0, 7, 0, ZoneOffset.UTC);

        PriceSnapshot saved = priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, 1234L, fetchedAt));
        PriceSnapshot read = priceSnapshotRepository.findById(saved.getId()).orElseThrow();

        assertThat(read.getCollectedAt().toInstant()).isEqualTo(collectedAt.toInstant());
        assertThat(read.getFetchedAt().toInstant()).isEqualTo(fetchedAt.toInstant());
        assertThat(read.getMinPrice()).isEqualTo(1234L);
    }

    @Test
    void duplicateSnapshotViolatesUniqueConstraint() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("dup-1", "Dup item", "ENHANCEMENT"));
        OffsetDateTime tick = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        priceSnapshotRepository.saveAndFlush(new PriceSnapshot(item, tick, 100L, tick));

        // Same (tracked_item_id, collected_at) -> DB UNIQUE violation (DATA-01 idempotency).
        assertThrows(DataIntegrityViolationException.class, () ->
                priceSnapshotRepository.saveAndFlush(new PriceSnapshot(item, tick, 200L, tick)));
    }

    @Test
    void collectionRunRecordsStartFinishAndCounts() {
        OffsetDateTime started = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime finished = started.plusMinutes(2);

        CollectionRun saved = collectionRunRepository.save(
                new CollectionRun(started, finished, 10, 9, 1, "SUCCESS"));
        CollectionRun read = collectionRunRepository.findById(saved.getId()).orElseThrow();

        assertThat(read.getItemsAttempted()).isEqualTo(10);
        assertThat(read.getItemsSucceeded()).isEqualTo(9);
        assertThat(read.getItemsFailed()).isEqualTo(1);
        assertThat(read.getStatus()).isEqualTo("SUCCESS");
        assertThat(read.getStartedAt().toInstant()).isEqualTo(started.toInstant());
        assertThat(read.getFinishedAt().toInstant()).isEqualTo(finished.toInstant());
    }
}
