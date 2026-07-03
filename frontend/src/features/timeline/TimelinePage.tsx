import { useEffect } from 'react'
import { PackageSearch, TriangleAlert } from 'lucide-react'

import { useCollectionHealth, useItems, useTimeline } from '@/lib/queries'
import { deriveCollectionEmptyKind } from '@/lib/collectionEmptyState'
import { ApiError } from '@/lib/api'
import { ItemSelect } from '@/features/_shared/ItemSelect'
import { LatestPriceCard } from '@/features/_shared/LatestPriceCard'
import { useTimelineParams } from '@/features/timeline/useTimelineParams'
import { RangeControls } from '@/features/timeline/RangeControls'
import { PriceTimelineChart } from '@/features/timeline/PriceTimelineChart'
import { aggregateDailyAverage } from '@/features/timeline/dailyBuckets'
import { EventMarkerLegend } from '@/features/timeline/EventMarkerLegend'
import { DownsampleBadge } from '@/features/timeline/DownsampleBadge'
import { ErrorState } from '@/components/state/ErrorState'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

// last-30-days window as ISO instants — used by the 200-empty CTA to reset the range (D-03/D-09).
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000
function last30Days(): [string, string] {
  const now = Date.now()
  return [new Date(now - THIRTY_DAYS_MS).toISOString(), new Date(now).toISOString()]
}

/*
  ChartArea — the chart scope, kept in its OWN error boundary so a chart failure never blanks the
  LatestPriceCard and vice-versa (Phase-7 D-08 / Phase-8 D-07). It is HONEST about every outcome
  (D-09, TIME-05): instead of one generic error it reads ApiError.status and shows distinct copy for
  400 (to≤from) / 404 (unknown item) / 200-empty (no data in range) — each with its own next action.
*/
function ChartArea({
  itemId,
  from,
  to,
  onResetRange,
}: {
  itemId: number
  from: string
  to: string
  onResetRange: () => void
}) {
  const timeline = useTimeline(itemId, from, to)
  // 17-01 (D-04 Timeline): shared health hook (queryKey dedupe → no extra network) drives whether the
  // 200-empty range reads as '이 품목 시계열 수집 중'(pipeline alive) vs no-data. The range recovery
  // CTA and the 400/404/network branches below are untouched (we only reframe the snapshots-empty copy).
  const { data: health } = useCollectionHealth()

  if (timeline.status === 'pending') {
    return (
      <div className="space-y-3" role="status" aria-busy="true">
        <Skeleton className="h-[360px] w-full" />
        <p className="text-muted-foreground text-sm">불러오는 중…</p>
      </div>
    )
  }

  if (timeline.status === 'error') {
    const err = timeline.error
    const status = err instanceof ApiError ? err.status : null
    if (status === 400) {
      return (
        <Alert variant="destructive" className="max-w-md">
          <TriangleAlert />
          <AlertTitle>조회 기간을 다시 확인해 주세요</AlertTitle>
          <AlertDescription>
            종료일이 시작일보다 같거나 빠릅니다. 시작일 이후의 종료일을 선택하고 다시 불러오세요.
          </AlertDescription>
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
    return <ErrorState onRetry={() => timeline.refetch()} />
  }

  const { snapshots, events } = timeline.data

  if (snapshots.length === 0) {
    // 17-01 D-04 Timeline: reframe the 200-empty range with collection state, keeping the '최근 30일'
    // recovery CTA. 'collecting' → the collector is alive and this item's series is still filling up.
    const collecting = deriveCollectionEmptyKind(health) === 'collecting'
    return (
      <div className="flex flex-col items-center gap-4 py-16 text-center">
        <h2 className="text-xl font-semibold">
          {collecting ? '이 품목 시계열 수집 중' : '이 기간에는 표시할 시세가 없어요'}
        </h2>
        <p className="text-muted-foreground max-w-md text-base">
          {collecting
            ? '수집기가 이 품목의 시세를 모으는 중입니다. 잠시 후 다시 확인하거나, 아래 "최근 30일"로 기간을 넓혀보세요.'
            : '선택한 기간에 수집된 가격이 없습니다. 기간을 넓히거나 아래 "최근 30일"을 눌러보세요.'}
        </p>
        <Button onClick={onResetRange}>최근 30일 보기</Button>
      </div>
    )
  }

  // The timeline always renders ONE point per KST day (일별 평균 최저가, quick 260630-h16). The
  // backend's downsampled/bucketWidth flags are superseded here: after daily re-aggregation the
  // series is ALWAYS a day-bucket average regardless of how much raw data exists, so the badge and
  // tooltip are pinned to that ('버킷 평균 · 1일' + 'N개 평균'). Events keep their own instants.
  const dailySnapshots = aggregateDailyAverage(snapshots)

  return (
    <div className="space-y-4">
      {/* legend left, downsample badge top-right (09-UI-SPEC Layout). */}
      <div className="flex items-start justify-between gap-4">
        <EventMarkerLegend />
        <DownsampleBadge downsampled bucketWidth="day" />
      </div>
      <PriceTimelineChart
        snapshots={dailySnapshots}
        events={events}
        downsampled
        bucketWidth="day"
      />
    </div>
  )
}

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

  // Pull the selected item once so its enrichment (icon/role) flows to the identity card (ICON-04).
  const selected = items?.find((i) => i.id === itemId)
  const displayName = selected?.displayName ?? ''

  return (
    <div className="space-y-6">
      <h1 className="text-[28px] leading-tight font-semibold">품목 타임라인</h1>

      {/* Control bar: selector + presets + date inputs (wraps on narrow widths). */}
      <div className="flex flex-wrap items-end gap-4">
        <ItemSelect value={itemId} onChange={setItem} />
        <RangeControls from={from} to={to} onRangeChange={setRange} />
      </div>

      {/* Latest-price card — rendered only once a selection is resolved (its own AsyncBoundary). */}
      {itemId != null && (
        <LatestPriceCard
          itemId={itemId}
          displayName={displayName}
          iconUrl={selected?.iconUrl ?? null}
          roleGroup={selected?.roleGroup ?? null}
        />
      )}

      {/* Chart area — own error scope; only fetches once a selection is resolved (itemId != null). */}
      {itemId != null && (
        <ChartArea
          itemId={itemId}
          from={from}
          to={to}
          onResetRange={() => setRange(...last30Days())}
        />
      )}
    </div>
  )
}
