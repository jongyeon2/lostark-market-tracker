import { useLatestPrice } from '@/lib/queries'
import { Card, CardContent } from '@/components/ui/card'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import type { RoleGroup } from '@/features/_shared/roleGroup'
import { formatKst } from '@/lib/formatKst'

// Timeline-specific lightweight latest-price card (D-07 — NOT the Dashboard ItemCard, which is a
// grid-unified card of different shape). Runs its OWN useLatestPrice + its OWN <AsyncBoundary>
// (Phase-7 D-08 / Phase-8 D-07 isolation) so a latest-price failure never hides the chart and
// vice-versa. Shared by Phase 9 & 10. Read-only: no polling, no write surface.
//
// Horizontal single-row layout (quick 260630-gct): identity [icon · name · badge] → 골드 → 시간 as
// ONE flex row. The success children are a fragment (no wrapper) and AsyncBoundary returns
// `<>{children}</>`, so 골드/시간 land as DIRECT flex children alongside the identity block — every
// gap is the same (uniform gap-6). w-fit keeps the card snug to its content (CardContent has no
// @container, so the intrinsic width propagates freely — unlike the old CardHeader @container).
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
    <Card className="w-fit">
      <CardContent className="flex items-center gap-6">
        {/* Identity (icon/name/role badge) stays visible regardless of the price-area state — the
            selected item's enrichment is passed in, already loaded (ICON-04·D-04). */}
        <div className="flex items-center gap-2">
          <ItemIcon iconUrl={iconUrl ?? null} roleGroup={roleGroup ?? null} size="md" />
          <span className="text-base font-semibold whitespace-nowrap">{displayName}</span>
          <RoleBadge roleGroup={roleGroup ?? null} />
        </div>
        {/* 골드 · 시간 — fragment children land as direct flex siblings, so their gap equals the
            identity↔price gap (one uniform gap-6 across the whole row). */}
        <AsyncBoundary status={status} onRetry={() => refetch()}>
          {data && (
            <>
              <p className="text-xl font-semibold tabular-nums whitespace-nowrap">
                {data.minPrice.toLocaleString('ko-KR')} G
              </p>
              <p className="text-muted-foreground text-sm font-semibold whitespace-nowrap">
                {formatKst(data.collectedAt)} KST
              </p>
            </>
          )}
        </AsyncBoundary>
      </CardContent>
    </Card>
  )
}
