import { useLatestPrice } from '@/lib/queries'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import type { RoleGroup } from '@/features/_shared/roleGroup'
import { formatKst } from '@/lib/formatKst'

// Timeline-specific lightweight latest-price card (D-07 — NOT the Dashboard ItemCard, which is a
// grid-unified card of different shape). Runs its OWN useLatestPrice + its OWN <AsyncBoundary>
// (Phase-7 D-08 / Phase-8 D-07 isolation) so a latest-price failure never hides the chart and
// vice-versa. Shared by Phase 9 & 10. Read-only: no polling, no write surface.
export function LatestPriceCard({
  itemId,
  displayName,
  iconUrl,
  roleGroup,
}: {
  itemId: number
  displayName: string
  // Optional so existing callers compile; undefined is treated as null (no icon → fallback glyph).
  iconUrl?: string | null
  roleGroup?: RoleGroup | null
}) {
  const { status, data, refetch } = useLatestPrice(itemId)

  return (
    <Card className="w-fit min-w-56">
      <CardHeader>
        {/* Identity (icon/name/role badge) stays visible regardless of the price-area state — the
            selected item's enrichment is passed in, already loaded (ICON-04·D-04). */}
        <CardTitle className="flex items-center gap-2 text-xl">
          <ItemIcon iconUrl={iconUrl ?? null} roleGroup={roleGroup ?? null} size="md" />
          <span>{displayName}</span>
          <RoleBadge roleGroup={roleGroup ?? null} />
        </CardTitle>
      </CardHeader>
      <CardContent>
        <AsyncBoundary status={status} onRetry={() => refetch()}>
          {data && (
            <div className="flex flex-col gap-2">
              <p className="text-xl font-semibold tabular-nums">
                {data.minPrice.toLocaleString('ko-KR')} G
              </p>
              <p className="text-muted-foreground text-sm font-semibold">
                {formatKst(data.collectedAt)} KST
              </p>
            </div>
          )}
        </AsyncBoundary>
      </CardContent>
    </Card>
  )
}
