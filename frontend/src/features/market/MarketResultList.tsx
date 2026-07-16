import { useState } from 'react'

import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import type { MarketSearchItem } from '@/lib/schemas'

/*
  거래소 검색 결과 목록 (아바타·모험의 서 공용). 한 행 = [아이콘][이름·등급][최저가·최근거래가].
  GemCard의 규율을 따른다: 가격이 없으면(즉시구매 매물 없음) 값을 지어내지 않고 "즉시구매 매물 없음"으로
  정직히 표기한다(currentMinPrice=null). 클릭 불가 — 이 품목들은 시계열을 저장하지 않아 열 곳이 없다.

  로스트아크 등급 색은 이 앱의 디자인 토큰(slate 기반)에 없다. 새 색을 만들지 않고 Badge outline 하나로
  통일한다 — 등급 "이름"은 보이되(전설/영웅…) 게임의 등급별 색을 재현하지는 않는다(신규 색 0 원칙).
*/
export function MarketResultList({ items }: { items: MarketSearchItem[] }) {
  return (
    <ul className="space-y-2">
      {/* key에 index를 섞는다 — 같은 id가 한 페이지에 여러 번 올 수 있다(같은 아이템을 서로 다른
          가격에 여러 명이 등록). id만으로는 React 키가 충돌한다(실측: 아바타 무기에서 동일 id 중복). */}
      {items.map((item, i) => (
        <li key={`${item.id}-${i}`}>
          <MarketRow item={item} />
        </li>
      ))}
    </ul>
  )
}

function MarketRow({ item }: { item: MarketSearchItem }) {
  return (
    <Card className="py-3">
      <CardContent className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-2">
          <MarketIcon iconUrl={item.iconUrl} />
          <span className="min-w-0 truncate font-medium">{item.name}</span>
          <Badge variant="outline" className="shrink-0">
            {item.grade}
          </Badge>
        </div>
        <div className="shrink-0 space-y-0.5 text-right">
          <PriceLine label="최저가" value={item.currentMinPrice} emphasize />
          <PriceLine label="최근" value={item.recentPrice} />
        </div>
      </CardContent>
    </Card>
  )
}

/*
  가격 한 줄. null이면 값을 지어내지 않는다(0골드도, 마지막 값도 아니다 — GemCard 선례).
  최저가는 강조(기본 크기), 최근거래가는 보조(작게·회색).
*/
function PriceLine({
  label,
  value,
  emphasize = false,
}: {
  label: string
  value: number | null
  emphasize?: boolean
}) {
  if (value === null) {
    if (!emphasize) return null // 최근거래가는 없으면 줄 자체를 숨긴다(최저가만 "매물 없음"을 말한다)
    return <p className="text-muted-foreground text-sm">즉시구매 매물 없음</p>
  }
  return (
    <p
      className={
        emphasize
          ? 'flex items-center justify-end gap-1 text-base font-medium tabular-nums'
          : 'flex items-center justify-end gap-1 text-xs tabular-nums text-muted-foreground'
      }
    >
      <span className="text-muted-foreground text-xs font-normal">{label}</span>
      <span aria-hidden="true">🪙</span>
      {value.toLocaleString('ko-KR')}
    </p>
  )
}

/* 고정 슬롯 아이콘 — GemIcon 선례(shift-0, onError 시 슬롯만 유지하고 조용히 비움). */
function MarketIcon({ iconUrl }: { iconUrl: string }) {
  const [failed, setFailed] = useState(false)
  return (
    <span className="bg-muted flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-sm">
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
