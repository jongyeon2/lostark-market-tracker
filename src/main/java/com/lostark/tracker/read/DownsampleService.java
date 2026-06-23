package com.lostark.tracker.read;

import com.lostark.tracker.domain.PriceSnapshot;
import com.lostark.tracker.repository.PriceBucketView;
import com.lostark.tracker.repository.PriceSnapshotRepository;
import com.lostark.tracker.web.dto.PricePoint;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Server-side downsampling for the timeline (7A / D-07/08/09). The client passes no parameter: when
 * an item's raw window snapshot count exceeds {@link #TARGET_MAX_POINTS} (~500) the range is
 * aggregated into {@code avg(min_price)} buckets via PostgreSQL {@code date_trunc}; at or below it
 * the raw points are returned unchanged.
 *
 * <p>Bucket width is dynamic (D-09): {@code "hour"} while the hour-bucket count would stay within the
 * target, else {@code "day"} for longer ranges — keeping the bucket count near/below the target. The
 * {@code unit} is a server-chosen whitelist value, never client input.
 */
@Service
public class DownsampleService {

    /** Soft cap on returned points before the server aggregates (D-09 N≈500). */
    static final int TARGET_MAX_POINTS = 500;

    /** The chosen snapshot points plus the downsample metadata for the response (D-08). */
    public record DownsampleResult(boolean downsampled, String bucketWidth, List<PricePoint> snapshots) {
    }

    private final PriceSnapshotRepository priceSnapshotRepository;

    public DownsampleService(PriceSnapshotRepository priceSnapshotRepository) {
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    /**
     * Decide raw-vs-bucketed for an item's window. {@code rawSnapshots} is the already-fetched window
     * (from {@link WindowQueryService}) so the common small-range path needs no extra query.
     */
    public DownsampleResult downsample(long itemId, OffsetDateTime from, OffsetDateTime to,
                                       List<PriceSnapshot> rawSnapshots) {
        if (rawSnapshots.size() <= TARGET_MAX_POINTS) {
            List<PricePoint> raw = rawSnapshots.stream()
                    .map(s -> new PricePoint(s.getCollectedAt(), s.getMinPrice(), null))
                    .toList();
            return new DownsampleResult(false, null, raw);
        }

        String unit = chooseBucketUnit(from, to);
        List<PricePoint> buckets = priceSnapshotRepository
                .aggregateByBucket(itemId, unit, from, to).stream()
                .map(DownsampleService::toPoint)
                .toList();
        return new DownsampleResult(true, unit, buckets);
    }

    /**
     * Hour buckets while the span (in hours) stays within the target point budget, else day buckets.
     * With the ~500 target this switches to "day" past ~21 days, so a 30-day range buckets by day.
     */
    private String chooseBucketUnit(OffsetDateTime from, OffsetDateTime to) {
        long spanHours = Duration.between(from, to).toHours();
        return spanHours <= TARGET_MAX_POINTS ? "hour" : "day";
    }

    private static PricePoint toPoint(PriceBucketView bucket) {
        // Re-offset the native timestamptz Instant to UTC; both query bounds are UTC, so it is exact.
        return new PricePoint(bucket.getBucketStart().atOffset(ZoneOffset.UTC),
                Math.round(bucket.getAvgMinPrice()),
                (int) bucket.getSampleCount());
    }
}
