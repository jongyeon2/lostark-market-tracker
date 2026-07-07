---
quick_id: 260707-usn
slug: dashboard-card-price-label
status: complete
date: 2026-07-07
type: ui-copy
tags: [frontend, dashboard, copy, terminology]
commits:
  - cdb1cc1  # feat: unify dashboard card price term (최저가)
duration: ~10min
---

# Quick Task 260707-usn: 대시보드 카드 가격 용어 통일 Summary

**대시보드 카드(`ItemCard`)의 가격을 차트와 동일하게 "최저가"로 명명 — 라벨 없던 🪙 숫자에 "최저가" 라벨 추가 + "최신가 수집 중"→"최저가 수집 중". 프론트 표시 문자열만 변경.**

## Changes
- `ItemCard.tsx`:
  - success 가격 라인에 muted **"최저가"** 라벨 추가(🪙·숫자 앞) — `minPrice`가 차트 "최저가"와 동일 지표임을 명시.
  - 빈 상태 `'최신가 수집 중'` → `'최저가 수집 중'` + 상단/인라인 주석 2곳 동기화.
  - 레이아웃(py-3 compact 행)·`<Link>`·`useCollectionHealth` 로직 무변경.

## Task Commits
1. **ItemCard 가격 라벨 + 빈 상태 통일** — `cdb1cc1`

## Verification
- `cd frontend && npm run build` **그린** (built in 3.46s).

## Scope note
- `LatestPriceCard`(타임라인/영향 페이지)는 단일 flex 행 레이아웃이 민감(uniform gap-6, w-fit)해 이번 범위에서 제외. 동일 지표를 라벨 없이 노출 중 — 통일 원하면 후속 quick 권장.
- init.quick가 한글 설명으로 빈 slug 반환(메모리 [[gsd-phase-insert-korean-slug]]) → 수동 slug `dashboard-card-price-label` 지정.
