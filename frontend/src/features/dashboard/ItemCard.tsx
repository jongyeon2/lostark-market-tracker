import { Link } from 'react-router-dom'

import { useCollectionHealth, useLatestPrice } from '@/lib/queries'
import { deriveCollectionEmptyKind } from '@/lib/collectionEmptyState'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import { formatKst } from '@/lib/formatKst'
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

  Phase 16 (CARD-01/CARD-02): the meaningless category code line is removed (do NOT reintroduce), and the
  whole <Card> is a react-router <Link> to /timeline?item={id} — a semantic <a> so Ctrl/middle-click
  new-tab, keyboard Enter, and right-click menu come for free. Only ?item= is passed; useTimelineParams
  reads it as the single source and the last-30-days default fills the range. accent blue-600 stays
  reserved for the focus-visible ring; hover is elevation/border only.
  Read-only: no re-sort, no polling, no write surface.
*/
export function ItemCard({ item }: { item: TrackedItem }) {
  const { status, data } = useLatestPrice(item.id)
  // Shared health hook (queryKey ['collection-health']) — dedupe means no extra request per card.
  const { data: health } = useCollectionHealth()
  const emptyKind = deriveCollectionEmptyKind(health)

  return (
    <Link
      to={`/timeline?item=${item.id}`}
      className="group block rounded-xl focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none"
    >
      <Card className="cursor-pointer py-3 transition-shadow hover:border-foreground/20 hover:shadow-md">
        {/* D-12: compact horizontal row — identity left, latest-price right (py-3, gold 🪙). */}
        <CardContent className="flex items-center justify-between gap-4">
          {/* Identity (left) — always visible, from the already-loaded item. */}
          <div className="flex min-w-0 items-center gap-2">
            <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
            <span className="truncate font-medium">{item.displayName}</span>
            <RoleBadge roleGroup={item.roleGroup} />
          </div>

          {/* Per-card latest-price area (right) — inline-mapped so a 404 stays a card-level empty, not the
              screen ErrorState. pending→skeleton, error/404→collection-aware '최저가 수집 중'(alive) or
              '데이터 없음'(NO_RUNS/실패), success→price+KST. */}
          <div className="shrink-0 text-right">
            {status === 'pending' && <Skeleton className="ml-auto h-6 w-28" />}
            {status === 'error' && (
              <p className="text-muted-foreground text-sm">
                {emptyKind === 'collecting' ? '최저가 수집 중' : '데이터 없음'}
              </p>
            )}
            {status === 'success' && data && (
              <div className="space-y-0.5">
                <p className="flex items-center justify-end gap-1 text-base font-medium tabular-nums">
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
        </CardContent>
      </Card>
    </Link>
  )
}
