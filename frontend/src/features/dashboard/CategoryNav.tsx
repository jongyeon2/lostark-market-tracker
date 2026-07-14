import { cn } from '@/lib/utils'

import type { Category, CategoryGroup } from './categories'

/*
  CategoryNav — the dashboard's category filter (Phase 23, UX-01/UX-02). Stateless: it renders the
  derived categories and reports selection via onSelect; DashboardPage owns the selected state. One
  component, two responsive faces — a grouped vertical nav on lg+, a horizontal scrollable chip row
  below lg (same leaves, same order). Active state is never color-alone: it pairs the reserved
  primary accent with background + weight + aria-current so identity survives without color.
  Reuses existing tokens only (primary/muted/ring) — no new colors, icons, or spacing.
*/

const GROUP_ORDER: readonly CategoryGroup[] = ['각인', '재료']

export function CategoryNav({
  categories,
  selectedId,
  onSelect,
}: {
  categories: Category[]
  selectedId: string | null
  onSelect: (id: string) => void
}) {
  return (
    <nav aria-label="카테고리" className="lg:sticky lg:top-6 lg:self-start">
      {/* 데스크톱(lg+) — 2단계 그룹 세로 nav. */}
      <div className="hidden lg:block">
        <div className="space-y-4">
          {GROUP_ORDER.map((group) => {
            const leaves = categories.filter((c) => c.group === group)
            if (leaves.length === 0) return null
            return (
              <div key={group} className="space-y-1">
                <h2 className="text-muted-foreground px-2 text-sm font-semibold tracking-wide">
                  {group}
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
        </div>
      </div>

      {/* 모바일(<lg) — 가로 스크롤 칩(그룹 헤더 생략, leaf만). */}
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
        'flex w-full items-center justify-between gap-2 rounded-md border-l-2 px-2 py-1.5 text-left text-sm transition-colors',
        'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
        active
          ? 'border-primary text-primary bg-muted font-semibold'
          : 'text-foreground hover:bg-muted/60 border-transparent font-medium',
      )}
    >
      <span className="truncate">{cat.label}</span>
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
