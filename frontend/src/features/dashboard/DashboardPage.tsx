import { useMemo, useState } from 'react'

import { useGems, useItems } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { formatKst } from '@/lib/formatKst'
import { sortByRole } from '@/features/_shared/roleGroup'
import type { GemPrice } from '@/lib/schemas'

import { CategoryNav } from './CategoryNav'
import { deriveCategories, filterByCategory, firstCategoryId, GEM_CATEGORY_ID } from './categories'
import { GemCard } from './GemCard'
import { ItemCard } from './ItemCard'
import { ItemImpactSection } from './ItemImpactSection'
import { NewsPanel } from './NewsPanel'

/*
  DashboardPage — the client dashboard, a maplanet-style 3-column layout (Phase 23, UX-01/UX-02):
  좌 CategoryNav(카테고리 필터) / 중앙 선택 카테고리 물품 / 우 NewsPanel. useItems() is client-sorted
  by role via sortByRole (backend stays 0-line); deriveCategories groups the sorted items into
  non-empty leaves (각인서 딜러/서포터 + 재료 4 itemGroup — categories.ts is the single source). Picking a
  leaf filters the center list; selection is local useState only (A4 — YAGNI, no URL/router state).

  Phase 28 (DASH-01): 보석 joins as a STANDALONE leaf and the /gems page is gone. Gems are not
  TrackedItems — different API (useGems), no Id, no time series — so filterByCategory cannot reach
  them; the center swaps its data source on GEM_CATEGORY_ID instead. Each source keeps its OWN
  AsyncBoundary so a gem failure never blanks the items and vice-versa (D-07).

  Phase 28 (DASH-02/DASH-03): the /impact page is gone too — clicking an item card now selects it and
  renders that item's event-impact directly BELOW the list, so you never navigate away from the item
  you were already looking at. The list itself lives in a fixed-height scroll box on lg+ so the center
  column lines up with the news panel and the impact section stays visible without scrolling past 11
  cards. Both new pieces of state are local useState (A4 YAGNI, same as the category).

  The center item list and the news panel each keep their OWN AsyncBoundary so one side failing never
  blanks the other (D-07, preserved from the 2-column version). NewsPanel is UNCHANGED.
  Mobile (< lg) collapses to one column in source order: 칩 탭 → 물품 → 이벤트 영향 → 소식, and the
  scroll box drops its fixed height (a nested scroller fights the page scroll on a phone).
  Read-only: no write UI, no polling.
*/

/*
  물품 카드 박스 높이 = 416px(26rem). 사용자 요청은 "우측 소식 박스 위 선 ~ 첫 진행중 이벤트 카드
  아래"였고 라이브 실측이 417px이었다(2026-07-15). 그 지점을 CSS로 추적할 수는 없다 — 소식 패널 맨
  위가 쿠폰이라 관리자가 쿠폰을 6개 등록하면 그 지점이 ~637px로 밀리고, "옆 컬럼 특정 요소의 아래"를
  가리키는 CSS는 없다(subgrid로도 다른 컬럼 내부 요소는 못 잡는다). JS 측정은 리사이즈·데이터 변경마다
  재측정하는 레이아웃 코드를 부른다. 고정값이라 쿠폰 수가 바뀌면 어긋나지만, 어긋남은 우측 패널
  안에서만 보이고 중앙 박스 높이는 항상 같아 화면이 흔들리지 않는다 (사용자 승인 2026-07-15).
*/
const ITEM_BOX_HEIGHT = 'lg:h-[26rem] lg:overflow-y-auto'
export function DashboardPage() {
  const { status, data, refetch } = useItems()
  const gemsQuery = useGems()

  const sorted = useMemo(() => (data ? sortByRole(data) : []), [data])
  const gems = gemsQuery.data?.gems ?? []
  const categories = useMemo(() => deriveCategories(sorted, gems.length), [sorted, gems.length])

  // Local selection; default/fallback = first non-empty leaf. Deriving the EFFECTIVE id each render
  // (rather than syncing with useEffect) self-heals when a data change removes the picked category —
  // no stale or empty selection can persist.
  const [picked, setPicked] = useState<string | null>(null)
  const selectedId =
    picked && categories.some((c) => c.id === picked) ? picked : firstCategoryId(categories)

  // 이벤트 영향 대상. 기본은 없음 — 카드를 눌러야 뜬다.
  const [selectedItemId, setSelectedItemId] = useState<number | null>(null)
  const [impactWindow, setImpactWindow] = useState(24)

  const isGems = selectedId === GEM_CATEGORY_ID
  const visible = selectedId && !isGems ? filterByCategory(sorted, selectedId) : []
  const selectedItem = sorted.find((i) => i.id === selectedItemId)

  /*
    카테고리를 바꾸면 선택을 버린다. 안 그러면 재료를 보고 있는데 아래엔 각인서의 이벤트 영향이 남아
    "무엇의 영향인지"가 화면과 어긋난다 — 선택한 카드가 목록에 보이지도 않는 채로.
  */
  function pickCategory(id: string) {
    setPicked(id)
    setSelectedItemId(null)
  }

  return (
    <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 lg:grid-cols-[11rem_minmax(0,1fr)_20rem]">
      {/* 좌(lg) / 상단(모바일) — 카테고리 필터. 로딩 중엔 leaf가 없어 자연 축소. */}
      <CategoryNav
        categories={categories}
        items={sorted}
        selectedId={selectedId}
        onSelect={pickCategory}
      />

      {/* 중앙 — [물품/보석 스크롤 박스] + [선택 물품의 이벤트 영향]. */}
      <div className="flex min-w-0 flex-col gap-6">
        {isGems ? (
          <GemSection gems={gems} query={gemsQuery} />
        ) : (
          <>
            <div className={ITEM_BOX_HEIGHT}>
              <AsyncBoundary
                status={status}
                isEmpty={(data?.length ?? 0) === 0}
                onRetry={() => refetch()}
              >
                <div className="space-y-3">
                  {visible.map((item) => (
                    <ItemCard
                      key={item.id}
                      item={item}
                      selected={item.id === selectedItemId}
                      onSelect={setSelectedItemId}
                    />
                  ))}
                </div>
              </AsyncBoundary>
            </div>

            {/* 보석 카테고리에선 렌더조차 안 한다 — 보석은 선택이 불가능하므로 안내 문구도 거짓이 된다. */}
            {selectedItemId != null ? (
              <ItemImpactSection
                itemId={selectedItemId}
                displayName={selectedItem?.displayName ?? ''}
                window={impactWindow}
                onWindowChange={setImpactWindow}
              />
            ) : (
              <p className="text-muted-foreground text-sm">
                물품을 선택하면 이벤트 전후 시세 변화가 여기에 표시됩니다.
              </p>
            )}
          </>
        )}
      </div>

      {/* 우(lg) / 하단(모바일) — 로아 소식. 물품과 독립 boundary(한쪽 실패가 다른 쪽 안 가림, D-07). */}
      <NewsPanel />
    </div>
  )
}

/*
  보석 블록. 헤딩이 무엇을 재는 값인지 말하므로 카드는 숫자만 말한다.

  "1시간마다 갱신"이라고 쓰지 않는다: Phase 27이 시간당 기록 폴러를 켰지만 그건 DB에만 쓴다. 이 화면이
  읽는 건 여전히 온디맨드 캐시라 아무도 안 보면 갱신되지 않는다 — 주기를 약속하면 거짓이 된다.
  대신 기준 시각을 노출해 언제 기준 값인지 정직하게 알린다.
*/
function GemSection({ gems, query }: { gems: GemPrice[]; query: ReturnType<typeof useGems> }) {
  const updatedAt = query.data?.updatedAt
  return (
    <section className="space-y-3">
      <div className="space-y-0.5">
        <h2 className="text-base font-semibold">현재 보석 시세값</h2>
        {updatedAt ? (
          <p className="text-muted-foreground text-sm">기준 시각 {formatKst(updatedAt)}</p>
        ) : null}
      </div>
      <AsyncBoundary
        status={query.status}
        isEmpty={gems.length === 0}
        onRetry={() => query.refetch()}
        emptyHeading="보석 시세를 불러오지 못했어요"
        emptyBody="잠시 후 다시 시도해 주세요."
      >
        <div className="space-y-3">
          {gems.map((gem) => (
            <GemCard key={`${gem.series}-${gem.level}`} gem={gem} />
          ))}
        </div>
      </AsyncBoundary>
    </section>
  )
}
