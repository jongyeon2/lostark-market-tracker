import { useState } from 'react'

import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { MarketSearchItem } from '@/lib/schemas'

import { gradeBadgeClass } from './grades'

/*
  거래소 검색 결과 목록 (아바타·모험의 서 공용). 한 행 = [아이콘][이름·등급][최저가·최근거래가].
  GemCard의 규율을 따른다: 가격이 없으면(즉시구매 매물 없음) 값을 지어내지 않고 "즉시구매 매물 없음"으로
  정직히 표기한다(currentMinPrice=null). 클릭 불가 — 이 품목들은 시계열을 저장하지 않아 열 곳이 없다.

  등급은 색으로도 말한다(사용자 결정 2026-07-20, 이전 "신규 색 0" 방침을 뒤집음). 로아 유저는 거래소에서
  초록/파랑/보라/주황을 등급으로 즉시 읽는데, 흑백 outline 배지 하나로 통일하는 건 유저가 이미 아는
  신호를 버리는 쪽이었다. 단 색은 **배지에만** 넣는다 — 게임처럼 이름까지 칠해봤더니 행마다 본문
  색이 달라져 목록이 산만했다(라이브 QA). 이름은 전부 기본 본문색.
  색은 라벨의 보조다 — 등급 문자열은 색과 무관하게 늘 함께 뜬다(색 하나에 의존 금지).
  색 값과 게임 색을 그대로 못 쓰는 이유는 index.css의 --grade-* 주석, 매핑은 grades.ts.
*/
export function MarketResultList({
  items,
  // 목록 배치는 호출부가 정한다. 모험의 서는 좁은 1열(max-w-3xl)이라 기본값이 맞고, 아바타는 넓은
  // 폭을 2열로 쓴다 — 행 자체는 양쪽이 같아야 하므로 컴포넌트를 쪼개는 대신 컨테이너만 열어둔다.
  className = 'space-y-2',
}: {
  items: MarketSearchItem[]
  className?: string
}) {
  return (
    <ul className={className}>
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
  // 모르는 등급이면 색 없이 기본 outline 그대로 — 자세한 이유는 grades.ts.
  const badgeClass = gradeBadgeClass(item.grade)
  return (
    <Card className="py-3">
      <CardContent className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-2">
          <MarketIcon iconUrl={item.iconUrl} />
          <span className="min-w-0 truncate font-medium">{item.name}</span>
          <Badge variant="outline" className={cn('shrink-0', badgeClass)}>
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
