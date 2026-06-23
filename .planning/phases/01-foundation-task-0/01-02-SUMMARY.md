---
phase: 01-foundation-task-0
plan: 02
subsystem: database
tags: [flyway, jpa, postgres, hibernate-validate, timestamptz, testcontainers, rest]

# Dependency graph
requires:
  - phase: 01-01
    provides: Spring Boot 스켈레톤, ddl-auto=validate 구성, PostgresRedisContainers 테스트 베이스, docker-compose
provides:
  - 잠긴 4테이블의 Flyway V1 스키마 (tracked_item, price_snapshot, game_event, collection_run)
  - 스키마를 미러링하는 JPA 엔티티 + EventType enum (부팅 시 검증)
  - TrackedItem/PriceSnapshot/CollectionRun 리포지토리
  - 라이브 Postgres 경유 POST/GET /api/items HTTP 라운드트립
  - UNIQUE 멱등성, TIMESTAMPTZ UTC, 품목 식별, collection_run을 증명하는 SchemaRoundTripIT
affects: [02-collection-pipeline, 03-read-api-cache, 04-admin-events, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns: [flyway-versioned-ddl, jpa-validate-mirrors-flyway, OffsetDateTime<->TIMESTAMPTZ-UTC, dto-bound-controller]

key-files:
  created:
    - src/main/resources/db/migration/V1__init_schema.sql
    - src/main/java/com/lostark/tracker/domain/{TrackedItem,PriceSnapshot,GameEvent,CollectionRun,EventType}.java
    - src/main/java/com/lostark/tracker/repository/{TrackedItem,PriceSnapshot,CollectionRun}Repository.java
    - src/main/java/com/lostark/tracker/web/ItemController.java
    - src/main/java/com/lostark/tracker/web/dto/{TrackedItemRequest,TrackedItemResponse}.java
    - src/test/java/com/lostark/tracker/SchemaRoundTripIT.java
  modified: []

key-decisions:
  - "TIMESTAMPTZ 컬럼을 java.time.OffsetDateTime으로 매핑; 결정적 라운드트립 위해 hibernate.jdbc.time_zone=UTC"
  - "price_snapshot.tracked_item_id를 @ManyToOne(LAZY)로 매핑; validate 일치를 위해 @Table에 UNIQUE 선언"
  - "CollectionRunRepository 추가(계획 파일 목록 밖) — DATA-04 증명에 필요"
  - "JSON 계약은 이 스켈레톤 라운드트립에서 camelCase (Spring/Jackson 기본)"

patterns-established:
  - "모든 스키마 변경은 새 Flyway V{n} 파일; 엔티티는 미러링하고 JPA는 validate만 (D-02)"
  - "컨트롤러는 검증된 DTO에 바인딩, 엔티티에 절대 직접 안 함 (mass-assignment 방지)"
  - "통합 테스트는 PostgresRedisContainers 상속; 인스턴트는 toInstant()로 비교해 UTC 동일성 단언"

requirements-completed: [DATA-01, DATA-02, DATA-03, DATA-04]

# Metrics
duration: ~12분
completed: 2026-06-20
---

# Phase 01 / Plan 02: 잠긴 데이터 모델 요약

**부팅 시 검증되는 JPA 엔티티가 딸린 Flyway 소유 4테이블 스키마, 그리고 실제 HTTP /api/items 라운드트립과 UNIQUE 멱등성·TIMESTAMPTZ UTC 동작을 증명하는 Testcontainers IT.**

## 성능

- **소요 시간:** ~12분
- **완료:** 2026-06-20
- **태스크:** 2개
- **생성 파일:** 12개

## 주요 성과
- `V1__init_schema.sql`이 PostgreSQL 타입만으로 4테이블, 8개 TIMESTAMPTZ 컬럼, `UNIQUE(tracked_item_id, collected_at)`(범위 쿼리 인덱스 겸용) 잠금
- JPA 엔티티 + `EventType` enum이 스키마 미러링; `ddl-auto=validate`에서 앱이 깔끔히 부팅
- `POST/GET /api/items`가 라이브 Postgres로 tracked_item 라운드트립
- `SchemaRoundTripIT`가 DATA-01(UNIQUE 위반), DATA-02(KST 경계 UTC 라운드트립), DATA-03(HTTP 품목 식별), DATA-04(collection_run 카운트) 증명
- `avg_price` / `trade_count`는 의도적으로 부재(D-06) — 01-03 Task 0 스파이크로 게이트

## 태스크 커밋

1. **Task 1: Flyway V1 + JPA 엔티티 + 리포지토리 (validate)** — `9303a30` (feat)
2. **Task 2: /api/items 라운드트립 + SchemaRoundTripIT** — `efbf069` (feat)

## 생성/수정 파일
- `src/main/resources/db/migration/V1__init_schema.sql` — 4테이블 DDL, UNIQUE, TIMESTAMPTZ
- `domain/{TrackedItem,PriceSnapshot,GameEvent,CollectionRun,EventType}.java` — 엔티티 + enum
- `repository/{TrackedItem,PriceSnapshot,CollectionRun}Repository.java` — Spring Data JPA
- `web/ItemController.java` + `web/dto/{TrackedItemRequest,TrackedItemResponse}.java` — HTTP 슬라이스
- `test/.../SchemaRoundTripIT.java` — DATA-01..04 통합 증명

## 결정 사항
- **OffsetDateTime + `hibernate.jdbc.time_zone=UTC`**(01-01에서 설정)로 결정적 TIMESTAMPTZ 라운드트립; 테스트는 `toInstant()` 비교.
- **@ManyToOne(LAZY)** for `price_snapshot.tracked_item_id`; `@Table(uniqueConstraints=...)`가 DB UNIQUE를 미러링해 `validate` 일치.
- **camelCase JSON** 스켈레톤 라운드트립용 (snake_case 외부 계약이 필요하면 Phase 3 읽기 API 사안).

## 계획 대비 이탈

### 자동 수정 이슈

**1. [필수 누락] CollectionRunRepository 추가**
- **발견 시점:** Task 2 — DATA-04는 `collection_run` 영속/조회가 필요하나, 계획 파일 목록엔 TrackedItem/PriceSnapshot 리포지토리만 명시됨.
- **수정:** 확립된 리포지토리 패턴을 따라 `CollectionRunRepository extends JpaRepository<CollectionRun, Long>` 추가.
- **검증:** `collectionRunRecordsStartFinishAndCounts` 테스트 통과.
- **커밋:** `9303a30`

---
**총 이탈:** 1건(DATA-04 충족에 필요). 스코프 확장 없음.

## 마주친 이슈
없음 — 01-01의 `api.version=1.44` 수정이 이어져 모든 Testcontainers 실행이 깔끔히 연결됨.

## 사용자 셋업 필요
없음.

## 다음 페이즈 준비도
- 공유 영속성 모듈이 잠기고 검증됨; **01-03**(Task 0 API 스파이크)이 실제 API로 `avg_price`/`trade_count` 가용성과 품목 매칭 규칙을 확인하고 모델 잠금을 비준할 준비 완료.
- `ddl-auto=validate`는 향후 엔티티/스키마 드리프트가 부팅 시 즉시 실패하게 함 — 프로젝트가 보여주려는 규율 신호.

---
*Phase: 01-foundation-task-0*
*완료: 2026-06-20*
