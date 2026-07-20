import { useState } from 'react'
import { PackageSearch } from 'lucide-react'

import { useEventImpact } from '@/lib/queries'
import { ApiError, type EventImpactSort } from '@/lib/api'
import { CorrelationBanner } from '@/features/impact/CorrelationBanner'
import { EventImpactTable } from '@/features/impact/EventImpactTable'
import { EventImpactCards } from '@/features/impact/EventImpactCards'
import { EVENT_MARKERS } from '@/features/timeline/eventMarkers'
import { ErrorState } from '@/components/state/ErrorState'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import type { EventType } from '@/lib/schemas'

/*
  ItemImpactSection — the event-impact results for the item selected in the dashboard's center column
  (Phase 28, DASH-02). The /impact PAGE is gone: it made you navigate away and re-pick, with a selector,
  the item you were already looking at.

  What moved is only the COMPOSITION. Every part that decides what the numbers mean is reused as-is —
  EventImpactTable / EventImpactCards / impactFormat / ImpactStatusBadge / CorrelationBanner are
  untouched. Re-implementing any of them would fork the 10-UI-SPEC copy contract (the 404/empty
  wording below is verbatim from it) and let the table and cards drift apart. WindowControls is the
  one part that did NOT come along: the window picker was dropped (사용자 결정 2026-07-15) and the
  component had no other caller, so it was deleted rather than left orphaned.

  Both responsive views render; CSS (md breakpoint) toggles which is visible (D-04/D-09) — unchanged.
  LatestPriceCard is deliberately NOT included: the selected item's card sits directly above and
  already shows its 최저가, so repeating it would just push the events further down. Neither is the
  item's NAME: the card you just clicked is right above, still wearing its selected state.
*/

/*
  이벤트 전후 비교 창(시간). 사용자가 고르던 6/24/72 프리셋을 제거하고 기본값으로 고정했다
  (사용자 결정 2026-07-15) — 대시보드에서 카드를 눌러 바로 보는 흐름에서는 창 조절이 화면만 차지했다.
  백엔드는 1~168을 받으므로 이 상수는 항상 유효하다.
*/
const IMPACT_WINDOW_HOURS = 24

/*
  한 번에 보여줄 이벤트 수. 이 화면은 등록된 이벤트를 전부 그렸는데, 이벤트가 쌓이면 대시보드에서
  끝없이 스크롤해야 한다(사용자 지적 2026-07-20). 자르는 일은 **서버가** 한다 — 클라이언트에서만
  거르면 만 건짜리 응답(3~4MB)을 받아놓고 거르는 셈이라 페이로드는 그대로다.

  50은 "한 화면을 넘지만 스크롤이 끝나는" 크기다. 더 보려는 사람은 STEP만큼 늘려 다시 받는다.
*/
const INITIAL_LIMIT = 50
const LIMIT_STEP = 50

const SORT_OPTIONS: { value: EventImpactSort; label: string }[] = [
  { value: 'occurred_desc', label: '최신순' },
  { value: 'occurred_asc', label: '오래된순' },
]

export function ItemImpactSection({ itemId }: { itemId: number }) {
  const [types, setTypes] = useState<EventType[]>([])
  const [sort, setSort] = useState<EventImpactSort>('occurred_desc')
  const [limit, setLimit] = useState(INITIAL_LIMIT)

  /*
    필터·정렬이 바뀌면 limit을 되돌린다. 거래소 검색이 검색어·정렬 변경 시 1페이지로 가는 것과 같은
    이유 — 좁아진 결과에 이전의 늘어난 limit을 들고 가면 "더 보기"를 몇 번 눌렀는지가 화면에 남는다.
    itemId가 바뀔 때도 마찬가지라 함께 리셋한다.
  */
  function pickTypes(next: EventType[]) {
    setTypes(next)
    setLimit(INITIAL_LIMIT)
  }
  function pickSort(next: EventImpactSort) {
    setSort(next)
    setLimit(INITIAL_LIMIT)
  }

  return (
    <section className="space-y-4">
      <h2 className="text-base font-semibold">이벤트 영향</h2>
      {/* 상관 ≠ 인과 — 표보다 먼저 읽히도록 위에(D-10). changeRate는 시점 상관이지 인과가 아니다. */}
      <CorrelationBanner />
      <ImpactFilters types={types} sort={sort} onTypes={pickTypes} onSort={pickSort} />
      <ImpactResults
        key={itemId}
        itemId={itemId}
        window={IMPACT_WINDOW_HOURS}
        types={types}
        sort={sort}
        limit={limit}
        onMore={() => setLimit((n) => n + LIMIT_STEP)}
      />
    </section>
  )
}

/*
  종류 칩(다중 선택) + 정렬. 아무것도 안 고르면 전체 — "전체" 칩을 따로 두지 않는다. 선택이 없다는
  것과 전체를 고른 것은 결과가 같고, 칩을 하나 더 두면 "전체 + 로아ON"처럼 모순되는 조합이 생긴다.

  색·라벨은 EVENT_MARKERS를 그대로 쓴다. 차트 마커·범례·이벤트 배지가 이미 그 색이라, 여기서 다른
  색을 쓰면 같은 이벤트가 화면마다 다른 색이 된다. 선택 상태는 색만이 아니라 배경+테두리+굵기+
  aria-pressed로도 말한다(색 하나에 의존 금지).
*/
function ImpactFilters({
  types,
  sort,
  onTypes,
  onSort,
}: {
  types: EventType[]
  sort: EventImpactSort
  onTypes: (next: EventType[]) => void
  onSort: (next: EventImpactSort) => void
}) {
  const entries = Object.entries(EVENT_MARKERS) as [EventType, { color: string; koLabel: string }][]
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      {/*
        칩 7개를 한 줄에 넣는다. 기본 크기(text-sm·px-3·gap-2)로는 1920px에서 21px 모자라 '일반 패치'
        하나만 다음 줄로 떨어졌다 — 한 줄만 내려간 모양이 보기 불편하다는 피드백(2026-07-20).
        실측(가용 862px): px-2.5 → 여유 +7 / +gap-1.5 → +19 / +text-xs → **+98**.
        앞의 둘은 창을 조금만 줄여도 다시 접히므로 글자까지 줄여 여유를 확보했다(사용자 허용).

        라벨은 손대지 않는다. koLabel은 EVENT_MARKERS에서 오고 차트 범례·이벤트 배지가 같은 문자열을
        쓰므로, 여기서 줄이면 타임라인 범례까지 같이 바뀐다.
        ⚠️ 그래도 ~1670px 미만에서는 두 줄이 된다. 그 아래까지 한 줄로 만들려면 공유 라벨을 줄이는
        수밖에 없어 하지 않았다.
      */}
      <div className="flex flex-wrap gap-1.5" role="group" aria-label="이벤트 종류 필터">
        {entries.map(([type, { color, koLabel }]) => {
          const active = types.includes(type)
          return (
            <button
              key={type}
              type="button"
              aria-pressed={active}
              onClick={() => onTypes(active ? types.filter((t) => t !== type) : [...types, type])}
              style={active ? { borderColor: color, color } : undefined}
              className={cn(
                'focus-visible:ring-ring flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs transition-colors',
                'focus-visible:ring-2 focus-visible:outline-none',
                active
                  ? 'bg-muted font-semibold'
                  : 'text-muted-foreground hover:bg-muted/60 border-transparent font-medium',
              )}
            >
              <span
                className="inline-block size-2 shrink-0 rounded-sm"
                style={{ backgroundColor: color }}
                aria-hidden="true"
              />
              {koLabel}
            </button>
          )
        })}
      </div>
      <div className="flex shrink-0 gap-1">
        {SORT_OPTIONS.map((o) => (
          <button
            key={o.value}
            type="button"
            aria-pressed={sort === o.value}
            onClick={() => onSort(o.value)}
            className={cn(
              'focus-visible:ring-ring rounded-md px-3 py-1 text-sm transition-colors',
              'focus-visible:ring-2 focus-visible:outline-none',
              sort === o.value
                ? 'bg-primary text-primary-foreground font-semibold'
                : 'bg-muted text-foreground hover:bg-muted/60 font-medium',
            )}
          >
            {o.label}
          </button>
        ))}
      </div>
    </div>
  )
}

/*
  ImpactResults — its OWN error scope, HONEST about every outcome (D-03/D-09): it reads
  ApiError.status and shows distinct copy for 404 (unknown item) / 200-empty (no events, D-11) — each
  with its own next action. Copy is verbatim from 10-UI-SPEC; the backend occurred_at-desc order is
  preserved.

  The 400 branch that used to live here (window ≤0 or >168, with a "24시간으로 보기" reset button) is
  GONE with the window picker: the window is now the constant 24, which the backend always accepts, so
  a 400 is unreachable. Keeping it would be dead code claiming an outcome that cannot happen, and its
  button would "reset" 24 to 24. A genuine 400 (contract change) still surfaces via ErrorState below.
*/
function ImpactResults({
  itemId,
  window,
  types,
  sort,
  limit,
  onMore,
}: {
  itemId: number
  window: number
  types: EventType[]
  sort: EventImpactSort
  limit: number
  onMore: () => void
}) {
  const impact = useEventImpact(itemId, window, { types, sort, limit })

  if (impact.status === 'pending') {
    return (
      <div className="space-y-3" role="status" aria-busy="true">
        <Skeleton className="h-[320px] w-full" />
        <p className="text-muted-foreground text-sm">불러오는 중…</p>
      </div>
    )
  }

  if (impact.status === 'error') {
    const status = impact.error instanceof ApiError ? impact.error.status : null
    if (status === 404) {
      return (
        <Alert className="max-w-md">
          <PackageSearch />
          <AlertTitle>존재하지 않는 품목이에요</AlertTitle>
          <AlertDescription>
            선택한 품목을 찾을 수 없습니다. 위 목록에서 다른 품목을 선택해 주세요.
          </AlertDescription>
        </Alert>
      )
    }
    // network/other — inherited shared error copy ('백엔드에 연결하지 못했어요…').
    return <ErrorState onRetry={() => impact.refetch()} />
  }

  const { events, totalCount } = impact.data

  // D-11 / 17-01 D-04 Impact: 200 + empty events (admin registered 0 events) — distinct from per-row
  // insufficient_data (handled inside EventImpactTable/Cards, untouched).
  if (events.length === 0) {
    return (
      <div className="flex flex-col items-center gap-4 py-16 text-center">
        <h2 className="text-xl font-semibold">등록된 이벤트가 없어요</h2>
        <p className="text-muted-foreground max-w-md text-base">
          이벤트 전후 가격 비교에는 더 많은 데이터·이벤트가 필요합니다. 관리자가 게임 이벤트를 등록하고 수집이 쌓이면 여기에 전후 변화가 표시됩니다.
        </p>
      </div>
    )
  }

  // Both responsive views render; CSS (md breakpoint) toggles which is visible (D-04/D-09).
  const hasMore = events.length < totalCount
  return (
    <>
      <EventImpactTable events={events} />
      <EventImpactCards events={events} />
      {/*
        잘려 있다는 사실을 숨기지 않는다. 전체 건수는 서버가 totalCount로 알려주므로(필터 적용 후,
        limit 적용 전) "전체 N건 중 M건"이 실제 값이다. 다 보고 있을 땐 굳이 분수로 말하지 않는다.
      */}
      <div className="flex flex-col items-center gap-3 pt-2">
        <p className="text-muted-foreground text-sm tabular-nums">
          {hasMore
            ? `전체 ${totalCount.toLocaleString('ko-KR')}건 중 ${events.length.toLocaleString('ko-KR')}건`
            : `${totalCount.toLocaleString('ko-KR')}건`}
        </p>
        {hasMore ? (
          <Button variant="outline" onClick={onMore} disabled={impact.isFetching}>
            {impact.isFetching ? '불러오는 중…' : `더 보기 (+${LIMIT_STEP})`}
          </Button>
        ) : null}
      </div>
    </>
  )
}
