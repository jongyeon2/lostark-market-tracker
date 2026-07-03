import type { CollectionHealth } from '@/lib/schemas'

/*
  17-01 / D-03: the SINGLE source of the "수집 중" vs "데이터 없음" judgment for the three read screens
  (Dashboard·Timeline·Impact). Screens never re-interpret the raw CollectionHealth.status literals
  themselves — they call deriveCollectionEmptyKind() and branch on the returned kind. The judgment is
  read straight off GET /api/health/collection (CollectionHealth), the same contract HealthCard/
  StatusBadge already consume, with zero backend change (D-08).

  - 'collecting' — a run exists and the pipeline is alive (SUCCESS / PARTIAL_SUCCESS / any non-terminal
    status), so data is simply still accumulating. This is the honest "the collector is working" state.
  - 'no-data'    — NO_RUNS (never ran) or FAILED, or a diagnostic summaryMessage (AUTH_ERROR /
    RATE_LIMITED) that means the last attempt produced nothing usable.

  When health is still undefined (React Query pending) the consumer already renders a loading/skeleton
  state, so we default to 'collecting' — the calm "alive" framing rather than a false "no data".
*/
export type CollectionEmptyKind = 'collecting' | 'no-data'

export function deriveCollectionEmptyKind(health: CollectionHealth | undefined): CollectionEmptyKind {
  if (!health) return 'collecting'
  if (health.status === 'NO_RUNS' || health.status === 'FAILED') return 'no-data'
  if (health.summaryMessage === 'AUTH_ERROR' || health.summaryMessage === 'RATE_LIMITED') return 'no-data'
  return 'collecting'
}

/** Convenience predicate for screens that only need the alive/'수집 중' framing. */
export function isCollecting(health: CollectionHealth | undefined): boolean {
  return deriveCollectionEmptyKind(health) === 'collecting'
}
