import { useEffect } from 'react'

import { useItems } from '@/lib/queries'
import { ItemSelect } from '@/features/_shared/ItemSelect'
import { LatestPriceCard } from '@/features/_shared/LatestPriceCard'
import { useTimelineParams } from '@/features/timeline/useTimelineParams'
import { RangeControls } from '@/features/timeline/RangeControls'

/*
  TimelinePage — composes the headline timeline screen (TIME-01/05) top-to-bottom per 09-UI-SPEC:
  control bar [ItemSelect][7/30/90일][시작일/종료일] → LatestPriceCard → chart area (legend + chart +
  downsample badge). URL searchParams are the single source of truth (useTimelineParams, D-04): the
  selector/range controls are dumb controlled components wired to setItem/setRange here.

  D-06 default selection: ?item= wins when present; otherwise the FIRST item is auto-selected once
  items load, so the chart is never a blank '품목을 선택하세요' prompt on entry. With the D-03 last-30-days
  default range (containing the seed 8-day window), entry shows a non-empty chart with the 2 demo markers.
*/
export function TimelinePage() {
  const { itemId, from, to, setItem, setRange } = useTimelineParams()
  const { data: items } = useItems()

  // D-06: auto-select the first item when the URL carries no ?item=. The guard makes this a no-op
  // once itemId is resolved (so a present ?item= always wins and there is no selection thrash).
  useEffect(() => {
    if (itemId == null && items && items.length > 0) {
      setItem(items[0].id)
    }
  }, [itemId, items, setItem])

  const displayName = items?.find((i) => i.id === itemId)?.displayName ?? ''

  return (
    <div className="space-y-6">
      <h1 className="text-[28px] leading-tight font-semibold">품목 타임라인</h1>

      {/* Control bar: selector + presets + date inputs (wraps on narrow widths). */}
      <div className="flex flex-wrap items-end gap-4">
        <ItemSelect value={itemId} onChange={setItem} />
        <RangeControls from={from} to={to} onRangeChange={setRange} />
      </div>

      {/* Latest-price card — rendered only once a selection is resolved (its own AsyncBoundary). */}
      {itemId != null && <LatestPriceCard itemId={itemId} displayName={displayName} />}

      {/* Chart area (legend + 360px chart + downsample badge, status-branched) — Task 2. */}
    </div>
  )
}
