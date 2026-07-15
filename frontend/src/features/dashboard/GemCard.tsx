import { useState } from 'react'

import { Card, CardContent } from '@/components/ui/card'
import type { GemPrice } from '@/lib/schemas'

/*
  GemCard — one 티어4 보석 as a card in the dashboard's center column (Phase 28, DASH-01).

  NOT clickable, and that is not a styling choice. 경매장 gives no Id and carries no history
  (Phase 24 §H5), so a gem has no timeline and no event-impact to open — there is nowhere to go.
  ItemCard's affordances (hover elevation, cursor-pointer, the whole card being a <button>/<Link>)
  would promise a destination that does not exist. So this is a plain <div> that inherits ItemCard's
  LAYOUT (icon+name left / price right) and none of its interaction.

  Why not reuse ItemCard with a flag: that would make the item card carry a condition about gems it
  has nothing to do with. Two shapes, two components.

  Why the name says the series ("8레벨 겁화") when the backend's displayName is just "8레벨": Phase 26
  put 계열 in a group heading above the rows, so the row only had to say the level. Here the cards are a
  flat list under one 보석 category (사용자 결정 2026-07-15 — no level sub-categories), so each card must
  name its own gem. The backend contract is UNCHANGED — series and level are both already in the
  response, the frontend just composes them.
*/

/*
  가격 없는 세 경우는 전부 카드 단위 한 줄로 끝난다 — 화면 전체 에러가 아니다(ItemCard가 404를
  카드 수준 한 줄로 처리한 것과 같은 규율). 값을 지어내지 않는다: 0골드도, 마지막 값 재사용도 없다.
*/
const NO_PRICE_COPY: Record<Exclude<GemPrice['status'], 'OK'>, string> = {
  NO_BUYOUT: '즉시구매 매물 없음',
  // 고장이 아니라 수집(Core Value)에 예산을 양보한 것 — 곧 회복되므로 재시도를 안내한다.
  RATE_LIMITED: '잠시 후 다시',
  FETCH_FAILED: '불러오지 못함',
}

export function GemCard({ gem }: { gem: GemPrice }) {
  return (
    <Card className="py-3">
      <CardContent className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-2">
          <GemIcon iconUrl={gem.iconUrl} />
          <span className="truncate font-medium">
            {gem.level}레벨 {gem.series}
          </span>
        </div>
        <div className="shrink-0 text-right">
          <GemPriceCell gem={gem} />
        </div>
      </CardContent>
    </Card>
  )
}

function GemPriceCell({ gem }: { gem: GemPrice }) {
  if (gem.status === 'OK') {
    // status=OK인데 가격이 null이면 백엔드 계약 위반이다(둘은 함께 움직여야 한다). 일어나선 안 되지만,
    // 일어나도 빈 칸이나 크래시가 아니라 없다고 말한다 — 화면은 계약을 신뢰하되 의존하지는 않는다.
    if (gem.minBuyPrice === null) {
      return <p className="text-muted-foreground text-sm">{NO_PRICE_COPY.FETCH_FAILED}</p>
    }
    return (
      <p className="flex items-center justify-end gap-1 text-base font-medium tabular-nums">
        <span className="text-muted-foreground text-xs font-normal">즉시구매</span>
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
  넣으려면 공용 컴포넌트에 보석 분기를 더해 기존 화면을 오염시켜야 한다. 보석 아이콘 6종은 로아 CDN에
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
