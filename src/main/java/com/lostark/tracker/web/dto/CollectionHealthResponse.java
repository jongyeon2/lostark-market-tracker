package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.CollectionRun;

import java.time.OffsetDateTime;

/**
 * The collection-pipeline health snapshot (OPS-01, D-12): the latest run's timestamps, per-item
 * counts, status, and the categorical {@code summaryMessage} marker (AUTH_ERROR / RATE_LIMITED or
 * null). Counts/timestamps/markers ONLY — there is deliberately no field for the API key,
 * Authorization header, or any secret (the marker is already safe by Phase 2 construction).
 */
public record CollectionHealthResponse(
        OffsetDateTime lastRunAt,
        OffsetDateTime startedAt,
        int itemsAttempted,
        int itemsSucceeded,
        int itemsFailed,
        String status,
        String summaryMessage
) {
    /** Map the latest run; {@code lastRunAt} is its finish instant (D-12). */
    public static CollectionHealthResponse from(CollectionRun run) {
        return new CollectionHealthResponse(
                run.getFinishedAt(),
                run.getStartedAt(),
                run.getItemsAttempted(),
                run.getItemsSucceeded(),
                run.getItemsFailed(),
                run.getStatus(),
                run.getSummaryMessage());
    }

    /** Defined response when the pipeline has not run yet — no crash, status {@code NO_RUNS}. */
    public static CollectionHealthResponse noRuns() {
        return new CollectionHealthResponse(null, null, 0, 0, 0, "NO_RUNS", null);
    }
}
