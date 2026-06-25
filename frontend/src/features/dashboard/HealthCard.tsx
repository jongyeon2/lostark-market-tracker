import { useCollectionHealth } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { formatKst } from '@/lib/formatKst'
import { cn } from '@/lib/utils'
import type { CollectionHealth } from '@/lib/schemas'

import { StatusBadge } from './StatusBadge'
import { SummaryMarker } from './SummaryMarker'

/*
  HealthCard — the Dashboard's focal, full-width proof-of-life widget (DASH-01/02). It consumes the
  07-02 useCollectionHealth() hook and wraps ITS OWN body in <AsyncBoundary> (Phase 7 D-08), so a
  health failure renders an in-card ErrorState (+'다시 불러오기') and never blanks the item grid 08-03
  places below it — per-widget isolation (D-07). On success it composes the 08-01 primitives
  (StatusBadge for the four-grade status, SummaryMarker for AUTH_ERROR/RATE_LIMITED) — it does not
  re-derive the mapping. NO_RUNS is a SUCCESS-path waiting branch (calm copy, not an error — 완료조건
  #5), since health is a single object, not an empty list (AsyncBoundary isEmpty is not used here).
  Read-only: one hook, no polling, no write surface, no backend change.
*/
export function HealthCard() {
  const { status, data, refetch } = useCollectionHealth()

  return (
    <Card>
      <AsyncBoundary status={status} onRetry={() => refetch()}>
        {data && <HealthContent health={data} />}
      </AsyncBoundary>
    </Card>
  )
}

function HealthContent({ health }: { health: CollectionHealth }) {
  const { status, summaryMessage, lastRunAt, itemsAttempted, itemsSucceeded, itemsFailed } = health
  const isNoRuns = status === 'NO_RUNS'

  return (
    <>
      {/* Title (Heading 20/600) left, four-grade StatusBadge right. */}
      <CardHeader className="flex flex-row items-center justify-between gap-4">
        <CardTitle className="text-xl">수집 헬스</CardTitle>
        <StatusBadge status={status} />
      </CardHeader>

      <CardContent className="space-y-2">
        {isNoRuns ? (
          // NO_RUNS waiting branch — calm copy, NOT an error (완료조건 #5). Matches the shared
          // EmptyState typography (Heading + muted Body) so a not-yet-run pipeline reads as waiting.
          <div className="space-y-1">
            <p className="text-xl font-semibold">아직 수집 실행 기록이 없어요</p>
            <p className="text-muted-foreground text-base">
              스케줄러가 첫 수집을 마치면 시도·성공·실패와 마지막 실행 시각이 여기 표시됩니다.
            </p>
          </div>
        ) : (
          <>
            {/* Counts equal the raw /api/health/collection numbers; tabular-nums aligns digits.
                실패 turns text-down only when there are real failures. */}
            <p className="text-sm font-semibold tabular-nums">
              시도 {itemsAttempted} · 성공 {itemsSucceeded} ·{' '}
              <span className={cn(itemsFailed > 0 && 'text-down')}>실패 {itemsFailed}</span>
            </p>
            <p className="text-muted-foreground text-sm font-semibold">
              마지막 실행 {lastRunAt ? formatKst(lastRunAt) : '—'}
            </p>
          </>
        )}

        {/* Diagnostic marker (AUTH_ERROR/RATE_LIMITED) — renders nothing when summaryMessage is null. */}
        <SummaryMarker summaryMessage={summaryMessage} />
      </CardContent>
    </>
  )
}
