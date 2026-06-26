# Requirements: 로스트아크 거래소 시세 트래커 — v1.1 Frontend Demo Dashboard

**Defined:** 2026-06-25
**Milestone:** v1.1 Frontend Demo Dashboard
**Core Value (this milestone):** v1.0의 신뢰 가능한 수집·서빙 파이프라인이 만든 데이터를, README의 curl 데모 대신 **브라우저 대시보드로 직관적으로 보여줘** 면접관이 클릭 한 번으로 백엔드의 가치를 읽게 한다. (백엔드 Core Value 자체는 v1.0에서 불변.)

> 선행 사실: v1.0 MVP(24/24 요구사항) 배포·검증 완료. 이 마일스톤은 기존 **read 엔드포인트만 소비**하며 수집 파이프라인을 건드리지 않는다.

## 확정 결정 (이 마일스톤)

| Decision | 선택 | 근거 |
|----------|------|------|
| 차트 라이브러리 | **Recharts** | React 선언형 `<LineChart>`+`<ReferenceLine>`로 이벤트 세로 마커가 자연스럽고 TS 지원 좋음, 시계열 1차트 데모에 가장 가벼운 적합안 |
| 백엔드 연동 | **Vite 프록시 (dev, 백엔드 무변경)** | `vite.config` `server.proxy`로 `/api`→`localhost:8080` → 브라우저가 동일 출처로 인식, CORS 불필요, dev에서 백엔드 코드 0줄 변경. 단일 출처 정적 서빙은 선택(stretch) phase로 분리 |
| 마일스톤 버전 | **v1.1** | 백엔드 동작 무변경 + 기존 API 위 데모 표면 추가 → minor |
| 데이터 기준 | **seed 프로파일 우선** | 합성 8일 스냅샷 + 데모 이벤트로 빈 화면 없이 3화면 재현(실키 불필요), 실데이터는 동일 화면이 그대로 수용 |

## 실제 API 계약 (코드 실측, 2026-06-25)

프론트가 소비하는 공개 read 엔드포인트(전부 `permitAll`, 인증 불필요):

| 엔드포인트 | 응답 형태 |
|-----------|-----------|
| `GET /api/health/collection` | `{ lastRunAt, startedAt, itemsAttempted, itemsSucceeded, itemsFailed, status, summaryMessage }` · status ∈ `NO_RUNS`/`PARTIAL_SUCCESS`/그 외, summaryMessage ∈ `AUTH_ERROR`/`RATE_LIMITED`/null (시크릿 없음) |
| `GET /api/items` | `[ { id, externalItemId, displayName, category, active } ]` — active만, displayName 정렬 |
| `GET /api/items/{id}/latest` | `{ itemId, minPrice, collectedAt }` (Redis 캐시) |
| `GET /api/items/{id}/prices?from=&to=` | `{ downsampled, bucketWidth, snapshots: [ { collectedAt, minPrice, sampleCount } ], events: [ { occurredAt, eventType, title } ] }` · 400(to≤from) / 404(없는 품목) / 200-empty(빈 범위) |
| `GET /api/items/{id}/event-impact?window=N` | `{ itemId, window, events: [ { id, eventType, title, occurredAt, status, preAnchorAt, postAnchorAt, prePrice, postPrice, changeRate } ] }` · status ∈ `ok`/`insufficient_data` · window 정수 1..168 · 400/404 규칙 동일 |

- `eventType` ∈ `LOA_ON` / `MAJOR_UPDATE` / `SEASON_END` / `BALANCE_PATCH`
- 모든 시각은 UTC ISO-8601(`...Z`). **화면은 KST로 표시하되 데이터는 UTC 원본 유지** (v1.0의 off-by-9h 가드를 UI까지 연장).
- `changeRate`는 **시점 상관**이지 인과가 아니다 (PROJECT 전제 5).

## v1.1 Requirements

각 항목은 로드맵 페이즈(7~11)에 매핑된다. 사용자 중심·테스트 가능·원자적.

### Frontend Foundation (FND) — Phase 7

- [x] **FND-01**: 개발자가 `frontend/`에서 `npm run dev`로 Vite + React + TypeScript + Tailwind 앱을 띄울 수 있다
- [x] **FND-02**: 브라우저가 Vite 프록시를 통해 `/api/*`를 로컬 백엔드(:8080)로 호출하고 CORS 에러 없이 응답을 받는다 (타입 정의가 위 실측 DTO 필드와 일치)
- [x] **FND-03**: 사용자가 상단 내비게이션으로 Dashboard / Item Timeline / Event Impact 3개 화면을 오갈 수 있다
- [x] **FND-04**: 모든 화면이 로딩 / 비어있음 / 에러 상태를 일관된 공용 컴포넌트로 표시한다
- [x] **FND-05**: UTC로 내려온 시각이 화면에서 KST로 표시되고(off-by-9h 방지), 차트·정렬 계산은 UTC 인스턴트 기준이다

### Dashboard (DASH) — Phase 8

- [x] **DASH-01**: 사용자가 Dashboard에서 수집 파이프라인 헬스(마지막 실행 시각·시도/성공/실패 카운트·status)를 본다
- [x] **DASH-02**: status가 `NO_RUNS` / `PARTIAL_SUCCESS` / 정상일 때 각각 구분되게 표시되고 `summaryMessage` 마커가 노출된다 (시크릿 노출 없음)
- [x] **DASH-03**: 사용자가 추적 중인 활성 품목 목록(displayName, category)을 본다
- [x] **DASH-04**: 각 품목의 최신 시세(minPrice, 수집 시각 KST)가 요약 카드로 보인다

### Item Timeline (TIME) — Phase 9

- [x] **TIME-01**: 사용자가 품목을 선택하면 해당 품목의 최신가 카드가 보인다
- [x] **TIME-02**: 선택 품목의 가격 시계열(min_price)이 Recharts 라인 차트로 그려진다 (x축 시간 KST, 데이터 UTC)
- [x] **TIME-03**: 응답이 다운샘플(`downsampled=true`)이면 이를 배지로 표시하고 `bucketWidth` 버킷 평균선을 그린다 (raw일 땐 원점)
- [x] **TIME-04**: 기간과 겹치는 게임 이벤트가 차트 위 세로 마커(`ReferenceLine`)로 `eventType`별 구분·title 툴팁과 함께 표시된다
- [x] **TIME-05**: 사용자가 조회 기간(from/to)을 바꿀 수 있고, 빈 기간(200 empty)·잘못된 기간(400)·없는 품목(404)이 각각 적절히 처리된다

### Event Impact (IMPCT) — Phase 10

- [ ] **IMPCT-01**: 사용자가 window(시간, 1..168)를 지정해 선택 품목의 이벤트별 전후 변화율을 조회한다
- [ ] **IMPCT-02**: 각 이벤트가 prePrice / postPrice / changeRate(%) 와 함께 occurred_at 내림차순으로 표시된다
- [ ] **IMPCT-03**: `status=ok` 와 `insufficient_data` 가 구분 표시되고, insufficient는 희소(anchor null) vs stale(anchor 있으나 오래됨)을 `preAnchorAt`/`postAnchorAt`로 구분해 이유를 보여준다
- [ ] **IMPCT-04**: "상관 ≠ 인과(이벤트가 가격을 올렸다고 단정하지 않음)" 안내 문구가 화면에 분명히 노출된다

### Demo Surface (DEMO) — Phase 11

- [ ] **DEMO-01**: 리뷰어가 seed 프로파일 백엔드 + `npm run dev`만으로 3개 화면을 전부 비어있지 않게 재현할 수 있다
- [ ] **DEMO-02**: `frontend/README` + 루트 README가 실행 순서(seed 백엔드 → dev 서버 → 화면)와 스크린샷을 문서화한다
- [ ] **DEMO-03** *(선택/stretch)*: Spring 정적 서빙으로 프론트 빌드를 단일 출처(`docker compose up` 한 번)에 패키징한다 — 명시적 선택 항목, 슬립 시 v2

## Future Requirements (v2 / 연기)

추적하되 이 마일스톤 범위 밖.

- **FE-V2-01**: 관리자 쓰기 UI(이벤트/품목 CRUD를 `X-Admin-Secret`로 화면에서) — MVP는 read-only 시각화
- **FE-V2-02**: 실시간 자동 갱신(폴링/SSE/WebSocket) — MVP는 수동/마운트 시 fetch
- **FE-V2-03**: 다크모드·테마 토글, i18n
- **FE-V2-04**: 실배포(Railway/Fly/Render) + 프론트 CI(타입체크/빌드) 게이트 — backend `DEPLOY-V2-01`와 함께
- **FE-V2-05**: 카테고리 베이스라인 대비 초과상승률 시각화 (backend `IMPACT-V2-01` 선행 필요)

## Out of Scope

명시적 제외. 스코프 크리프 방지용.

| Feature | Reason |
|---------|--------|
| 수집 파이프라인/백엔드 도메인 로직 변경 | 이 마일스톤은 기존 read API **소비만** — 파이프라인 불변경(사용자 제약) |
| 인증 / 회원가입 / 세션 | 데모 시각화, 공개 read 엔드포인트만 사용 |
| 실시간 WebSocket / 푸시 | MVP는 fetch-on-load (FE-V2-02로 연기) |
| 관리자 쓰기 UI(CRUD 화면) | MVP는 read-only 시각화 (FE-V2-01로 연기) |
| 배포 자동화(CD) | MVP는 로컬 재현 데모 (FE-V2-04로 연기) |
| 실 API 키 의존 | seed 프로파일 기준으로 화면 완성 — 실데이터는 동일 화면이 수용 |
| Spring CORS 설정 추가 | Vite 프록시로 dev 동일 출처 달성 → 백엔드 변경 회피 (단일 출처는 DEMO-03 정적 서빙) |

## Traceability

페이즈별 요구사항 매핑. 로드맵 생성 시 채워진다.

| Requirement | Phase | Status |
|-------------|-------|--------|
| FND-01..05 | Phase 7 | Pending |
| DASH-01..04 | Phase 8 | Pending |
| TIME-01..05 | Phase 9 | Pending |
| IMPCT-01..04 | Phase 10 | Pending |
| DEMO-01..03 | Phase 11 | Pending |

**Coverage:**
- v1.1 requirements: 21 total (FND 5 + DASH 4 + TIME 5 + IMPCT 4 + DEMO 3)
- Mapped to phases: 21
- Unmapped: 0 ✓
- DEMO-03은 선택(stretch) — 슬립 가능, 나머지 20개는 필수

---
*Requirements defined: 2026-06-25 — v1.1 Frontend Demo Dashboard*
