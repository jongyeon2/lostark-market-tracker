import { useMemo, useState } from 'react'

import { useItems } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { sortByRole } from '@/features/_shared/roleGroup'

import { CategoryNav } from './CategoryNav'
import { deriveCategories, filterByCategory, firstCategoryId } from './categories'
import { ItemCard } from './ItemCard'
import { NewsPanel } from './NewsPanel'

/*
  DashboardPage — the client dashboard, a maplanet-style 3-column layout (Phase 23, UX-01/UX-02):
  좌 CategoryNav(카테고리 필터) / 중앙 선택 카테고리 물품 / 우 NewsPanel. useItems() is client-sorted
  by role via sortByRole (backend stays 0-line); deriveCategories groups the sorted items into
  non-empty leaves (각인서 딜러/서포터 + 재료 4 itemGroup — categories.ts is the single source). Picking a
  leaf filters the center list; selection is local useState only (A4 — YAGNI, no URL/router state).

  The center item list and the news panel each keep their OWN AsyncBoundary so one side failing never
  blanks the other (D-07, preserved from the 2-column version). ItemCard·NewsPanel are UNCHANGED.
  Mobile (< lg) collapses to one column in source order: 칩 탭 → 물품 → 소식. Read-only: no chart,
  no write UI, no polling.
*/
export function DashboardPage() {
  const { status, data, refetch } = useItems()

  const sorted = useMemo(() => (data ? sortByRole(data) : []), [data])
  const categories = useMemo(() => deriveCategories(sorted), [sorted])

  // Local selection; default/fallback = first non-empty leaf. Deriving the EFFECTIVE id each render
  // (rather than syncing with useEffect) self-heals when a data change removes the picked category —
  // no stale or empty selection can persist.
  const [picked, setPicked] = useState<string | null>(null)
  const selectedId =
    picked && categories.some((c) => c.id === picked) ? picked : firstCategoryId(categories)

  const visible = selectedId ? filterByCategory(sorted, selectedId) : []

  return (
    <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 lg:grid-cols-[11rem_minmax(0,1fr)_20rem]">
      {/* 좌(lg) / 상단(모바일) — 카테고리 필터. 로딩 중엔 leaf가 없어 자연 축소. */}
      <CategoryNav
        categories={categories}
        items={sorted}
        selectedId={selectedId}
        onSelect={setPicked}
      />

      {/* 중앙 — 선택 카테고리 물품. 기존 물품 AsyncBoundary 유지(pending/error/0건 판정은 원본 data 길이). */}
      <AsyncBoundary status={status} isEmpty={(data?.length ?? 0) === 0} onRetry={() => refetch()}>
        <div className="space-y-3">
          {visible.map((item) => (
            <ItemCard key={item.id} item={item} />
          ))}
        </div>
      </AsyncBoundary>

      {/* 우(lg) / 하단(모바일) — 로아 소식. 물품과 독립 boundary(한쪽 실패가 다른 쪽 안 가림, D-07). */}
      <NewsPanel />
    </div>
  )
}
