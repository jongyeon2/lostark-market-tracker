package com.lostark.tracker.repository;

import java.time.Instant;

/**
 * Spring Data interface projection for one downsample bucket produced by the PostgreSQL
 * {@code date_trunc} aggregate (D-07): the bucket start instant, the average {@code min_price}
 * across the bucket, and how many raw snapshots it covers. Property names match the (quoted)
 * column aliases in {@link PriceSnapshotRepository#aggregateByBucket}.
 *
 * <p>{@code bucketStart} is typed as {@link Instant} because Hibernate returns the native
 * {@code timestamptz} column as an {@code Instant}; the service re-offsets it to UTC. Both bound
 * timestamps are UTC, so the instant is exact (off-by-9h guard, D-11).
 */
public interface PriceBucketView {

    Instant getBucketStart();

    double getAvgMinPrice();

    long getSampleCount();
}
