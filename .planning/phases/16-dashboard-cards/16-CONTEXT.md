# Phase 16: 대시보드 카드 개선 - Context

**Gathered:** 2026-07-03
**Status:** Ready for planning

<domain>
## Phase Boundary

대시보드 품목 카드(`ItemCard`)에서 **물품 고유 번호(현재 표시되는 `item.category` 코드, 예 "50010")를 제거**하고, **카드 클릭 시 해당 품목이 선택된 상태로 품목 타임라인(`/timeline`)으로 딥링크 이동**하게 만든다.

순수 프론트엔드 변경(백엔드 0줄). 대상 파일은 사실상 `ItemCard.tsx` 하나(딥링크 URL은 기존 타임라인 계약을 그대로 소비). v1.2 enrichment(아이콘·역할 배지)와 기존 카드 정보(골드 가격·수집 시각)는 **회귀 없이 유지**.

**In scope:** category 코드 줄 제거 · 카드 전체를 타임라인 딥링크로 · hover/포커스 클릭 피드백.
**Out of scope:** 타임라인 화면 변경 · 새 선택 메커니즘 · 카드에 새 데이터/필터 추가 · 백엔드 변경.

</domain>

<decisions>
## Implementation Decisions

### 카드 표시 (CARD-01)
- **D-01:** 카드 하단의 `item.category` 코드 줄(`ItemCard.tsx`의 `<p>{item.category}</p>`, 예 "50010")을 **완전 제거**한다. 제거 후 카드는 **아이콘 · 품목명 · 역할 배지 · 골드 가격 · 수집 시각**만 표시 — Goal 문구("아이콘·이름·골드·수집시각만")에 정합. `itemGroup`("강화재료" 등)으로 대체하지 않는다(완전 제거 택함).
- **D-02:** 아이콘(`ItemIcon`)·역할 배지(`RoleBadge`)는 v1.2 enrichment로 **유지**(성공기준 3, 회귀 금지). 골드 가격(`minPrice` `toLocaleString`)·수집 시각(`formatKst`)·per-card `useLatestPrice` 팬아웃도 그대로.

### 카드 클릭 딥링크 (CARD-02)
- **D-03:** 카드 전체를 react-router **`<Link to="/timeline?item={item.id}">`**로 감싼다. 시맨틱 `<a>`라서 **Ctrl/중클릭 새 탭 · 키보드 Enter · 우클릭 메뉴**가 기본 동작. `onClick`+`useNavigate`는 채택하지 않음(접근성·새 탭 지원 우위).
- **D-04:** 딥링크 대상 URL은 **`/timeline?item={id}`**. 타임라인은 이미 `useTimelineParams`가 `?item=`을 **단일 출처(Phase 7 D-04)**로 읽어 자동 선택하므로, 타임라인 쪽 변경이나 새 선택 메커니즘은 **불필요**. 기간(`from`/`to`)은 넘기지 않는다 — 타임라인 기본(최근 30일)이 처리.

### Claude's Discretion
- **D-05:** hover/포커스 **시각 피드백 수준은 Claude 위임**. 제약: accent `blue-600`은 **focus-visible 링에만** 예약(UI-SPEC), hover는 카드 elevation(그림자)·보더 강조로 처리(accent fill 아님), `cursor-pointer`. 기존 카드 미감·8pt 스페이싱과 일관되게 planner/executor가 결정.
- **D-06:** `<Link>` 래핑 시 접근성 마감(포커스 링이 카드 외곽에 자연스럽게, 카드 내부 `RoleBadge`/텍스트가 중첩 인터랙티브 요소가 되지 않게)은 Claude 재량.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·목표
- `.planning/ROADMAP.md` §"Phase 16: 대시보드 카드 개선" — Goal + 성공기준 3개(고유번호 제거 / 클릭 딥링크 / enrichment 무회귀)
- `.planning/REQUIREMENTS.md` — CARD-01(고유번호 제거), CARD-02(클릭 딥링크)

### 소비할 기존 계약(딥링크)
- `.planning/phases/07-frontend-foundation/07-CONTEXT.md` — D-04 URL-as-state(`?item=&from=&to=`) 결정. 타임라인 딥링크의 근거 계약.

외부 ADR/스펙은 없음 — 나머지 결정은 위 Implementation Decisions에 완전 캡처됨.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/features/dashboard/ItemCard.tsx` — **이번 변경의 유일한 핵심 파일**. 현재 `<CardHeader>`에 아이콘+이름+역할배지, 그 아래 `<p>{item.category}</p>`(제거 대상), `<CardContent>`에 가격+수집시각. 이 `<Card>`를 `<Link>`로 감싸고 category 줄 제거.
- `frontend/src/features/timeline/useTimelineParams.ts` — `?item=`을 읽어 `itemId`로 파싱(있으면 자동 선택). 딥링크가 곧바로 동작하는 이유. **수정 불필요**(읽기 전용 소비).
- `frontend/src/features/timeline/TimelinePage.tsx` — `?item=`이 있으면 그 아이템, 없으면 첫 아이템 자동 선택(D-06). 카드 딥링크가 도착하는 화면. **수정 불필요**.
- `frontend/src/features/_shared/ItemIcon.tsx`, `RoleBadge.tsx` — 카드에 유지되는 enrichment(회귀 금지).
- `frontend/src/components/ui/card.tsx` — 카드 프리미티브. hover/focus 스타일은 className으로 오버라이드.

### Established Patterns
- **URL-as-state(Phase 7 D-04):** 타임라인 선택/기간이 URL searchParams에 있음 → 딥링크·새로고침·뒤로가기·스크린샷이 그대로 재현. 카드는 `/timeline?item={id}`로 링크만 하면 됨.
- **react-router `<Link>`:** 공개 셸은 `createBrowserRouter` + `<Link>`/`NavLink` 사용(TopNav). 카드 링크도 동일 라우터 컨텍스트에서 동작.
- **shadcn 카드 오버라이드:** hover/focus는 `cn(...)` className으로(기존 블록 0줄 유지).

### Integration Points
- `ItemCard`가 `DashboardPage`의 그리드에서 렌더됨(`frontend/src/features/dashboard/DashboardPage.tsx`) — 그리드/정렬은 무변경, 카드 내부만 변경.
- 딥링크 목적지: `frontend/src/main.tsx`의 `{ path: 'timeline' }` 라우트(AppLayout 하위) → `?item=` 쿼리로 진입.

</code_context>

<specifics>
## Specific Ideas

- "고유 번호"의 실체는 카드에 렌더되는 `item.category` 코드(예 "50010")로 확정 — 사용자가 카드에서 보는 그 의미 없는 숫자를 없애는 것이 CARD-01의 의도.
- 딥링크는 "카드에서 본 그 품목을 타임라인에서 곧바로" — `?item={id}`로 선택 상태 이월(추가 클릭 없이 차트가 그 품목으로 뜸).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 16-dashboard-cards*
*Context gathered: 2026-07-03*
