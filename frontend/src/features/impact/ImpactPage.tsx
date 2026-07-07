import { useEffect } from 'react'
import { PackageSearch, TriangleAlert } from 'lucide-react'

import { useItems, useEventImpact } from '@/lib/queries'
import { ApiError } from '@/lib/api'
import { ItemSelect } from '@/features/_shared/ItemSelect'
import { LatestPriceCard } from '@/features/_shared/LatestPriceCard'
import { useImpactParams } from '@/features/impact/useImpactParams'
import { WindowControls } from '@/features/impact/WindowControls'
import { CorrelationBanner } from '@/features/impact/CorrelationBanner'
import { EventImpactTable } from '@/features/impact/EventImpactTable'
import { EventImpactCards } from '@/features/impact/EventImpactCards'
import { ErrorState } from '@/components/state/ErrorState'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

/*
  ImpactResults — the results scope, kept in its OWN error boundary so a results failure never blanks
  the LatestPriceCard and vice-versa (D-12, Phase-7 D-08 isolation). It is HONEST about every outcome
  (D-03/D-09): instead of one generic error it reads ApiError.status and shows distinct copy for
  400 (window ≤0 or >168) / 404 (unknown item) / 200-empty (no events, D-11) — each with its own next
  action. Copy is verbatim from 10-UI-SPEC; the backend occurred_at-desc order is preserved.
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
  // insufficient_data (handled inside EventImpactTable/Cards, untouched). The 10-UI-SPEC heading
  // "등록된 이벤트가 없어요" is kept; the body is reframed onto the honest "전후 비교엔 더 많은
  // 데이터·이벤트가 필요" framing (Impact is empty mainly because admin events are absent, so the
  // collection-health judgment is only a framing input here — the 400/404/network branches stay as-is).
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

/*
  ImpactPage — composes the event-impact screen (IMPCT-01..04) top-to-bottom per 10-UI-SPEC Layout:
  (1) the always-on 상관 ≠ 인과 banner at the content top (read context before the table, D-10),
  (2) control bar [ItemSelect][WindowControls], (3) LatestPriceCard (D-12), (4) the results area.
  URL searchParams are the single source of truth (useImpactParams, D-01): the selector + window
  controls are dumb controlled components wired to setItem/setWindow here.

  D-06 default selection (inherited from Phase-9): a present ?item= always wins; otherwise the FIRST
  item is auto-selected once items load, so entry is never a blank '품목을 선택하세요' prompt. With the
  D-02 default window 24h, entry shows non-empty results against the seed backend.
*/
export function ImpactPage() {
  const { itemId, window, setItem, setWindow } = useImpactParams()
  const { data: items } = useItems()

  // D-06: auto-select the first item when the URL carries no ?item=. The guard makes this a no-op
  // once itemId is resolved (a present ?item= always wins; no selection thrash).
  useEffect(() => {
    if (itemId == null && items && items.length > 0) {
      setItem(items[0].id)
    }
  }, [itemId, items, setItem])

  // Pull the selected item once so its enrichment (icon/role) flows to the identity card (ICON-05,
  // shown ONCE here — not on each event card, to avoid colliding with Event/Status badges, D-04).
  const selected = items?.find((i) => i.id === itemId)
  const displayName = selected?.displayName ?? ''

  return (
    <div className="space-y-6">
      {/* 상관 ≠ 인과 — content-top, full width, always visible (D-10). */}
      <CorrelationBanner />

      {/* Control bar: selector + window controls (wraps on narrow widths). */}
      <div className="flex flex-wrap items-end gap-4">
        <ItemSelect value={itemId} onChange={setItem} />
        <WindowControls window={window} onWindowChange={setWindow} />
      </div>

      {/* Latest-price card — its own AsyncBoundary, rendered once a selection resolves (D-12). */}
      {itemId != null && (
        <LatestPriceCard
          itemId={itemId}
          displayName={displayName}
          iconUrl={selected?.iconUrl ?? null}
          roleGroup={selected?.roleGroup ?? null}
        />
      )}

      {/* Results area — own async/error scope, independent of the latest-price card (D-12). */}
      {itemId != null && (
        <ImpactResults itemId={itemId} window={window} onResetWindow={() => setWindow(24)} />
      )}
    </div>
  )
}
