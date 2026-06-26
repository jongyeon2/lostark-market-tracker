import { useEffect } from 'react'

import { useItems } from '@/lib/queries'
import { ItemSelect } from '@/features/_shared/ItemSelect'
import { LatestPriceCard } from '@/features/_shared/LatestPriceCard'
import { useImpactParams } from '@/features/impact/useImpactParams'
import { WindowControls } from '@/features/impact/WindowControls'
import { CorrelationBanner } from '@/features/impact/CorrelationBanner'

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

  const displayName = items?.find((i) => i.id === itemId)?.displayName ?? ''

  return (
    <div className="space-y-6">
      {/* 상관 ≠ 인과 — content-top, full width, always visible (D-10). */}
      <CorrelationBanner />

      <h1 className="text-[28px] leading-tight font-semibold">이벤트 영향</h1>

      {/* Control bar: selector + window controls (wraps on narrow widths). */}
      <div className="flex flex-wrap items-end gap-4">
        <ItemSelect value={itemId} onChange={setItem} />
        <WindowControls window={window} onWindowChange={setWindow} />
      </div>

      {/* Latest-price card — its own AsyncBoundary, rendered once a selection resolves (D-12). */}
      {itemId != null && <LatestPriceCard itemId={itemId} displayName={displayName} />}

      {/* Results area — Task 2 wires <ImpactResults itemId window onResetWindow /> here. */}
    </div>
  )
}
