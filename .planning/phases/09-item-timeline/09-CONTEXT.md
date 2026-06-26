# Phase 9: Item Timeline - Context

**Gathered:** 2026-06-26
**Status:** Ready for planning

<domain>
## Phase Boundary

품목 **하나**의 가격 시계열(min_price)을 Recharts 라인 차트로 그리고, 그 위에 게임 이벤트를 `eventType`별 세로 마커(`ReferenceLine`)로 겹쳐 "언제·무슨 이벤트 즈음에 움직였나"를 보게 한다 — 이 프로젝트 헤드라인의 시각화. item selector로 품목을 고르고, from/to 기간을 바꾸며, 다운샘플 응답을 정직하게 표시하고, 빈/잘못된/없는 케이스를 각각 처리한다. (TIME-01~05)

**Read-only.** 기존 read API(`/api/items`, `/api/items/{id}/latest`, `/api/items/{id}/prices?from=&to=`)만 소비하고 백엔드 `src/`는 0줄 변경. **데이터 레이어는 Phase 7에서 이미 완성** — `useTimeline(id, from, to)` 훅 · `timelineSchema`(downsampled/bucketWidth/snapshots/events) · `getTimeline` 경계 검증이 존재. Phase 9는 이 위에 **시각화·인터랙션 UI만** 얹는다.

**범위 밖(다른 phase/마일스톤):** 이벤트별 전후 변화율 표·insufficient_data·"상관≠인과" 고지(Phase 10), 데모 문서·정적 서빙(Phase 11), 실시간 자동 갱신(FE-V2-02), 관리자 쓰기 UI(FE-V2-01), 배치 latest 엔드포인트(백엔드 변경→v2). 마커 색의 정확한 hex·타이포·간격 등 정밀 시각 토큰은 `/gsd-ui-phase 9`의 09-UI-SPEC 소관 — 본 CONTEXT는 의미→색 *매핑*과 인터랙션 동작까지만 잠근다.
</domain>

<decisions>
## Implementation Decisions

### 이벤트 마커 & 차트 서사 (TIME-04)
- **D-01:** **eventType 4색 + 범례.** 4종(`LOA_ON`/`MAJOR_UPDATE`/`SEASON_END`/`BALANCE_PATCH`) 각각에 고유 색의 `ReferenceLine`을 그리고 차트 근처에 색 범례를 둔다. 4종이 색으로 즉시 구분돼 면접 서사가 명확. 단색+아이콘 구분은 기각. **의미→색 매핑은 여기서 잠금**, 정확한 hex·범례 위치는 09-UI-SPEC 소관.
- **D-02:** **마커 정보는 호버 툴팁(title).** 마커에 호버 시 이벤트 `title`을 표시(ROADMAP 완료조건 #4 명시). 차트는 깔끔하게 유지하고 정보는 온디맨드. 상시 라벨은 기각(마커 밀집 시 겹침). **주의(research/구현 영역):** Recharts는 차트당 Tooltip 1개라 `ReferenceLine` 호버와 가격선 데이터 Tooltip의 공존이 까다롭다 — 정확한 구현 방식(커스텀 Tooltip, ReferenceLine label, 별도 호버 타깃 등)은 researcher/planner가 Recharts 최신 API로 확정.

### 조회 기간 컨트롤 & 기본 윈도우 (TIME-02, TIME-05)
- **D-03:** **기본 조회 범위 = 최근 30일.** URL에 from/to가 없을 때의 기본값. seed 8일 윈도우가 항상 포함돼 마커 2개가 다 보이고 진입 즉시 비어있지 않음(데모 첫인상). 다운샘플 트리거 여부는 백엔드 임계값에 따름(검증 항목).
- **D-04:** **프리셋 버튼(7일/30일/90일) + from/to 날짜 입력 병행.** 프리셋으로 데모 클릭 한 번, 날짜 입력으로 커스텀 범위 + `to≤from`(400) 케이스 도달 가능(ROADMAP 완료조건 #5 검증 충족). **from/to(및 D-05의 item id)는 URL searchParams에 인코딩** — Phase 7 D-04(포워드 노트) 잠금 결정의 실현. 딥링크·스크린샷 재현성.

### 품목 셀렉터 & 진입 동작 (TIME-01)
- **D-05:** **드롭다운 select 셀렉터.** 타임라인은 단일 품목 집중 화면이라 컴팩트한 드롭다운이 차트·최신가 카드에 공간을 남긴다. seed 품목은 소수. Dashboard식 카드/목록·검색 콤보박스는 기각.
- **D-06:** **진입 기본 선택 = URL `?item=` 우선, 없으면 `/api/items` 첫 품목 자동 선택.** 진입 즉시 비어있지 않은 차트(데모 친화). 선택 변경은 URL에 기록(D-04와 동일 searchParams 정책). "품목을 선택하세요" 빈 프롬프트는 기각.
- **D-07:** **셀렉터 + 최신가 카드를 공용 위치에 추출해 Phase 9·10이 공유.** Phase 10(Event Impact)이 "item selector·최신가 카드 재사용"을 ROADMAP 의존성으로 명시 → 공용 위치(예: `src/features/_shared/` 또는 `src/components/`, 정확한 경로는 planner 재량)에 두고 Phase 9·10이 동일 import. 최신가 카드는 기존 `useLatestPrice(id)` 훅을 재사용하되 **타임라인용 경량 표시 컴포넌트는 신규**(Dashboard `ItemCard`는 그리드용 통합 카드라 단일 선택 화면과 형태가 다름 → 그대로 재사용은 기각). 계층 의존이 impact→timeline로 흐르지 않도록 공용 위치 우선.

### 다운샘플 & 에러 정직성 (TIME-03, TIME-05)
- **D-08:** **다운샘플 = 배지 + 선 스타일 구분.** `downsampled=true`면 "버킷 평균(bucketWidth)" 배지 + 점 없는 평균선, raw면 개별 점이 있는 원점선. "지금 보는 건 원점이 아니라 버킷 평균"을 시각으로도 정직하게 전달(honest-data 에토스). 배지만+동일선은 기각(시각 신호가 배지 하나에만 의존). 선 스타일·점 표시 세부는 09-UI-SPEC/planner가 다듬되 배지 노출과 raw/평균 구분 의도는 잠금.
- **D-09:** **400 / 404 / 200-empty 셋 각각 구분 안내.** `400`(to≤from)=기간 입력 안내, `404`(없는 품목)=없는 품목 안내, `200-empty`(빈 기간)=데이터 없음을 각각 구분된 카피로. ROADMAP 완료조건 #5가 명시 요구 + honest-data 에토스. `ApiError`가 HTTP status를 보유하므로 `<AsyncBoundary>` + status 분기로 구현 가능. 일반 ErrorState 하나로 합치기는 기각.

### Claude's Discretion
- 마커 호버 툴팁과 가격선 Tooltip의 정확한 Recharts 공존 구현(D-02), 마커 밀집/겹침 시 처리(seed는 2개라 단순).
- y축 골드 단위 포맷(천 단위 구분·축약 "1.2M" 등), x축 KST 라벨 밀도/포맷, 차트 높이·반응형 브레이크포인트.
- 프리셋 정확 라벨/개수(7/30/90일은 출발점), 날짜 입력 위젯 형태(native date input vs 커스텀).
- 공용 컴포넌트의 정확한 폴더 경로·명명(D-07), 최신가 카드 내부 필드 배열.
- raw 응답의 점(dot) 표시 여부 정밀 임계(데이터 많을 때 점 생략) 등 D-08 선 스타일 세부.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·범위·API 계약
- `.planning/REQUIREMENTS.md` — TIME-01~05 정의 + **"실제 API 계약(코드 실측)" 표**(`/api/items`, `/latest`, `/prices?from=&to=`의 정확한 필드·상태값·400/404/200-empty 규칙, eventType enum). **MUST read** — 차트·셀렉터 표시 필드는 이 표와 1:1.
- `.planning/ROADMAP.md` §"Phase 9: Item Timeline" — Goal · 완료 조건(5) · 검증 방법(다운샘플 트리거·400/404/empty·off-by-9h 대조) · 사용자 확인 포인트(마커 서사·다운샘플 배지 정직성).

### 상속 결정·디자인 계약 (재정의 금지)
- `.planning/phases/07-frontend-foundation/07-CONTEXT.md` — **D-01~D-12 상속(MUST read)**. 특히 **D-04(필터 상태=URL searchParams)** = 본 D-04/D-06의 근거, React Query 훅(D-01/02), zod 경계 검증(D-05/06), AsyncBoundary+상태 컴포넌트(D-07/08), formatKst off-by-9h 가드(D-09), React Router 셸(D-03), feature 폴더(D-11), api/queries 모듈(D-12).
- `.planning/phases/08-dashboard/08-CONTEXT.md` — Phase 8 패턴 상속(MUST read). 카드별 `<AsyncBoundary>` 격리(D-07/08), `useLatestPrice(id)` 소비 방식(D-03), status/빈 상태 위젯 단위 처리. 본 D-07(최신가 카드)·D-09(에러 분기)가 이 위에 얹힘.
- `.planning/phases/07-frontend-foundation/07-UI-SPEC.md` — 디자인 시스템(shadcn slate·new-york, lucide-react, Inter, 토큰·카피 계약). `/gsd-ui-phase 9`의 09-UI-SPEC이 이를 상속해 차트·마커 색·배지 시각 계약을 만든다. **마커 4색 hex·범례·배지 시각은 거기서 확정**(본 CONTEXT는 의미→색 매핑만).

### 프로젝트 원칙
- `.planning/PROJECT.md` §"Key Decisions"·§"Context" — **백엔드 무변경**(사용자 제약), UTC→KST off-by-9h 가드(차트 정렬은 UTC epoch, 라벨만 KST), honest-data 에토스(다운샘플 정직 표시·null loud-fail), **상관≠인과**(Phase 10 핵심이나 마커 서사도 과대 주장 금지).

### 백엔드 실측 근거 (다운샘플·쿼리 계약 — 검증 권장)
- `src/main/java/...` prices 엔드포인트 컨트롤러/서비스 — **검증 항목 2건:** (1) `bucketWidth` 실제 반환 포맷(스키마는 `z.string()` "e.g. 1h"으로 모델, ROADMAP은 "hour/day" 표현 — 실제 문자열 확인 필요), (2) `from`/`to` 쿼리 파라미터가 기대하는 시각 포맷(ISO instant `...Z` 여부)과 다운샘플 트리거 임계 범위. researcher가 실측해 D-03/D-08을 정합.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (이미 존재 — 신규 데이터 배관 불필요)
- `frontend/src/lib/queries.ts` — **`useTimeline(id, from, to)` 훅 이미 존재**(`['timeline', id, from, to]` 키). `useLatestPrice(id)`도 존재(최신가 카드용). `useItems()`(셀렉터 옵션용).
- `frontend/src/lib/schemas.ts` — **`timelineSchema`(downsampled·bucketWidth nullable·snapshots[{collectedAt,minPrice,sampleCount}]·events[{occurredAt,eventType,title}]), `eventTypeSchema`(4 enum) 이미 정의.** 표시 타입은 `z.infer` 파생.
- `frontend/src/lib/api.ts` — **`getTimeline(id, from, to)` 경계 `.parse` 검증 + `ApiError(status, url)`** 이미 존재. 400/404가 `ApiError`로 throw돼 status 분기(D-09) 가능.
- `frontend/src/components/state/{AsyncBoundary,LoadingState,EmptyState,ErrorState}.tsx` — 차트/카드별 상태 격리(D-09)에 재사용.
- `frontend/src/lib/formatKst.ts` — x축 라벨·최신가 카드 시각 KST 표시(off-by-9h 가드). **차트 x축 위치·정렬은 raw UTC epoch(ms) 기준, 라벨만 KST**(Phase 7 D-09).
- `frontend/src/components/ui/{card,badge,button,skeleton,alert}.tsx` — shadcn. 최신가 카드·다운샘플 배지·프리셋 버튼 빌드 블록. `badge.tsx`는 Phase 8에서 이미 추가됨.
- `frontend/src/features/dashboard/{ItemCard,StatusBadge}.tsx` — 패턴 참고(단, 최신가 카드는 D-07에 따라 타임라인용 경량 신규).

### Established Patterns
- **컴포넌트별 `<AsyncBoundary status isEmpty onRetry>` 래핑** = 성공 경로만 작성 + 분기 중앙화(Phase 7 D-08 / Phase 8 D-07~08). 차트·최신가 카드 각각 자기 boundary.
- zod 경계 검증으로 null/unknown을 "loudly fail"(D-06). nullable `sampleCount`/`bucketWidth`를 차트로 silent 전파 차단.
- 모든 시각 UTC ISO-8601(`...Z`) → 표시만 KST, 정렬·계산은 UTC 인스턴트.

### Integration Points
- **`frontend/src/features/timeline/TimelinePage.tsx`** = 현재 빈 골격(Phase 7). Phase 9에서 **이 파일을 셀렉터 + 최신가 카드 + 차트 + 마커로 교체**. 하위 컴포넌트는 timeline feature 폴더 + 공용(D-07).
- **Recharts 미설치** — `frontend/package.json` deps에 없음. **`npm install recharts` 필요**(라이브러리 선택은 REQUIREMENTS에서 잠금, 설치만). React 19 호환 버전 확인 권장.
- **URL searchParams** — React Router(Phase 7 D-03) `useSearchParams`로 `?item=&from=&to=` 인코딩/디코딩(D-04/D-06). 라우터는 Phase 7에서 이 방향으로 셋업됨.
- Vite 프록시 `/api`→`:8080`, **seed 프로파일 백엔드** 기동 전제(8일 윈도우·데모 이벤트 2개로 비어있지 않게 재현).
</code_context>

<specifics>
## Specific Ideas

- **포트폴리오 서사:** 가격선 위에 이벤트 세로 마커를 겹쳐 "로아온/대형 업데이트 즈음 고변동"의 서사를 **한 차트로** 읽게 하는 것이 이 프로젝트 헤드라인 — 단, 마커는 "시점이 겹친다(상관)"이지 인과가 아님(과대 주장 금지, Phase 10에서 명시 고지).
- **다운샘플 정직성:** raw 점 vs 버킷 평균선을 시각으로 구분 + 배지 = "큰 범위에선 원점이 아니라 집계를 본다"를 숨기지 않는 점이 honest-data 에토스의 차트 연장(면접 포인트).
- **데이터 레이어 선완성의 배당:** Phase 7에서 5개 DTO를 모두 zod로 잠갔기에 Phase 9는 차트/인터랙션에만 집중 — "경계 검증을 먼저 세우면 화면은 성공 경로만 쓴다"는 설계의 실증.
</specifics>

<deferred>
## Deferred Ideas

- **이벤트별 전후 변화율·insufficient_data·"상관≠인과" 고지** — Phase 10 (Event Impact). 본 phase는 마커 시각화까지만.
- **배치 latest 엔드포인트** — Phase 8 Deferred 유지(백엔드 변경 → v2). 타임라인은 단일 품목이라 영향 없음.
- **실시간 자동 갱신(폴링/SSE)** — FE-V2-02. Phase 7 D-02에서 비활성.
- **차트 줌/팬·브러시 범위 선택** — 언급되지 않았으나 잠재 v2 후보. MVP는 프리셋+날짜 입력(D-04)으로 충분.
- **마커 클릭→Event Impact 딥링크** — Phase 10 도입 후 자연 연결(현 phase 범위 밖, D-07 공용 셀렉터가 연결 기반).

None other — 논의는 Phase 9 범위(TIME-01~05) 내에 머물렀다.
</deferred>

---

*Phase: 9-item-timeline*
*Context gathered: 2026-06-26*
