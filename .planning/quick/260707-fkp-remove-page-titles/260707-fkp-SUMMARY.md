---
quick_id: 260707-fkp
slug: remove-page-titles
status: complete
date: 2026-07-07
commit: daa7c24
---

# Quick Task 260707-fkp: 데모 3화면 페이지 제목 제거 Summary

**대시보드 / 품목 타임라인 / 이벤트 영향 페이지의 `<h1>` 제목을 제거해 진입 시 컨트롤·카드·차트가 곧바로 보이도록 함**

## What changed
- `DashboardPage.tsx` — "대시보드" h1 제거 (space-y-6 컨테이너 첫 자식이 grid)
- `TimelinePage.tsx` — "품목 타임라인" h1 제거 (첫 자식이 컨트롤 바)
- `ImpactPage.tsx` — "이벤트 영향" h1 제거 (CorrelationBanner 상관≠인과 배너는 최상단 유지)

부모 컨테이너가 모두 `space-y-6`라 나머지 자식 간격은 자연스럽게 유지됨. 백엔드/lib 0줄, 기존 마크업·로직 무변경.

## Verification
- `cd frontend && npm run build` (`tsc -b && vite build`) 그린
- 페이지 제목 h1 부재 grep 확인, `src/` 백엔드 diff 0줄

## Commit
- `daa7c24` feat(260707-fkp): remove page-title headings from the 3 demo screens

## Notes
- 별건으로 dev DB 합성 seed 블록 정리(품목 타임라인 데이터 정직화)는 이 quick 작업과 분리해 별도 진행. gap 백필(서버 off 구간을 API 일별 Stats로 채우기)은 신규 phase로 계획 예정.

---
*Quick task: 260707-fkp-remove-page-titles*
*Completed: 2026-07-07*
