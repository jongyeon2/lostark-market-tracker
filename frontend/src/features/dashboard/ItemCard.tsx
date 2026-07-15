import { Link } from 'react-router-dom'
import { LineChart } from 'lucide-react'

import { useCollectionHealth, useLatestPrice } from '@/lib/queries'
import { deriveCollectionEmptyKind } from '@/lib/collectionEmptyState'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import { formatKst } from '@/lib/formatKst'
import { cn } from '@/lib/utils'
import type { TrackedItem } from '@/lib/schemas'

/*
  ItemCard — one active item as one full-width horizontal row (17.1 D-12): [ItemIcon][이름][RoleBadge]
  on the left, [minPrice 골드][수집 시각] on the right. Identity (displayName) comes from the already-loaded
  useItems() list and is ALWAYS visible. The latest-price area runs its OWN useLatestPrice(item.id) —
  per-card fan-out (D-03): React Query parallelizes/caches/retries each request independently, so one
  item's latest failure never hides a sibling row or the admin health card (D-04, D-07, D-08). The price
  area maps its three async states INLINE (deliberately NOT via the shared <AsyncBoundary>): a
  404/uncollected first-run reads as a small card-level line, NOT the screen's big '다시 불러오기'
  ErrorState (최초 수집 전은 빈 상태에 가깝다).

  The card-level uncollected line shares the SAME useCollectionHealth() the admin health card reads
  (React Query dedupes the queryKey → zero extra network) and asks collectionEmptyState.ts whether the
  pipeline is alive: '최저가 수집 중' when the collector is running (SUCCESS/PARTIAL/…), '데이터 없음' only
  when NO_RUNS/실패.

  Phase 16 (CARD-01/CARD-02): the meaningless category code line is removed (do NOT reintroduce).

  🔑 Phase 28 (DASH-02/DASH-03): the card is no longer ONE big <Link>. Clicking it now SELECTS the item
  so the dashboard shows that item's event-impact below, and the timeline moved to a small 차트 link.
  They are SIBLINGS, not nested: a <Link> inside a <button> (or vice-versa) is invalid HTML — browsers
  recover unpredictably, the keyboard tab order breaks, and a screen reader hears one control where
  there are two. Both stay native elements, so Ctrl/middle-click new-tab, Enter, and the right-click
  menu keep working on 차트 for free.

  Selection is never color-alone (D-01): border + background + aria-pressed carry it too. accent
  blue-600 stays reserved for the focus-visible ring.
  Read-only: no re-sort, no polling, no write surface.
*/
export function ItemCard({
  item,
  selected,
  onSelect,
}: {
  item: TrackedItem
  selected: boolean
  onSelect: (id: number) => void
}) {
  const { status, data } = useLatestPrice(item.id)
  // Shared health hook (queryKey ['collection-health']) — dedupe means no extra request per card.
  const { data: health } = useCollectionHealth()
  const emptyKind = deriveCollectionEmptyKind(health)

  return (
    <Card
      className={cn(
        'py-0 transition-colors',
        selected ? 'border-primary bg-muted' : 'hover:border-foreground/20',
      )}
    >
      <div className="flex items-stretch">
        {/* 선택 버튼 — 카드 본문 전체. 누르면 아래에 이 물품의 이벤트 영향이 뜬다. */}
        <button
          type="button"
          aria-pressed={selected}
          onClick={() => onSelect(item.id)}
          className="focus-visible:ring-ring flex min-w-0 flex-1 cursor-pointer flex-col items-start gap-1 rounded-l-xl px-4 py-3 text-left focus-visible:ring-2 focus-visible:outline-none sm:flex-row sm:items-center sm:justify-between sm:gap-4 sm:px-6"
        >
          {/* Identity (left) — always visible, from the already-loaded item.
              min-w-0 + flex-1 is load-bearing, not cosmetic: without flex-1 this box shrinks to width 0
              on a narrow card (the price block is shrink-0 and claims ~163px of a ~177px content box),
              and its icon/badge then OVERFLOW the zero-width box and paint on top of the price. The name
              truncates instead of pushing anything. Measured at 375px, 2026-07-15. */}
          <div className="flex w-full min-w-0 flex-1 items-center gap-2">
            <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
            <span className="min-w-0 truncate font-medium">{item.displayName}</span>
            <RoleBadge roleGroup={item.roleGroup} />
          </div>

          {/* Per-card latest-price area (right) — inline-mapped so a 404 stays a card-level empty, not the
              screen ErrorState. pending→skeleton, error/404→collection-aware '최저가 수집 중'(alive) or
              '데이터 없음'(NO_RUNS/실패), success→price+KST. */}
          <div className="shrink-0 text-left sm:text-right">
            {status === 'pending' && <Skeleton className="h-6 w-28 sm:ml-auto" />}
            {status === 'error' && (
              <p className="text-muted-foreground text-sm">
                {emptyKind === 'collecting' ? '최저가 수집 중' : '데이터 없음'}
              </p>
            )}
            {status === 'success' && data && (
              <div className="space-y-0.5">
                <p className="flex items-center gap-1 text-base font-medium tabular-nums sm:justify-end">
                  <span className="text-muted-foreground text-xs font-normal">최저가</span>
                  <span aria-hidden="true">🪙</span>
                  {data.minPrice.toLocaleString('ko-KR')}
                </p>
                <p className="text-muted-foreground text-xs font-semibold">
                  수집 시각 {formatKst(data.collectedAt)}
                </p>
              </div>
            )}
          </div>
        </button>

        {/* 차트 링크 — 버튼의 형제(중첩 금지). 여전히 semantic <a>라 새 탭·키보드가 공짜. */}
        <Link
          to={`/timeline?item=${item.id}`}
          aria-label={`${item.displayName} 가격 차트 보기`}
          className="border-border text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:ring-ring flex shrink-0 items-center gap-1 rounded-r-xl border-l px-3 text-xs font-semibold transition-colors focus-visible:ring-2 focus-visible:outline-none"
        >
          <LineChart className="size-4" aria-hidden="true" />
          차트
        </Link>
      </div>
    </Card>
  )
}
