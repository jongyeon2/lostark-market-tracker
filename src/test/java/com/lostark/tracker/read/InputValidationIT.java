package com.lostark.tracker.read;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.TimelineResponse;
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
 * Proves the {@code /prices} input-validation contract end-to-end (API-05, Success Criterion 5,
 * D-13): {@code from>to} -> 400, {@code window<=0} (to==from) -> 400, missing item -> 404, malformed
 * date -> 400, and a valid-but-empty range -> 200 with empty arrays. The 400 and 404 bodies both
 * carry the custom {@code {timestamp,status,error,message}} shape (D-10).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InputValidationIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-06-21T15:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-06-21T18:00:00Z");

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        gameEventRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    @Test
    void fromAfterToReturns400WithErrorContract() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));

        ResponseEntity<String> resp = getRaw(item.getId(), TO, FROM); // from > to

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"status\":400");
        assertThat(resp.getBody()).contains("\"error\":\"Bad Request\"");
        assertThat(resp.getBody()).contains("\"timestamp\"");
        assertThat(resp.getBody()).contains("\"message\"");
    }

    @Test
    void zeroWindowReturns400() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));

        ResponseEntity<String> resp = getRaw(item.getId(), FROM, FROM); // to == from -> window <= 0

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"status\":400");
    }

    @Test
    void missingItemReturns404() {
        ResponseEntity<String> resp = getRaw(999999L, FROM, TO); // valid range, no such item

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("\"status\":404");
        assertThat(resp.getBody()).contains("\"error\":\"Not Found\"");
    }

    @Test
    void malformedDateReturns400NotServerError() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));

        ResponseEntity<String> resp = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", String.class,
                item.getId(), "not-a-date", TO.toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"status\":400");
    }

    @Test
    void validButEmptyRangeReturns200WithEmptyArrays() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
        // Item exists, valid range, but no snapshots/events -> 200 empty (NOT 404; D-13).

        ResponseEntity<TimelineResponse> resp = rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                item.getId(), FROM.toString(), TO.toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().downsampled()).isFalse();
        assertThat(resp.getBody().snapshots()).isEmpty();
        assertThat(resp.getBody().events()).isEmpty();
    }

    private ResponseEntity<String> getRaw(long itemId, OffsetDateTime from, OffsetDateTime to) {
        return rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", String.class,
                itemId, from.toString(), to.toString());
    }
}
