---
quick_id: 260630-gct
slug: latestpricecard
status: complete
date: 2026-06-30
files_modified:
  - frontend/src/features/_shared/LatestPriceCard.tsx
---

# Quick 260630-gct: LatestPriceCard 가로 레이아웃 Summary

**Timeline·Impact 공유 `LatestPriceCard`를 세로 2단에서 가로 한 줄로 변경 — [아이콘+이름+배지] → 골드 → 시간을 모든 요소 동일 gap(24px)으로 균등 배치, 카드 폭은 내용맞춤(w-fit). playwright 실측으로 가로 한 줄·균등 gap·잘림0 검증.**

## 결정 (AskUserQuestion 확정)

모든 요소 동일 gap 균등 배치(justify-between 아님), 카드 폭 내용맞춤(w-fit), 골드·시간도 가로 나란히.

## 수정 (`frontend/src/features/_shared/LatestPriceCard.tsx`)

- `CardHeader`/`CardTitle` 제거 → `CardContent` 하나에 `flex items-center gap-6`.
- 정체성(아이콘+이름+배지)은 inner `flex gap-2` 한 덩어리; 골드·시간은 success **fragment**로 렌더.
  `AsyncBoundary`가 `<>{children}</>`(DOM 노드 없음)를 반환 → 골드·시간 `<p>`가 CardContent flex의
  직계 자식이 되어 정체성↔골드↔시간 gap이 모두 동일(gap-6 = 24px).
- `min-w-56`·`[container-type:normal]` 제거(CardContent엔 @container 없어 불필요), `w-fit` 유지.
- 각 텍스트 `whitespace-nowrap`. `AsyncBoundary` 격리 유지. `ItemCard`(dashboard) 무변경.

## 검증

- `npm run build`(tsc -b && vite build) **그린**.
- playwright(5173 dev, item=7 "유물 저주받은 인형 각인서" 38,000G):
  - CardContent 직계 자식 3개: `DIV.flex`(정체성)·`P.text-xl`(골드)·`P.text-muted-foreground`(시간).
  - **sameRow=true**(top 256/258/262, items-center) — 가로 한 줄.
  - **flexGap=24px**, 정체성→골드 24px·골드→시간 24px — **균등 gap 확인**.
  - cardWidth 630 ≈ scrollW 628, **clipped=false** — 잘림 0.

## 결과

가로 한 줄로 [아이콘·이름·배지] · 골드 · 시간이 균등 간격 배치되어 어색함 없이 정렬. 긴 각인서
이름에서도 가로 유지. Timeline·Impact 동시 적용(공유 컴포넌트 1곳). 데이터·계약·백엔드 무변경.
