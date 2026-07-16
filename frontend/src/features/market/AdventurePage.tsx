import { useEffect, useState } from 'react'
import { Search } from 'lucide-react'

import { useAdventure } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Input } from '@/components/ui/input'

import { MarketResultList } from './MarketResultList'
import { Pagination } from './Pagination'
import { SortSelect, parseSortValue, type SortValue } from './SortSelect'
import { useDebouncedValue } from './useDebouncedValue'

/*
  모험의 서 실시간 시세 검색 (거래소 카테고리 100000). 인게임 거래소처럼 검색바로 찾는다 — 저장하지 않는
  온디맨드 조회라 차트·이벤트 영향은 없고 현재 시세만 보여준다. 검색은 디바운스(300ms), 정렬 변경·검색어
  변경 시 페이지를 1로 되돌린다(2페이지를 보다 검색하면 결과 없는 페이지에 갇히는 걸 막는다).
*/
export function AdventurePage() {
  const [query, setQuery] = useState('')
  const [sortValue, setSortValue] = useState<SortValue>('min_price:asc')
  const [page, setPage] = useState(1)
  const debouncedQuery = useDebouncedValue(query)
  const { sort, dir } = parseSortValue(sortValue)

  // 검색어·정렬이 바뀌면 1페이지로. 안 그러면 필터가 좁아졌을 때 빈 페이지에 남는다.
  useEffect(() => {
    setPage(1)
  }, [debouncedQuery, sortValue])

  const { status, data, refetch } = useAdventure({ q: debouncedQuery, sort, dir, page })

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">모험의 서</h1>
        <p className="text-muted-foreground text-sm">이름으로 검색해 현재 시세를 확인하세요.</p>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="모험의 서 이름 검색"
            className="pl-9"
            aria-label="모험의 서 이름 검색"
          />
        </div>
        <SortSelect value={sortValue} onChange={setSortValue} />
      </div>

      <AsyncBoundary
        status={status}
        isEmpty={(data?.items.length ?? 0) === 0}
        onRetry={() => refetch()}
        emptyHeading="결과가 없어요"
        emptyBody="다른 이름으로 검색해 보세요."
      >
        {data && (
          <div className="space-y-6">
            <MarketResultList items={data.items} />
            <Pagination page={data.pageNo} totalCount={data.totalCount} onPageChange={setPage} />
          </div>
        )}
      </AsyncBoundary>
    </div>
  )
}
