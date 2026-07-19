---
quick_id: 260719-wl0
slug: dashboard-nav-polish
description: 대시보드 헤더/네비 다듬기 3건 — 스크롤바 시프트 · 보석 아이콘 · 공지 좌측 이동
date: 2026-07-19
status: planned
---

# Quick Task 260719-wl0 — 대시보드 헤더/네비 다듬기

프론트(`frontend/src`)만 수정. 백엔드 0줄. 데이터 흐름·API 무변경.

## 항목 1 — 페이지 전환 시 헤더 좌우 이동 (버그)

**원인:** `AppLayout`·`TopNav`이 `mx-auto max-w-[90rem]`로 중앙정렬인데, 페이지가 길면 세로
스크롤바(~15px)가 생겨 가용 폭이 줄고 짧으면 넓어져 콘텐츠가 좌우로 튄다. 스크롤바 有(대시보드·
모험의서) ↔ 無(품목 타임라인·아바타) 전환에서 발생 — 사용자가 관찰한 두 그룹과 일치.

**수정:** `frontend/src/index.css` `@layer base`에 `html { scrollbar-gutter: stable; }` 1줄.
스크롤바 자리를 항상 예약 → 폭 고정 → 모든 라우트에서 이동 제거.

## 항목 2 — 보석 카테고리 아이콘 추가

**현황:** 각인서(실 게임 아이콘)·재료(`Hammer` 글리프)는 그룹 헤더에 아이콘이 붙지만, 보석은
헤더 없는 단독 leaf라 아이콘 자리가 없다.

**수정:** `CategoryNav.tsx` — `NavLeaf`에 optional `icon?: LucideIcon` 추가, 보석 leaf에
lucide `Gem`(💎) 전달. 재료 `Hammer` 선례와 동일(대체 글리프). 데스크톱 좌측 카테고리에 표시.

## 항목 3 — 공지사항 좌측 이동 + "로스트아크 소식" 제목 삭제

**목표:** 우측 소식 박스에서 공지사항만 좌측 카테고리(보석 밑, 구분선 포함) 아래로 옮기고, 우측은
제목 없이 쿠폰+진행중 이벤트만.

**수정:**
- `NewsPanel.tsx`: `NoticeSection`을 self-contained `NoticeRail`(export)로 추출 — 자체
  `useNews` + 자체 `AsyncBoundary`(D-07 격리 유지) + 상단 구분선(`border-t`) + "공지사항" 헤딩.
  `NewsPanel`은 Card 헤더("로스트아크 소식") 제거, 쿠폰 + 진행중 이벤트만. 미사용 import 정리.
- `DashboardPage.tsx`: 좌측 grid 셀을 `lg:sticky lg:top-20 lg:self-start` 래퍼로 감싸
  `CategoryNav` + `NoticeRail`(`hidden lg:block`) 배치. sticky는 CategoryNav `<nav>`에서
  래퍼로 이동(필터+공지 한 덩어리로 고정 → 스크롤 중첩 방지). 모바일은 우측 하단에
  `NoticeRail`(`lg:hidden`) 1개 더 렌더(칩→물품→영향→쿠폰·이벤트→공지 순서 유지).

## 검증

- `npm run build`(tsc+vite) 무오류 · 린트 통과(미사용 import 0).
- 육안: 4개 라우트 전환 시 헤더 고정 / 보석 💎 / 좌측 공지·우측 제목없음 (사용자 QA).

## 커밋 (원자적, 사용자 검토 후)

1. `fix(dashboard): scrollbar-gutter stable — 라우트 전환 헤더 시프트 제거` (index.css)
2. `feat(dashboard): 보석 카테고리에 Gem 아이콘` (CategoryNav.tsx)
3. `refactor(dashboard): 공지사항 좌측 레일 이동 + 소식 제목 제거` (NewsPanel.tsx, DashboardPage.tsx)
