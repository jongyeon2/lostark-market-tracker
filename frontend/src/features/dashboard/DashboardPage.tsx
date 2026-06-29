import { useItems } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { sortByRole } from '@/features/_shared/roleGroup'

import { HealthCard } from './HealthCard'
import { ItemCard } from './ItemCard'

/*
  DashboardPage — the Phase-8 dashboard as one top-to-bottom scroll realizing the D-01 hierarchy
  'health(파이프라인 살아있음) → 무엇을 추적 → 지금 얼마': the full-width HealthCard (08-02) on top, then the
  active-item card grid from useItems() below it (separated by lg/24px). Replaces the Phase-7
  temporary placeholder list. The grid wraps its OWN AsyncBoundary (pending/error/0-items) INDEPENDENTLY
  of the HealthCard's boundary (D-07/D-08), so a grid error or empty never hides the health card and
  vice-versa. D-07: the grid is client-sorted by role group (DEALER→SUPPORT→MATERIAL→null)→name via
  sortByRole — the backend stays 0-line. Each card fans out its own latest request (D-03/D-04).
  Read-only: no item selector, chart, polling, or write UI.
*/
export function DashboardPage() {
  const { status, data, refetch } = useItems()

  // D-07: role-group then name, on a NEW array (sortByRole is non-mutating). isEmpty still reads the
  // original data?.length so an empty list is judged before sorting.
  const sorted = data ? sortByRole(data) : []

  return (
    <div className="space-y-6">
      <h1 className="text-[28px] leading-tight font-semibold">대시보드</h1>

      {/* ① health (파이프라인 살아있음) — full-width, its own boundary. */}
      <HealthCard />

      {/* ② 무엇을 추적 + ③ 지금 얼마 — responsive item-card grid, its own boundary (independent of health). */}
      <AsyncBoundary status={status} isEmpty={(data?.length ?? 0) === 0} onRetry={() => refetch()}>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {sorted.map((item) => (
            <ItemCard key={item.id} item={item} />
          ))}
        </div>
      </AsyncBoundary>
    </div>
  )
}
