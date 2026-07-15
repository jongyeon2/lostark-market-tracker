import { PackageSearch } from 'lucide-react'

import { useEventImpact } from '@/lib/queries'
import { ApiError } from '@/lib/api'
import { CorrelationBanner } from '@/features/impact/CorrelationBanner'
import { EventImpactTable } from '@/features/impact/EventImpactTable'
import { EventImpactCards } from '@/features/impact/EventImpactCards'
import { ErrorState } from '@/components/state/ErrorState'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'

/*
  ItemImpactSection — the event-impact results for the item selected in the dashboard's center column
  (Phase 28, DASH-02). The /impact PAGE is gone: it made you navigate away and re-pick, with a selector,
  the item you were already looking at.

  What moved is only the COMPOSITION. Every part that decides what the numbers mean is reused as-is —
  EventImpactTable / EventImpactCards / impactFormat / ImpactStatusBadge / CorrelationBanner are
  untouched. Re-implementing any of them would fork the 10-UI-SPEC copy contract (the 404/empty
  wording below is verbatim from it) and let the table and cards drift apart. WindowControls is the
  one part that did NOT come along: the window picker was dropped (사용자 결정 2026-07-15) and the
  component had no other caller, so it was deleted rather than left orphaned.

  Both responsive views render; CSS (md breakpoint) toggles which is visible (D-04/D-09) — unchanged.
  LatestPriceCard is deliberately NOT included: the selected item's card sits directly above and
  already shows its 최저가, so repeating it would just push the events further down. Neither is the
  item's NAME: the card you just clicked is right above, still wearing its selected state.
*/

/*
  이벤트 전후 비교 창(시간). 사용자가 고르던 6/24/72 프리셋을 제거하고 기본값으로 고정했다
  (사용자 결정 2026-07-15) — 대시보드에서 카드를 눌러 바로 보는 흐름에서는 창 조절이 화면만 차지했다.
  백엔드는 1~168을 받으므로 이 상수는 항상 유효하다.
*/
const IMPACT_WINDOW_HOURS = 24

export function ItemImpactSection({ itemId }: { itemId: number }) {
  return (
    <section className="space-y-4">
      <h2 className="text-base font-semibold">이벤트 영향</h2>
      {/* 상관 ≠ 인과 — 표보다 먼저 읽히도록 위에(D-10). changeRate는 시점 상관이지 인과가 아니다. */}
      <CorrelationBanner />
      <ImpactResults itemId={itemId} window={IMPACT_WINDOW_HOURS} />
    </section>
  )
}

/*
  ImpactResults — its OWN error scope, HONEST about every outcome (D-03/D-09): it reads
  ApiError.status and shows distinct copy for 404 (unknown item) / 200-empty (no events, D-11) — each
  with its own next action. Copy is verbatim from 10-UI-SPEC; the backend occurred_at-desc order is
  preserved.

  The 400 branch that used to live here (window ≤0 or >168, with a "24시간으로 보기" reset button) is
  GONE with the window picker: the window is now the constant 24, which the backend always accepts, so
  a 400 is unreachable. Keeping it would be dead code claiming an outcome that cannot happen, and its
  button would "reset" 24 to 24. A genuine 400 (contract change) still surfaces via ErrorState below.
*/
function ImpactResults({ itemId, window }: { itemId: number; window: number }) {
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
