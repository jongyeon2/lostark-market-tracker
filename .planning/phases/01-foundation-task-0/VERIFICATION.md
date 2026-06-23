# Phase 1 검증 — Foundation + Task 0

**검증 시각:** 2026-06-20
**방식:** 구현된 코드베이스 대비 ROADMAP Phase 1 성공 기준의 목표 역산 검사 + 클린 `./gradlew clean build`(컴파일 + Testcontainers 통합 테스트 + 패키지).
**빌드 증거:** `./gradlew clean build` → **BUILD SUCCESSFUL** (SmokeContextTest + SchemaRoundTripIT가 실제 Postgres 16 + Redis 7에서 통과; MarketsApiSpikeTest는 `@Disabled`, 네트워크 없음).

## 성공 기준

| # | 기준 | 판정 | 증거 |
|---|------|------|------|
| 1 | `docker-compose up` → Postgres + Redis 기동, 앱 연결, DDL이 4테이블 생성 | ✅ PASS | `docker-compose.yml`(postgres:16 + redis:7, 헬스체크); `V1__init_schema.sql`이 tracked_item / price_snapshot / game_event / collection_run 생성; `SmokeContextTest`가 `ddl-auto=validate` 하에 Flyway 적용된 채 Testcontainers 대비 전체 컨텍스트 부팅 |
| 2 | price_snapshot `UNIQUE(tracked_item_id, collected_at)`가 DB 레벨에서 강제 | ✅ PASS | V1 `CONSTRAINT uq_price_snapshot_item_time UNIQUE (tracked_item_id, collected_at)`; `SchemaRoundTripIT.duplicateSnapshotViolatesUniqueConstraint`가 중복 insert 시 `DataIntegrityViolationException` 단언 |
| 3 | 모든 타임스탬프 컬럼이 `TIMESTAMPTZ`, UTC 인스턴트로 저장/조회 | ✅ PASS | V1에 8개 `TIMESTAMPTZ` 컬럼; 엔티티가 `hibernate.jdbc.time_zone=UTC`로 `OffsetDateTime` 매핑; `SchemaRoundTripIT.timestamptzRoundTripsAsUtcInstant`가 KST 자정 경계 인스턴트를 UTC 동일하게 라운드트립 |
| 4 | Task 0 스파이크가 avg_price/trade_count 가용성 확인 + 매칭 규칙 문서화 | ✅ PASS | `TASK0-FINDINGS.md`(필드 가용성 매트릭스; D-06: avg_price/trade_count 일단위 제공; D-05: external_item_id = API `Id` + display_name; 레이트리밋 100/min; 종료 게이트 PASS); 블로킹 사람 체크포인트에서 비준; 설계 문서 LOCKED |
| 5 | external_item_id + display_name으로 모호함 없이 tracked_item insert/조회 | ✅ PASS | `ItemController` POST/GET `/api/items`; `TrackedItemRepository.findByExternalItemId`; `SchemaRoundTripIT.itemInsertedAndReadBackViaHttp`(실제 HTTP 라운드트립이 external_item_id + display_name 에코) |

**커버된 요구사항:** DATA-01(UNIQUE 멱등성), DATA-02(TIMESTAMPTZ UTC), DATA-03(매칭 규칙 + 품목 식별), DATA-04(collection_run), DIST-01(docker-compose 로컬 재현).

## 보너스 / 견고화 산출물
- 공유 `PostgresRedisContainers` Testcontainers 베이스 — Phase 2-5가 재사용하는 영속성/테스트 계약.
- Docker Engine 29.x 호환성 해결(`api.version=1.44` 핀) — Linux CI 이식 가능.
- 스파이크 클라이언트의 JWT 키 견고화(bearer 접두 + 붙여넣기로 주입된 공백 제거).

## Phase 2 이월
- `avg_price` 추가용 `V2__add_price_metrics.sql`(= `YDayAvgPrice`).
- per-tick `CurrentMinPrice` → `price_snapshot.min_price` 수집; `x-ratelimit-*` + `Retry-After`로 토큰버킷이 100/min 준수.
- `trade_count`는 per-tick 모델에서 제외 유지(일단위 전용) — 선택적 v2 daily-stats 테이블.

## 판정

**✅ PHASE 1 PASS** — 5개 성공 기준 전부 충족; 빌드 + 통합 테스트 그린; 데이터 모델이 측정된 현실 위에 잠김.

---
*Phase: 01-foundation-task-0 · 검증 2026-06-20*
