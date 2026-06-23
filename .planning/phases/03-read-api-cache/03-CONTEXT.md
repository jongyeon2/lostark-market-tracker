# Phase 3: Read API + Cache - Context

**Gathered:** 2026-06-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1/2가 쌓은 `price_snapshot`·`game_event`·`collection_run` 위에 **조회·서빙 계층**을 올린다 — (a) Redis 캐시-어사이드로 서빙하는 최신가, (b) 타임라인(기간 스냅샷 + 겹치는 이벤트), (c) 큰 범위 서버 측 다운샘플, (d) 입력 검증 계약, (e) 수집 헬스 엔드포인트. (API-01..05, OPS-01)

이 페이즈는 "리뷰어가 curl로 워치리스트·최신가·타임라인·헬스를 읽을 수 있다"까지 책임진다. **백엔드 전용 — 프론트엔드 UI는 명시적 제외**(REQUIREMENTS Out of Scope). 관리자/이벤트 CRUD(Phase 4), event-impact(Phase 5)는 후속. 단 **4A 공유 윈도우 쿼리는 지금 추출**해 Phase 5가 재사용한다.

**선행 페이즈에서 잠긴 것(planner 변경 금지):** 4테이블 모델 / `price_snapshot UNIQUE(tracked_item_id, collected_at)` / 모든 시간 `TIMESTAMPTZ`(UTC) / `collected_at`=틱 정규화 분단위(D-15), `fetched_at`=실 호출시각 / `collection_run` 라이프사이클 + `summary_message` 마커(AUTH_ERROR/RATE_LIMITED, Phase 2 D-08/D-14) / `min_price`=`CurrentMinPrice`, `avg_price`·`trade_count` 제외 / DB=PostgreSQL, 테스트=Testcontainers / `GET /api/items`는 이미 walking-skeleton으로 존재.

</domain>

<decisions>
## Implementation Decisions

### 최신가 캐시 (API-02 · 02-01 핵심 학습 쇼케이스 연장)
- **D-01:** **Evict-on-write 캐시-어사이드.** 읽기는 캐시 미스 시 DB 조회 후 채움(lazy fill), **수집기가 스냅샷을 쓸 때 해당 품목 latest 키를 삭제(evict)**. 정석 cache-aside(read-fill + invalidate 둘 다 쇼케이스) — write-through/TTL-only 대신 evict로 "캐시 무효화" 패턴을 명시. 무효화 훅은 **Phase 2 `PriceCollector.persistSnapshot` 성공 직후**(insert 경로 공유 — 설계 Worktree A·B 충돌 주의, read/evict만 추가). 안전망 TTL은 둘 수 있으나(≤ 1–2 수집주기) 1차 무효화 신호는 evict.
- **D-02:** 캐시 값 = **`min_price` + `collected_at`** (작은 DTO/JSON 직렬화). latest 응답이 "얼마 + 언제 시점"을 함께 줘 의미 있는 최소 세트. 전체 스냅샷 DTO는 과잉.
- **D-03:** **수동 RedisTemplate 캐시-어사이드**(명시적 get/set/evict). Spring `@Cacheable`/`@CacheEvict` 추상화 배제 — Phase 2 직접 구현 토큰버킷(02-CONTEXT D-03)과 같은 hand-rolled 철학, 면접 설명 신호. 값 직렬화용 `RedisTemplate<String, ?>`(JSON) 구성 신규(토큰버킷의 `StringRedisTemplate`과 별개 빈, 동일 커넥션 팩토리).

### 타임라인 응답 + 공유 윈도우 쿼리 (API-03 · 4A)
- **D-04:** `GET /api/items/{id}/prices?from=&to=` 응답 = **두 배열 `{snapshots:[...], events:[...]}`**. 스냅샷(`collected_at`+`min_price`, `collected_at` asc)과 겹치는 `game_event`(`occurred_at`+`event_type`+`title`)를 분리. 클라이언트 차트 오버레이에 단순·명확하고 **다운샘플과 직교**(스냅샷만 축소, 이벤트는 그대로).
- **D-05:** 이벤트 겹침 = **`from ≤ occurred_at ≤ to` (양끝 포함)**. `game_event.occurred_at`은 단일 인스턴트라 단순 containment. 경계 UTC 기준, 빈/경계 범위 테스트 명확.
- **D-06:** **4A 공유 윈도우 쿼리 추출.** 공유 서비스 계층(`WindowQueryService` 또는 동등)이 `PriceSnapshotRepository.findByTrackedItem_IdAndCollectedAtBetween(...)` + **`GameEventRepository`(신규).findByOccurredAtBetween(...)`**를 조합. **timeline(Phase 3)과 event-impact(Phase 5)가 동일 메서드 재사용** — 설계 4A/T6(DRY), N+1 회피. `PriceSnapshotRepository`는 Phase 2 insert 경로 공유 → **read 전용 메서드만 추가**.

### 다운샘플 (API-04 · 7A)
- **D-07:** 버킷 집계 = **`avg(min_price)` per 버킷**. 응답 버킷 = `bucket_start` + `avg_min_price` + `sample_count`. 추세선 표준·단순(OHLC는 v2). PostgreSQL `date_trunc` 집계.
- **D-08:** 발동 = **raw 점 수 > N이면 같은 `/prices`가 자동 다운샘플**, 이하면 raw 그대로 반환. 응답에 `downsampled:true/false` + 버킷 폭 메타. 클라이언트는 파라미터 불필요, 서버가 페이로드 보호(`?interval=`은 v2). 성공기준 4(30일→제한 점) 자연 충족.
- **D-09:** 목표 점 수 상한 **N ≈ 500**. 버킷 폭 = **`date_trunc` 동적** — 범위가 작으면 `hour`, 크면 `day`(범위/N에 맞춰 시/일 경계 선택). 정확 임계/표현은 planner 재량.

### 입력 검증 · 에러 · 헬스 (API-05 · OPS-01)
- **D-10:** 4xx 에러 바디 = **커스텀 `@RestControllerAdvice`** → `{timestamp, status, error, message}` 일관 JSON(현재 advice 없음 → 신규). 4xx 계약 명확, 테스트 단언 쉬움.
- **D-11:** 응답 타임존 = **UTC ISO-8601(`...Z`), 서버 변환 없음**. `OffsetDateTime` UTC 직렬화, 저장(UTC TIMESTAMPTZ)과 일치 → **off-by-9h 차단**. 표시(KST)는 클라이언트 몫(설계 "UTC 저장 + KST 표시"). 범위 쿼리 경계도 UTC.
- **D-12:** `GET /api/health/collection` 필드 = **`last_run_at`(=마지막 run `finished_at`) + `started_at` + attempted/succeeded/failed + `status` + `summary_message` 마커(AUTH_ERROR/RATE_LIMITED)**. Phase 2가 생산한 collection_run을 소비, 운영 성숙도 showcase. **API 키/Authorization/민감정보 절대 노출 금지.** `stale` 경고 플래그(마지막 run N분 경과)는 선택(미채택 — 재량 후보).
- **D-13:** 입력 검증 계약 = **성공기준 5 그대로.** `from>to`→400, `window≤0`→400, 없는 `itemId`→404, **유효하나 데이터 없는 범위→200 빈 배열**. "없는 품목(404)"과 "유효하나 빈 데이터(200 [])"를 구분.

### Claude's Discretion
- 캐시 키 네이밍(`item:{id}:latest` 등)·안전망 TTL 값·RedisTemplate JSON 직렬화 구성·latest 응답 DTO 정확 필드명
- `/api/items`는 **active만 반환**(현재 `findAll` → `findByActiveTrue` 권장)·정렬 순서
- `/latest`에서 스냅샷 0건 품목 → **404(no price yet)** vs 204; 없는 item → 404
- 다운샘플 버킷 폭 hour↔day 전환 임계 정확값·`date_trunc` 표현·sample_count 포함 형태
- 패키지/레이어 구조(web/service/repository), `@RestControllerAdvice` 위치, 에러 코드 enum 여부
- **타임라인/다운샘플 결과는 캐시 안 함**(latest만 캐시) — 범위 쿼리는 DB 직조회(공유 윈도우 쿼리)
- health `stale` 플래그 채택 여부

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 설계 / 리뷰 (필수 — 구현 확정 레이어)
- `docs/design/yeonjong-unknown-design-20260619-221517.md` — Phase 3 핵심: **§3 Redis 역할(최신가 캐시-어사이드 + 쓰기 무효화 / cache stale 실패모드)**, **확정 8결정 중 4A(공유 윈도우 쿼리)·7A(큰 범위 서버 다운샘플 `date_trunc`)·3A(collection_run + `/health/collection`)**, **§Worktree B. Read API(latest 캐시-어사이드/공유 윈도우/timeline/다운샘플)** 와 **A·B persistence 공유 충돌 경고**, **§Test Strategy(캐시히트 시 DB 0회·범위 쿼리·캐시 무효화)**, **§Implementation Tasks T6(공유 윈도우)·T9(다운샘플)·T10(캐시 무효화)·T5(collection_run+health)**.
- `docs/reviews/yeonjong-unknown-eng-review-test-plan-20260620-102515.md` — 테스트 플랜: 공유 윈도우 경계·**UTC/KST off-by-9h**, collection_run 기록+health 읽기, **입력검증(from>to·window≤0·404·빈 범위)**, 캐시 무효화·캐시 히트.

### Task 0 실측 / 선행 데이터 모델 (필수 — 사실 근거)
- `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — `min_price`=`CurrentMinPrice`, `external_item_id`=String(Id), 모든 시간 TIMESTAMPTZ(UTC).
- `.planning/phases/02-collection-pipeline/02-CONTEXT.md` — Phase 2 잠금: `collected_at` 틱 정규화(D-15)·`fetched_at` 분리, `collection_run` 라이프사이클(D-12)·`summary_message` 마커(D-14/D-08, 키 금지), `UNIQUE` 멱등(D-16). **Phase 3 health·timeline·latest가 이 데이터를 소비**.

### 프로젝트 계획
- `.planning/PROJECT.md` — Core Value(수집·저장·서빙), Redis 역할(cache-aside), Key Decisions(4A 공유 윈도우 / 7A 다운샘플).
- `.planning/REQUIREMENTS.md` — API-01..05, OPS-01 (+ DATA·COLL 연관).
- `.planning/ROADMAP.md` (§Phase 3) — 목표 + 5개 성공 기준 + 3개 plan 분할(03-01 캐시 / 03-02 타임라인+4A / 03-03 다운샘플+검증+health).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`src/main/java/com/lostark/tracker/web/ItemController.java`** — 이미 `GET /api/items`(list) + `POST` 존재. **API-01 사실상 충족** (`TrackedItemResponse`: id/externalItemId/displayName/category/active). Phase 3: `latest`/`prices` 엔드포인트 추가 + `/api/items`를 **active 필터**(`findByActiveTrue`)로 정렬할지 결정.
- **`src/main/java/com/lostark/tracker/ratelimit/RedisTokenBucket.java` + `RateLimiterConfig`** — Redis 연결/구성 패턴 재사용처. 캐시는 **별도 `RedisTemplate`(값 직렬화)** 신규(토큰버킷은 `StringRedisTemplate` 그대로).
- **`repository/{PriceSnapshotRepository, CollectionRunRepository, TrackedItemRepository}`** — Between 범위 쿼리·health 조회·active 필터 추가 대상. `PriceSnapshotRepository`는 **Phase 2 insert 경로 공유 → read 메서드만 추가**. `GameEventRepository`는 **신규**(기존 `game_event` 테이블 조회).
- **`src/test/java/com/lostark/tracker/support/PostgresRedisContainers.java`** — Testcontainers(Postgres+Redis) 공유 베이스. Phase 3 통합테스트(**캐시히트 DB 0회**·범위 쿼리·검증·health) 재사용.
- **도메인:** `PriceSnapshot`(collected_at/min_price/**fetched_at**), `GameEvent`(occurred_at/event_type/title/description), `CollectionRun`(started_at/finished_at/items_*/status/**summary_message**).

### Established Patterns
- **Flyway 포워드 + `ddl-auto=validate`** — Phase 3는 **읽기 전용, 신규 컬럼/테이블 없음 예상** → 마이그레이션 불필요. 부득이 추가 시 **V3**(Phase 2 V2 다음 번호, 충돌 금지).
- `application.yml`: redis 구성됨, `jdbc.time_zone=UTC`, actuator health 노출 — UTC 직렬화 일관성의 기반.
- 에러 처리: 현재 전역 advice 없음 → **`@RestControllerAdvice` 신규 도입**(D-10).

### Integration Points
- **소비:** Phase 2가 생산한 `collection_run`(health) · `price_snapshot`(latest/timeline/다운샘플)을 Phase 3가 읽는다.
- **캐시 무효화 훅:** D-01의 evict가 **Phase 2 `PriceCollector.persistSnapshot`에 삽입**됨 — insert 경로 공유, 설계 Worktree A·B 충돌 주의.
- **4A 공유 윈도우 쿼리는 Phase 5 event-impact가 재사용** — 지금 추출(read 전용).

</code_context>

<specifics>
## Specific Ideas

- **캐시히트 검증(office-hours 핵심):** `PriceSnapshotRepository`를 spy해 **2번째 latest 읽기에서 DB 호출 0회** 단언(또는 캐시 히트 카운터).
- **off-by-9h 가드:** UTC 저장 ↔ UTC 응답 일관, 범위 쿼리 경계 UTC. KST는 응답에서 변환하지 않음.
- **`summary_message` 마커 vocab:** `AUTH_ERROR`/`RATE_LIMITED`만 — 키 값 등 민감정보 절대 금지.
- **타임라인:** `{snapshots[], events[]}` 두 배열, `from ≤ occurred_at ≤ to` 포함, 스냅샷 `collected_at` asc.
- **다운샘플:** `avg(min_price)` 버킷 + `sample_count`, raw>N(≈500) 자동, `date_trunc` 동적(시/일), 응답 `downsampled` 메타.

</specifics>

<deferred>
## Deferred Ideas

- **OHLC 다운샘플**(open/high/low/close) — v2(avg로 MVP 충족).
- **`?interval=1h` 명시 다운샘플 파라미터** — v2(자동 N 기준 발동).
- **health `stale` 플래그**(마지막 run N분 경과 경고) — 선택, 시간 남으면.
- **타임라인/다운샘플 결과 캐싱** — v2(latest만 캐시, 범위 쿼리는 DB 직조회).
- **`avg_price`(목록 `YDayAvgPrice`) per-tick 컬럼 / `V3__add_price_metrics.sql`** — Phase 2에서 이월된 deferred, **Phase 3 범위 아님**(min_price만으로 조회 충족).
- **KST 표시/포맷** — 클라이언트(프론트엔드 명시적 제외).
- **Micrometer 캐시 hit/miss 카운터** — v2(MVP는 캐시히트 테스트로 증명).

</deferred>

---

*Phase: 3-Read API + Cache*
*Context gathered: 2026-06-23*
