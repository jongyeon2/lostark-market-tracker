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
import { NewsPanel, NoticeRail } from './NewsPanel'

/*
  DashboardPage — the client dashboard, a maplanet-style 3-column layout (Phase 23, UX-01/UX-02):
  좌 CategoryNav(카테고리 필터)+공지 레일(lg) / 중앙 선택 카테고리 물품 / 우 쿠폰·진행 이벤트. useItems() is client-sorted
  by role via sortByRole (backend stays 0-line); deriveCategories groups the sorted items into
  non-empty leaves (각인서 딜러/서포터 + 재료 4 itemGroup — categories.ts is the single source). Picking a
  leaf filters the center list; selection is local useState only (A4 — YAGNI, no URL/router state).

  Phase 28 (DASH-01): 보석 joins as a STANDALONE leaf and the /gems page is gone. Gems are not
  TrackedItems — different API (useGems), no Id, no time series — so filterByCategory cannot reach
  them; the center swaps its data source on GEM_CATEGORY_ID instead. Each source keeps its OWN
  AsyncBoundary so a gem failure never blanks the items and vice-versa (D-07).

  Phase 28 (DASH-02/DASH-03): the /impact page is gone too — clicking an item card now selects it and
  renders that item's event-impact directly BELOW the list, so you never navigate away from the item
  you were already looking at. The list itself lives in a height-CAPPED scroll box on lg+ (see
  ITEM_BOX_MAX_HEIGHT) so the impact section stays visible without scrolling past 11 cards, while a
  short category still shrinks to its content instead of trailing blank space. Both new pieces of
  state are local useState (A4 YAGNI, same as the category).

  The center item list and each news widget keep their OWN AsyncBoundary so one side failing never
  blanks the other (D-07, preserved from the 2-column version). 공지사항은 좌측 레일(NoticeRail)로
  옮겼고(2026-07-19), 우측 NewsPanel엔 제목 없이 쿠폰+진행 이벤트만 남는다.
  Mobile (< lg) collapses to one column in source order: 칩 탭 → 물품 → 이벤트 영향 → 쿠폰·이벤트 → 공지,
  and the scroll box drops its height cap (a nested scroller fights the page scroll on a phone).
  Read-only: no write UI, no polling.
*/

/*
  물품 카드 박스의 높이 상한 = 628px = 카드 8장 + 그 사이 간격 7개 (8×68 + 7×12, 실측 2026-07-16).
  6장(468px)은 답답하다는 피드백으로 8장까지 늘렸다.

  🔑 고정 높이(h-)가 아니라 상한(max-h-)이다 — 사용자 결정 2026-07-16. 카테고리별 물품 수가
  11/7/11/8/6/6이라 628px로 고정하면 6개짜리(재련보조·아크그리드젬)는 아래 160px가 빈 채로 남는다.
  상한으로 두면 8장 이상일 때만 628px에서 스크롤이 시작되고, 그 미만이면 내용만큼 줄어 빈칸이 없다.
  대가는 카테고리를 바꿀 때 아래 이벤트 영향의 세로 위치가 움직이는 것(수용됨) — 어차피 목록 전체가
  바뀌는 순간이다.

  옆 소식 패널 높이에 맞추지 않는다: 그 지점은 관리자가 등록한 쿠폰 수에 따라 움직이지만
  (쿠폰 1개=417px / 2개=469px 실측) 카드 높이는 고정이라 이쪽 배수가 안 흔들린다.
  rem(39.25rem) 대신 px로 두는 건 저 계산식이 그대로 읽히기 때문이다.
*/
const ITEM_BOX_MAX_HEIGHT = 'lg:max-h-[628px] lg:overflow-y-auto'
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

  const isGems = selectedId === GEM_CATEGORY_ID
  const visible = selectedId && !isGems ? filterByCategory(sorted, selectedId) : []

  /*
    카테고리를 바꾸면 선택을 버린다. 안 그러면 재료를 보고 있는데 아래엔 각인서의 이벤트 영향이 남아
    "무엇의 영향인지"가 화면과 어긋난다 — 선택한 카드가 목록에 보이지도 않는 채로.
  */
  function pickCategory(id: string) {
    setPicked(id)
    setSelectedItemId(null)
  }

  return (
    // 자체 max-w 없음 — 폭은 AppLayout <main>이 소유한다. 여기에도 max-w를 두면 같은 매직넘버가
    // 두 곳이 되고, 실제로 그래서 껍데기만 넓혔을 때 그리드가 옛 값에서 다시 잘렸다(2026-07-15).
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-[20rem_minmax(0,1fr)_20rem] lg:gap-12">
      {/* 좌(lg) / 상단(모바일) — 카테고리와 공지를 각각 별개의 흰 카드로 나눈다(사용자 결정 2026-07-20:
          둘을 한 박스에 넣으니 불편 → 박스 분리 + 구분선 제거). 폭·패딩은 우측 소식 박스와 동일(20rem·p-6).
          카드 스타일은 lg:*로만 걸어 모바일은 카드 없이 칩 행이 노출되고, 공지는 NewsPanel 하단 카드로
          내려간다. 두 카드를 한 sticky 블록(space-y-6)으로 묶어 긴 본문 스크롤에도 좌측이 통째로 머문다.
          top-20(80px)=TopNav 72px+여백8, self-start라야 셀이 행 높이로 늘지 않아 sticky가 동작한다. */}
      <div className="lg:sticky lg:top-20 lg:self-start lg:space-y-6">
        {/* 카테고리 카드 */}
        <div className="lg:bg-card lg:rounded-xl lg:border lg:px-6 lg:py-6 lg:shadow-sm">
          <CategoryNav
            categories={categories}
            items={sorted}
            selectedId={selectedId}
            onSelect={pickCategory}
          />
        </div>
        {/* 공지 카드 — 카테고리와 별개 박스. lg 전용(모바일은 NewsPanel 하단 카드). */}
        <div className="hidden lg:block lg:bg-card lg:rounded-xl lg:border lg:px-6 lg:py-6 lg:shadow-sm">
          <NoticeRail />
        </div>
      </div>

      {/* 중앙 — [물품/보석 스크롤 박스] + [선택 물품의 이벤트 영향]. */}
      <div className="flex min-w-0 flex-col gap-6">
        {isGems ? (
          <GemSection gems={gems} query={gemsQuery} />
        ) : (
          <>
            <div className={ITEM_BOX_MAX_HEIGHT}>
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
              <ItemImpactSection itemId={selectedItemId} />
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
