import { Gem, Hammer } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { cn } from '@/lib/utils'
import type { TrackedItem } from '@/lib/schemas'

import {
  groupIconUrl,
  GEM_CATEGORY_ID,
  type Category,
  type CategoryGroup,
} from '@/features/_shared/categories'

/*
  CategoryNav — the shared category rail (Phase 23, UX-01/UX-02; quick-260811 moved dashboard→_shared
  so the timeline reuses it, the categories.ts precedent). Stateless: it renders the derived categories
  and reports selection via onSelect; the caller (DashboardPage / TimelinePage) owns the selected state. One
  component, two responsive faces — a grouped vertical nav on lg+, a horizontal scrollable chip row
  below lg (same leaves, same order). Active state is never color-alone: it pairs the reserved
  primary accent with background + weight + aria-current so identity survives without color.
  Reuses existing tokens only (primary/muted/ring) — no new colors or spacing.

  Each desktop group header carries a rule directly beneath it (quick-260715): without one, the group
  label and its leaves read as siblings in the same flat list rather than a heading over its members.

  Both groups wear an icon, from whichever source can honestly supply one: 각인서 uses its real game
  icon (groupIconUrl — all 18 books share it), while 재료 has no such shared icon (every material's art
  differs) and wears a drawn Hammer glyph instead. GROUP_GLYPH holds that fallback.

  STANDALONE leaves (group: null) render after the groups with no header of their own (Phase 28) —
  보석 is the only one. It has no sub-division to head (사용자 결정: no level sub-categories, all six at
  once), and a header repeating its single child's name ("보석 > 보석") would be noise. The existing
  space-y-4 between blocks already separates it. The mobile chip row needs ZERO change: it always
  ignored groups and rendered leaves flat, so the gem chip joins automatically.

  Because it has no group header, the desktop 보석 leaf would sit icon-less beside the icon-bearing
  group headers (각인서/재료) — so it wears a Gem glyph ON THE LEAF itself, the same fallback stance as
  재료's Hammer (a drawn glyph where no shared game icon exists). Desktop only, matching the group
  headers whose icons are also lg-only; the mobile chips stay icon-less across the board.
*/

const GROUP_ORDER: readonly CategoryGroup[] = ['각인서', '재료']

/** Drawn fallback for a group with no representative game icon — the ItemIcon precedent (Phase 14). */
const GROUP_GLYPH: Partial<Record<CategoryGroup, typeof Hammer>> = { 재료: Hammer }

export function CategoryNav({
  categories,
  items,
  selectedId,
  onSelect,
}: {
  categories: Category[]
  /** Loaded items — the source the group header icon is derived from (never re-filtered here). */
  items: readonly TrackedItem[]
  selectedId: string | null
  onSelect: (id: string) => void
}) {
  return (
    // sticky 고정은 이제 상위(DashboardPage 좌측 레일 래퍼)가 소유한다 — 필터와 공지 레일을 한
    // sticky 블록으로 묶어야 본문 스크롤 시 공지가 고정된 필터 "뒤로" 겹치지 않기 때문. 여기선 순수
    // 콘텐츠만 렌더한다(top-20 산정 근거는 DashboardPage 래퍼 주석 참조).
    <nav aria-label="카테고리">
      {/* 데스크톱(lg+) — 2단계 그룹 세로 nav + 그룹 없는 단독 leaf. */}
      <div className="hidden lg:block">
        <div className="space-y-4">
          {GROUP_ORDER.map((group) => {
            const leaves = categories.filter((c) => c.group === group)
            if (leaves.length === 0) return null
            const iconUrl = groupIconUrl(items, group)
            const Glyph = GROUP_GLYPH[group]
            return (
              <div key={group} className="space-y-1">
                <h2 className="border-border text-muted-foreground flex items-center gap-1.5 border-b px-2 pb-1.5 text-sm font-semibold tracking-wide">
                  {group}
                  {/* alt=""/aria-hidden — the group label right beside it already names the icon. */}
                  {iconUrl ? (
                    <img src={iconUrl} alt="" loading="lazy" className="size-4 shrink-0 rounded-sm" />
                  ) : Glyph ? (
                    <Glyph className="size-4 shrink-0" aria-hidden="true" />
                  ) : null}
                </h2>
                <ul className="space-y-0.5">
                  {leaves.map((cat) => (
                    <li key={cat.id}>
                      <NavLeaf cat={cat} active={cat.id === selectedId} onSelect={onSelect} />
                    </li>
                  ))}
                </ul>
              </div>
            )
          })}

          {/* 단독 leaf(group: null) — 헤더 없이, 그룹 leaf와 같은 들여쓰기·같은 NavLeaf.
              보석은 그룹 헤더가 없어 아이콘을 못 받으므로 leaf 자체에 Gem 글리프를 준다(재료 Hammer 선례). */}
          {categories.some((c) => c.group === null) && (
            <ul className="space-y-0.5">
              {categories
                .filter((c) => c.group === null)
                .map((cat) => (
                  <li key={cat.id}>
                    <NavLeaf
                      cat={cat}
                      active={cat.id === selectedId}
                      onSelect={onSelect}
                      icon={cat.id === GEM_CATEGORY_ID ? Gem : undefined}
                    />
                  </li>
                ))}
            </ul>
          )}
        </div>
      </div>

      {/* 모바일(<lg) — 가로 스크롤 칩(그룹 헤더 생략, leaf만). 단독 leaf도 자동 포함. */}
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 lg:hidden">
        {categories.map((cat) => (
          <Chip key={cat.id} cat={cat} active={cat.id === selectedId} onSelect={onSelect} />
        ))}
      </div>
    </nav>
  )
}

function NavLeaf({
  cat,
  active,
  onSelect,
  icon: Icon,
}: {
  cat: Category
  active: boolean
  onSelect: (id: string) => void
  /** Optional leading glyph — 헤더가 없어 그룹 아이콘을 못 받는 단독 leaf(보석)에 균형을 준다. */
  icon?: LucideIcon
}) {
  return (
    <button
      type="button"
      aria-current={active ? 'true' : undefined}
      onClick={() => onSelect(cat.id)}
      className={cn(
        'flex w-full items-center justify-between gap-2 rounded-md border-l-2 px-2 py-1.5 text-left text-sm transition-colors',
        'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
        active
          ? 'border-primary text-primary bg-muted font-semibold'
          : 'text-foreground hover:bg-muted/60 border-transparent font-medium',
      )}
    >
      <span className="flex min-w-0 items-center gap-1.5">
        {/* aria-hidden — 바로 옆 라벨이 이미 이름을 말한다(그룹 헤더 아이콘과 동일 규칙). */}
        {Icon ? <Icon className="size-4 shrink-0" aria-hidden="true" /> : null}
        <span className="truncate">{cat.label}</span>
      </span>
      <span className="text-muted-foreground shrink-0 text-xs font-semibold tabular-nums">
        {cat.count}
      </span>
    </button>
  )
}

function Chip({
  cat,
  active,
  onSelect,
}: {
  cat: Category
  active: boolean
  onSelect: (id: string) => void
}) {
  return (
    <button
      type="button"
      aria-current={active ? 'true' : undefined}
      onClick={() => onSelect(cat.id)}
      className={cn(
        'flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium whitespace-nowrap transition-colors',
        'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
        active ? 'bg-primary text-primary-foreground' : 'bg-muted text-foreground',
      )}
    >
      {cat.label}
      <span
        className={cn(
          'text-xs tabular-nums',
          active ? 'text-primary-foreground/80' : 'text-muted-foreground',
        )}
      >
        {cat.count}
      </span>
    </button>
  )
}
