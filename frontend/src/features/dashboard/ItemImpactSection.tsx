import { PackageSearch, TriangleAlert } from 'lucide-react'

import { useEventImpact } from '@/lib/queries'
import { ApiError } from '@/lib/api'
import { WindowControls } from '@/features/impact/WindowControls'
import { CorrelationBanner } from '@/features/impact/CorrelationBanner'
import { EventImpactTable } from '@/features/impact/EventImpactTable'
import { EventImpactCards } from '@/features/impact/EventImpactCards'
import { ErrorState } from '@/components/state/ErrorState'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

/*
  ItemImpactSection — the event-impact results for the item selected in the dashboard's center column
  (Phase 28, DASH-02). The /impact PAGE is gone: it made you navigate away and re-pick, with a selector,
  the item you were already looking at.

  What moved is only the COMPOSITION. Every part that decides what the numbers mean is reused as-is —
  EventImpactTable / EventImpactCards / impactFormat / ImpactStatusBadge / CorrelationBanner /
  WindowControls are untouched. Re-implementing any of them would fork the 10-UI-SPEC copy contract
  (the 400/404/empty wording below is verbatim from it) and let the table and cards drift apart.

  Both responsive views render; CSS (md breakpoint) toggles which is visible (D-04/D-09) — unchanged.
  LatestPriceCard is deliberately NOT included: the selected item's card sits directly above and
  already shows its 최저가, so repeating it would just push the events further down.
*/
export function ItemImpactSection({
  itemId,
  displayName,
  window,
  onWindowChange,
}: {
  itemId: number
  displayName: string
  window: number
  onWindowChange: (w: number) => void
}) {
  return (
    <section className="space-y-4">
      <div className="space-y-1">
        <h2 className="text-base font-semibold">
          이벤트 영향{displayName ? ` · ${displayName}` : ''}
        </h2>
      </div>
      {/* 상관 ≠ 인과 — 표보다 먼저 읽히도록 위에(D-10). changeRate는 시점 상관이지 인과가 아니다. */}
      <CorrelationBanner />
      <WindowControls window={window} onWindowChange={onWindowChange} />
      <ImpactResults itemId={itemId} window={window} onResetWindow={() => onWindowChange(24)} />
    </section>
  )
}

/*
  ImpactResults — its OWN error scope, HONEST about every outcome (D-03/D-09): instead of one generic
  error it reads ApiError.status and shows distinct copy for 400 (window ≤0 or >168) / 404 (unknown
  item) / 200-empty (no events, D-11) — each with its own next action. Copy is verbatim from
  10-UI-SPEC; the backend occurred_at-desc order is preserved. Moved from ImpactPage unchanged.
*/
function ImpactResults({
  itemId,
  window,
  onResetWindow,
}: {
  itemId: number
  window: number
  onResetWindow: () => void
}) {
  const impact = useEventImpact(itemId, window)

  if (impact.status === 'pending') {
    return (
      <div className="space-y-3" role="status" aria-busy="true">
        <Skeleton className="h-[320px] w-full" />
        <p className="text-muted-foreground text-sm">불러오는 중…</p>
      </div>
    )
  }

  if (impact.status === 'error') {
    const status = impact.error instanceof ApiError ? impact.error.status : null
    if (status === 400) {
      // D-03 recovery: out-of-range window → distinct copy + "24시간으로 보기" resets to the default.
      return (
        <Alert variant="destructive" className="max-w-md">
          <TriangleAlert />
          <AlertTitle>윈도우 값을 다시 확인해 주세요</AlertTitle>
          <AlertDescription>
            윈도우는 1~168시간 사이의 정수여야 합니다. 프리셋 버튼을 누르거나 범위 안의 값을 입력해 다시 불러오세요.
          </AlertDescription>
          <div className="col-start-2 mt-3">
            <Button onClick={onResetWindow}>24시간으로 보기</Button>
          </div>
        </Alert>
      )
    }
    if (status === 404) {
      return (
        <Alert className="max-w-md">
          <PackageSearch />
          <AlertTitle>존재하지 않는 품목이에요</AlertTitle>
          <AlertDescription>
            선택한 품목을 찾을 수 없습니다. 위 목록에서 다른 품목을 선택해 주세요.
          </AlertDescription>
        </Alert>
      )
    }
    // network/other — inherited shared error copy ('백엔드에 연결하지 못했어요…').
    return <ErrorState onRetry={() => impact.refetch()} />
  }

  const { events } = impact.data

  // D-11 / 17-01 D-04 Impact: 200 + empty events (admin registered 0 events) — distinct from per-row
  // insufficient_data (handled inside EventImpactTable/Cards, untouched).
  if (events.length === 0) {
    return (
      <div className="flex flex-col items-center gap-4 py-16 text-center">
        <h2 className="text-xl font-semibold">등록된 이벤트가 없어요</h2>
        <p className="text-muted-foreground max-w-md text-base">
          이벤트 전후 가격 비교에는 더 많은 데이터·이벤트가 필요합니다. 관리자가 게임 이벤트를 등록하고 수집이 쌓이면 여기에 전후 변화가 표시됩니다.
        </p>
      </div>
    )
  }

  // Both responsive views render; CSS (md breakpoint) toggles which is visible (D-04/D-09).
  return (
    <>
      <EventImpactTable events={events} />
      <EventImpactCards events={events} />
    </>
  )
}
