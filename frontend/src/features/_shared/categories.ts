import type { TrackedItem } from '@/lib/schemas'

/*
  Single source of truth (mirrors _shared/roleGroup.ts) for the app's category taxonomy
  (Phase 23, UX-01). Leaves group under 각인서 / 재료; each leaf is a filter predicate over the
  already-loaded item list. 각인서 leaves split by roleGroup (DEALER/SUPPORT); 재료 leaves split by
  the material itemGroup (재련재료/상급재련/재련보조/아크그리드젬). Items with roleGroup=null or an
  unknown itemGroup match no leaf — the same silent exclusion the old dashboard applied (curation
  has none). Pure functions only: no data fetching, no React.

  🔑 Lives in _shared because TWO screens now read it (quick-260723-jx1): the dashboard's
  CategoryNav and the timeline's ItemPicker. It used to sit in features/dashboard/, and importing
  it from features/timeline/ would have broken the layer rule this codebase already set for
  ItemSelect (D-07: 'extracted to _shared so the layer dependency never flows impact→timeline').

  ⚠️ 보석 leaf (GEM_CATEGORY_ID) is DASHBOARD-ONLY. Callers opt in by passing gemCount > 0; the
  timeline passes 0 because gems are not TrackedItems and have no time series, so a 보석 tab there
  would lead to a chart that cannot exist.
*/

export type CategoryGroup = '각인서' | '재료'

/*
  보석 leaf id (Phase 28, DASH-01). 보석은 CATEGORY_DEFS에 없다 — 그 목록은 TrackedItem 술어이고
  보석은 TrackedItem이 아니다(경매장 · Id 없음 · 다른 API · 시계열 미보유). match 술어를 억지로
  만들려면 TrackedItem 모양의 가짜 보석을 지어내야 한다. 그래서 보석은 별도 leaf로만 존재하고,
  "이 카테고리가 보석인가"는 DashboardPage가 이 id로 판정해 데이터 소스를 갈아끼운다.
*/
export const GEM_CATEGORY_ID = 'gems'

interface CategoryDef {
  id: string
  label: string
  group: CategoryGroup
  match: (item: TrackedItem) => boolean
}

/*
  재료 leaf 라벨 = item_group 값 그대로(WatchlistSeeder 단일 출처). 순서 = 스펙업 진행 순서.
  강화재료는 폐지됐다(quick-260715) — 융화재료는 재련에 반드시 들어가므로 재련재료에 통합했다.
*/
const MATERIAL_GROUPS = ['재련재료', '상급재련', '재련보조', '아크그리드젬'] as const

// Canonical display order: 각인서(딜러→서포터) → 재료(재련→상급재련→재련보조→아크그리드젬).
const CATEGORY_DEFS: readonly CategoryDef[] = [
  { id: 'dealer', label: '딜러 각인', group: '각인서', match: (i) => i.roleGroup === 'DEALER' },
  { id: 'support', label: '서포터 각인', group: '각인서', match: (i) => i.roleGroup === 'SUPPORT' },
  ...MATERIAL_GROUPS.map(
    (g): CategoryDef => ({ id: `mat-${g}`, label: g, group: '재료', match: (i) => i.itemGroup === g }),
  ),
]

/**
 * A derived category: its definition plus the count of matching items in the current data.
 * {@link Category.group} is null for a standalone leaf that belongs under no group header (보석).
 */
export interface Category {
  id: string
  label: string
  group: CategoryGroup | null
  count: number
}

/*
  Non-empty leaves in canonical order, each with its item count. Empty leaves are dropped so a
  not-yet-populated bucket never renders a dead tab; a group whose leaves are all empty therefore
  disappears too (the nav only ever reads this list).

  보석 (Phase 28) appends LAST as a standalone leaf (group: null) — 사용자 결정 2026-07-15: no level
  sub-categories, all six render at once. It follows the same drop-when-empty rule: gems that failed to
  load or returned nothing produce no category at all, rather than a tab leading to an empty panel.
*/
export function deriveCategories(items: readonly TrackedItem[], gemCount = 0): Category[] {
  const leaves: Category[] = CATEGORY_DEFS.map(({ id, label, group, match }) => ({
    id,
    label,
    group,
    count: items.filter(match).length,
  })).filter((c) => c.count > 0)

  if (gemCount > 0) {
    leaves.push({ id: GEM_CATEGORY_ID, label: '보석', group: null, count: gemCount })
  }
  return leaves
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

/*
  Groups whose members share ONE icon, so a real game icon can stand for the whole group. Only 각인서
  qualifies: all 18 relic engraving recipes carry the same use_9_25.png (a grade glyph, not a
  per-engraving art) — which is exactly why it reads as "the 각인서 icon". Every 재료 group is the
  opposite: each material has its own distinct icon, so no member could represent the others, and 재료
  wears a drawn glyph (CategoryNav's Hammer) instead.
*/
const ICONIC_GROUPS: readonly CategoryGroup[] = ['각인서']

/**
 * The game icon standing for a group's header, or null when no member can represent the group.
 * Derived from the loaded data (the first member's iconUrl) rather than a hardcoded CDN URL, so it
 * follows the seed automatically if the game's icon ever changes.
 */
export function groupIconUrl(
  items: readonly TrackedItem[],
  group: CategoryGroup,
): string | null {
  if (!ICONIC_GROUPS.includes(group)) return null
  const defs = CATEGORY_DEFS.filter((d) => d.group === group)
  return items.find((i) => defs.some((d) => d.match(i)))?.iconUrl ?? null
}
