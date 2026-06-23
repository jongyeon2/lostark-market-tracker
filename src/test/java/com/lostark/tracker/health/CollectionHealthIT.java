package com.lostark.tracker.health;

import com.lostark.tracker.domain.CollectionRun;
import com.lostark.tracker.repository.CollectionRunRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.CollectionHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves {@code GET /api/health/collection} (OPS-01, Success Criterion 5, D-12) on Testcontainers:
 * the snapshot reflects the MOST RECENT {@code collection_run} (timestamps, counts, status, marker),
 * and — the security-critical rule — the raw JSON body leaks no key/Authorization/secret.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectionHealthIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    CollectionRunRepository collectionRunRepository;

    @BeforeEach
    void clean() {
        collectionRunRepository.deleteAll();
    }

    @Test
    void healthReflectsLatestRunAndLeaksNoSecret() {
        // Older run.
        CollectionRun older = new CollectionRun(
                OffsetDateTime.parse("2026-06-21T15:00:00Z"), OffsetDateTime.parse("2026-06-21T15:01:00Z"),
                3, 3, 0, "SUCCESS");
        collectionRunRepository.save(older);
        // Newer run (latest by started_at) — partial success with a RATE_LIMITED marker.
        CollectionRun newer = new CollectionRun(
                OffsetDateTime.parse("2026-06-21T15:10:00Z"), null, 3, 2, 1, "RUNNING");
        newer.finish(OffsetDateTime.parse("2026-06-21T15:11:30Z"), 2, 1, "PARTIAL_SUCCESS");
        newer.setSummaryMessage("RATE_LIMITED");
        collectionRunRepository.save(newer);

        ResponseEntity<CollectionHealthResponse> resp =
                rest.getForEntity("/api/health/collection", CollectionHealthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CollectionHealthResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.lastRunAt().toInstant())
                .isEqualTo(OffsetDateTime.parse("2026-06-21T15:11:30Z").toInstant());
        assertThat(body.startedAt().toInstant())
                .isEqualTo(OffsetDateTime.parse("2026-06-21T15:10:00Z").toInstant());
        assertThat(body.itemsAttempted()).isEqualTo(3);
        assertThat(body.itemsSucceeded()).isEqualTo(2);
        assertThat(body.itemsFailed()).isEqualTo(1);
        assertThat(body.status()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(body.summaryMessage()).isEqualTo("RATE_LIMITED");

        // No-secret guard (D-12): the raw body carries no key/Authorization/bearer/token.
        String raw = rest.getForObject("/api/health/collection", String.class).toLowerCase();
        assertThat(raw).doesNotContain("key");
        assertThat(raw).doesNotContain("authorization");
        assertThat(raw).doesNotContain("bearer");
        assertThat(raw).doesNotContain("token");
    }

    @Test
    void noRunsReturns200WithNoRunsStatus() {
        ResponseEntity<CollectionHealthResponse> resp =
                rest.getForEntity("/api/health/collection", CollectionHealthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo("NO_RUNS");
        assertThat(resp.getBody().lastRunAt()).isNull();
        assertThat(resp.getBody().itemsAttempted()).isZero();
    }
}
