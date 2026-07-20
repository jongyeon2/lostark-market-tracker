import { useMemo, useState } from 'react'
import { Map as MapIcon, Search } from 'lucide-react'

import { useAdventure } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import type { MarketSearchItem } from '@/lib/schemas'

import { MarketResultList } from './MarketResultList'
import { SortSelect, parseSortValue, type SortValue } from './SortSelect'
import { ALL_LABEL, groupByContinent, type ContinentGroup } from './tomes'
import { useDebouncedValue } from './useDebouncedValue'

/*
  모험의 서 실시간 시세 — 대륙별 조회. 모험의 서는 대륙마다 달성도를 채우는 수집품 묶음이라,
  "이름으로 찾기"가 아니라 "루테란 서부를 채우려면 뭐가 얼마인지"가 실제 질문이다. 그래서 좌측에
  대시보드와 같은 카테고리 네비(대륙 20개 + 전체)를 두고 고른 대륙의 7개만 보여준다.

  🔑 서버가 한 번에 전량(~140)을 준다. 대륙 필터·이름 검색·정렬이 **전부 이 140행 위 로컬 연산**이라
  대륙을 아무리 눌러도 네트워크 요청이 0이고 페이지네이션도 없다. 대륙 매핑을 서버가 아니라 여기
  두는 이유와 그 매핑의 출처·검증은 tomes.ts 주석에 있다.

  저장하지 않는 온디맨드 조회라 차트·이벤트 영향은 없고 현재 시세만 보여준다.
*/
export function AdventurePage() {
  const { status, data, refetch } = useAdventure()
  const [continent, setContinent] = useState(ALL_LABEL)
  const [query, setQuery] = useState('')
  const [sortValue, setSortValue] = useState<SortValue>('min_price:asc')
  const debouncedQuery = useDebouncedValue(query)
  const { sort, dir } = parseSortValue(sortValue)

  const items = useMemo(() => data?.items ?? [], [data])
  const groups = useMemo(() => groupByContinent(items), [items])

  /*
    선택 대륙이 사라지면(표 갱신·데이터 변화) 전체로 되돌린다. useEffect로 동기화하지 않고 매 렌더
    유효값을 파생하는 건 대시보드 selectedId와 같은 이유 — 죽은 선택이 남을 수 없다.
  */
  const selected = groups.some((g) => g.name === continent) ? continent : ALL_LABEL
  const scope = selected === ALL_LABEL ? items : (groups.find((g) => g.name === selected)?.items ?? [])

  const visible = useMemo(() => {
    const q = debouncedQuery.trim()
    const filtered = q === '' ? scope : scope.filter((i) => i.name.includes(q))
    return [...filtered].sort(compareBy(sort, dir))
  }, [scope, debouncedQuery, sort, dir])

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-[16rem_minmax(0,1fr)]">
      <div className="lg:sticky lg:top-20 lg:self-start">
        {/* 제목·부제 없음 — 사유는 AvatarPage와 동일(사용자 결정 2026-07-20). */}
        {/* 대륙 네비는 목록이 있어야 그릴 수 있다 — 로딩·실패 시엔 우측 boundary만 말하게 두고
            여기선 아무것도 그리지 않는다(빈 카테고리 껍데기가 뜨는 것보다 낫다). */}
        {groups.length > 0 ? (
          <ContinentNav groups={groups} total={items.length} selected={selected} onSelect={setContinent} />
        ) : null}
      </div>

      <div className="min-w-0 space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={selected === ALL_LABEL ? '수집품 이름 검색' : `${selected} 안에서 검색`}
              className="pl-9"
              aria-label="수집품 이름 검색"
            />
          </div>
          <SortSelect value={sortValue} onChange={setSortValue} />
        </div>

        <AsyncBoundary
          status={status}
          isEmpty={visible.length === 0}
          onRetry={() => refetch()}
          emptyHeading="결과가 없어요"
          emptyBody="다른 대륙이나 이름으로 찾아보세요."
        >
          <MarketResultList items={visible} className="grid gap-2 xl:grid-cols-2" />
        </AsyncBoundary>
      </div>
    </div>
  )
}

/*
  가격 정렬. 🔑 가격이 없는 행(즉시구매 매물 없음, null)은 방향과 무관하게 **항상 뒤로** 보낸다.
  null을 0으로 취급하면 낮은가격순에서 "제일 싼 물건"으로 맨 위에 올라오는데, 그건 값이 없다는
  사실을 0원이라고 지어내는 것이다(MarketResultList가 null을 "매물 없음"으로 정직히 쓰는 규율).
*/
function compareBy(sort: 'min_price' | 'recent_price', dir: 'asc' | 'desc') {
  const pick = (i: MarketSearchItem) =>
    sort === 'min_price' ? i.currentMinPrice : i.recentPrice
  return (a: MarketSearchItem, b: MarketSearchItem) => {
    const x = pick(a)
    const y = pick(b)
    if (x === null && y === null) return a.name.localeCompare(b.name, 'ko-KR')
    if (x === null) return 1
    if (y === null) return -1
    return dir === 'asc' ? x - y : y - x
  }
}

/*
  대륙 네비 — 대시보드 CategoryNav와 같은 두 얼굴: lg 흰 카드 안 세로 목록 / 모바일 가로 스크롤 칩.
  상위 그룹은 `대륙` 하나뿐이라 지도 글리프를 단다(재료의 Hammer·보석의 Gem 선례 — 그룹 구성원이
  공유하는 게임 아이콘이 없을 땐 그린 글리프를 쓴다).

  활성 상태는 색만이 아니라 배경+굵기+aria-current로도 표시한다(색 하나에 의존 금지).
*/
function ContinentNav({
  groups,
  total,
  selected,
  onSelect,
}: {
  groups: ContinentGroup[]
  total: number
  selected: string
  onSelect: (name: string) => void
}) {
  const leaves = [{ name: ALL_LABEL, count: total }, ...groups.map((g) => ({ name: g.name, count: g.items.length }))]
  return (
    <nav aria-label="대륙">
      <div className="lg:bg-card lg:rounded-xl lg:border lg:px-4 lg:py-4 lg:shadow-sm">
        <h2 className="text-muted-foreground mb-2 hidden items-center gap-1.5 text-xs font-semibold lg:flex">
          <MapIcon className="size-4 shrink-0" aria-hidden="true" />
          대륙
        </h2>
        <ul className="hidden gap-0.5 lg:block">
          {leaves.map((leaf) => (
            <li key={leaf.name}>
              <ContinentButton leaf={leaf} active={leaf.name === selected} onSelect={onSelect} full />
            </li>
          ))}
        </ul>
        <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 lg:hidden">
          {leaves.map((leaf) => (
            <ContinentButton key={leaf.name} leaf={leaf} active={leaf.name === selected} onSelect={onSelect} />
          ))}
        </div>
      </div>
    </nav>
  )
}

function ContinentButton({
  leaf,
  active,
  onSelect,
  full = false,
}: {
  leaf: { name: string; count: number }
  active: boolean
  onSelect: (name: string) => void
  full?: boolean
}) {
  return (
    <button
      type="button"
      aria-current={active ? 'true' : undefined}
      onClick={() => onSelect(leaf.name)}
      className={cn(
        'focus-visible:ring-ring rounded-md text-sm whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:outline-none',
        full
          ? 'flex w-full items-center justify-between gap-2 border-l-2 px-2 py-1.5 text-left'
          : 'shrink-0 rounded-full px-3 py-1.5',
        active
          ? full
            ? 'border-primary text-primary bg-muted font-semibold'
            : 'bg-primary text-primary-foreground font-medium'
          : full
            ? 'text-foreground hover:bg-muted/60 border-transparent font-medium'
            : 'bg-muted text-foreground font-medium',
      )}
    >
      <span className="truncate">{leaf.name}</span>
      {/* 개수는 데스크톱 목록에서만 — 칩에 붙이면 가로 폭만 먹고 스크롤을 늘린다. */}
      {full ? <span className="text-muted-foreground shrink-0 text-xs tabular-nums">{leaf.count}</span> : null}
    </button>
  )
}
