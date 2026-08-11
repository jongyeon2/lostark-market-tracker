import { useState } from 'react'
import { ChevronDown, ChevronRight } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { cn } from '@/lib/utils'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import type { Category, CategoryGroup } from '@/features/_shared/categories'
import type { TrackedItem } from '@/lib/schemas'

/*
  TimelineItemTree — 타임라인 전용 좌측 아코디언 트리(quick-260811, 사용자 피드백). 대시보드의 flat
  CategoryNav와 상호작용이 달라 공유하지 않고 별도로 둔다(대시보드는 카테고리로 중앙 카드를 필터,
  여기는 리프를 펼쳐 그 안의 품목을 직접 고른다). taxonomy·품목 목록은 여전히 _shared/categories에서
  파생된 값을 주입받으므로 두 화면의 분류가 갈라지지 않는다.

  구조: 그룹 헤더(각인서/재료, 라벨) → 리프 카테고리(딜러 각인 등, 토글 버튼) → 펼치면 그 카테고리
  품목 목록(작은 아이콘 + 이름, 선택 항목 하이라이트). 리프 클릭 = 펼침/접힘, 품목 클릭 = onSelectItem.

  🔑 아코디언(한 번에 하나, 사용자 결정) — expandedId 로컬 state 하나. 기본값은 "선택 품목이 속한
  카테고리"를 따른다(undefined 센티넬 → activeCatId): 진입·딥링크·첫 품목 자동선택 후에도 해당
  카테고리가 저절로 열려 있어 useEffect 동기화가 필요 없다. 사용자가 토글을 만지면 그때부터 수동값이
  이긴다(같은 걸 다시 누르면 null=전부 접힘). 선택은 펼친 카테고리 안에서만 일어나므로 활성·펼침이
  어긋나지 않는다.
*/

const GROUP_ORDER: readonly CategoryGroup[] = ['각인서', '재료']

export function TimelineItemTree({
  categories,
  itemsOf,
  selectedItemId,
  onSelectItem,
}: {
  categories: Category[]
  /** 카테고리 id → 그 카테고리 품목(정렬된 상위 목록에서 파생). 트리는 데이터를 재필터하지 않는다. */
  itemsOf: (categoryId: string) => TrackedItem[]
  selectedItemId: number | null
  onSelectItem: (id: number) => void
}) {
  // 선택 품목이 속한 카테고리 — 자동 펼침 기본값.
  const activeCatId =
    categories.find((c) => itemsOf(c.id).some((i) => i.id === selectedItemId))?.id ?? null

  // undefined = 수동 조작 전(activeCatId를 따름) / null = 전부 접음 / string = 그 카테고리만 펼침.
  const [manualExpanded, setManualExpanded] = useState<string | null | undefined>(undefined)
  const expandedId = manualExpanded === undefined ? activeCatId : manualExpanded

  function toggle(categoryId: string) {
    setManualExpanded(expandedId === categoryId ? null : categoryId)
  }

  const leavesOf = (group: CategoryGroup | null) => categories.filter((c) => c.group === group)

  const renderLeaves = (leaves: Category[]) => (
    <ul className="space-y-0.5">
      {leaves.map((cat) => (
        <LeafToggle
          key={cat.id}
          cat={cat}
          items={itemsOf(cat.id)}
          expanded={expandedId === cat.id}
          selectedItemId={selectedItemId}
          onToggle={() => toggle(cat.id)}
          onSelectItem={onSelectItem}
        />
      ))}
    </ul>
  )

  return (
    <nav aria-label="품목" className="space-y-4">
      {GROUP_ORDER.map((group) => {
        const leaves = leavesOf(group)
        if (leaves.length === 0) return null
        return (
          <div key={group} className="space-y-1">
            {/* 그룹 헤더는 라벨(토글 아님, 사용자 결정: "큰 카테고리 말고 그 안의 리프에 토글"). */}
            <h2 className="border-border text-muted-foreground border-b px-2 pb-1.5 text-sm font-semibold tracking-wide">
              {group}
            </h2>
            {renderLeaves(leaves)}
          </div>
        )
      })}

      {/* 그룹 없는 단독 리프(기타 등) — 헤더 없이 같은 토글. */}
      {leavesOf(null).length > 0 && renderLeaves(leavesOf(null))}
    </nav>
  )
}

function LeafToggle({
  cat,
  items,
  expanded,
  selectedItemId,
  onToggle,
  onSelectItem,
}: {
  cat: Category
  items: TrackedItem[]
  expanded: boolean
  selectedItemId: number | null
  onToggle: () => void
  onSelectItem: (id: number) => void
}) {
  const Chevron: LucideIcon = expanded ? ChevronDown : ChevronRight
  return (
    <li>
      <button
        type="button"
        aria-expanded={expanded}
        onClick={onToggle}
        className={cn(
          'flex w-full items-center justify-between gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors',
          'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
          expanded ? 'text-foreground font-semibold' : 'text-foreground hover:bg-muted/60 font-medium',
        )}
      >
        <span className="flex min-w-0 items-center gap-1.5">
          <Chevron className="text-muted-foreground size-4 shrink-0" aria-hidden="true" />
          <span className="truncate">{cat.label}</span>
        </span>
        <span className="text-muted-foreground shrink-0 text-xs font-semibold tabular-nums">
          {cat.count}
        </span>
      </button>

      {expanded && (
        <ul className="mt-0.5 space-y-0.5 pl-4">
          {items.map((item) => {
            const active = item.id === selectedItemId
            return (
              <li key={item.id}>
                <button
                  type="button"
                  aria-current={active ? 'true' : undefined}
                  onClick={() => onSelectItem(item.id)}
                  className={cn(
                    'flex w-full items-center gap-1.5 rounded-md border-l-2 px-2 py-1.5 text-left text-sm transition-colors',
                    'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
                    active
                      ? 'border-primary text-primary bg-muted font-semibold'
                      : 'text-foreground hover:bg-muted/60 border-transparent font-medium',
                  )}
                >
                  {/* aria-hidden — 바로 옆 이름이 이미 무엇인지 말한다(대시보드 규칙 계승). */}
                  <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
                  <span className="truncate">{item.displayName}</span>
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </li>
  )
}
