import { Link } from 'react-router-dom'

import { useCollectionHealth, useLatestPrice } from '@/lib/queries'
import { deriveCollectionEmptyKind } from '@/lib/collectionEmptyState'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import { formatKst } from '@/lib/formatKst'
import type { TrackedItem } from '@/lib/schemas'

/*
  ItemCard — one active item as one unified card (D-01). Identity (displayName) comes from
  the already-loaded useItems() list and is ALWAYS visible. The latest-price area runs its OWN
  useLatestPrice(item.id) — per-card fan-out (D-03): React Query parallelizes/caches/retries each
  request independently, so one item's latest failure never hides a sibling card or the health card
  (D-04, D-07, D-08). The price area maps its three async states INLINE (deliberately NOT via the
  shared <AsyncBoundary>): a 404/uncollected first-run reads as a small card-level collection-aware line,
  NOT the screen's big '다시 불러오기' ErrorState (08-CONTEXT line 39 — 최초 수집 전은 빈 상태에 가깝다).
  Read-only: no re-sort, no polling, no write surface.

  17-01 (D-04 Dashboard): the card-level uncollected state is no longer a fixed placeholder line.
  It shares the SAME useCollectionHealth() the health card reads (React Query dedupes the queryKey →
  zero extra network) and asks collectionEmptyState.ts whether the pipeline is alive: '최신가 수집 중'
  when the collector is running (SUCCESS/PARTIAL/…), '데이터 없음' only when NO_RUNS/실패. The 404 still
  reads as a small card-level empty (NOT the screen ErrorState) — inline mapping is deliberately kept.

  Phase 16 (CARD-01/CARD-02): the meaningless category code line is removed, and the whole <Card>
  is a react-router <Link> to /timeline?item={id} — a semantic <a> so Ctrl/middle-click new-tab,
  keyboard Enter, and right-click menu come for free (D-03). Only ?item= is passed; useTimelineParams
  reads it as the single source and the last-30-days default fills the range (D-04). accent blue-600
  stays reserved for the focus-visible ring; hover is elevation/border only (D-05).
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
      <Card className="cursor-pointer transition-shadow hover:border-foreground/20 hover:shadow-md">
        {/* Identity stays visible regardless of the price-area state (it is already loaded). */}
        <CardHeader>
          {/* [icon][name][role badge] inline (ICON-02·D-04) — icon/badge come from the already-loaded item. */}
          <CardTitle className="flex items-center gap-2 text-base">
            <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
            <span>{item.displayName}</span>
            <RoleBadge roleGroup={item.roleGroup} />
          </CardTitle>
        </CardHeader>

        <CardContent>
          {/* Per-card latest-price area — inline-mapped so a 404 stays a card-level empty, not the
              screen ErrorState. pending→skeleton, error/404→collection-aware '최신가 수집 중'(alive) or
              '데이터 없음'(NO_RUNS/실패), success→price+KST. */}
          {status === 'pending' && <Skeleton className="h-7 w-32" />}
          {status === 'error' && (
            <p className="text-muted-foreground text-sm">
              {emptyKind === 'collecting' ? '최신가 수집 중' : '데이터 없음'}
            </p>
          )}
          {status === 'success' && data && (
            <div className="space-y-1">
              <p className="text-base tabular-nums">{data.minPrice.toLocaleString('ko-KR')}</p>
              <p className="text-muted-foreground text-sm font-semibold">
                수집 시각 {formatKst(data.collectedAt)}
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </Link>
  )
}
