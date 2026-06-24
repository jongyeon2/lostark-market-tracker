# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Overview

빈 프로젝트에서 시작해, 먼저 데이터 모델을 실제 API로 잠그고(Task 0) 골격을 세운 뒤, 신뢰 가능한 10분 수집 파이프라인 → Redis 캐시 조회 API → 관리자 이벤트 관리 순으로 수직 슬라이스를 쌓는다. 그 위에 헤드라인 기능인 이벤트 상관(event-impact)을 올리되, 2주차 말 하드 게이트(수집·조회·시드 데모가 단단한가)를 통과할 때만 진행한다. 마지막으로 CI·시드·README 데모 표면으로 포트폴리오 산출물을 완성한다.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1): Urgent insertions (marked INSERTED)

- [x] **Phase 1: Foundation + Task 0** - 실행 가능한 골격 + 실측으로 잠근 데이터 모델 (completed 2026-06-20)
- [x] **Phase 2: Collection Pipeline** - 레이트리밋·재시도·부분 실패를 처리하는 신뢰 가능한 10분 수집기 (completed 2026-06-22)
- [x] **Phase 3: Read API + Cache** - Redis 캐시 latest/타임라인/다운샘플 + 헬스 (completed 2026-06-23)
- [x] **Phase 4: Admin + Events** - 시크릿 인증 뒤 품목/이벤트 CRUD (completed 2026-06-24)
- [x] **Phase 5: Event Impact (게이트 조건부)** - 이벤트 전후 변화율 + 데이터 충분성 가드 (completed 2026-06-24)
- [ ] **Phase 6: Distribution + Docs** - CI · 격리 시드 · README 데모 표면

## Phase Details

### Phase 1: Foundation + Task 0

**Goal**: Spring Boot 앱이 실행되고, Task 0 API 검증으로 실제 응답 필드를 확인해 데이터 모델(4개 테이블)을 잠근다.
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04, DIST-01
**Success Criteria** (what must be TRUE):

  1. `docker-compose up`으로 Postgres + Redis가 뜨고, 앱이 연결되어 DDL이 4개 테이블(tracked_item, price_snapshot, game_event, collection_run)을 생성한다
  2. price_snapshot의 `UNIQUE(tracked_item_id, collected_at)`가 DB 레벨에서 강제된다 — 중복 insert 시 제약 위반
  3. 모든 타임스탬프 컬럼이 `TIMESTAMPTZ`로 저장되고 UTC 인스턴트로 다시 읽힌다
  4. Task 0 스파이크가 실제 API 응답에 avg_price·trade_count 제공 여부를 확인하고, external_item_id 매칭 규칙을 결정·문서화한다
  5. tracked_item을 external_item_id + display_name으로 모호함 없이 insert/조회할 수 있다

**Plans**: 3 plans

Plans:
**Wave 1**

- [x] 01-01-PLAN.md — 스캐폴딩 + docker-compose(Postgres+Redis) + 프로파일 + 스모크 IT (DIST-01)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02-PLAN.md — 4테이블 Flyway DDL + JPA 엔티티(ddl-auto=validate) + 실HTTP 라운드트립 IT (DATA-01..04)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 01-03-PLAN.md — Task 0 API 검증 스파이크 → 응답 필드/매칭 규칙 확정 + 모델 잠금 (DATA-03)

### Phase 2: Collection Pipeline

**Goal**: 10분 주기로 거래소 시세를 신뢰 가능하게 수집한다 — 레이트리밋, 429 재시도, 부분 실패, 실행 이력 기록.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: COLL-01, COLL-02, COLL-03, COLL-04, COLL-05
**Success Criteria** (what must be TRUE):

  1. 20분 가동 시 시작/종료 시각과 성공/실패 카운트가 올바른 collection_run 행이 최소 2개 쌓인다
  2. 레이트 예산을 넘는 요청은 토큰버킷이 스로틀하고, 앱 재시작 후 Redis에서 토큰 수가 올바르게 복원된다
  3. 429는 Retry-After + 지수 백오프로 최대 3회 재시도 후 해당 품목을 스킵한다 — 크래시 없이 failure_count 증가
  4. 한 품목 실패가 같은 틱의 다른 품목 price_snapshot 적재를 막지 않는다
  5. fixedDelay가 틱 중복을 막고, UNIQUE 제약이 중복 틱의 중복 스냅샷을 막는다

**Plans**: 3 plans

Plans:

**Wave 1**

- [x] 02-01: LostarkApiClient(JWT, 429/401/5xx 구분) + Redis 토큰버킷(lazy refill / Bucket4j)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 02-02: PriceCollector @Scheduled + 틱 내 병렬 팬아웃 + allOf().join() + 타임아웃

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 02-03: 상한 재시도 + 부분 실패 스킵-라이트 + collection_run 기록

### Phase 3: Read API + Cache

**Goal**: 캐시된 최신가, 타임라인(스냅샷+이벤트), 큰 범위 다운샘플, 헬스 엔드포인트를 제공한다.
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: API-01, API-02, API-03, API-04, API-05, OPS-01
**Success Criteria** (what must be TRUE):

  1. `GET /api/items`가 id·external_item_id·display_name JSON 배열을 반환한다
  2. `GET /api/items/{id}/latest`가 Redis에서 최신가를 서빙하고, 새 스냅샷 쓰기 후 캐시가 갱신된다
  3. `GET /api/items/{id}/prices?from=&to=`가 윈도우 내 스냅샷과 겹치는 game_event를 함께 반환한다
  4. 30일 범위가 서버 측에서 제한된 점 수로 다운샘플된다
  5. from>to→400, 없는 품목→404, 빈 범위→200 빈 배열; `/api/health/collection`이 last_run_at + 카운트를 반환한다

**Plans**: 3 plans

Plans:

**Wave 1**

- [x] 03-01: latest 캐시-어사이드 + 쓰기 시 무효화

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 03-02: 공유 윈도우 쿼리 + 타임라인(스냅샷+이벤트)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 03-03: 큰 범위 다운샘플 + 입력 검증 + /health/collection

### Phase 4: Admin + Events

**Goal**: 시크릿 인증 뒤에서 관리자가 게임 이벤트와 워치리스트 품목을 관리한다.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: ADMIN-01, ADMIN-02, ADMIN-03
**Success Criteria** (what must be TRUE):

  1. 시크릿 없이 `/api/admin/**` 요청은 401, 시크릿 있으면 처리된다
  2. 관리자가 게임 이벤트를 POST/GET/PUT/DELETE하고 DB에 created_at/updated_at이 채워진다
  3. 관리자가 tracked item을 POST/DELETE하면 `GET /api/items`에 즉시 반영된다

**Plans**: 2 plans

Plans:

**Wave 1**

- [x] 04-01: 이벤트/품목 CRUD 컨트롤러 + 서비스

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 04-02: Spring Security 공유 시크릿 게이트(/api/admin/**)

### Phase 5: Event Impact (게이트 조건부)

**Goal**: 이벤트별 ±N시간 전후 가격 변화율을 데이터 충분성·staleness 가드와 함께 계산한다. **2주차 말 하드 게이트를 통과할 때만 진행하며, 슬립 시 v2로 강등.**
**Mode:** mvp
**Depends on**: Phase 3, Phase 4
**Requirements**: IMPACT-01, IMPACT-02
**Success Criteria** (what must be TRUE):

  1. `GET /api/items/{id}/event-impact?window=Nh`가 이벤트별 pre_avg·post_avg·change_rate%·앵커 시각 목록을 반환한다
  2. 희소 윈도우는 발견한 앵커 시각(또는 null)과 함께 `"status":"insufficient_data"`를 반환한다
  3. staleness 허용치를 벗어난 앵커는 그 앵커 시각과 함께 `insufficient_data`를 반환해 호출자가 이유를 안다

**Plans**: 2 plans

Plans:

**Wave 1**

- [x] 05-01: event-impact v1 지표 계산 + 범위 조회 배치(N+1 제거, 공유 윈도우 쿼리 재사용)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 05-02: 충분성(앞뒤≥1) + staleness 허용치 + insufficient_data + 앵커 시각 응답

### Phase 6: Distribution + Docs

**Goal**: CI·격리 시드·README 데모 표면으로 리뷰어가 README만 보고 재현할 수 있게 한다.
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: DIST-02, DIST-03, DIST-04
**Success Criteria** (what must be TRUE):

  1. GitHub Actions CI가 매 푸시에 Testcontainers로 돌고 README에 그린 배지가 뜬다
  2. 시드 프로파일이 합성 데이터(스냅샷 7일+, 이벤트 2개+)를 넣어 타임라인·event-impact가 비어있지 않게 응답한다
  3. README가 아키텍처 다이어그램·트레이드오프 설명·curl 예시 3개+·샘플 JSON·로컬 셋업·시드 모드·보존 정책 한 줄을 담는다
  4. 리뷰어가 README만 따라 `docker-compose up` + 시드 모드로 비어있지 않은 event-impact 응답을 재현한다

**Plans**: 2 plans

Plans:

- [ ] 06-01: GitHub Actions CI(Testcontainers) + 격리 시드 프로파일/픽스처
- [ ] 06-02: README 데모 표면(아키텍처·curl·샘플 JSON·시드 모드·보존 정책)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6
(parallelization=true: Phase 4는 Phase 1 후 Phase 2·3과 병렬 가능; Phase 5는 3·4 의존; 공유 persistence 모듈 충돌 주의.)

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation + Task 0 | 3/3 | Complete   | 2026-06-20 |
| 2. Collection Pipeline | 3/3 | Complete   | 2026-06-22 |
| 3. Read API + Cache | 3/3 | Complete   | 2026-06-23 |
| 4. Admin + Events | 2/2 | Complete   | 2026-06-24 |
| 5. Event Impact (게이트 조건부) | 2/2 | Complete   | 2026-06-24 |
| 6. Distribution + Docs | 0/2 | Not started | - |
