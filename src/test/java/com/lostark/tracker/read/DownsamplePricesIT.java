package com.lostark.tracker.read;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.support.PostgresRedisContainers;
import com.lostark.tracker.web.dto.PricePoint;
import com.lostark.tracker.web.dto.TimelineResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves server-side auto-downsampling on {@code /prices} (API-04, Success Criterion 4, D-07/08/09)
 * on Testcontainers: a window whose raw point count exceeds N (~500) comes back as bounded
 * {@code avg(min_price)} {@code date_trunc} buckets with {@code sampleCount} and {@code downsampled:true};
 * a small window comes back raw with {@code downsampled:false}; and the events array is returned in
 * full either way (events are never downsampled).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownsamplePricesIT extends PostgresRedisContainers {

    @Autowired
    TestRestTemplate rest;
    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;
    @Autowired
    GameEventRepository gameEventRepository;
    @Autowired
    TrackedItemRepository trackedItemRepository;

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-21T15:00:00Z");

    @BeforeEach
    void clean() {
        priceSnapshotRepository.deleteAll();
        gameEventRepository.deleteAll();
        trackedItemRepository.deleteAll();
    }

    @Test
    void largeRangeAutoDownsamplesToHourlyAverageBuckets() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
        // 600 raw points at 1-min steps over 10 hours (> N=500 -> downsample). Each hour's 60 points
        // share one price = (hourIndex+1)*100, so a bucket's avg(min_price) equals that price exactly.
        List<PriceSnapshot> seed = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            OffsetDateTime at = START.plusMinutes(i);
            long price = (i / 60 + 1) * 100L;
            seed.add(new PriceSnapshot(item, at, price, at));
        }
        priceSnapshotRepository.saveAll(seed);
        gameEventRepository.save(new GameEvent(EventType.MAJOR_UPDATE, "patch", START.plusHours(2), null));

        OffsetDateTime to = START.plusHours(10); // covers all 600 points
        ResponseEntity<TimelineResponse> resp = getTimeline(item.getId(), START, to);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        TimelineResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.downsampled()).isTrue();
        assertThat(body.bucketWidth()).isEqualTo("hour");
        // 10 distinct hour buckets, well under the N=500 cap.
        assertThat(body.snapshots()).hasSize(10);
        assertThat(body.snapshots().size()).isLessThanOrEqualTo(500);
        // First bucket starts on the hour and averages hour-0's price (100) over its 60 samples.
        PricePoint firstBucket = body.snapshots().get(0);
        assertThat(firstBucket.collectedAt().toInstant()).isEqualTo(START.toInstant());
        assertThat(firstBucket.minPrice()).isEqualTo(100L);
        assertThat(firstBucket.sampleCount()).isEqualTo(60);
        // Every bucket carries a sample count; ascending bucket order.
        assertThat(body.snapshots()).allSatisfy(p -> assertThat(p.sampleCount()).isPositive());
        assertThat(body.snapshots()).extracting(p -> p.collectedAt().toInstant()).isSorted();
        // Events are returned in full, never downsampled.
        assertThat(body.events()).hasSize(1);
    }

    @Test
    void smallRangeReturnsRawPointsNotDownsampled() {
        TrackedItem item = trackedItemRepository.save(new TrackedItem("1001", "itemA", "50010"));
        snap(item, START, 100);
        snap(item, START.plusMinutes(1), 200);
        snap(item, START.plusMinutes(2), 300);
        gameEventRepository.save(new GameEvent(EventType.LOA_ON, "minor", START.plusMinutes(1), null));

        ResponseEntity<TimelineResponse> resp = getTimeline(item.getId(), START, START.plusHours(1));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        TimelineResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.downsampled()).isFalse();
        assertThat(body.bucketWidth()).isNull();
        assertThat(body.snapshots()).extracting(p -> p.minPrice()).containsExactly(100L, 200L, 300L);
        // Raw points carry no sampleCount.
        assertThat(body.snapshots()).allSatisfy(p -> assertThat(p.sampleCount()).isNull());
        // Events array is present and unchanged in the raw case too.
        assertThat(body.events()).hasSize(1);
    }

    private ResponseEntity<TimelineResponse> getTimeline(long itemId, OffsetDateTime from, OffsetDateTime to) {
        return rest.getForEntity(
                "/api/items/{id}/prices?from={from}&to={to}", TimelineResponse.class,
                itemId, from.toString(), to.toString());
    }

    private void snap(TrackedItem item, OffsetDateTime collectedAt, long minPrice) {
        priceSnapshotRepository.save(new PriceSnapshot(item, collectedAt, minPrice, collectedAt));
    }
}
