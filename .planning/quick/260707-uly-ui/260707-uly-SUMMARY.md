---
quick_id: 260707-uly
slug: ui
status: complete
date: 2026-07-07
type: ui-copy
tags: [frontend, recharts, copy, favicon, customer-facing]
commits:
  - 2c9aa5f  # feat: customer-friendly chart labels
  - bf37c9d  # feat: 📈 favicon
duration: ~15min
---

# Quick Task 260707-uly: 차트 라벨 순화 + 파비콘 교체 Summary

**차트 범례/툴팁의 개발자 용어를 로아 유저가 바로 읽히는 "평균 거래가 / 최저가"로 바꾸고, 없던 파비콘을 📈 SVG로 추가. 프론트 표시 문자열 + 정적 자산만 변경, 백엔드 무변경.**

## Decisions (user, AskUserQuestion)
- 라벨 = **간결형**: 주황 "평균 거래가", 파랑 "최저가"(툴팁 동일). 두 지표 2라인 구분 유지.
- 파비콘 = **📈 상승 차트 이모지**.

## Changes
1. **`PriceTimelineChart.tsx`** — Legend name "백필·일평균(거래가)"→"평균 거래가", "실측 최저호가(일별)"→"최저가"; 툴팁 "최저호가"→"최저가", "일평균 거래가"→"평균 거래가". dataKey·색·연속선/마커 규칙(D-02 지표 구분) 무변경.
2. **`frontend/public/favicon.svg`**(신규) + **`frontend/index.html`** — 📈 이모지 SVG 파비콘 + `<link rel="icon" type="image/svg+xml" href="/favicon.svg">`.

## Task Commits
1. **차트 라벨 순화** — `2c9aa5f`
2. **📈 파비콘** — `bf37c9d`

## Verification
- `cd frontend && npm run build` **그린**(2619 modules, built in 3.94s).
- dist 반영 확인: `dist/index.html`에 favicon 링크, `dist/favicon.svg` 복사됨.

## Impact
- 범례/툴팁이 고객 용어로 표시 — "백필"·"실측"·"호가" 제거.
- 웹 탭에 📈 아이콘(브라우저 기본 대체). SVG 파비콘이라 에셋 바이너리 불필요.

## Notes
- SVG 파비콘은 최신 브라우저 지원. 구형 폴백(.ico) 필요 시 별도 추가 가능(현재 미포함).
- 다른 화면(대시보드 카드 등) 용어는 이번 범위 밖 — 필요 시 후속.
