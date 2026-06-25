import { useLatestPrice } from '@/lib/queries'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatKst } from '@/lib/formatKst'
import type { TrackedItem } from '@/lib/schemas'

/*
  ItemCard — one active item as one unified card (D-01). Identity (displayName/category) comes from
  the already-loaded useItems() list and is ALWAYS visible. The latest-price area runs its OWN
  useLatestPrice(item.id) — per-card fan-out (D-03): React Query parallelizes/caches/retries each
  request independently, so one item's latest failure never hides a sibling card or the health card
  (D-04, D-07, D-08). The price area maps its three async states INLINE (deliberately NOT via the
  shared <AsyncBoundary>): a 404/uncollected first-run reads as a small card-level '최신가 아직 없음',
  NOT the screen's big '다시 불러오기' ErrorState (08-CONTEXT line 39 — 최초 수집 전은 빈 상태에 가깝다).
  Read-only: no re-sort, no polling, no write surface.
*/
export function ItemCard({ item }: { item: TrackedItem }) {
  const { status, data } = useLatestPrice(item.id)

  return (
    <Card>
      {/* Identity stays visible regardless of the price-area state (it is already loaded). */}
      <CardHeader>
        <CardTitle className="text-xl">{item.displayName}</CardTitle>
        <p className="text-muted-foreground text-sm font-semibold">{item.category}</p>
      </CardHeader>

      <CardContent>
        {/* Per-card latest-price area — inline-mapped so a 404 stays a card-level empty, not the
            screen ErrorState. pending→skeleton, error/404→'최신가 아직 없음', success→price+KST. */}
        {status === 'pending' && <Skeleton className="h-7 w-32" />}
        {status === 'error' && <p className="text-muted-foreground text-sm">최신가 아직 없음</p>}
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
  )
}
