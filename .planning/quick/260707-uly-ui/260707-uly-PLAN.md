---
quick_id: 260707-uly
slug: ui
date: 2026-07-07
type: ui-copy
tags: [frontend, recharts, copy, favicon, customer-facing]
---

# Quick Task 260707-uly: 차트 라벨 순화 + 파비콘 교체

## Problem
- 차트 범례/툴팁 용어("백필·일평균(거래가)", "실측 최저호가(일별)")가 개발자 용어라 일반 고객(로아 유저)이 이해하기 어렵다.
- `index.html`에 favicon 링크가 없어 브라우저 기본 아이콘이 뜬다.

## Decisions (user, AskUserQuestion)
- 라벨: **간결형** — 주황(거래 평균) = "평균 거래가", 파랑(최저 호가) = "최저가". 툴팁도 동일. 두 지표 2라인 구분은 유지.
- 파비콘: **📈 상승 차트 이모지** (SVG 파일 + 링크).

## Tasks

### Task 1 — 차트 라벨 순화 (PriceTimelineChart)
- **files:** `frontend/src/features/timeline/PriceTimelineChart.tsx`
- **action:** Legend `name` 두 개 교체("백필·일평균(거래가)"→"평균 거래가", "실측 최저호가(일별)"→"최저가"). 툴팁 라벨 교체("최저호가"→"최저가", "일평균 거래가"→"평균 거래가"). 지표 구분(2 dataKey, 색·연속선 규칙) 무변경.
- **verify:** `cd frontend && npm run build` 그린.
- **done:** 범례·툴팁이 고객 용어로 표시.

### Task 2 — 파비콘 교체 (📈)
- **files:** `frontend/public/favicon.svg` (신규), `frontend/index.html`
- **action:** `favicon.svg`에 📈 이모지 SVG 작성. `index.html <head>`에 `<link rel="icon" type="image/svg+xml" href="/favicon.svg" />` 추가.
- **verify:** `cd frontend && npm run build` 그린(파비콘은 public/ 정적 자산으로 dist에 복사됨).
- **done:** 웹 탭에 📈 아이콘 렌더.

## Guard
백엔드·수집/캐시/serving·Core Value 경로 무변경. 프론트 표시 문자열 + 정적 자산만 변경.
