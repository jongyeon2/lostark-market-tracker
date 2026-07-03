# Phase 17: 실데이터 전환 - Context

**Gathered:** 2026-07-03
**Status:** Ready for planning

<domain>
## Phase Boundary

데모를 seed 합성 데이터 기준에서 **실수집(collection_run 기반) 데이터** 기준으로 전환한다. 실 API 키로 수집기를 구동해 실데이터를 축적하고(REALDATA-01), 3화면이 실수집 데이터를 표시하며 seed 프로파일은 로컬/테스트 전용으로 격리하고(REALDATA-02), 초기 미축적 상태를 빈 화면 없이 '수집 중/데이터 없음'으로 정직히 표시한다(REALDATA-03). 수집/캐시/event-impact 핵심 경로는 회귀 없이 그린(Core Value 가드).

**이 페이즈의 실제 델타(스카우트 확인):** 백엔드 수집 로직은 **0줄**. 변경은 (1) 실데이터 검증(실키로 로컬 1회 구동 → `collection_run` SUCCESS·실스냅샷 적재), (2) 프론트 빈 상태 정직성(collection_run 기반 '수집 중/데이터 없음' + 화면별 문구), (3) seed 격리·볼륨 위생, (4) README/데모 서사 재편에 한정된다.

**In scope:** 실데이터 모드 전환(프로파일 기본값) · 실키 로컬 수집 검증 · 빈 상태 정직 표시(3화면) · `EmptyState` seed 하드코딩 copy 교체 · seed 프로파일 로컬/테스트 전용 격리 · README·frontend/README 서사 재편.
**Out of scope (Phase 18):** 공개 배포 · Dockerfile · 라이브 데모 URL · 배포 전용 `prod`/`live` 프로파일 · 서빙 방식(정적/단일 출처) · 배포 직전 보안 게이트.

</domain>

<decisions>
## Implementation Decisions

### 전환 범위 & 프로파일 (REALDATA-01)
- **D-01:** Phase 17 = 실데이터 **모드 전환 + 로컬 검증**까지. REALDATA-01 성공기준("실 API 키로 수집기가 구동되어 실데이터가 축적된다 — collection_run SUCCESS")은 **실키로 로컬 1회 구동**해 `collection_run` SUCCESS + 실스냅샷 적재를 확인하는 것으로 충족한다. 시간에 걸친 상시 축적과 공개 배포는 **Phase 18**. 경계를 깨끗이 유지해 18과 중복 없음.
- **D-02:** 라이브 실행 프로파일 = 기존 **`dev` 재사용**. `application-dev.yml`이 이미 `LOSTARK_API_KEY`로 실수집(initial-delay-ms=10000)하므로 Phase 17은 dev로 실데이터를 검증·전환한다. 배포 전용 `prod`/`live` 프로파일(외부 DB URL·프로덕션 로깅·서빙 방식)은 **Phase 18에서 신설** — 지금 새 프로파일을 만들면 18과 겹친다.

### 초기 빈 상태 정직성 (REALDATA-03)
- **D-03:** 빈 상태를 **collection_run 상태 기반**으로 구분한다 — '수집 중'(run 존재/SUCCESS인데 스냅샷이 아직 희소·직전 시작) vs '데이터 없음'(NO_RUNS 또는 실패). 판정 근거는 기존 **`GET /api/health/collection`**(`CollectionHealthResponse.status`, `NO_RUNS` 포함) 재사용 — 프론트 `HealthCard`가 이미 이 엔드포인트를 소비. REALDATA-03의 "'수집 중/데이터 없음'" 문구와 정확히 정합하고, "수집 파이프라인이 살아있는가"를 정직히 보여주는 Core Value 가시화의 프론트 확장.
- **D-04:** 빈 상태 문구는 **화면별 맞춤**. Dashboard(최신가 수집 중) · Timeline(이 품목 시계열 수집 중) · Impact(이벤트 전후 비교엔 더 많은 데이터·이벤트 필요). 기존 `AsyncBoundary`의 per-screen `emptyHeading`/`emptyBody` override를 활용한다. Impact는 **기존 sparse/stale 가드 위에** 얹으며 재구현하지 않는다 — 각 화면이 비는 이유가 다르므로 자연스러움.
- **D-05:** 현재 `EmptyState` 기본 copy가 seed 하드코딩(`'seed 프로파일 백엔드를 기동하면 시세가 채워집니다 (SPRING_PROFILES_ACTIVE=seed).'`)이므로 **실데이터 기준 문구로 교체**한다. 공유 기본값을 실데이터 서사로 바꾸고, 화면별 세부는 D-04의 override로 처리.

### seed 격리 & 볼륨 위생 (REALDATA-02, Core Value 가드)
- **D-06:** `SeedDataRunner`의 **`@Profile("seed")` 코드 격리를 유지**한다(REALDATA-02 "seed 프로파일은 로컬/테스트 전용으로만 남는다"). 실데이터(dev) 인스턴스는 seed를 심지 않으므로 프로파일 수준에서 자연 격리 — 별도 런타임 감지/거부 코드는 추가하지 않는다(Core Value 0줄 가드와 정합, 1인 데모엔 과함).
- **D-07:** 실데이터 검증은 **깨끗한 볼륨에서 시작**한다(`docker compose down -v` 또는 새 DB). 기존 dev 볼륨에 과거 seed/합성 `collection_run`·snapshot이 남아 있으면 "실수집 데이터만" 증명이 오염된다 — **11-04 gap closure 전례**(과거 seed 합성 SUCCESS run이 헬스 카드에 표시)가 근거.
- **D-08:** **불변 가드** — 수집(`PriceCollector`)/캐시/event-impact 로직 **0줄**. Phase 17 델타는 read-path 문구·문서·프로파일 기본값·검증 절차에 한정한다.

### 데모 서사 재편 (REALDATA-02)
- **D-09:** README "한눈에 3단계 재현"은 **seed를 로컬 재현 경로로 유지**한다(API 키 불필요, 리뷰어 진입장벽 0). 단 seed 라벨을 **'로컬/테스트 전용 · 합성 데이터'**로 명확히 하고, 실데이터(dev)를 **'라이브 데모의 실체'**로 헤드라인 재프레임한다. 공개 라이브 URL 추가는 Phase 18.
- **D-10:** `README.md`와 `frontend/README.md`를 함께 재편한다(단일 진실 원천 유지) — seed↔실데이터 역할 구분을 정직히 서술.

### Claude's Discretion
- 화면별 빈 상태의 정확한 카피 문구·아이콘·톤(07-UI-SPEC 카피 계약·상태 컴포넌트 패턴과 일관되게).
- '수집 중' vs '데이터 없음' 판정 임계값(예: 스냅샷 0인지, run SUCCESS 유무인지)과 프론트 판정 로직 배치(쿼리 파생 상태 vs 컴포넌트 로컬).
- `EmptyState` 공유 기본 문구의 정확한 문안 및 override prop 전달 지점.
- README 재편의 정확한 문장·섹션 배치·seed 라벨 문안.
- 깨끗한 볼륨 시작(D-07)을 검증 절차/실행 노트로 문서화할지 여부와 위치.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·목표·경계
- `.planning/ROADMAP.md` §"Phase 17: 실데이터 전환" — Goal + 성공기준 4개(실키 수집 SUCCESS · 3화면 실데이터·seed 로컬/테스트 전용 · 초기 빈 상태 정직 · Core Value 그린)
- `.planning/REQUIREMENTS.md` — REALDATA-01/02/03 + 불변 제약(프론트 직접 호출 금지·실키 미기재·Core Value 가드)
- `.planning/PROJECT.md` — Core Value(수집 신뢰성), v1.3 핵심 결정(데모=실데이터만·조기 배포 후 축적·seed 로컬/테스트 전용), Key Decisions 표(특히 11-04 seed 합성 collection_run gap closure)

### 프로파일 & 수집 (백엔드 — 필수 실측, 로직 무변경)
- `src/main/resources/application.yml` — 기본 설정(`lostark.api.key`·`admin.api.secret` env only, Flyway, JPA validate)
- `src/main/resources/application-dev.yml` — **dev 프로파일**(실수집, `initial-delay-ms=10000`) — D-02 라이브 실행 프로파일
- `src/main/resources/application-seed.yml` — **seed 프로파일**(합성, `initial-delay-ms=3600000`으로 수집기 밀어냄) — D-06 격리 유지 대상
- `src/main/java/com/lostark/tracker/seed/SeedDataRunner.java` — `@Profile("seed")` `@Order(2)` 합성 시더(D-06)
- `src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java` — 합성 데이터 생성기(참조)
- `src/main/java/com/lostark/tracker/collect/PriceCollector.java` · `CollectionConfig.java` · `WatchlistSeeder.java` — 실수집 스케줄러·워치리스트(**불변, 0줄** — D-08)

### 빈 상태 & 헬스 (프론트/백엔드 계약)
- `src/main/java/com/lostark/tracker/web/dto/CollectionHealthResponse.java` + `src/main/java/com/lostark/tracker/health/CollectionHealthService.java` + `web/HealthController.java` — `GET /api/health/collection`: `status`(`NO_RUNS` 포함)·시도/성공/실패 카운트·시각. **D-03 빈 상태 판정 근거(무변경 소비)**
- `frontend/src/components/state/EmptyState.tsx` — seed 하드코딩 기본 copy(**교체 대상, D-05**)
- `frontend/src/components/state/AsyncBoundary.tsx` — per-screen `emptyHeading`/`emptyBody` override(D-04)
- `frontend/src/features/dashboard/HealthCard.tsx` — `/health/collection` 소비 선례(빈 상태 판정 참조)

### 데모 서사
- `README.md` §"실행 방법"(L75–116) — "한눈에 3단계"·"두 가지 실행 프로파일"(**D-09 재편 대상**)
- `frontend/README.md` — 프론트 실행/seed 안내(**D-10 함께 재편**)

외부 ADR/spec 문서는 없음 — API·프로파일 계약은 위 소스 실측이 단일 출처이고, 나머지 결정은 위 Implementation Decisions에 완전 캡처됨.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `application-{dev,seed,test}.yml` — 프로파일 격리 기제(D-02/D-06): dev=실수집, seed=합성(수집기 1h 밀어냄), test=Testcontainers(1h). 실데이터/합성/테스트 분리가 이미 확립.
- `CollectionHealthResponse` / `HealthController`(`GET /api/health/collection`) — collection_run 상태 단일 출처(D-03). `NO_RUNS`/SUCCESS/부분/실패 status 제공, 최신 1건 반환.
- `AsyncBoundary` + `EmptyState`/`LoadingState`/`ErrorState` — 상태 컴포넌트. per-screen override로 화면별 빈 상태 문구 처리(D-04/05).
- `HealthCard`(dashboard) — `/health/collection` 소비 선례. 빈 상태 판정 로직 참조점.
- `SeedDataRunner`(`@Profile("seed")` `@Order(2)`) — 이미 프로파일 격리(D-06). 코드 유지.

### Established Patterns
- **Spring 프로파일 격리(dev/seed/test)** — 실데이터/합성/테스트 분리의 기존 기제. Phase 17은 이 위에서 "실데이터=dev, seed=로컬/테스트" 서사를 확정.
- **"loudly fail" + zod `.parse`-at-boundary** — 프론트 상태 처리(Phase 7). 빈/에러/로딩은 상태 컴포넌트로.
- **collection_run 실행 이력 → `/health/collection` → 헬스 카드** — 수집 신뢰성 가시화(Core Value). D-03이 이 가시화를 3화면 빈 상태 판정으로 확장.
- **11-04 gap closure 전례** — seed 합성 SUCCESS run이 stale run을 덮어 헬스 카드에 표시된 이슈. 실데이터 볼륨 위생(D-07)의 직접 근거.

### Integration Points
- 프론트 3화면 빈 상태: `DashboardPage`/`TimelinePage`/`ImpactPage`가 `AsyncBoundary`를 소비하는 지점(D-04) — 화면별 `emptyHeading`/`emptyBody` override 주입.
- `EmptyState` 기본 copy 교체(D-05) — 공유 컴포넌트 1곳.
- `README.md` + `frontend/README.md` 재편(D-09/10) — 단일 진실 원천.
- 검증: dev 프로파일 실키 구동 → `collection_run` SUCCESS + 실스냅샷(REALDATA-01), 깨끗한 볼륨에서(D-07).

</code_context>

<specifics>
## Specific Ideas

- **포트폴리오 정직성 이중 트랙 서사:** "합성(seed)으로 언제든 재현 + 실수집(dev)이 라이브의 실체"를 정직히 구분한다 — 면접 설명 포인트. seed는 리뷰어 진입장벽 0을 위해 존치하되 '합성'임을 숨기지 않는다(D-09).
- **'수집 중/데이터 없음' 구분(D-03)** 은 단순한 빈 화면 회피를 넘어, `collection_run`으로 "수집 파이프라인이 살아있는가"를 정직히 보여주는 **Core Value 가시화의 프론트 확장**이다.

</specifics>

<deferred>
## Deferred Ideas

- **공개 배포 · Dockerfile · 라이브 데모 URL · `prod`/`live` 프로파일 · 서빙 방식(정적/단일 출처) · 배포 직전 보안 게이트** — Phase 18 (DEPLOY-01..04).
- **실데이터 상시 축적(시간에 걸친 시계열·event-impact 성숙)** — 배포 후 자연 축적(Phase 18 이후 운영). Phase 17은 "축적이 시작됨"을 로컬 검증까지만.
- **수집 부트스트랩 정교화(initial-delay·watchlist 타이밍)** — 이미 dev에 존재(quick 260630-em5로 `collection.initial-delay-ms=10000` 반영). 추가 고도화는 필요 시 후속.

None 외 모두 위에 보존됨 — 논의는 Phase 17 스코프(실데이터로 어떻게 수집·서빙·표시·격리·서술하나) 안에 머물렀다.

</deferred>

---

*Phase: 17-real-data-transition*
*Context gathered: 2026-07-03*
