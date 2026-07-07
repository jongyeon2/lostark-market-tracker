---
quick_id: 260707-fkp
slug: remove-page-titles
type: quick
date: 2026-07-07
---

<objective>
데모 3화면(대시보드 / 품목 타임라인 / 이벤트 영향)의 페이지 제목 `<h1>`을 제거해, 진입 시 제목 글 없이 곧바로 정보(컨트롤/카드/차트)가 보이도록 한다. 백엔드 0줄, frontend `npm run build` 그린.
</objective>

<tasks>

<task type="auto">
  <name>Task 1: 3개 페이지 제목 h1 제거</name>
  <files>frontend/src/features/dashboard/DashboardPage.tsx, frontend/src/features/timeline/TimelinePage.tsx, frontend/src/features/impact/ImpactPage.tsx</files>
  <action>
    각 페이지의 `<h1 className="text-[28px] leading-tight font-semibold">…</h1>` 한 줄을 제거한다:
    - DashboardPage: "대시보드" h1 제거 → `space-y-6` 컨테이너의 첫 자식이 grid가 됨.
    - TimelinePage: "품목 타임라인" h1 제거 → 첫 자식이 컨트롤 바가 됨.
    - ImpactPage: "이벤트 영향" h1 제거 → CorrelationBanner(상관≠인과)는 그대로 최상단 유지, 그 아래 컨트롤 바.
    부모가 모두 `space-y-6`라 나머지 자식 간격은 그대로 자연스럽게 유지된다. 다른 마크업/로직은 무변경, 백엔드 0줄.
  </action>
  <verify>
    <automated>cd frontend && npm run build</automated>
  </verify>
  <acceptance_criteria>
    - 3개 페이지에서 제목 h1이 제거된다 (grep로 "대시보드"/"품목 타임라인"/"이벤트 영향" h1 부재)
    - `cd frontend && npm run build` 그린
    - 백엔드 diff 0줄, 기존 컨트롤/카드/차트/배너 회귀 없음
  </acceptance_criteria>
  <done>3화면 진입 시 제목 없이 정보가 바로 렌더되고 빌드가 그린이다.</done>
</task>

</tasks>

<output>
Create `.planning/quick/260707-fkp-remove-page-titles/260707-fkp-SUMMARY.md` when done
</output>
