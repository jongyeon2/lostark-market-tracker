# Phase 8: Dashboard - Context

**Gathered:** 2026-06-25
**Status:** Ready for planning

<domain>
## Phase Boundary

한 화면에서 ① 수집 파이프라인 health(마지막 실행 시각·시도/성공/실패 카운트·status·summaryMessage) ② 활성 품목 목록(displayName·category) ③ 품목별 최신가 요약(minPrice·collectedAt)을 보여줘, 면접관이 "데이터가 실제로 흐른다"를 즉시 읽게 한다. (DASH-01~04)

**Read-only.** 기존 read API 3개(`/api/health/collection`, `/api/items`, `/api/items/{id}/latest`)만 소비하고 백엔드 `src/`는 0줄 변경. Phase 7이 잠근 골격(React Query 훅·AsyncBoundary·zod 스키마·formatKst·feature 폴더) 위에 Dashboard 위젯을 얹는다.

**범위 밖(다른 phase/마일스톤):** 차트·시계열(Phase 9), 이벤트 영향(Phase 10), 자동 갱신(FE-V2-02), 관리자 쓰기 UI(FE-V2-01), 배치 latest 엔드포인트(백엔드 변경 → v2). 배지 색/타이포 등 정밀 시각 토큰은 `/gsd-ui-phase 8`의 08-UI-SPEC 소관 — 본 CONTEXT는 status 의미 *등급*까지만 잠근다.
</domain>

<decisions>
## Implementation Decisions

### 레이아웃·정보 위계 (DASH-03 + DASH-04)
- **D-01:** **품목당 통합 카드 그리드.** 품목 하나 = 카드 하나에 displayName·category·minPrice·collectedAt(KST)를 한 단위로 묶는다. 화면 상단에 health 카드를 별도로 두고, 그 아래 품목 카드 그리드를 배치 → "health(파이프라인 살아있음) → 무엇을 추적 → 지금 얼마"의 정보 위계(ROADMAP 사용자 확인 포인트). DASH-03(목록)과 DASH-04(최신가)는 한 카드 안에서 결합되며 분리 섹션·단일 테이블 안은 기각.
- **D-02:** 품목 카드 정렬은 `/api/items` 응답 순서(백엔드가 displayName 정렬)를 그대로 따른다 — 프론트 재정렬 없음.

### latest 패칭 전략 (DASH-04)
- **D-03:** **카드별 개별 `useLatestPrice(id)` 팬아웃.** 각 품목 카드가 자기 `useLatestPrice(id)` 훅을 호출하고 자기 `<AsyncBoundary>`로 감싼다. React Query가 N개 요청을 자동 병렬·개별 캐시·개별 재시도하므로 한 품목 latest 실패가 다른 카드·health를 가리지 않는다. `useQueries` 집계나 일괄 프리페치는 기각(카드별 독립성·D-08 패턴과의 결합이 우선). 기존 `frontend/src/lib/queries.ts`의 `useLatestPrice` 훅을 그대로 사용.
- **D-04:** 배치 latest 엔드포인트가 없으므로 품목 N개 → latest N요청은 불가피. seed 워치리스트는 소수라 허용 가능한 비용으로 본다. 다수 품목 시의 배치 API는 백엔드 변경이라 이 마일스톤 밖(→ Deferred). 카드별 fan-out을 "정직한 단순함"으로 채택하고, 포트폴리오 설명 시 트레이드오프를 명시한다.

### health status 의미 등급 (DASH-01 + DASH-02)
- **D-05:** **실측 status 4값을 4등급으로 매핑.** 백엔드 실측(`PriceCollector.java`·`CollectionHealthResponse.java`): `SUCCESS`=정상, `PARTIAL_SUCCESS`=경고, `FAILED`(전부 실패)=위험, `NO_RUNS`(미실행)=대기(중립). ROADMAP은 NO_RUNS/PARTIAL_SUCCESS만 언급하지만 `FAILED`도 별도 '위험' 등급으로 구분(3등급 합침·2등급 단순화 기각). 등급의 *색/배지 시각*은 08-UI-SPEC 소관, 의미 매핑은 여기서 잠금.
- **D-06:** **`summaryMessage`는 진단 보조 마커.** `AUTH_ERROR`/`RATE_LIMITED`는 PARTIAL_SUCCESS/FAILED 상황에서 health 카드 안에 진단 보조 텍스트로 노출하고, `null`이면 표시하지 않는다. 응답에 시크릿 필드가 없으므로(백엔드가 카테고리컬 마커만 내려줌) 화면에도 시크릿은 없다(DASH-02 시크릿 비노출 = 응답 형태가 이미 보장).

### 빈/대기 상태 처리 (완료조건 #5)
- **D-07:** **카드별 독립 처리.** 빈/대기를 화면 단위가 아니라 위젯 단위로 격리한다. (a) health `NO_RUNS` → health 카드 자체가 '아직 수집 실행 없음' 대기 카피를 보이고 화면 나머지는 정상 렌더. (b) 특정 품목 latest가 404/미수집(최초 수집 전) → 그 품목 카드만 '데이터 없음', 나머지 카드·health는 영향 없음. (c) `/api/items`가 빈 배열(품목 0개) → 품목 그리드 영역에 `EmptyState`. 화면 단위·혼합 처리는 기각.
- **D-08:** 카드별 격리는 D-03(카드별 훅)·Phase 7 D-08(`<AsyncBoundary>` per-컴포넌트)과 자연 결합 — 각 카드가 자기 pending/error/empty를 그린다. health 카드도 자체 `useCollectionHealth()` + `<AsyncBoundary>`.

### Claude's Discretion
- 그리드 컬럼 수·반응형 브레이크포인트, 카드 내부 필드 배열 순서, health 카드의 카운트 표현(시도/성공/실패를 개별 숫자 vs 묶음) — 08-UI-SPEC/planner 재량.
- `collectedAt`·`lastRunAt`를 절대 KST(formatKst 기존 패턴) vs 상대시간("3분 전")으로 보일지 — 기본은 절대 KST, 상대시간은 선택. 
- `isEmpty` 술어 세부(품목 0개 판정), health 카드와 품목 그리드의 수직 간격·섹션 헤더 유무.
- 품목 latest 404를 '데이터 없음(EmptyState)'으로 볼지 '에러(ErrorState)'로 볼지 — 최초 수집 전은 빈 상태에 가깝다는 방향만 제시, 정확한 분기는 구현 재량.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·범위·API 계약
- `.planning/REQUIREMENTS.md` — DASH-01~04 정의 + **"실제 API 계약(코드 실측)" 표**(health/items/latest DTO의 정확한 필드·상태값). **MUST read** — zod 스키마·표시 필드는 이 표와 1:1.
- `.planning/ROADMAP.md` §"Phase 8: Dashboard" — Goal · 완료 조건(5) · 검증 방법 · 사용자 확인 포인트.

### 상속 결정·디자인 계약 (Phase 7 — 재정의 금지)
- `.planning/phases/07-frontend-foundation/07-CONTEXT.md` — **D-01~D-12 상속(MUST read)**. React Query 훅(D-01/02), zod 경계 검증(D-05/06), AsyncBoundary+상태 컴포넌트(D-07/08), formatKst off-by-9h 가드(D-09), feature 폴더 구조(D-11), api/queries 모듈(D-12). 본 Phase 8 결정은 모두 이 위에 얹힘.
- `.planning/phases/07-frontend-foundation/07-UI-SPEC.md` — Phase 7 디자인 시스템(shadcn slate·토큰·카피 계약). Phase 8의 `/gsd-ui-phase 8`이 이를 상속해 Dashboard 위젯 시각 계약(08-UI-SPEC)을 만든다. status 배지 색·카드 시각은 거기서 확정.

### 백엔드 실측 근거 (status 값 증명 — 읽기 권장)
- `src/main/java/com/lostark/tracker/collect/PriceCollector.java` — status 문자열 산출(`SUCCESS`/`FAILED`/`PARTIAL_SUCCESS`) + summaryMessage(`AUTH_ERROR`/`RATE_LIMITED`/null) 결정 로직.
- `src/main/java/com/lostark/tracker/web/dto/CollectionHealthResponse.java` — `noRuns()` 팩토리(`NO_RUNS`) + 응답 필드 형태(시크릿 없음).

### 프로젝트 원칙
- `.planning/PROJECT.md` §"Key Decisions"·§"Context" — **백엔드 무변경**(사용자 제약), UTC→KST off-by-9h 가드, honest-data 에토스(시크릿 비노출·null 명시 처리).
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/lib/queries.ts` — `useCollectionHealth()`, `useItems()`, `useLatestPrice(id)` 훅 **이미 존재**. Phase 8은 새 훅 없이 이 셋을 소비.
- `frontend/src/lib/schemas.ts` — `collectionHealthSchema`(status·summaryMessage nullable), `trackedItemSchema`, `latestPriceSchema` zod 정의 완료. 표시 타입은 `z.infer` 파생.
- `frontend/src/lib/api.ts` — `getCollectionHealth`/`getItems`/`getLatestPrice` 경계 `.parse` 검증 포함.
- `frontend/src/components/state/{AsyncBoundary,LoadingState,EmptyState,ErrorState}.tsx` — D-07/08 상태 컴포넌트. 카드별로 재사용(D-07/08).
- `frontend/src/components/ui/{card,alert,button,skeleton}.tsx` — shadcn. health/품목 카드 빌드 블록.
- `frontend/src/lib/formatKst.ts` — collectedAt·lastRunAt KST 표시(D-09 off-by-9h 가드).

### Established Patterns
- **카드별 `<AsyncBoundary status isEmpty onRetry>` 래핑** = 성공 경로만 작성 + 분기 중앙화(Phase 7 D-08). Phase 8의 health 카드·품목 카드 모두 이 패턴.
- 모든 시각은 UTC ISO-8601(`...Z`) → 표시만 KST, 정렬·계산은 UTC 인스턴트(off-by-9h 가드).
- zod 경계 검증으로 null/unknown을 "loudly fail" — insufficient/누락 데이터가 화면으로 silent 전파 차단.

### Integration Points
- **현 `frontend/src/features/dashboard/DashboardPage.tsx`** = Phase 7의 임시 `useItems()` 목록(`<li>`) 증명. Phase 8에서 **이 파일을 health 카드 + 품목 카드 그리드로 교체**. 새 하위 컴포넌트(예: `HealthCard`, `ItemCard`)는 `src/features/dashboard/` 아래.
- Vite 프록시 `/api`→`:8080`, **seed 프로파일 백엔드** 기동 전제(빈 화면 없이 재현).
- health status 실측 도메인: `SUCCESS`/`PARTIAL_SUCCESS`/`FAILED`/`NO_RUNS`; summaryMessage `AUTH_ERROR`/`RATE_LIMITED`/null.

</code_context>

<specifics>
## Specific Ideas

- **포트폴리오 서사:** "한 화면에서 수집이 살아있음 + 무엇을 추적 + 지금 얼마"를 한눈에 — 백엔드 OPS 헬스 엔드포인트를 데모 표면으로 끌어올린 점, 카드별 fan-out의 트레이드오프(단순함 vs N요청)를 정직하게 설명하는 점이 면접 포인트.
- **status 4등급의 정직함:** ROADMAP이 빠뜨린 `FAILED`를 별도 '위험'으로 구분 — "실측 코드를 읽고 계약을 채운다"는 본 프로젝트 에토스의 프론트 연장.
</specifics>

<deferred>
## Deferred Ideas

- **배치 latest 엔드포인트**(`/api/items/latest?ids=` 류) — 다수 품목 시 N요청을 1요청으로. 백엔드 변경이라 이 마일스톤 밖(v2). 현재는 카드별 fan-out(D-03/04).
- **실시간 자동 갱신**(폴링/SSE) — FE-V2-02. Phase 7 D-02에서 비활성.
- **상대시간 표시**("3분 전") — Claude 재량으로 남김(기본 절대 KST). 채택 시에도 차트 정렬은 UTC.
- **품목 카드에서 타임라인/임팩트로 딥링크** — Phase 9~10 selector 도입 후 자연 연결(현 phase 범위 밖).

None other — 논의는 Phase 8 범위(DASH-01~04) 내에 머물렀다.

</deferred>

---

*Phase: 8-dashboard*
*Context gathered: 2026-06-25*
