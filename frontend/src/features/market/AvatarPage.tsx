import { useEffect, useState } from 'react'
import { Search } from 'lucide-react'

import { useAvatar, useMarketClasses } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { cn } from '@/lib/utils'

import { MarketResultList } from './MarketResultList'
import { Pagination } from './Pagination'
import { SortSelect, parseSortValue, type SortValue } from './SortSelect'
import { useDebouncedValue } from './useDebouncedValue'

/*
  아바타 실시간 시세 검색. 인게임 거래소를 본떴다: 직업 드롭다운(필수) + 부위 사이드바 + 검색바 + 정렬.
  저장하지 않는 온디맨드 조회라 현재 시세만 보여준다.

  🔑 직업 선택이 필수다(사용자 결정). 부위 하나가 ~2,100개라 직업 없이 열면 목록이 무의미하게 길다.
  직업 미선택 시 안내만 띄우고 쿼리를 실행하지 않는다(useAvatar가 enabled로 막고, 백엔드도 class 없으면 400).

  부위 사이드바는 "전체" 없이 10개만(사용자 결정: 전체 빼고 전부). 기본은 무기 — 직업을 고르면 무기 부위가
  바로 뜬다. 대시보드 CategoryNav처럼 lg에선 좌측 세로 목록, 모바일에선 가로 스크롤 칩이다.
*/

const PARTS: { code: string; label: string }[] = [
  { code: '20005', label: '무기' },
  { code: '20010', label: '머리' },
  { code: '20020', label: '얼굴1' },
  { code: '20030', label: '얼굴2' },
  { code: '20050', label: '상의' },
  { code: '20060', label: '하의' },
  { code: '20070', label: '상하의 세트' },
  { code: '21400', label: '악기' },
  { code: '21500', label: '아바타 상자' },
  { code: '21600', label: '이동 효과' },
]

export function AvatarPage() {
  const classes = useMarketClasses()
  const [characterClass, setCharacterClass] = useState('')
  const [part, setPart] = useState(PARTS[0].code)
  const [query, setQuery] = useState('')
  const [sortValue, setSortValue] = useState<SortValue>('min_price:asc')
  const [page, setPage] = useState(1)
  const debouncedQuery = useDebouncedValue(query)
  const { sort, dir } = parseSortValue(sortValue)

  // 직업·부위·검색어·정렬이 바뀌면 1페이지로.
  useEffect(() => {
    setPage(1)
  }, [characterClass, part, debouncedQuery, sortValue])

  const result = useAvatar({ characterClass, part, q: debouncedQuery, sort, dir, page })

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-xl font-semibold">아바타</h1>
        <p className="text-muted-foreground text-sm">직업을 선택하고 이름으로 검색해 현재 시세를 확인하세요.</p>
      </div>

      {/* 직업 드롭다운 — 필수. 로딩 중엔 비활성. */}
      <div className="max-w-xs">
        <Select
          value={characterClass}
          onValueChange={setCharacterClass}
          disabled={classes.status !== 'success'}
        >
          <SelectTrigger className="w-full" aria-label="직업 선택">
            <SelectValue placeholder="직업을 선택하세요" />
          </SelectTrigger>
          <SelectContent>
            {(classes.data ?? []).map((c) => (
              <SelectItem key={c} value={c}>
                {c}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {characterClass === '' ? (
        <div className="border-border text-muted-foreground rounded-lg border border-dashed py-16 text-center text-sm">
          직업을 먼저 선택하면 해당 직업의 아바타 시세가 표시됩니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[11rem_minmax(0,1fr)]">
          <PartNav part={part} onSelect={setPart} />

          <div className="min-w-0 space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="relative flex-1">
                <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
                <Input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="아바타 이름 검색"
                  className="pl-9"
                  aria-label="아바타 이름 검색"
                />
              </div>
              <SortSelect value={sortValue} onChange={setSortValue} />
            </div>

            <AsyncBoundary
              status={result.status}
              isEmpty={(result.data?.items.length ?? 0) === 0}
              onRetry={() => result.refetch()}
              emptyHeading="결과가 없어요"
              emptyBody="다른 부위나 이름으로 검색해 보세요."
            >
              {result.data && (
                <div className="space-y-6">
                  <MarketResultList items={result.data.items} />
                  <Pagination
                    page={result.data.pageNo}
                    totalCount={result.data.totalCount}
                    onPageChange={setPage}
                  />
                </div>
              )}
            </AsyncBoundary>
          </div>
        </div>
      )}
    </div>
  )
}

/*
  부위 선택 — 대시보드 CategoryNav와 같은 두 얼굴: lg 세로 목록 / 모바일 가로 칩. 활성 상태는 색만이
  아니라 배경+굵기+aria-current로도 표시한다(색 하나에 의존 금지, 대시보드와 동일 규율).
*/
function PartNav({ part, onSelect }: { part: string; onSelect: (code: string) => void }) {
  return (
    <nav aria-label="부위" className="lg:sticky lg:top-20 lg:self-start">
      <ul className="hidden gap-0.5 lg:block">
        {PARTS.map((p) => (
          <li key={p.code}>
            <PartButton p={p} active={p.code === part} onSelect={onSelect} full />
          </li>
        ))}
      </ul>
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 lg:hidden">
        {PARTS.map((p) => (
          <PartButton key={p.code} p={p} active={p.code === part} onSelect={onSelect} />
        ))}
      </div>
    </nav>
  )
}

function PartButton({
  p,
  active,
  onSelect,
  full = false,
}: {
  p: { code: string; label: string }
  active: boolean
  onSelect: (code: string) => void
  full?: boolean
}) {
  return (
    <button
      type="button"
      aria-current={active ? 'true' : undefined}
      onClick={() => onSelect(p.code)}
      className={cn(
        'focus-visible:ring-ring rounded-md text-sm whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:outline-none',
        full
          ? 'flex w-full items-center border-l-2 px-2 py-1.5 text-left'
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
      {p.label}
    </button>
  )
}
