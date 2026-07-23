import { useMemo } from 'react'

import { useItems } from '@/lib/queries'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { sortByRole } from '@/features/_shared/roleGroup'
import { deriveCategories, filterByCategory, type Category } from '@/features/_shared/categories'
import { cn } from '@/lib/utils'
import type { TrackedItem } from '@/lib/schemas'

/*
  ItemPicker — 타임라인의 품목 선택기(quick-260723-jx1). 카테고리 칩 한 줄 + 그 카테고리의 품목 칩
  그리드, 2단. 앞선 `ItemSelect`(Radix select 드롭다운)를 대체한다.

  왜 바꿨나 (사용자 지적 2026-07-23):
  - 49개(딜러11·서포터7·재련재료11·상급재련8·재련보조6·아크그리드젬6)를 드롭다운 하나에 넣어 두고
    스크롤로 찾게 하고 있었다.
  - 더 나쁜 건 트리거가 **역할군 라벨만**("재료") 보여줘서, 지금 무엇을 보고 있는지 선택기만 봐서는
    알 수 없었다는 것이다. 원래 주석의 진단("긴 각인서 이름이 좁은 트리거에서 넘친다")은 맞았지만,
    넘치면 넓히거나 말줄임할 일이지 이름을 지울 일이 아니었다.

  이제 한 카테고리당 최대 11개라 스크롤이 필요 없고, 선택한 품목의 이름이 활성 칩에 항상 떠 있다.
  taxonomy는 대시보드 CategoryNav와 **같은 모듈**(_shared/categories)을 쓰므로 두 화면의 분류가
  갈라질 수 없다.

  🔑 로컬 state가 없다. 활성 카테고리는 "지금 선택된 품목이 속한 카테고리"로 매 렌더 유도한다 —
  카테고리를 누르면 그 카테고리의 첫 품목을 즉시 onChange하므로 value는 언제나 활성 카테고리 안에
  있고, 둘이 어긋난 상태 자체가 존재할 수 없다(useEffect 동기화 불필요, 대시보드의 selectedId 유도와
  같은 수법). 외부에서 ?item= 이 바뀌어도 자동으로 따라간다.

  보석은 없다: gemCount=0으로 deriveCategories를 부른다. 보석은 TrackedItem이 아니라 시계열이 없어서,
  탭을 만들면 그릴 수 없는 차트로 데려가게 된다.
*/

/*
  어느 leaf에도 안 잡히는 물품을 담는 가상 카테고리. 지금은 비어 있다(실측 2026-07-23:
  11+7+31 = 49 = 전체, 누락 0). 그래도 두는 이유는 앞선 ItemSelect가 명시적으로 보장하던 것이기
  때문이다 — "null roleGroup is included as a '기타' section so NO curated item is ever dropped
  (ICON-07)". 시드가 늘어 분류가 밀렸을 때 물품이 조용히 사라지는 대신 여기 뜬다.
*/
const OTHER_CATEGORY_ID = 'other'

export function ItemPicker({
  value,
  onChange,
}: {
  value: number | null
  onChange: (id: number) => void
}) {
  const { status, data } = useItems()

  const sorted = useMemo(() => (data ? sortByRole(data) : []), [data])

  // 카테고리 목록 = 대시보드와 동일한 leaf들 + (있다면) 기타. 보석은 제외(gemCount=0).
  const categories = useMemo<Category[]>(() => {
    const leaves = deriveCategories(sorted, 0)
    const matched = new Set(leaves.flatMap((c) => filterByCategory(sorted, c.id).map((i) => i.id)))
    const others = sorted.filter((i) => !matched.has(i.id))
    return others.length > 0
      ? [...leaves, { id: OTHER_CATEGORY_ID, label: '기타', group: null, count: others.length }]
      : leaves
  }, [sorted])

  const itemsOf = useMemo(() => {
    const matched = new Set(
      deriveCategories(sorted, 0).flatMap((c) => filterByCategory(sorted, c.id).map((i) => i.id)),
    )
    return (categoryId: string): TrackedItem[] =>
      categoryId === OTHER_CATEGORY_ID
        ? sorted.filter((i) => !matched.has(i.id))
        : filterByCategory(sorted, categoryId)
  }, [sorted])

  // 선택기 실패가 화면을 비우면 안 된다 — pending/error는 여기서 인라인 처리하고, 절대
  // 화면 단위 ErrorState로 올리지 않는다(ItemSelect에서 이어받은 규칙).
  if (status === 'pending') {
    return (
      <div className="space-y-2" role="status" aria-busy="true">
        <Skeleton className="h-8 w-full max-w-xl" />
        <Skeleton className="h-8 w-full max-w-lg" />
      </div>
    )
  }

  if (status === 'error' || categories.length === 0) {
    return <p className="text-muted-foreground text-sm">품목을 불러오지 못했어요</p>
  }

  // 활성 카테고리 = 선택된 품목이 속한 곳. 못 찾으면 첫 카테고리(진입 직후 value가 아직 null일 때).
  const activeCategory =
    categories.find((c) => itemsOf(c.id).some((i) => i.id === value)) ?? categories[0]
  const visibleItems = itemsOf(activeCategory.id)

  // 카테고리를 누르면 그 카테고리의 첫 품목으로 즉시 이동한다. 차트는 itemId가 null일 수 없고
  // (빈 화면 금지, TimelinePage D-06과 같은 원칙), 아무 일도 안 일어나면 활성 칩 없는 상태가 되어
  // 오히려 더 헷갈린다. 정확한 품목은 한 번 더 누르면 된다.
  function pickCategory(categoryId: string) {
    const first = itemsOf(categoryId)[0]
    if (first) onChange(first.id)
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2" role="group" aria-label="카테고리 선택">
        {categories.map((cat) => (
          <Chip
            key={cat.id}
            active={cat.id === activeCategory.id}
            onClick={() => pickCategory(cat.id)}
          >
            {cat.label}
            <span
              className={cn(
                'text-xs tabular-nums',
                cat.id === activeCategory.id ? 'text-primary-foreground/80' : 'text-muted-foreground',
              )}
            >
              {cat.count}
            </span>
          </Chip>
        ))}
      </div>

      <div className="flex flex-wrap gap-2" role="group" aria-label="품목 선택">
        {visibleItems.map((item) => (
          <Chip key={item.id} active={item.id === value} onClick={() => onChange(item.id)}>
            {/* aria-hidden — 바로 옆 이름이 이미 무엇인지 말한다(CategoryNav와 같은 규칙). */}
            <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
            {item.displayName}
          </Chip>
        ))}
      </div>
    </div>
  )
}

/*
  칩 하나. 대시보드 CategoryNav의 모바일 Chip과 같은 모양을 쓴다 — 두 화면이 같은 분류를 같은
  생김새로 보여줘야 학습이 한 번으로 끝난다. 활성 상태는 색만으로 표시하지 않는다(D-01):
  배경 + 굵기 + aria-current가 함께 간다.
*/
function Chip({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      aria-current={active ? 'true' : undefined}
      onClick={onClick}
      className={cn(
        'flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm whitespace-nowrap transition-colors',
        'focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none',
        active
          ? 'bg-primary text-primary-foreground font-semibold'
          : 'bg-muted text-foreground hover:bg-muted/70 font-medium',
      )}
    >
      {children}
    </button>
  )
}
