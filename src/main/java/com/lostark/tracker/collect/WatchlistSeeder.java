package com.lostark.tracker.collect;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.TrackedItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the curated watchlist into {@code tracked_item} on startup (D-01/D-02). Idempotent: each
 * entry is upserted by {@code external_item_id}, so re-running adds no duplicates. Active only
 * under the {@code dev}/{@code seed} profiles — never under {@code test} (integration tests insert
 * their own fixtures).
 *
 * <p>Each entry maps to a {@code POST /markets/items} call as (CategoryCode = {@code category},
 * ItemName = {@code displayName}); the response is matched back by {@code Id == externalItemId}
 * (D-05). So {@code category} holds the numeric leaf CategoryCode, NOT a label.
 *
 * <p><b>Id verification status:</b> only {@code 66102101} (수호석 조각) is confirmed against the live
 * API by the Task 0 spike. The remaining refining-material Ids are domain-knowledge candidates in
 * the same {@code 50010} leaf and should be validated against the live API before a production run
 * — an unverified Id simply resolves to skip-light (NOT_FOUND) at collection time, never a crash.
 */
@Component
@Profile({"dev", "seed"})
public class WatchlistSeeder implements ApplicationRunner {

    /** A watchlist entry: stable Id, display name (ItemName filter), and leaf CategoryCode. */
    private record SeedItem(String externalItemId, String displayName, String category) {
    }

    // Curated high-volatility refining/honing materials (leaf CategoryCode 50010 = 재련 재료).
    private static final List<SeedItem> WATCHLIST = List.of(
            new SeedItem("66102101", "수호석 조각", "50010"),   // Task-0 verified
            new SeedItem("66102102", "파괴석 조각", "50010"),
            new SeedItem("66102103", "정제된 파괴강석", "50010"),
            new SeedItem("66102104", "정제된 수호강석", "50010"),
            new SeedItem("66102105", "운명의 파괴석", "50010"),
            new SeedItem("66102106", "운명의 수호석", "50010"),
            new SeedItem("66110221", "운명의 돌파석", "50010"),
            new SeedItem("66110222", "운명의 수호석 결정", "50010"),
            new SeedItem("66111121", "운명의 파편 주머니(소)", "50020"),
            new SeedItem("66111122", "운명의 파편 주머니(중)", "50020"),
            new SeedItem("66111123", "운명의 파편 주머니(대)", "50020"),
            new SeedItem("66130141", "찬란한 명예의 돌파석", "50010")
    );

    private final TrackedItemRepository trackedItemRepository;

    public WatchlistSeeder(TrackedItemRepository trackedItemRepository) {
        this.trackedItemRepository = trackedItemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedItem seed : WATCHLIST) {
            // Idempotent upsert by external_item_id: insert when absent, otherwise leave existing
            // (Phase 4 admin CRUD owns later edits — the seeder must not clobber manual changes).
            trackedItemRepository.findByExternalItemId(seed.externalItemId())
                    .orElseGet(() -> trackedItemRepository.save(
                            new TrackedItem(seed.externalItemId(), seed.displayName(), seed.category())));
        }
    }
}
