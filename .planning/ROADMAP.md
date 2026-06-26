# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- 🚧 **v1.1 Frontend Demo Dashboard** — Phases 7–11 (정의됨 2026-06-25)

## Overview (v1.1)

v1.0이 만든 신뢰 가능한 read API(`/api/health/collection`, `/api/items`, `/latest`, `/prices`, `/event-impact`)를 **브라우저 대시보드로 시각화**한다. 백엔드는 건드리지 않고(Vite 프록시로 dev 동일 출처) `frontend/`에 React + TypeScript + Tailwind + Recharts 앱을 세운다. 순서: 골격·타입드 API 클라이언트(7) → Dashboard(8) → Item Timeline(9) → Event Impact(10) → 데모 표면·문서(11). 모든 화면은 **seed 프로파일 백엔드** 기준으로 비어있지 않게 완성하고, 시각은 UTC 데이터를 KST로 표시한다(off-by-9h 가드 연장).

---

## Phases (v1.0 — SHIPPED)

<details>
<summary>✅ v1.0 MVP (Phases 1–6) — SHIPPED 2026-06-25</summary>

- [x] **Phase 1: Foundation + Task 0** — 2026-06-20
- [x] **Phase 2: Collection Pipeline** — 2026-06-22
- [x] **Phase 3: Read API + Cache** — 2026-06-23
- [x] **Phase 4: Admin + Events** — 2026-06-24
- [x] **Phase 5: Event Impact (게이트 조건부)** — 2026-06-24
- [x] **Phase 6: Distribution + Docs** — 2026-06-25

전체 상세는 [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md) 참조.

</details>

## Phases (v1.1 — Frontend Demo Dashboard)

**Phase Numbering:** v1.0의 마지막 phase(6)에 이어 7부터 연속 번호.

- [x] **Phase 7: Frontend Foundation** — Vite+React+TS+Tailwind 골격, 프록시, 타입드 API 클라이언트, 앱 셸 (completed 2026-06-25)
- [x] **Phase 8: Dashboard** — collection health + 활성 품목 목록 + 품목별 최신가 요약 (completed 2026-06-25)
- [ ] **Phase 9: Item Timeline** — item selector + 최신가 카드 + 가격 라인 차트 + 이벤트 마커
- [ ] **Phase 10: Event Impact** — 이벤트별 전후 변화율 + ok/insufficient_data + 상관≠인과 문구
- [ ] **Phase 11: Demo Surface + Docs** — seed 재현 + frontend/README + 루트 README + (선택) 정적 서빙

## Phase Details

### Phase 7: Frontend Foundation

**Goal**: `frontend/`에 Vite + React + TypeScript + Tailwind 앱을 세우고, Vite 프록시로 백엔드 변경 없이 `/api`를 호출하며, 실측 DTO와 일치하는 타입드 API 클라이언트와 3화면 내비게이션·공용 상태 컴포넌트를 마련한다.
**Depends on**: Nothing (v1.1 첫 phase) — 실행 시 로컬에 seed 프로파일 백엔드가 떠 있어야 함
**Requirements**: FND-01, FND-02, FND-03, FND-04, FND-05

**완료 조건 (Success Criteria — 무엇이 TRUE여야 하나):**

  1. `frontend/`에서 `npm install && npm run dev`가 에러 없이 Vite 개발 서버를 띄운다
  2. 브라우저에서 프론트가 `/api/items`를 Vite 프록시 경유로 호출해 CORS 에러 없이 200 응답을 렌더한다 (백엔드 코드 변경 0줄)
  3. 상단 내비게이션으로 Dashboard / Item Timeline / Event Impact 라우트를 오갈 수 있다 (빈 골격 화면이라도)
  4. 로딩 / 비어있음 / 에러 상태를 그리는 공용 컴포넌트가 존재하고 최소 한 화면에서 동작한다
  5. UTC ISO-8601 시각을 KST로 포맷하는 공용 헬퍼가 있고, 타입 정의가 5개 응답 DTO 필드와 1:1 일치한다

**검증 방법 (Verification):**

  - seed 프로파일 백엔드 기동(`SPRING_PROFILES_ACTIVE=seed`) 후 `npm run dev` → 브라우저 콘솔/네트워크 탭에 CORS·404 에러 없음 확인
  - 네트워크 탭에서 `/api/items` 요청이 프록시되어 200 + 품목 배열 반환 확인
  - `npm run build`(또는 `tsc --noEmit`)가 타입 에러 0으로 통과 — 타입드 클라이언트와 DTO 일치 증명
  - 백엔드를 끄고 새로고침 → 에러 상태 컴포넌트가 뜨는지(크래시 아님) 확인

**사용자 확인 포인트 (User confirmation):**

  - dev 서버가 뜬 화면에서 3개 탭 네비게이션이 동작하고, 앱 셸의 레이아웃/타이포가 의도대로인지 직접 클릭 확인
  - "백엔드 무변경"이 지켜졌는지 — `git status`에 `src/` 변경이 없음을 함께 확인

### Phase 8: Dashboard

**Goal**: 한 화면에서 수집 파이프라인이 살아있음(health) + 추적 품목 목록 + 품목별 최신가 요약을 보여줘, 면접관이 "데이터가 실제로 흐른다"를 즉시 읽게 한다.
**Depends on**: Phase 7
**Requirements**: DASH-01, DASH-02, DASH-03, DASH-04

**완료 조건 (Success Criteria):**

  1. `/api/health/collection`의 lastRunAt(KST)·itemsAttempted/Succeeded/Failed·status가 카드로 표시된다
  2. status `NO_RUNS` / `PARTIAL_SUCCESS` / 정상이 시각적으로 구분되고 `summaryMessage`(AUTH_ERROR/RATE_LIMITED/null)가 노출된다 — 시크릿 필드는 응답에 없으므로 화면에도 없다
  3. `/api/items`의 활성 품목이 displayName·category로 목록 표시된다
  4. 각 품목의 `/api/items/{id}/latest`(minPrice, collectedAt KST)가 요약으로 보인다
  5. 품목 0개 또는 health `NO_RUNS`일 때 빈/대기 상태가 깨지지 않고 표시된다

**검증 방법 (Verification):**

  - seed 백엔드 기준: health 카드의 카운트가 `curl /api/health/collection` 응답 수치와 일치
  - 품목 목록 개수·이름이 `curl /api/items` 결과와 일치, 비활성 품목은 안 보임
  - 각 품목 카드의 minPrice가 `curl /api/items/{id}/latest`와 일치
  - (옵션) admin 시크릿으로 이벤트/품목을 바꾼 뒤 새로고침 시 반영되는지 — 단 화면에서 쓰기는 하지 않음

**사용자 확인 포인트 (User confirmation):**

  - Dashboard가 "수집이 살아있고 무엇을 추적하며 지금 얼마인지"를 한눈에 전달하는지 — 정보 위계가 맞는지 직접 확인
  - status 색/배지(특히 PARTIAL_SUCCESS·NO_RUNS)가 오해 없이 읽히는지 확인

### Phase 9: Item Timeline

**Goal**: 품목 하나의 가격 시계열을 라인 차트로 그리고, 그 위에 게임 이벤트를 세로 마커로 겹쳐 "언제·무슨 이벤트 즈음에 움직였나"를 보게 한다 — 이 프로젝트 헤드라인의 시각화.
**Depends on**: Phase 7 (Phase 8과 병렬 가능)
**Requirements**: TIME-01, TIME-02, TIME-03, TIME-04, TIME-05

**완료 조건 (Success Criteria):**

  1. item selector에서 품목을 고르면 최신가 카드와 차트가 그 품목으로 갱신된다
  2. `/api/items/{id}/prices?from=&to=`의 snapshots가 Recharts 라인으로 그려지고 x축이 KST 시간, y축이 min_price다
  3. `downsampled=true` 응답이면 배지로 표시하고 `bucketWidth`(hour/day) 평균선을 그린다 (raw면 원점선)
  4. events가 `eventType`별로 구분된 세로 마커(`ReferenceLine`)로 겹쳐지고 호버 시 title이 보인다
  5. 빈 기간(200 빈 배열) → "데이터 없음", 잘못된 기간(to≤from, 400) → 입력 안내, 없는 품목(404) → 에러 상태가 각각 표시된다

**검증 방법 (Verification):**

  - seed가 깐 8일 윈도우로 조회 → 라인에 점이 차고 데모 이벤트 2개가 마커로 보임
  - 큰 범위를 조회해 `downsampled=true`가 트리거되는지, 배지·버킷선이 뜨는지 확인 (네트워크 응답의 `downsampled`/`bucketWidth`와 화면 일치)
  - `to ≤ from`으로 조회 → 400 처리 UI, 존재하지 않는 id → 404 UI, 미래 빈 구간 → 200 empty UI 확인
  - 차트 x축 라벨이 KST인데 정렬/위치는 UTC 인스턴트 기준으로 맞는지(off-by-9h 없음) 데모 이벤트 시각과 대조

**사용자 확인 포인트 (User confirmation):**

  - 이벤트 마커가 가격선과 겹쳐 "서사"가 읽히는지 — 마커 색/범례/툴팁이 명확한지 직접 확인
  - 다운샘플 배지가 "지금 보는 건 원점이 아니라 버킷 평균"임을 정직하게 전달하는지 확인

**Plans:** 2/5 plans executed

Plans:
- **Wave 1**
  - [x] 09-01-PLAN.md — recharts/@radix-select 설치 + 수기 select 블록 + 이벤트 마커 색·버킷 라벨 토큰 (D-01/D-08)
  - [x] 09-03-PLAN.md — useTimelineParams URL 상태(?item=&from=&to=, 기본 30일) + RangeControls(프리셋·KST 날짜 입력) (D-03/D-04)
- **Wave 2** *(blocked on Wave 1 completion)*
  - [ ] 09-02-PLAN.md — 공용 ItemSelect 드롭다운 + 타임라인 전용 LatestPriceCard (D-05/D-07)
  - [ ] 09-04-PLAN.md — Recharts 가격선 차트 + eventType ReferenceLine 마커·범례 + 다운샘플 배지 (D-01/D-02/D-08)
- **Wave 3** *(blocked on Wave 2 completion)*
  - [ ] 09-05-PLAN.md — TimelinePage 통합 + 기본 선택(D-06) + 400/404/200-empty 분기(D-09) + 시각 checkpoint

### Phase 10: Event Impact

**Goal**: 이벤트별 전후 변화율을 표로 보여주되, `insufficient_data`의 이유(희소 vs stale)를 구분하고 "상관 ≠ 인과"를 분명히 고지해 — 데이터에 정직한 분석 화면을 만든다.
**Depends on**: Phase 9 (item selector·최신가 카드 재사용; Phase 9 후 진행 권장)
**Requirements**: IMPCT-01, IMPCT-02, IMPCT-03, IMPCT-04

**완료 조건 (Success Criteria):**

  1. window(시간, 1..168) 입력으로 `/api/items/{id}/event-impact?window=N`을 조회한다 (기본값 예: 24)
  2. 각 이벤트가 occurred_at 내림차순으로 title·eventType·occurredAt(KST)·prePrice·postPrice·changeRate(%)와 함께 표시된다
  3. `status=ok`와 `insufficient_data`가 배지로 구분되고, insufficient는 `preAnchorAt`/`postAnchorAt`로 희소(anchor null)와 stale(anchor 있으나 오래됨)을 구분해 이유 문구를 보여준다
  4. changeRate가 부호·퍼센트로 가독성 있게 포맷된다(상승/하락 색 구분)
  5. "상관 ≠ 인과" 안내 문구가 화면 상단/근처에 분명히 노출된다 · window≤0/>168(400), 없는 품목(404) 처리

**검증 방법 (Verification):**

  - seed의 데모 이벤트(5분 오프셋 배치로 non-zero changeRate 의도)로 조회 → ok 이벤트가 0이 아닌 변화율을 보임
  - 윈도우를 좁혀 anchor가 비도록 만들어 `insufficient_data`(희소)가 뜨는지, 오래된 구간으로 stale 케이스가 구분되는지 확인 — 응답 JSON의 status/anchor 필드와 화면 일치
  - window=0/200 입력 → 400 UI, 없는 id → 404 UI 확인
  - changeRate %·부호가 `curl` 응답 raw 값과 부합

**사용자 확인 포인트 (User confirmation):**

  - "상관 ≠ 인과" 고지가 충분히 눈에 띄어 과대해석을 막는지 직접 확인 (이 프로젝트 신뢰성의 핵심)
  - insufficient의 "왜"(희소/stale)가 사용자에게 납득되게 쓰였는지 확인

### Phase 11: Demo Surface + Docs

**Goal**: 리뷰어가 seed 백엔드 + `npm run dev`만으로 3화면을 재현하도록 문서화하고, 시각적 마감과 (선택) 단일 출처 정적 서빙으로 포트폴리오 데모 표면을 완성한다.
**Depends on**: Phase 8, Phase 9, Phase 10
**Requirements**: DEMO-01, DEMO-02, DEMO-03 *(DEMO-03은 선택/stretch)*

**완료 조건 (Success Criteria):**

  1. 새 클론에서 seed 프로파일 백엔드 기동 → `npm install && npm run dev` → 3개 화면이 전부 비어있지 않게 뜬다
  2. `frontend/README`가 사전조건(백엔드 seed 기동)·실행 명령·프록시 동작·3화면 스크린샷을 담는다
  3. 루트 README에 프론트 데모 섹션(실행 순서 + 스크린샷 링크)이 추가되되, 기존 curl 데모 표면은 유지된다
  4. 로딩/빈/에러 상태와 기본 반응형이 3화면에서 일관되게 마감된다
  5. *(선택 DEMO-03)* `npm run build` 산출물을 Spring `resources/static`에 서빙해 단일 출처(`docker compose up` 한 번)로 데모 가능 — 채택 시 정적 서빙이 read 엔드포인트와 충돌 없이 동작

**검증 방법 (Verification):**

  - 클린 클론(또는 별도 디렉터리)에서 README만 따라 3화면 재현 — 빈 화면/깨짐 없음
  - 스크린샷이 현재 화면과 일치(최신화)하는지 확인
  - `npm run build` 성공(타입체크 포함), 정적 서빙 채택 시 `docker compose up` 후 단일 포트에서 화면+API 동작
  - 루트 README의 기존 curl/샘플 JSON 섹션이 그대로 남아있는지(회귀 없음) 확인

**사용자 확인 포인트 (User confirmation):**

  - 면접관 관점에서 README가 "5분 안에 띄워볼 수 있다"를 충족하는지 직접 따라 해보고 확인
  - DEMO-03(정적 서빙)을 이번에 포함할지 / v2로 미룰지 최종 결정

## Progress

**Execution Order (v1.1):**
Phase 7 → (8 · 9 병렬 가능) → 10 → 11
(Phase 8·9는 Phase 7의 골격·타입드 클라이언트 위에서 독립적; Phase 10은 9의 selector/카드 재사용; Phase 11은 8·9·10 완료 의존.)

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 7. Frontend Foundation | v1.1 | 3/3 | Complete   | 2026-06-25 |
| 8. Dashboard | v1.1 | 3/3 | Complete   | 2026-06-25 |
| 9. Item Timeline | v1.1 | 2/5 | In Progress|  |
| 10. Event Impact | v1.1 | 0/? | Not started | — |
| 11. Demo Surface + Docs | v1.1 | 0/? | Not started | — |

*Plan 수는 `/gsd-plan-phase [N]`에서 확정.*
