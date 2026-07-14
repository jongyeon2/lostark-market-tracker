import type { TrackedItem } from '@/lib/schemas'

/*
  Single source of truth (mirrors _shared/roleGroup.ts) for the dashboard's category taxonomy
  (Phase 23, UX-01). The left CategoryNav groups leaves under 각인 / 재료; each leaf is a filter
  predicate over the already-loaded item list. 각인 leaves split by roleGroup (DEALER/SUPPORT);
  재료 leaves split by the material itemGroup (강화재료/재련재료/상급재련/재련보조/아크그리드젬). Items
  with roleGroup=null or an unknown itemGroup match no leaf — the same silent exclusion the old
  dashboard applied (curation has none). Pure functions only: no data fetching, no React.
*/

export type CategoryGroup = '각인' | '재료'

interface CategoryDef {
  id: string
  label: string
  group: CategoryGroup
  match: (item: TrackedItem) => boolean
}

/** 재료 leaf 라벨 = item_group 값 그대로(WatchlistSeeder 단일 출처). 순서 = 스펙업 진행 순서. */
const MATERIAL_GROUPS = ['강화재료', '재련재료', '상급재련', '재련보조', '아크그리드젬'] as const

// Canonical display order: 각인(딜러→서포터) → 재료(강화→재련→상급재련→재련보조→아크그리드젬).
const CATEGORY_DEFS: readonly CategoryDef[] = [
  { id: 'dealer', label: '딜러 각인', group: '각인', match: (i) => i.roleGroup === 'DEALER' },
  { id: 'support', label: '서포터 각인', group: '각인', match: (i) => i.roleGroup === 'SUPPORT' },
  ...MATERIAL_GROUPS.map(
    (g): CategoryDef => ({ id: `mat-${g}`, label: g, group: '재료', match: (i) => i.itemGroup === g }),
  ),
]

/** A derived category: its definition plus the count of matching items in the current data. */
export interface Category {
  id: string
  label: string
  group: CategoryGroup
  count: number
}

/*
  Non-empty leaves in canonical order, each with its item count. Empty leaves are dropped so a
  not-yet-populated bucket never renders a dead tab; a group whose leaves are all empty therefore
  disappears too (the nav only ever reads this list).
*/
export function deriveCategories(items: readonly TrackedItem[]): Category[] {
  return CATEGORY_DEFS.map(({ id, label, group, match }) => ({
    id,
    label,
    group,
    count: items.filter(match).length,
  })).filter((c) => c.count > 0)
}

/** Items belonging to the selected leaf (empty array when the id is unknown). */
export function filterByCategory(items: readonly TrackedItem[], id: string): TrackedItem[] {
  const def = CATEGORY_DEFS.find((d) => d.id === id)
  return def ? items.filter(def.match) : []
}

/** First non-empty leaf id — the default/fallback selection; null when there is no data yet. */
export function firstCategoryId(categories: readonly Category[]): string | null {
  return categories[0]?.id ?? null
}
