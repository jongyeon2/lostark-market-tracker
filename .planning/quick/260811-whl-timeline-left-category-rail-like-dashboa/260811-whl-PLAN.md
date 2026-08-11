---
quick_id: 260811-whl
slug: timeline-left-category-rail
description: 타임라인 품목 선택을 대시보드처럼 좌측 카테고리 레일 + 우측 품목 칩·차트로 개편
date: 2026-08-11
status: in-progress
commits: []
---

# Quick Task 260811-whl — 타임라인 좌측 카테고리 레일 UI

## 왜

타임라인 품목 선택기(현 `ItemPicker`: 상단 가로 카테고리 칩 + 품목 칩 2단)를 **대시보드와 같은 좌측
세로 카테고리 레일**로 바꾼다. 사용자 요청: "대시보드 페이지에 보이는 것처럼 그래프 왼쪽에 큰 카테고리
밑으로 클릭할 수 있게." 선택된 상호작용안 = **좌측 레일(카테고리) + 우측 상단 품목 칩**(2단계).

## 핵심 설계 결정

- 🔑 **`CategoryNav`를 `features/dashboard/` → `features/_shared/`로 이동.** 타임라인이 대시보드 내부
  모듈을 import하면 이미 선 레이어 규칙(D-07, `categories.ts`를 _shared로 옮긴 그 이유)을 깬다.
  CategoryNav는 dashboard-local import가 없어(전부 _shared/lib) 이동이 깨끗하다. 사용처는 DashboardPage
  하나뿐 → import 경로만 갱신, 동작 무변경.
- **URL searchParams가 여전히 단일 출처**(`useTimelineParams`). 좌측 CategoryNav의 selectedId=활성
  카테고리(선택 품목이 속한 곳으로 매 렌더 유도 — ItemPicker의 무상태 수법 계승), onSelect=그 카테고리
  첫 품목으로 setItem. 우측 품목 칩 selected=itemId, onClick=setItem. **어긋난 상태가 존재할 수 없다.**
- **보석 제외 유지**(gemCount=0) — 시계열 없어 그릴 수 없는 차트로 데려간다. '기타' 버킷도 계승(누락 0).
- **레이아웃**: `lg:grid-cols-[18rem_minmax(0,1fr)]` 2열. 좌 sticky 흰 카드(대시보드와 동일 스타일)
  안 CategoryNav / 우 [품목 칩] + [기간] + [LatestPriceCard] + [차트]. 모바일은 1열 stack(CategoryNav가
  가로 칩으로 자동 축약 → 품목 칩 → 차트), 기존 ItemPicker 2단과 동일 흐름.

## 작업

1. `git mv features/dashboard/CategoryNav.tsx features/_shared/CategoryNav.tsx` + 헤더 주석 "dashboard's
   category filter" → 공유 컴포넌트로 갱신.
2. `DashboardPage.tsx`: import `./CategoryNav` → `@/features/_shared/CategoryNav`.
3. `TimelinePage.tsx`: 2열 그리드 재구성. 카테고리 파생(deriveCategories(_,0)+기타)·활성카테고리·품목칩을
   여기서 계산(ItemPicker에서 이관), 좌 CategoryNav / 우 품목 칩+기간+카드+차트. ChartArea 무변경.
4. `ItemPicker.tsx` 삭제(사용처 소멸). `categories.ts`의 "timeline's ItemPicker" 주석을 레일로 갱신.

## 검증

- `npx tsc --noEmit`(타입) + `npm run build`(vite) 로컬 그린. Docker 불필요(프론트 전용).
- 대시보드 회귀 0(CategoryNav 이동은 import 경로만). 타임라인: 카테고리 클릭→첫 품목 자동 선택+차트 갱신,
  `?item=` 딥링크가 해당 카테고리 활성화, 모바일 stack 흐름 확인.
- 최종 게이트 = PR CI(frontend build).

## 범위 밖

- 백엔드 0줄. 이벤트 마커·차트 로직 무변경(선택 UI만). 대시보드 3열 레이아웃 무변경.
