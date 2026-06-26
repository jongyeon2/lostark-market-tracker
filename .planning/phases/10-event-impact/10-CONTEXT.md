# Phase 10: Event Impact - Context

**Gathered:** 2026-06-26
**Status:** Ready for planning

<domain>
## Phase Boundary

선택 품목의 **이벤트별 전후 변화율을 표(목록)**로 보여주는 화면 — `window`(1..168h) 입력으로 `GET /api/items/{id}/event-impact?window=N`을 조회해 각 `game_event`의 전후 변화율을 occurred_at 내림차순으로 표시하고, `insufficient_data`의 이유를 **희소(anchor null) vs stale(anchor 있으나 오래됨)**로 구분하며, **"상관 ≠ 인과"**를 화면 상단에 분명히 고지한다. 데이터에 정직한 분석 화면이 이 프로젝트 헤드라인 신뢰성의 얼굴이다. (IMPCT-01~04)

**Read-only.** 기존 read API(`/api/items`, `/api/items/{id}/latest`, `/api/items/{id}/event-impact?window=N`)만 소비하고 백엔드 `src/`는 0줄 변경. **데이터 레이어는 Phase 7에서 이미 완성** — `useEventImpact(id, window)` 훅 · `eventImpactSchema`/`eventImpactItemSchema`(status·preAnchorAt/postAnchorAt/prePrice/postPrice/changeRate 전부 nullable) · `getEventImpact` 경계 검증(400/404 → `ApiError`)이 존재. **Phase 9가 추출한 공용 `_shared/ItemSelect`·`_shared/LatestPriceCard`** 그대로 재사용. Phase 10은 이 위에 **window 컨트롤·결과 표/카드·정직성 카피·상관≠인과 배너만** 얹는다. **차트(Recharts) 불필요** — 표 화면.

**범위 밖(다른 phase/마일스톤):** 데모 문서·정적 서빙(Phase 11), 카테고리 베이스라인 대비 초과상승률(FE-V2-05 ← 백엔드 IMPACT-V2-01), 스코프 좁힘 파라미터(from/to·event id 필터 — 백엔드 v2, 엔드포인트 계약은 `window=N`만), 실시간 자동 갱신(FE-V2-02), 관리자 쓰기 UI(FE-V2-01). 상승/하락·배지·Alert의 정확한 hex·variant·정밀 카피·타이포는 `/gsd-ui-phase 10`의 10-UI-SPEC 소관 — 본 CONTEXT는 의미→색 *방향*, 정보 *배치*, 인터랙션 *동작*까지만 잠근다.
</domain>

<decisions>
## Implementation Decisions

### window 컨트롤 & 기본값 (IMPCT-01)
- **D-01:** **프리셋 버튼 + 숫자 입력 병행.** 프리셋(예: 6/24/72h)으로 데모 클릭 한 번 + 숫자 입력으로 커스텀·범위밖(400) 도달. Phase 9 `RangeControls`(프리셋+날짜 입력)와 동일 패턴. **window는 URL searchParams(`?window=`)에 인코딩** — Phase 7 D-04 / Phase 9 D-04 정책의 연장으로 `useImpactParams`가 `useTimelineParams`(`?item=&from=&to=`)를 미러. 딥링크·스크린샷 재현성. 정확 프리셋 라벨/개수·숫자 입력 위젯 형태(native number input vs 커스텀)는 planner/10-UI-SPEC 재량.
- **D-02:** **기본 window = 24h.** URL에 `window` 없을 때의 기본값. ROADMAP 완료조건 예시값이자 수집 10분 주기 기준 이벤트 전후 하루 윈도우로 충분. 기본값은 *반환*하되 eager write 안 함(bare `/impact`는 깨끗하게, 명시 변경만 URL에 기록 — Phase 9 D-03 정책과 동일).
- **D-03:** **window 범위밖(≤0 또는 >168) 입력 허용 → 400 UI 시연.** 클라이언트 클램프 안 함. 숫자 입력으로 0·169 같은 값 전송을 허용해 400 안내 UI를 노출 — ROADMAP 검증 항목(window=0→400)·honest-data 에토스 충족. `getEventImpact`가 400을 `ApiError(400)`로 throw → Phase 9 D-09식 status 분기로 처리.

### 결과 표시 형태 & 변화율 포맷 (IMPCT-02)
- **D-04:** **반응형 표↔카드.** 넓은 화면 = 표(table), 좁은 화면 = 카드 리스트. occurred_at 내림차순(백엔드 정렬 유지, 추가 정렬 불요). 각 행/카드 = `title` · `eventType` 배지 · `occurredAt`(KST) · `prePrice` · `postPrice` · `changeRate`(또는 insufficient 이유). ROADMAP "표로" + Phase 11 반응형 마감 동시 충족. 정확 브레이크포인트·컬럼 우선순위/생략 순서는 planner/10-UI-SPEC 재량.
- **D-05:** **changeRate 상승 = 빨강 / 하락 = 파랑 (한국 관례).** 국내 거래소·주식 MTS 관례와 일치 — 대상 청중(로아 유저·국내 면접관)에게 자연스러움. 서구(상승=초록) 기각. **의미→색 방향만 잠금**, 정확 hex·시맨틱 토큰은 10-UI-SPEC 소관.
- **D-06:** **changeRate = 부호 + % + 소수 1자리** (예: `+12.3%` / `-4.0%`). 부호 명시 + 읽히는 정밀도. `prePrice`/`postPrice`는 `toLocaleString('ko-KR')` G 단위 천단위 구분(`LatestPriceCard` 관례 재사용). 정확 반올림·소수 자릿수·0% 표시는 데이터 분포 보고 planner 재량.

### insufficient_data 이유 정직성 (IMPCT-03) — honest-data 에토스의 정점
- **D-07:** **ok/insufficient_data 상태 배지 + 이유 카피.** status 배지(색 구분) + insufficient 행/카드에 이유 문구. `eventType` 배지는 Phase 9 `eventMarkers.ts`/`EventMarkerLegend`의 4색 토큰(`LOA_ON`/`MAJOR_UPDATE`/`SEASON_END`/`BALANCE_PATCH`) 재사용. 배지만(이유 생략)은 기각 — IMPCT-03이 "왜"를 명시 요구.
- **D-08:** **희소 vs stale = 앵커 시각(KST) 노출로 구분.** `preAnchorAt`/`postAnchorAt`를 KST로 표시(없으면 "없음"). **희소** = 해당 앵커 `null`(윈도우 내 스냅샷 0개), **stale** = 앵커 시각은 있으나 row가 insufficient(앵커가 이벤트 `occurred_at`에서 30분 초과 → 백엔드가 판정). 프론트는 `anchor === null` 여부 + 앵커 시각만 표시하고 **30분 임계는 하드코딩하지 않는다**(백엔드 상수와 결합 회피 — 가장 정직). 도출 규칙: `status==='insufficient_data'` & (`preAnchorAt==null` ∥ `postAnchorAt==null`) → 그쪽 **희소**; 둘 다 non-null인데 insufficient → **stale**. 백엔드 의미 근거는 Phase 5 D-02~D-05(canonical_refs; researcher 실측 권장).
- **D-09:** **ok·insufficient 같은 목록에 혼합(배지로 구분).** occurred_at 내림차순 한 목록에 함께 — "충분성/staleness 가드가 작동함"을 그대로 시연(가드가 핵심 학습·면접 신호, Phase 5 D-06과 정렬). 별도 섹션 분리는 기각(단일 정렬 서사가 끊김).

### "상관 ≠ 인과" 고지 (IMPCT-04) — 프로젝트 신뢰성의 얼굴
- **D-10:** **화면 상단 상시 배너(Alert) · 닫기 불가 · 한 문장 간결.** shadcn `alert.tsx` 재사용. 표를 보기 전에 맥락을 먼저 읽게 + 항상 노출(과대해석 차단이 목적이라 숨기지 않음). 닫기 가능·행별 각주·표 캡션은 기각(핵심 고지가 사라지거나 시선에서 약화). **의미·배치·상시성만 잠금**, 정확 카피 문구·아이콘·variant는 10-UI-SPEC 계약. 톤 예시: "이 수치는 이벤트와 가격의 *시점 상관*일 뿐, 인과를 의미하지 않습니다."

### 엣지 케이스 (사용자 위임 — 합리적 기본값 잠금)
- **D-11:** **이벤트 0개(200 + 빈 `events` 배열) → Phase 7 `EmptyState`로 "등록된 이벤트 없음".** per-row insufficient와 별개 케이스(관리자가 이벤트를 0개로 둔 경우). honest-data 빈 상태.
- **D-12:** **`LatestPriceCard`(공용, Phase 9 D-07)를 impact 화면에도 표시.** 타임라인과 일관된 "지금 얼마인지" 맥락 제공 + ROADMAP Phase 10 의존성("최신가 카드 재사용") 실현. 자기 `<AsyncBoundary>` 격리 유지(최신가 실패가 결과 표를 가리지 않음).

### Claude's Discretion
- 정확 프리셋 라벨/개수(6/24/72h는 출발점), 숫자 입력 위젯 형태, window 입력 검증 UX(입력 즉시 fetch vs 제출 버튼).
- 반응형 브레이크포인트·표 컬럼 우선순위/생략 순서, 카드 레이아웃 필드 배열.
- changeRate 정확 반올림·소수 자릿수 미세조정, 0% 변화·null 가격 표시 방식.
- impact feature 폴더 하위 분할 구조, `useImpactParams` 정확 시그니처, 결과 표/카드 컴포넌트 분리 단위.
- 상관≠인과 Alert 아이콘·variant·정확 카피(10-UI-SPEC 계약을 따름).
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·범위·API 계약
- `.planning/REQUIREMENTS.md` — **IMPCT-01~04 정의 + "실제 API 계약(코드 실측)" 표** (`/api/items/{id}/event-impact?window=N`의 정확한 필드·`status`(ok/insufficient_data)·`preAnchorAt`/`postAnchorAt`/`prePrice`/`postPrice`/`changeRate`·`window` 정수 1..168·400/404 규칙·eventType enum). **MUST read** — 표/카드 표시 필드는 이 표와 1:1.
- `.planning/ROADMAP.md` §"Phase 10: Event Impact" — Goal · 완료 조건(5) · 검증 방법(seed 데모 이벤트 non-zero changeRate · 희소/stale 구분 · window=0/200→400 · % 부합) · 사용자 확인 포인트(상관≠인과 가시성 · insufficient "왜" 납득성).

### 백엔드 의미 근거 (희소/stale 도출 — 필수)
- `.planning/phases/05-event-impact/05-CONTEXT.md` — **D-01~D-09 백엔드 의미(MUST read).** 특히 D-02(충분성=앞뒤 각 ≥1, 한쪽 0개→앵커 `null`=희소), D-03(staleness 기본 30분 절대 허용치), D-04(양쪽 앵커 모두 신선해야 change_rate), D-05(insufficient_data 응답에 발견 앵커 시각 포함·없으면 null), D-07(per-event 목록·없는 item 404·0개 200 empty), D-08(window 정수 1..168·≤0→400·상한 캡). 본 phase D-08/D-09의 의미 근거.
- `src/main/java/com/lostark/tracker/read/EventImpactService.java` + `src/main/java/com/lostark/tracker/web/dto/EventImpactItem.java` — 실제 status/anchor 산출 로직·DTO 필드. **검증 권장:** insufficient_data일 때 anchor null vs non-null 분기가 프론트 희소/stale 도출(D-08)과 정합하는지 researcher 실측(둘 다 non-null인데 insufficient = stale 가정 확인).

### 상속 결정·디자인 계약 (재정의 금지)
- `.planning/phases/09-item-timeline/09-CONTEXT.md` — **상속(MUST read):** D-01(eventType 4색 매핑), D-04(필터 상태=URL searchParams), D-05(드롭다운 셀렉터), D-06(진입 기본 선택 ?item= 우선/없으면 첫 품목), D-07(공용 `_shared/ItemSelect`·`LatestPriceCard`), D-09(400/404 `ApiError.status` 분기 카피). 본 phase가 `_shared` 컴포넌트·URL 상태·에러 분기·색 토큰을 그대로 재사용.
- `.planning/phases/07-frontend-foundation/07-CONTEXT.md` — **상속(MUST read):** D-01(React Query 훅), D-02(fetch-on-mount·폴링 없음), D-05/06(zod 경계 검증·loudly fail), D-07/08(AsyncBoundary+상태 컴포넌트), D-09(formatKst off-by-9h·정렬은 UTC), D-11(feature 폴더), D-12(api/queries 모듈).
- `.planning/phases/07-frontend-foundation/07-UI-SPEC.md` · `.planning/phases/09-item-timeline/09-UI-SPEC.md` — 디자인 시스템(shadcn slate·new-york, lucide-react, Inter, 토큰·카피 계약) + Phase 9 eventType 색·배지 시각. 상승/하락 색 hex·status 배지 색·Alert variant·정확 카피는 거기서 확정(본 CONTEXT는 의미·방향·배치만 잠금). `/gsd-ui-phase 10` 실행 시 10-UI-SPEC이 이를 상속해 표/카드·배지·배너 시각 계약을 만든다.

### 프로젝트 원칙
- `.planning/PROJECT.md` §"Key Decisions"·§"Context"·전제 5 — **상관 ≠ 인과**(이 phase 핵심·과대주장 금지), honest-data 에토스(insufficient 이유 정직 노출·null loud-fail), UTC→KST off-by-9h 가드(표시만 KST·정렬은 UTC 인스턴트), **백엔드 무변경**(read API 소비만, 사용자 제약).
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (데이터 레이어 완비 — 신규 배관 불필요)
- `frontend/src/lib/queries.ts` — **`useEventImpact(id, window)` 훅 이미 존재**(`['event-impact', id, window]` 키). `useItems()`(셀렉터), `useLatestPrice(id)`(최신가 카드).
- `frontend/src/lib/schemas.ts` — **`eventImpactSchema`·`eventImpactItemSchema`·`eventImpactStatusSchema`(`'ok'`|`'insufficient_data'`) 이미 정의.** 모든 anchor/price/changeRate 필드 `.nullable()`(insufficient 시 null — null이 실수처럼 위장 못 함). `eventTypeSchema` 4 enum. 표시 타입은 `z.infer` 파생.
- `frontend/src/lib/api.ts` — **`getEventImpact(id, window)` 경계 `.parse` + `ApiError(status, url)`** 이미 존재. 400(window≤0/>168)/404가 `ApiError`로 throw돼 status 분기(D-03) 가능.
- `frontend/src/features/_shared/{ItemSelect, LatestPriceCard}.tsx` — **Phase 9 D-07이 추출한 공용 컴포넌트.** `ItemSelect`=controlled 드롭다운(URL 무관·`onChange`만), `LatestPriceCard`=자기 `useLatestPrice`+자기 `AsyncBoundary`. 그대로 import(D-12).
- `frontend/src/features/timeline/{useTimelineParams.ts, RangeControls.tsx}` — **패턴 소스(복사 아닌 미러).** `useImpactParams`(`?item=&window=`)는 `useTimelineParams`(`?item=&from=&to=`)를 미러; window 프리셋+숫자 입력 컨트롤은 `RangeControls`(프리셋+날짜 입력) 형태 차용(D-01).
- `frontend/src/features/timeline/{eventMarkers.ts, EventMarkerLegend.tsx}` — eventType 4색 토큰(Phase 9 D-01). 표/카드의 `eventType` 배지에 재사용(D-07).
- `frontend/src/components/state/{AsyncBoundary,LoadingState,EmptyState,ErrorState}.tsx` — 컴포넌트별 상태 격리(D-12)·이벤트 0개 빈 상태(D-11)·400/404 분기.
- `frontend/src/components/ui/{card,badge,button,alert,skeleton,select}.tsx` — shadcn. 표/카드·status 배지·프리셋 버튼·상관≠인과 Alert(D-10) 빌드 블록(전부 기존, 신규 블록 불요).
- `frontend/src/lib/formatKst.ts` — `occurredAt`·anchor 시각 KST 표시(off-by-9h 가드). 정렬은 백엔드 occurred_at desc 유지.

### Established Patterns
- **컴포넌트별 `<AsyncBoundary status isEmpty onRetry>` 래핑** = 성공 경로만 작성 + 분기 중앙화(Phase 7 D-08 / Phase 8 D-07~08 / Phase 9). impact 결과 영역·최신가 카드 각각 자기 boundary.
- **`ApiError.status` 분기 카피** = Phase 9 `ChartArea`의 400/404 분기 패턴을 impact 결과 영역에 그대로(단 400 사유가 window: ≤0/>168).
- zod 경계 검증으로 null을 "loudly fail"(Phase 7 D-06). insufficient의 nullable anchor/price/changeRate를 표/카드로 silent 전파 차단.
- 모든 시각 UTC ISO-8601(`...Z`) → 표시만 KST, 정렬·계산은 UTC 인스턴트.
- URL searchParams 단일 소스(React Router `useSearchParams`) — 딥링크·스크린샷 재현성.

### Integration Points
- **`frontend/src/features/impact/ImpactPage.tsx`** = 현재 빈 골격(Phase 7, 안내 텍스트만). Phase 10에서 **이 파일을 셀렉터 + window 컨트롤 + 최신가 카드 + 상관≠인과 배너 + 결과 표/카드로 교체.** 하위 컴포넌트는 impact feature 폴더 + 공용(`_shared`).
- **신규 라이브러리 불요** — Recharts 불필요(표/카드 화면). 데이터 레이어·shadcn 블록 모두 기존.
- **URL searchParams** — `?item=&window=`. Phase 9 `?item=&from=&to=`와 동일 메커니즘에 window 정수 1개 추가.
- Vite 프록시 `/api`→`:8080`, **seed 프로파일 백엔드** 기동 전제(데모 이벤트 5분 오프셋 배치로 non-zero changeRate 의도 — Phase 6 `SyntheticDemoData`, PROJECT Key Decision).
</code_context>

<specifics>
## Specific Ideas

- **honest-data 에토스의 정점:** `insufficient_data`를 숨기지 않고 ok와 같은 목록에 배지로 섞고(D-09), 희소/stale 이유를 앵커 시각으로 정직하게 노출(D-08) — "충분성·staleness 가드가 작동함"을 화면으로 시연(면접 신호). Phase 5 백엔드 가드의 프론트 연장.
- **상관 ≠ 인과 = 프로젝트 신뢰성의 얼굴:** 상단 상시 배너로 표를 보기 전에 맥락을 강제(D-10) — "이 사람은 데이터를 과대해석하지 않는다"를 면접관이 읽는 지점.
- **한국 관례 색(상승=빨강):** 로아 유저·국내 면접관 대상이라 국내 거래소/MTS 관례를 따름(D-05) — 도메인 청중 정렬이 차별점.
- **데이터 레이어 선완성의 배당:** Phase 7에서 event-impact까지 5개 DTO를 zod로 잠갔기에 Phase 10은 표시·인터랙션·정직성 카피에만 집중 — "경계 검증을 먼저 세우면 화면은 성공 경로만 쓴다"의 실증.
</specifics>

<deferred>
## Deferred Ideas

- **카테고리 베이스라인 대비 초과상승률 시각화** — FE-V2-05(백엔드 IMPACT-V2-01 선행 필요). MVP는 단일 품목 전후 변화율까지.
- **window·staleness 임계 `@ConfigurationProperties` 외부화** — 백엔드 CFG-V2-01. 프론트는 30분 임계를 하드코딩하지 않음(D-08)으로 이미 디커플.
- **스코프 좁힘 파라미터(from/to·event id 필터)** — 백엔드 v2(엔드포인트 계약은 `window=N`만 — 스코프 크리프 방지).
- **마커 클릭 → Event Impact 딥링크** — Phase 9 deferred. `_shared` 셀렉터·URL 상태가 연결 기반이나 본 phase 범위 밖.
- **실시간 자동 갱신(폴링/SSE)** — FE-V2-02. Phase 7 D-02에서 비활성.

None other — 논의는 Phase 10 범위(IMPCT-01~04) 내에 머물렀다.
</deferred>

---

*Phase: 10-event-impact*
*Context gathered: 2026-06-26*
