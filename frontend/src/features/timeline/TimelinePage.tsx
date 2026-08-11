import { useEffect, useMemo } from 'react'
import { PackageSearch, TriangleAlert } from 'lucide-react'

import { useCollectionHealth, useItems, useTimeline } from '@/lib/queries'
import { deriveCollectionEmptyKind } from '@/lib/collectionEmptyState'
import { ApiError } from '@/lib/api'
import { sortByRole } from '@/features/_shared/roleGroup'
import { deriveCategories, filterByCategory, type Category } from '@/features/_shared/categories'
import { LatestPriceCard } from '@/features/_shared/LatestPriceCard'
import { useTimelineParams } from '@/features/timeline/useTimelineParams'
import { TimelineItemTree } from '@/features/timeline/TimelineItemTree'
import { RangeControls } from '@/features/timeline/RangeControls'
import { PriceTimelineChart } from '@/features/timeline/PriceTimelineChart'
import { aggregateDailyAverage } from '@/features/timeline/dailyBuckets'
import { EventMarkerLegend } from '@/features/timeline/EventMarkerLegend'
import { DownsampleBadge } from '@/features/timeline/DownsampleBadge'
import { ErrorState } from '@/components/state/ErrorState'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import type { TrackedItem } from '@/lib/schemas'

// last-30-days window as ISO instants — used by the 200-empty CTA to reset the range (D-03/D-09).
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000
function last30Days(): [string, string] {
  const now = Date.now()
  return [new Date(now - THIRTY_DAYS_MS).toISOString(), new Date(now).toISOString()]
}

/*
  어느 leaf에도 안 잡히는 물품을 담는 가상 카테고리(ItemPicker에서 계승). 지금은 비어 있지만, 시드가
  늘어 분류가 밀렸을 때 물품이 조용히 사라지는 대신 여기 뜬다(ICON-07 "누락 0" 보장 승계).
*/
const OTHER_CATEGORY_ID = 'other'

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
            선택한 품목을 찾을 수 없습니다. 왼쪽 목록에서 다른 품목을 선택해 주세요.
          </AlertDescription>
        </Alert>
      )
    }
    // network/other — inherited shared error copy ('백엔드에 연결하지 못했어요…').
    return <ErrorState onRetry={() => timeline.refetch()} />
  }

  const { snapshots, events, backfill } = timeline.data

  // Empty only when BOTH series are empty: a backfill-only window (server was off, no live snapshots)
  // still renders the continuous daily-average line — the whole point of the Phase 17.4 backfill (D-04).
  if (snapshots.length === 0 && backfill.length === 0) {
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
        backfill={backfill}
      />
    </div>
  )
}

/*
  TimelinePage — the headline timeline screen (TIME-01/05). quick-260811: 품목 선택기를 좌측 세로
  아코디언 트리로 바꿨다(사용자 피드백). 좌(lg) TimelineItemTree — 그룹→리프 카테고리 토글→펼치면
  품목 목록에서 직접 선택. 우 [기간] + [LatestPriceCard] + [차트](우측 품목 칩은 제거). 모바일은 1열 stack
  (트리 → 차트). 대시보드 CategoryNav와 taxonomy(_shared/categories)만 공유하고 컴포넌트는 분리한다.

  URL searchParams가 단일 출처(useTimelineParams, D-04). D-06 default selection: ?item= wins when present;
  otherwise the FIRST item(역할군 정렬)이 자동 선택돼 진입 시 빈 '품목을 선택하세요'가 없다. 트리의 펼침은
  선택 품목의 카테고리를 자동으로 따르므로(TimelineItemTree 참조) 진입·자동선택 후에도 열려 있다. 보석은
  제외(gemCount=0) — 시계열이 없어 그릴 수 없는 차트로 간다.
*/
export function TimelinePage() {
  const { itemId, from, to, setItem, setRange } = useTimelineParams()
  const { status, data } = useItems()

  const sorted = useMemo(() => (data ? sortByRole(data) : []), [data])

  // 카테고리 목록 = 대시보드와 동일한 leaf들 + (있다면) 기타. 보석 제외(gemCount=0).
  const categories = useMemo<Category[]>(() => {
    const leaves = deriveCategories(sorted, 0)
    const matched = new Set(leaves.flatMap((c) => filterByCategory(sorted, c.id).map((i) => i.id)))
    const others = sorted.filter((i) => !matched.has(i.id))
    return others.length > 0
      ? [...leaves, { id: OTHER_CATEGORY_ID, label: '기타', group: null, count: others.length }]
      : leaves
  }, [sorted])

  const itemsOf = useMemo(() => {
    const matched = new Set(
      deriveCategories(sorted, 0).flatMap((c) => filterByCategory(sorted, c.id).map((i) => i.id)),
    )
    return (categoryId: string): TrackedItem[] =>
      categoryId === OTHER_CATEGORY_ID
        ? sorted.filter((i) => !matched.has(i.id))
        : filterByCategory(sorted, categoryId)
  }, [sorted])

  // D-06: auto-select the first item(역할군 정렬) when the URL carries no ?item=. The guard makes this a
  // no-op once itemId is resolved (so a present ?item= always wins and there is no selection thrash).
  useEffect(() => {
    if (itemId == null && sorted.length > 0) {
      setItem(sorted[0].id)
    }
  }, [itemId, sorted, setItem])

  const selected = sorted.find((i) => i.id === itemId)
  const displayName = selected?.displayName ?? ''

  return (
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-[18rem_minmax(0,1fr)] lg:gap-12">
      {/* 좌(lg)/상단(모바일) — 품목 아코디언 트리. 대시보드와 동일한 sticky 흰 카드.
          top-20(80px)=TopNav 72px+여백8, self-start라야 셀이 행 높이로 늘지 않아 sticky가 동작한다. */}
      <div className="lg:sticky lg:top-20 lg:self-start">
        <div className="lg:bg-card lg:rounded-xl lg:border lg:px-6 lg:py-6 lg:shadow-sm">
          {/* 선택기 실패가 화면을 비우면 안 된다 — pending/error는 여기서 인라인 처리(ItemPicker 규칙 계승). */}
          {status === 'pending' ? (
            <div className="space-y-2" role="status" aria-busy="true">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-4/5" />
              <Skeleton className="h-8 w-3/5" />
            </div>
          ) : status === 'error' || categories.length === 0 ? (
            <p className="text-muted-foreground text-sm">품목을 불러오지 못했어요</p>
          ) : (
            <TimelineItemTree
              categories={categories}
              itemsOf={itemsOf}
              selectedItemId={itemId}
              onSelectItem={setItem}
            />
          )}
        </div>
      </div>

      {/* 우 — [기간] + [최신가] + [차트]. 품목 선택은 좌측 트리가 전담(우측 품목 칩 없음). */}
      <div className="flex min-w-0 flex-col gap-6">
        <div className="flex flex-wrap items-end gap-4">
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
    </div>
  )
}
