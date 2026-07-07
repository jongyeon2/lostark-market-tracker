---
quick_id: 260707-usn
slug: dashboard-card-price-label
date: 2026-07-07
type: ui-copy
tags: [frontend, dashboard, copy, terminology]
---

# Quick Task 260707-usn: 대시보드 카드 가격 용어 차트와 통일

## Problem
대시보드 카드(`ItemCard`)는 가격(`minPrice`)을 라벨 없이 🪙 숫자로만 보여주고, 빈 상태 문구엔 "최신가"를 쓴다. 차트는 이제 같은 지표를 "최저가"로 부른다(quick 260707-uly) → 앱 전반 용어 불일치. `minPrice` = 현재 최저 판매가 = 차트 "최저가"와 동일 지표.

## Fix
`ItemCard`의 가격 용어를 차트와 통일:
1. 가격 숫자에 **"최저가" 라벨** 추가(🪙 앞, muted 소형) → 고객이 이 숫자가 최저가임을 인지, 차트와 일치.
2. 빈 상태 **"최신가 수집 중" → "최저가 수집 중"** + 관련 주석 정리.

범위: `ItemCard.tsx`만. `LatestPriceCard`(타임라인, 단일 flex 행 레이아웃 민감)는 이번 제외 — 필요 시 후속.

## Tasks

### Task 1 — ItemCard 가격 라벨 + 빈 상태 문구 통일
- **files:** `frontend/src/features/dashboard/ItemCard.tsx`
- **action:** success 가격 라인에 "최저가" muted 라벨 추가(🪙·숫자 앞). error 빈 상태 `'최신가 수집 중'`→`'최저가 수집 중'`. 상단/인라인 주석의 '최신가 수집 중' 표기도 동기화. 레이아웃(py-3 compact 행)·링크·health 로직 무변경.
- **verify:** `cd frontend && npm run build` 그린.
- **done:** 대시보드 카드가 "최저가" 용어로 차트와 통일.

## Guard
백엔드·수집/캐시/serving·Core Value 무변경. 프론트 표시 문자열만 변경.
