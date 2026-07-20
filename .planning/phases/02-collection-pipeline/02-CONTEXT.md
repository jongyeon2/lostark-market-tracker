# Phase 2: Collection Pipeline - Context

**Gathered:** 2026-06-21
**Status:** Ready for planning

<domain>
## Phase Boundary

10분 주기(`fixedDelay`)로 거래소 시세를 **신뢰 가능하게** 수집한다 — 레이트리밋(직접 만든 Redis 토큰버킷), 429/401/5xx 구분 처리, 틱 내 병렬 팬아웃 + 부분 실패 격리, `collection_run` 실행 이력 기록. (COLL-01..05)

이 페이즈는 "수집기가 돌아 `price_snapshot`이 멱등하게 쌓이고, 실행 이력이 남는다"까지 책임진다. **`/api/health/collection` 엔드포인트 자체는 Phase 3(OPS-01)** 의 몫 — Phase 2는 그 출처가 되는 `collection_run` 데이터(인증 에러 마커 포함)를 생산한다. 조회 API·캐시·관리자·event-impact는 후속 페이즈.

**Phase 1에서 잠긴 것(planner 변경 금지):** 4테이블 데이터 모델 / `price_snapshot UNIQUE(tracked_item_id, collected_at)` / 모든 시간 컬럼 `TIMESTAMPTZ`(UTC) / HTTP=RestClient(D-03) / `min_price`=`CurrentMinPrice`, `avg_price`·`trade_count`는 per-tick 제외(Task 0) / DB=PostgreSQL, 테스트=Testcontainers.

</domain>

<decisions>
## Implementation Decisions

### 워치리스트 부트스트랩
- **D-01:** `tracked_item`은 **데이터 시더(`ApplicationRunner`)** 로 채운다 — 큐레이션된 실제 워치리스트를 `external_item_id` 기준 **멱등 upsert**. Flyway 시드 마이그레이션 배제(D-02 "Flyway=스키마 소유, 데이터 아님"과 충돌). Phase 4 관리자 CRUD가 나중에 추가/삭제를 이 위에 얹는다. 시더는 `test` 프로파일에선 비활성(테스트가 자기 데이터를 insert) — planner 재량.
- **D-02:** 워치리스트 구성은 **고변동 큐레이션 소수(~10-20개)** — 융화재료/재련재료 등 로아온·시즌 종료·대형 업데이트 시 변동 큰 핵심 품목. Task 0에서 확인한 실제 `Id`(예: `66102101` 수호석 조각) + 도메인 지식 기반. 레이트 예산 여유 + 데모 신호 선명.

### 레이트리미터 (02-01 핵심 학습 쇼케이스)
- **D-03:** **직접 구현 Redis 토큰버킷 (lazy refill + Lua 원자성)**. 토큰 수 + 마지막 리필 시각을 Redis에 저장, 접근 시 경과시간으로 리필 계산(lazy), **Lua 스크립트로 consume+refill 원자화**. 앱 재시작 시 Redis 값에서 토큰 수 복원(**성공기준 2 직접 증명**). Bucket4j 배제 — "재시작 복원"이 라이브러리 뒤로 숨어 설명 신호가 약해짐. (PROJECT.md "레이트리밋=의도적 학습 쇼케이스"와 정렬.) 단일 API 키 → **글로벌 버킷 1개**(per-category 분할 아님).
- **D-04:** **순수 client-side 버킷** — 버킷이 단일 진실원, 설정된 레이트(100/min, 여유 두면 더 낮게)로만 스로틀. `x-ratelimit-limit/remaining/reset` 헤더는 **로그/관측용**(적극 reconcile 안 함). 429 `Retry-After`만 기존 재시도 경로에서 존중.

### 수집 팬아웃 & 동시성 (02-02)
- **D-05:** 팬아웃 단위 = **품목별 `ItemName` 호출**. 워치리스트 각 품목마다 `POST /markets/items`(`CategoryCode`+`ItemName`) 1콜, **응답을 `external_item_id`(Id)로 정확 대조**해 품목 선택. `ItemName`은 부분일치 필터이므로 Id 매칭 필수, 못 찾으면 skip-light. ~10-20콜/틱으로 100/min 여유. **COLL-02 "카테고리 호출"의 문자적 표현과 갈리지만**, 요구의 의도(병렬 팬아웃 + 전부 await + per-call 타임아웃)는 충족 — 다운스트림은 이 해석을 따른다. (실시간 `min_price`는 detail 엔드포인트에 없어 list 호출이 강제됨 — Task 0.)
- **D-06:** 동시성 수단 = **Spring `@Async` + `CompletableFuture.allOf().join()`** (설계 1A 명시 + 학습 쇼케이스). `@Async`×JPA LazyInit는 **새 트랜잭션 + 엔티티 ID 전달**로 처리. `fixedDelay`가 동기 틱을 직렬화 → 분산 락 불필요(1A). `@EnableScheduling`/`@EnableAsync` + 전용 `TaskExecutor` 구성.
- **D-07:** 타임아웃 = **보수적 고정값(per-call ~5s, 전체 join ~60-90s)**, `join 타임아웃 < 틱 간격(10분)`. 상수로 박는다(`@ConfigurationProperties` 외부화는 v2). **설계가 지목한 유일 CRITICAL gap** — 미설정 시 한 품목 hang이 틱 무한 대기.

### 에러 처리 & 재시도 (사용자 확정 — 02-01/02-03)
- **D-08 (401/403, fatal auth):** 401/403은 인증/권한 문제 → **재시도 안 함**. 잘못/만료된 키 신호로 보고 **새 외부 호출 중단**, 이미 진행 중인 fan-out은 **실패로 수렴**. `collection_run`: `succeeded=0`이면 `FAILED`, 일부 성공 후 auth가 터지면 `PARTIAL_SUCCESS`로 기록하되 `summary_message`에 `AUTH_ERROR` 마커. /health(Phase 3)는 마지막 run의 auth-error 여부만 노출 — **API 키/민감정보 절대 노출 금지**.
- **D-09 (429, rate-limited):** `Retry-After` 헤더 있으면 **우선 존중**, 없으면 **지수 백오프**. **최대 3회 재시도** 후에도 실패 시 해당 품목 실패 처리(`price_snapshot` 미저장). `failed` 카운트 + `summary_message`에 429 기록.
- **D-10 (5xx / timeout, transient):** **품목 단위 재시도** 대상 — 지수 백오프 + 최대 3회. 초과 시 **해당 품목만 실패**(다른 품목 적재 안 막음 — 성공기준 4). 일부 실패=`PARTIAL_SUCCESS`, 전건 실패=`FAILED`.
- **D-11 (그 외 4xx, 400/404):** 401/403/429 제외 4xx는 **재시도 안 함** — 요청 조건/매칭 문제로 보고 품목 실패 처리(`price_snapshot` 미저장), 사유를 `collection_run` summary 또는 item-level 실패 로그에 남김.

### Run 기록 & 상태 (02-03)
- **D-12:** `collection_run` 라이프사이클 = **시작 시 생성(`status=RUNNING`, `started_at`) → 종료 시 업데이트(`finished_at` + 카운트 + 최종 status)**. 프로세스가 틱 중간에 죽으면 `RUNNING` 행이 남아 "프로세스 다운"이 가시화(설계 "구멍이 수집 실패인지 다운인지 구분" 직접 충족). 쓰기 2회.
- **D-13:** status 어휘 = `RUNNING` → 종료 시 카운트 기반 `SUCCESS`(failed=0) / `PARTIAL_SUCCESS`(0<failed<attempted, 또는 일부성공 후 fatal) / `FAILED`(전건 실패 또는 fatal·succeeded=0). 어휘 미세조정은 planner 재량.
- **D-14:** `collection_run`에 **nullable `summary_message`(또는 `note`) 컬럼을 신규 Flyway 마이그레이션(V2)으로 추가** — `AUTH_ERROR`/429 등 마커 운반용. D-02(Flyway 포워드 마이그레이션) + Task 0이 예고한 후속 마이그레이션 패턴과 일관. status는 categorical enum 그대로 유지.

### 스냅샷 멱등성 — collected_at 정규화 (사용자 확정)
- **D-15:** `collected_at` = 각 품목 fetch 완료 시각이 **아님**. **수집 run의 공통 논리 시각** — run 시작 UTC `Instant`를 **분 단위 truncate**, 한 run의 **모든 `price_snapshot`이 같은 `collected_at` 공유**. 각 품목 실제 호출 완료 시각은 **`fetched_at`**(이미 엔티티에 존재)으로 따로 저장.
- **D-16:** `UNIQUE(tracked_item_id, collected_at)`가 논리 tick 기준 중복 스냅샷을 막는다. **같은 run 재시도해도 `collected_at` 불변** → 재시도 멱등. 수동 재실행으로 `(tracked_item_id, collected_at)` 충돌 시 중복 저장 안 함 — **기본 정책 skip 선호**(idempotent skip vs update를 planner가 명시).

### 테스트 & 프로세스
- **D-17:** Phase 2 PLAN은 위 정책들을 **테스트 가능한 단위**로 쪼갠다. 필수 테스트: 토큰버킷 소진·회복·**재시작 복원**, 401/403 fatal auth, 429 Retry-After·max3, 5xx/timeout 재시도, per-call/join 타임아웃(hang 1품목이 틱 안 막음), 부분 실패 격리(PARTIAL_SUCCESS/FAILED), `collected_at` 공유, 중복 스냅샷 방지. **Testcontainers(Postgres+Redis) 통합테스트 중심**(토큰버킷·@Async 트랜잭션·스케줄러는 단위 테스트만으론 거짓말함 — 설계 Test Strategy). **GSD가 phase/plan/verify의 source of truth**, Superpowers는 구현 시 **TDD + 코드리뷰 규율로만** 적용.

### Claude's Discretion
- 패키지/레이어 구조, `TaskExecutor` 풀 크기·큐, Lua 스크립트 세부, 토큰버킷 키 네이밍/TTL, 시더 품목 정확 목록, 백오프 base/jitter, status 어휘 미세조정, 중복 충돌 skip vs update 최종 선택, 시더의 test-프로파일 비활성 방식 — planner/executor 재량.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 설계 / 리뷰 (필수)
- `docs/specs/2026-06-19-project-design.md` — 하단 **"엔지니어링 리뷰 반영"** 섹션이 구현 확정 레이어. Phase 2는 특히: **§Failure Modes**(유일 critical = 팬아웃 타임아웃 미설정), **§Test Strategy**(통합테스트 중심 + GAP 묶음), **§Data Model Decisions**(collected_at 틱 정규화, fetched_at 분리), **§Worktree A. Collection**(LostarkApiClient/RateLimiter/PriceCollector/collection_run), **§Implementation Tasks T1·T2·T4·T5·T11**(팬아웃+타임아웃, UNIQUE 멱등, 429/401/5xx, collection_run, 토큰버킷 재시작).
- `docs/specs/2026-06-20-eng-review-test-plan.md` — 엔지니어링 리뷰 테스트 플랜(엣지 케이스 대상: 토큰버킷 재시작, @Async 트랜잭션 멱등, 429 타이밍, 부분 실패, collection_run+health).

### Task 0 실측 (필수 — 수집 동작의 사실 근거)
- `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — (a) 요청 포맷(`POST /markets/items`, CategoryCode leaf 필수, PageSize 10 페이징; detail엔 `CurrentMinPrice` 없음) / (b) 필드 매트릭스(`CurrentMinPrice`→min_price) / (c) 매칭 규칙(`external_item_id`=String(Id)) / (d) **레이트 100/min 확정 + x-ratelimit·Retry-After 헤더 존재** + **키 공백→401 gotcha**.
- `.planning/phases/01-foundation-task-0/01-CONTEXT.md` — 잠긴 D-01..D-06(빌드/Flyway/RestClient/Task 0 산출물).

### 프로젝트 계획
- `.planning/PROJECT.md` — Core Value, 제약, Key Decisions(1A 팬아웃+분산락 제거, 레이트리밋=학습 쇼케이스).
- `.planning/REQUIREMENTS.md` — COLL-01..05 (+ DATA-01·04 연관).
- `.planning/ROADMAP.md` (§Phase 2) — 목표 + 5개 성공 기준 + 3개 plan 분할(02-01/02/03).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`src/main/java/com/lostark/tracker/spike/LostarkSpikeClient.java`** — Task 0 스파이크 클라이언트. **Phase 2의 productize 대상**: `RestClient.builder`(baseUrl/auth 헤더), **키 정규화(`bearer ` 접두 제거 + 전 공백 strip — 401 gotcha 해결책)**, `searchMarketItems(CategoryCode+ItemName)`, `getItemDetail(id)`. 스파이크 클라이언트(@`spike` 프로파일, `@Disabled`)는 그대로 두고, 실 수집용 `LostarkApiClient`로 패턴을 옮긴다(429/401/5xx 구분 추가).
- **`src/test/java/com/lostark/tracker/support/PostgresRedisContainers.java`** — Testcontainers 공유 베이스(Postgres+Redis, `@DynamicPropertySource`). Phase 2 통합테스트가 재사용.
- **도메인/리포(잠금):** `TrackedItem`(external_item_id/display_name/category/active), `PriceSnapshot`(collected_at/min_price/**fetched_at**/UNIQUE), `CollectionRun`(started_at/finished_at/items_*/status) + `TrackedItemRepository`/`PriceSnapshotRepository`/`CollectionRunRepository`.

### Established Patterns
- **Flyway 포워드 마이그레이션 + `ddl-auto=validate`**(D-02) — `summary_message` 추가는 V2 마이그레이션으로(엔티티-스키마 일치 유지).
- `application.yml`: Flyway 활성, redis 구성됨, `jdbc.time_zone=UTC`, actuator health 노출. (Phase 2는 `@EnableScheduling`/`@EnableAsync` + TaskExecutor + 레이트리밋/수집 빈 신규 추가.)

### Integration Points
- Phase 1이 세운 **공유 persistence 모듈** 위에 수집기를 올린다. 이 페이즈가 생산한 `collection_run`/`price_snapshot`을 **Phase 3**이 `/api/health/collection`(OPS-01)·조회 API·캐시로 소비한다.
- `PriceSnapshotRepository`는 Phase 3의 공유 윈도우 쿼리(4A)와도 공유 — 충돌 주의(설계 Worktree A·B 경고). Phase 2는 insert 경로만 건드린다.

</code_context>

<specifics>
## Specific Ideas

- **list vs detail (Task 0):** 실시간 `min_price`(=`CurrentMinPrice`)는 **list 엔드포인트에만** 존재(detail은 일 단위 `Stats[]`만) → 수집은 list 호출 강제.
- **`ItemName`은 부분일치 필터** → 응답을 `external_item_id`(Id)로 정확 대조해 품목 선택; 미발견은 skip-light 실패.
- **키 공백 gotcha:** 래핑된 디스플레이에서 복사한 키에 토큰 중간 공백이 섞이면 서명 깨져 401 — productized 클라이언트도 `bearer ` 접두 + 전 공백 strip 유지(스파이크 클라이언트 검증됨).
- **`summary_message` 마커 vocab:** `AUTH_ERROR`, 429/`RATE_LIMITED` 등 — 민감정보(키 값) 절대 금지.
- **`collected_at`:** run 시작 UTC `Instant`를 분 단위 truncate, 한 run 전 품목 공유; 실제 호출시각은 `fetched_at`.

</specifics>

<deferred>
## Deferred Ideas

- **`avg_price`(목록 `YDayAvgPrice`, 일 단위) → `price_snapshot` 컬럼 추가**: Task 0가 예고한 별도 V2 마이그레이션(`V2__add_price_metrics.sql`). **Phase 2 범위 아님** — 수집은 `min_price`만으로 충족.
- **`trade_count` 일별 통계 테이블**(detail `Stats[]`, 1일 1회 스케줄) — Phase 5/v2 (event-impact가 볼륨 필요 시).
- **최소 관측성**: Micrometer 카운터(429/skipped tick/failed item/cache hit·miss) — v2(MVP 우선순위는 collection_run + 명시적 로그 + 실패 카운트).
- **매직 넘버 `@ConfigurationProperties` 외부화**(수집 주기·풀 크기·토큰버킷 레이트·재시도 수·타임아웃) — v2.
- **레이트리밋 헤더 적극 reconcile**(remaining/reset로 버킷 보정) — D-04에서 client-side로 결정, 보류.

</deferred>

---

*Phase: 2-Collection Pipeline*
*Context gathered: 2026-06-21*
