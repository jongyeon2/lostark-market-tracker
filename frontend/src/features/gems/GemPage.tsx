import { useState } from 'react'

import { useGems } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { formatKst } from '@/lib/formatKst'
import type { GemPrice } from '@/lib/schemas'

/*
  GemPage — 티어4 보석 현재가 (Phase 26, GEM-02). 별도 페이지인 것도, 행이 링크가 아닌 것도 취향이
  아니라 데이터의 성질에서 나온다(Phase 24 실측):

  - 경매장 응답엔 Id가 없고 보석은 시계열로 기록하지 않는다 → 갈 목적지가 없다. ItemCard는 카드 전체가
    /timeline?item={id} 링크지만(CARD-02), 여기서 그걸 흉내내면 눌리게 생겼는데 안 눌리는 가짜
    어포던스가 된다. 그래서 <div> 행이고 hover elevation·cursor-pointer가 없다.
  - 가격은 백엔드 Redis 스냅샷이다(경매장이 10분 수집과 레이트리밋 버킷을 공유하므로 캐시 필수).
    그래서 "5분마다 갱신"이라고 말하지 않는다 — 온디맨드 캐시라 아무도 안 보면 갱신되지 않는다.
    대신 기준 시각을 노출해 언제 기준 값인지 정직하게 알린다(UI-SPEC §갱신 시각).
  - 6종은 항상 6행. 가격이 없으면 행을 숨기지 않고 없다고 말한다(0골드·마지막 값 재사용 금지).
*/

const SERIES_LABEL: Record<string, string> = {
  겁화: '겁화 (딜러)',
  작열: '작열 (서포터)',
}

export function GemPage() {
  const { status, data, refetch } = useGems()
  const gems = data?.gems ?? []
  // 계열 등장 순서를 백엔드 카탈로그 순서(겁화 → 작열)에서 그대로 받는다 — 프론트가 순서를 재발명하지 않는다.
  const seriesOrder = [...new Set(gems.map((g) => g.series))]

  return (
    <Card className="max-w-3xl">
      <CardHeader>
        <CardTitle className="text-xl">티어4 보석 현재가</CardTitle>
        <p className="text-muted-foreground text-sm">
          최저 즉시구매가
          {data?.updatedAt ? ` · 기준 시각 ${formatKst(data.updatedAt)}` : null}
        </p>
      </CardHeader>
      <CardContent>
        <AsyncBoundary
          status={status}
          isEmpty={gems.length === 0}
          onRetry={() => refetch()}
          emptyHeading="보석 시세를 불러오지 못했어요"
          emptyBody="잠시 후 다시 시도해 주세요."
        >
          <div className="space-y-6">
            {seriesOrder.map((series) => (
              <GemGroup key={series} series={series} gems={gems.filter((g) => g.series === series)} />
            ))}
          </div>
        </AsyncBoundary>
      </CardContent>
    </Card>
  )
}

function GemGroup({ series, gems }: { series: string; gems: GemPrice[] }) {
  return (
    <section className="space-y-1">
      <h2 className="border-border text-muted-foreground border-b px-2 pb-1.5 text-sm font-semibold tracking-wide">
        {SERIES_LABEL[series] ?? series}
      </h2>
      <ul>
        {gems.map((gem) => (
          <li key={`${gem.series}-${gem.level}`}>
            <GemRow gem={gem} />
          </li>
        ))}
      </ul>
    </section>
  )
}

/*
  한 보석 행. 링크가 아니다 — 위 주석 참조. ItemCard의 가로 배치(아이콘+이름 좌 / 가격 우)만 승계하고
  상호작용은 승계하지 않는다.
*/
function GemRow({ gem }: { gem: GemPrice }) {
  return (
    <div className="flex items-center justify-between gap-4 px-2 py-3">
      <div className="flex min-w-0 items-center gap-2">
        <GemIcon iconUrl={gem.iconUrl} />
        <span className="truncate font-medium">{gem.displayName}</span>
      </div>
      <div className="shrink-0 text-right">
        <GemPriceCell gem={gem} />
      </div>
    </div>
  )
}

/*
  가격 자리. status와 minBuyPrice가 함께 움직이므로(백엔드 계약) null이 0인 척할 수 없다.
  가격 없는 세 경우는 전부 행 수준 한 줄로 끝난다 — 화면 전체 에러가 아니다(ItemCard가 404를
  카드 수준 한 줄로 처리한 것과 같은 규율).
*/
const NO_PRICE_COPY: Record<Exclude<GemPrice['status'], 'OK'>, string> = {
  NO_BUYOUT: '즉시구매 매물 없음',
  // 고장이 아니라 수집(Core Value)에 예산을 양보한 것 — 곧 회복되므로 재시도를 안내한다.
  RATE_LIMITED: '잠시 후 다시',
  FETCH_FAILED: '불러오지 못함',
}

function GemPriceCell({ gem }: { gem: GemPrice }) {
  if (gem.status === 'OK') {
    // status=OK인데 가격이 null이면 백엔드 계약 위반이다(둘은 함께 움직여야 한다). 일어나선 안 되지만,
    // 일어나도 빈 칸이나 크래시가 아니라 없다고 말한다 — 화면은 계약을 신뢰하되 의존하지는 않는다.
    if (gem.minBuyPrice === null) {
      return <p className="text-muted-foreground text-sm">{NO_PRICE_COPY.FETCH_FAILED}</p>
    }
    // 라벨 없이 골드만 — 무엇을 재는 값인지는 카드 부제("최저 즉시구매가")가 이미 말한다. 행마다
    // 반복하면 6번 같은 말을 하는 셈이라 정작 비교해야 할 숫자를 가린다(레벨 행에서 계열을 뺀 것과 같은 이유).
    // ItemCard가 행마다 "최저가"를 붙이는 건 카드 단위 부제가 없어서다 — 사정이 다르다.
    return (
      <p className="flex items-center justify-end gap-1 text-base font-medium tabular-nums">
        <span aria-hidden="true">🪙</span>
        {gem.minBuyPrice.toLocaleString('ko-KR')}
      </p>
    )
  }
  return <p className="text-muted-foreground text-sm">{NO_PRICE_COPY[gem.status]}</p>
}

/*
  고정 슬롯 아이콘(ItemIcon 선례의 시프트-0 규율). ItemIcon 자체는 재사용하지 않는다 — 그 컴포넌트의
  폴백 글리프가 roleGroup(DEALER/SUPPORT/MATERIAL)에 묶여 있는데 보석엔 roleGroup이 없어서, 끼워
  넣으려면 공용 컴포넌트에 보석 분기를 더해 기존 3화면을 오염시켜야 한다. 보석 아이콘 6종은 로아 CDN에
  전부 distinct하게 실재하므로 글리프 폴백이 필요 없고, onError 시 슬롯만 유지한 채 조용히 비운다.
*/
function GemIcon({ iconUrl }: { iconUrl: string }) {
  const [failed, setFailed] = useState(false)
  return (
    <span className="bg-muted flex size-6 shrink-0 items-center justify-center overflow-hidden rounded-sm">
      {failed ? null : (
        <img
          src={iconUrl}
          alt=""
          loading="lazy"
          className="size-full object-contain"
          onError={() => setFailed(true)}
        />
      )}
    </span>
  )
}

