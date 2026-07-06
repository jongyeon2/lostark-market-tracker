import { useItems } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { sortByRole } from '@/features/_shared/roleGroup'
import type { TrackedItem } from '@/lib/schemas'

import { ItemCard } from './ItemCard'

/*
  DashboardPage — the client dashboard focused on '무엇을 추적 → 지금 얼마'. Collection health is an
  operator concern and now lives only in the admin console (17.1 D-07), so the dashboard no longer
  renders it. useItems() is client-sorted by role group (DEALER→SUPPORT→MATERIAL→null)→name via
  sortByRole (backend stays 0-line), then split into two vertical sections — 각인(DEALER+SUPPORT) →
  재료(MATERIAL) (D-10). Each card is a full-width horizontal row (D-12). The list wraps its OWN
  AsyncBoundary (pending/error/0-items); no separate honesty widget is added (D-09 — per-card pending +
  boundary suffice). Read-only: no item selector, chart, polling, or write UI.
*/
export function DashboardPage() {
  const { status, data, refetch } = useItems()

  // D-11: sortByRole already orders DEALER→SUPPORT→MATERIAL→null, then name(ko-KR); we reuse it and
  // split by role group. 각인 = DEALER+SUPPORT (딜러→서포터→이름 order preserved), 재료 = MATERIAL.
  // isEmpty still reads the ORIGINAL data length so an empty list is judged before splitting.
  const sorted = data ? sortByRole(data) : []
  const engravings = sorted.filter((i) => i.roleGroup === 'DEALER' || i.roleGroup === 'SUPPORT')
  const materials = sorted.filter((i) => i.roleGroup === 'MATERIAL')

  return (
    <div className="space-y-6">
      <h1 className="text-[28px] leading-tight font-semibold">대시보드</h1>

      <AsyncBoundary status={status} isEmpty={(data?.length ?? 0) === 0} onRetry={() => refetch()}>
        {/* max-w-3xl keeps each row a comfortable reading width so 이름(좌)↔가격(우) stay close. */}
        <div className="max-w-3xl space-y-8">
          {/* 섹션 순서: 각인 → 재료 (D-10). RoleBadge on each card distinguishes 딜러/서포터 (D-11). */}
          <ItemSection title="각인" items={engravings} />
          <ItemSection title="재료" items={materials} />
        </div>
      </AsyncBoundary>
    </div>
  )
}

/*
  One category section: a lightweight heading (literal '각인'/'재료' — NOT ROLE_LABEL, since 각인 is the
  DEALER+SUPPORT union, D-11) over a vertical stack of full-width rows. Empty categories render nothing
  so a not-yet-populated bucket never shows a bare header.
*/
function ItemSection({ title, items }: { title: string; items: TrackedItem[] }) {
  if (items.length === 0) return null

  return (
    <section className="space-y-3">
      <h2 className="text-muted-foreground text-sm font-semibold tracking-wide">{title}</h2>
      <div className="space-y-3">
        {items.map((item) => (
          <ItemCard key={item.id} item={item} />
        ))}
      </div>
    </section>
  )
}
