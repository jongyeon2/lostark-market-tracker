import { useEffect, useMemo } from 'react'
import { PackageSearch, TriangleAlert } from 'lucide-react'

import { useCollectionHealth, useItems, useTimeline } from '@/lib/queries'
import { deriveCollectionEmptyKind } from '@/lib/collectionEmptyState'
import { ApiError } from '@/lib/api'
import { CategoryNav } from '@/features/_shared/CategoryNav'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { sortByRole } from '@/features/_shared/roleGroup'
import { deriveCategories, filterByCategory, type Category } from '@/features/_shared/categories'
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
import { cn } from '@/lib/utils'
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
  TimelinePage — the headline timeline screen (TIME-01/05). quick-260811: 품목 선택기를 대시보드와 같은
  좌측 세로 카테고리 레일로 바꿨다. 좌(lg) CategoryNav(_shared, 대시보드와 공유) — 카테고리 선택,
  우 [선택 카테고리 품목 칩] + [기간] + [LatestPriceCard] + [차트]. 모바일은 1열 stack(CategoryNav가
  가로 칩으로 자동 축약 → 품목 칩 → 차트).

  URL searchParams가 단일 출처(useTimelineParams, D-04). 🔑 로컬 카테고리 state가 없다 — 활성 카테고리는
  "선택된 품목이 속한 카테고리"로 매 렌더 유도하고(대시보드 selectedId 유도·이전 ItemPicker와 같은 수법),
  카테고리를 누르면 그 카테고리의 첫 품목을 즉시 setItem하므로 둘이 어긋난 상태가 존재할 수 없다.

  D-06 default selection: ?item= wins when present; otherwise the FIRST item(역할군 정렬)이 자동 선택돼
  진입 시 빈 '품목을 선택하세요'가 없다. 보석은 제외(gemCount=0) — 시계열이 없어 그릴 수 없는 차트로 간다.
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

  // 활성 카테고리 = 선택된 품목이 속한 곳. 못 찾으면 첫 카테고리(진입 직후 itemId가 아직 null일 때).
  const activeCategory =
    categories.find((c) => itemsOf(c.id).some((i) => i.id === itemId)) ?? categories[0]
  const visibleItems = activeCategory ? itemsOf(activeCategory.id) : []

  // 카테고리를 누르면 그 카테고리의 첫 품목으로 즉시 이동한다(빈 화면 금지, D-06). 정확한 품목은 우측
  // 칩에서 한 번 더 누르면 된다.
  function pickCategory(categoryId: string) {
    const first = itemsOf(categoryId)[0]
    if (first) setItem(first.id)
  }

  return (
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-[18rem_minmax(0,1fr)] lg:gap-12">
      {/* 좌(lg)/상단(모바일) — 카테고리 레일. 대시보드와 동일한 sticky 흰 카드 + CategoryNav.
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
            <CategoryNav
              categories={categories}
              items={sorted}
              selectedId={activeCategory?.id ?? null}
              onSelect={pickCategory}
            />
          )}
        </div>
      </div>

      {/* 우 — [선택 카테고리 품목 칩] + [기간] + [최신가] + [차트]. */}
      <div className="flex min-w-0 flex-col gap-6">
        {/* 품목 칩 — 선택된 카테고리의 품목들. 활성 칩에 선택 품목명이 항상 떠 있다. */}
        {visibleItems.length > 0 && (
          <div className="flex flex-wrap gap-2" role="group" aria-label="품목 선택">
            {visibleItems.map((item) => (
              <Chip key={item.id} active={item.id === itemId} onClick={() => setItem(item.id)}>
                {/* aria-hidden — 바로 옆 이름이 이미 무엇인지 말한다(CategoryNav와 같은 규칙). */}
                <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
                {item.displayName}
              </Chip>
            ))}
          </div>
        )}

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

/*
  칩 하나(ItemPicker에서 계승). 대시보드 CategoryNav의 모바일 Chip과 같은 모양 — 두 화면이 같은 분류를
  같은 생김새로 보여줘야 학습이 한 번으로 끝난다. 활성 상태는 색만으로 표시하지 않는다(D-01):
  배경 + 굵기 + aria-current가 함께 간다.
*/
function Chip({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      aria-current={active ? 'true' : undefined}
      onClick={onClick}
      className={cn(
        'flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm whitespace-nowrap transition-colors',
        'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
        active
          ? 'bg-primary text-primary-foreground font-semibold'
          : 'bg-muted text-foreground hover:bg-muted/70 font-medium',
      )}
    >
      {children}
    </button>
  )
}
