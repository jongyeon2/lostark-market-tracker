import { useEffect, useState } from 'react'
import { Search } from 'lucide-react'

import { useAvatar, useMarketClasses } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

import { classIconUrl, groupClasses, type ClassEntry } from './classes'
import { MarketResultList } from './MarketResultList'
import { Pagination } from './Pagination'
import { SortSelect, parseSortValue, type SortValue } from './SortSelect'
import { useDebouncedValue } from './useDebouncedValue'

/*
  아바타 실시간 시세 검색. 인게임 거래소를 본떴다: 직업 선택(필수) + 부위 사이드바 + 검색바 + 정렬.
  저장하지 않는 온디맨드 조회라 현재 시세만 보여준다.

  🔑 직업 선택이 필수다(사용자 결정). 부위 하나가 ~2,100개라 직업 없이 열면 목록이 무의미하게 길다.
  직업 미선택 시 안내만 띄우고 쿼리를 실행하지 않는다(useAvatar가 enabled로 막고, 백엔드도 class 없으면 400).

  직업 선택은 드롭다운이었다가 아이콘 바둑판으로 바뀌었다(사용자 결정 2026-07-20): 30개를 접힌 목록에서
  글자로만 훑는 것보다, 로아 유저가 실제로 직업을 식별하는 단위인 아이콘을 펼쳐두는 편이 빠르다.
  직업군·아이콘 매핑의 출처와 근거는 classes.ts에 있다.

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
      {/* 제목·부제 없음(사용자 결정 2026-07-20). 상단 네비의 '아바타'가 이미 활성 표시로 어느 화면인지
          말하고, 무엇을 하는 곳인지는 직업 바둑판과 부위 칩이 그 자체로 말한다. 시세를 보러 온 사람에게
          "시세를 확인하세요"는 정보가 0이다. 접근성용 제목은 각 영역의 aria-label이 대신한다. */}
      {/* 직업 바둑판 — 필수 선택. 목록 자체가 원격 데이터라 자체 boundary를 갖는다(D-07). */}
      <AsyncBoundary
        status={classes.status}
        isEmpty={(classes.data?.length ?? 0) === 0}
        onRetry={() => classes.refetch()}
        emptyHeading="직업 목록을 불러오지 못했어요"
        emptyBody="잠시 후 다시 시도해 주세요."
      >
        <ClassGrid
          classes={classes.data ?? []}
          selected={characterClass}
          onSelect={setCharacterClass}
        />
      </AsyncBoundary>

      {characterClass === '' ? (
        <div className="border-border text-muted-foreground rounded-lg border border-dashed py-16 text-center text-sm">
          직업을 먼저 선택하면 해당 직업의 아바타 시세가 표시됩니다.
        </div>
      ) : (
        <>
          {/* 부위 + 검색 + 정렬을 한 카드로. 필터를 전부 위에 모으면 위→아래가 실제 사용 순서
              (직업 → 부위 → 검색 → 결과)와 같아지고, 좌측 11rem 기둥이 사라져 상단 직업 카드와
              좌우 끝이 맞는다(사용자 결정 2026-07-20). sticky 부위 목록은 이때 포기한 것이다 —
              결과가 한 페이지 10개뿐이라 스크롤 중 부위를 바꿀 일이 드물다. */}
          <Card>
            <CardContent className="space-y-3">
              <PartNav part={part} onSelect={setPart} />
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
            </CardContent>
          </Card>

          <AsyncBoundary
            status={result.status}
            isEmpty={(result.data?.items.length ?? 0) === 0}
            onRetry={() => result.refetch()}
            emptyHeading="결과가 없어요"
            emptyBody="다른 부위나 이름으로 검색해 보세요."
          >
            {result.data && (
              <div className="space-y-6">
                {/* 2열 — 한 페이지가 10개라 5줄이면 끝나 스크롤 없이 한눈에 들어온다. 1열로 두면
                    넓은 화면에서 이름은 왼쪽 끝, 가격은 오른쪽 끝으로 벌어져 시선이 멀리 뛴다. */}
                <MarketResultList
                  items={result.data.items}
                  className="grid gap-2 lg:grid-cols-2"
                />
                <Pagination
                  page={result.data.pageNo}
                  totalCount={result.data.totalCount}
                  onPageChange={setPage}
                />
              </div>
            )}
          </AsyncBoundary>
        </>
      )}
    </div>
  )
}

/*
  직업 바둑판. 한 직업군 = 3열 블록이라 6인 그룹(전사·무도가)이 정확히 2줄로 떨어진다. 그 블록들을
  화면 폭만큼 가로로 늘어놓는다(사용자 결정 2026-07-20) — 인벤처럼 세로로 쭉 쌓으면 헤더 7개 + 11줄이
  상단을 다 먹어 정작 시세 목록이 한참 아래로 밀린다.

  구성원이 5명 이하인 그룹(헌터·마법사·가디언나이트)은 마지막 줄이 비지만 채우지 않는다. 빈칸을
  더미로 메우면 "여기도 직업이 있나" 하고 읽히고, 3열을 유지해야 블록끼리 좌우로 정렬된다.
*/
function ClassGrid({
  classes,
  selected,
  onSelect,
}: {
  classes: readonly string[]
  selected: string
  onSelect: (name: string) => void
}) {
  const groups = groupClasses(classes)
  return (
    // 흰 카드 한 장으로 감싼다(사용자 결정 2026-07-20): 바탕색 위에 타일만 떠 있으면 어디까지가
    // "직업 고르는 곳"인지 경계가 없어 중구난방으로 읽힌다. 아래 부위·검색·시세 영역과 면으로 분리한다.
    <Card>
      {/* 열 수를 폭에 따라 올린다 — 3열로 고정하면 1536px에서 블록 하나가 490px가 되고 타일이
          160px로 늘어나 24px 아이콘이 여백 한가운데 뜬다(사용자 피드백). 2xl에서 5열이면 블록당
          ~280px, 타일 ~90px로 아이콘·이름 비율이 맞는다. 7블록이라 마지막 줄은 늘 덜 찬다. */}
      <CardContent className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
        {groups.map((group) => (
          <section key={group.label} aria-label={`${group.label} 직업`} className="space-y-2">
            {/* 구분선은 라벨 밑줄로 단다 — 블록들이 좌우로 늘어서 있어 그리드 셀 사이에 선을 그으면
                열 경계인지 그룹 경계인지 모호해진다. 밑줄은 그 그룹의 폭만큼만 그어져 소속이 분명하다. */}
            <h2 className="border-b pb-1.5 text-sm font-semibold">{group.label}</h2>
            <div className="grid grid-cols-3 gap-1">
              {group.classes.map((entry) => (
                <ClassTile
                  key={entry.name}
                  entry={entry}
                  active={entry.name === selected}
                  onSelect={onSelect}
                />
              ))}
            </div>
          </section>
        ))}
      </CardContent>
    </Card>
  )
}

/*
  타일 = 아이콘(위) + 한글 직업명(아래). 선택 표시는 색 하나에 기대지 않는다(PartNav와 동일 규율):
  테두리 + 배경 + 굵기 + aria-pressed. 토글 버튼이므로 aria-current가 아니라 aria-pressed다.
*/
function ClassTile({
  entry,
  active,
  onSelect,
}: {
  entry: ClassEntry
  active: boolean
  onSelect: (name: string) => void
}) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={() => onSelect(entry.name)}
      className={cn(
        'focus-visible:ring-ring flex flex-col items-center gap-1 rounded-md border px-1 py-2 transition-colors focus-visible:ring-2 focus-visible:outline-none',
        active
          ? 'border-primary bg-muted text-primary font-semibold'
          : 'text-foreground hover:bg-muted/60 border-transparent font-medium',
      )}
    >
      <ClassIcon entry={entry} />
      {/* 직업명은 최대 6자('디스트로이어')라 잘릴 일이 없지만, 좁은 화면에서 타일 폭이 무너지는 것보다
          말줄임이 낫다. 타일 폭은 3열 그리드가 정한다. */}
      <span className="w-full truncate text-center text-xs">{entry.name}</span>
    </button>
  )
}

/*
  번들된 공식 SVG. 실패하면 슬롯만 남기고 조용히 비운다(MarketIcon·GemIcon 선례) — 아이콘이 없어도
  아래 이름으로 고를 수 있으니 타일을 통째로 죽이지 않는다. 매핑에 없는 신규 직업(iconSlug=null)도
  같은 빈 슬롯을 쓴다: 줄 이미지가 없는 것과 이미지를 못 받은 것은 화면에서 같은 상태다.

  🔑 다크모드에선 반전한다. 공식 SVG가 fill="#222222" 단색이라 어두운 배경에서 사실상 안 보인다.
  단색이라 invert가 정확히 #DDDDDD를 만들어 밝은 글리프가 된다 — mask-image로 currentColor를 상속시키거나
  흰색 아이콘 세트를 하나 더 번들하는 것보다 싸고, <img>를 유지하므로 onError 폴백도 그대로 살아있다.
  (색이 여러 개인 아이콘이었다면 invert는 색상까지 뒤집어 못 썼을 것이다.)
*/
function ClassIcon({ entry }: { entry: ClassEntry }) {
  const [failed, setFailed] = useState(false)
  const slug = failed ? null : entry.iconSlug
  return (
    // size-6(24px) — size-8은 이름보다 아이콘이 커서 타일이 아이콘 덩어리로 읽혔다(사용자 피드백).
    <span className="flex size-6 shrink-0 items-center justify-center">
      {slug !== null ? (
        <img
          src={classIconUrl(slug)}
          alt=""
          loading="lazy"
          /* 원본 CDN 파일은 대부분 fill="#222222"인데 5개(디스트로이어·워로드·홀리나이트·차원술사·
             가디언나이트)만 fill="white"라 라이트 모드에서 안 보인다. 예전엔 파일을 직접 고쳐서
             맞췄지만 핫링크는 원본을 못 바꾼다 — 단색 아이콘이므로 brightness-0으로 30개를 전부
             검정으로 눌러 통일하고, 다크에서 invert로 뒤집는다(원본 색과 무관하게 항상 맞는다). */
          className="size-full object-contain brightness-0 dark:invert"
          onError={() => setFailed(true)}
        />
      ) : null}
    </span>
  )
}

/*
  부위 선택 — 칩 한 줄. 예전엔 lg 세로 목록 / 모바일 가로 칩의 두 얼굴이었지만, 필터를 전부 상단
  카드로 모으면서 칩 하나로 통일했다(사용자 결정 2026-07-20). 10개뿐이라 넓은 화면에선 한 줄에 다
  들어가고, 좁아지면 wrap으로 접힌다 — 가로 스크롤은 안에 뭐가 더 있는지 안 보여서 쓰지 않는다.

  활성 상태는 색만이 아니라 배경+굵기+aria-current로도 표시한다(색 하나에 의존 금지, 대시보드 규율).
*/
function PartNav({ part, onSelect }: { part: string; onSelect: (code: string) => void }) {
  return (
    <nav aria-label="부위" className="flex flex-wrap gap-2">
      {PARTS.map((p) => {
        const active = p.code === part
        return (
          <button
            key={p.code}
            type="button"
            aria-current={active ? 'true' : undefined}
            onClick={() => onSelect(p.code)}
            className={cn(
              'focus-visible:ring-ring shrink-0 rounded-full px-3 py-1.5 text-sm font-medium whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:outline-none',
              active
                ? 'bg-primary text-primary-foreground'
                : 'bg-muted text-foreground hover:bg-muted/60',
            )}
          >
            {p.label}
          </button>
        )
      })}
    </nav>
  )
}
