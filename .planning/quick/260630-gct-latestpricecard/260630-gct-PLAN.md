---
quick_id: 260630-gct
slug: latestpricecard
date: 2026-06-30
status: complete
---

# Quick 260630-gct: LatestPriceCard 가로 레이아웃

## 문제

Timeline·Impact 공유 `LatestPriceCard`가 세로 2단(헤더[아이콘+이름+배지] 위 / 가격·시간 아래)으로
보임. 사용자 요청: 가로 한 줄로 [아이콘+이름+배지] → 골드 → 시간 나열.

## 결정 (AskUserQuestion 확정)

- 모든 요소 **동일 gap 균등 배치**(justify-between 아님), 카드 폭 **내용맞춤(w-fit)**.
- 골드·시간도 가로로 나란히.

## 수정 (`frontend/src/features/_shared/LatestPriceCard.tsx`)

- `CardHeader`/`CardTitle` 제거 → `CardContent` 하나에 `flex items-center gap-6`.
- 정체성(아이콘+이름+배지)은 inner `flex gap-2` 한 덩어리; 골드·시간은 success **fragment**로 렌더.
  `AsyncBoundary`가 `<>{children}</>`(DOM 노드 없음)를 반환하므로 골드·시간 `<p>`가 CardContent flex의
  직계 자식이 되어 정체성↔골드↔시간 gap이 모두 동일(gap-6).
- `min-w-56`·`[container-type:normal]` 제거(CardContent엔 @container 없어 불필요), `w-fit` 유지.
- 각 텍스트 `whitespace-nowrap`로 줄바꿈 방지. `AsyncBoundary` 격리 유지(pending/error/success).
- `ItemCard`(dashboard) 무변경.

## 검증

- `npm run build`(tsc -b && vite build) 그린.
- playwright(5173 dev 실데이터): Timeline 카드가 가로 한 줄, 정체성-골드-시간 **동일 gap**,
  잘림 0, 행 높이 1줄. 긴 이름(유물 저주받은 인형 각인서)에서도 가로 유지.
