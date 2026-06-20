# Phase 1: Foundation + Task 0 - Context

**Gathered:** 2026-06-20
**Status:** Ready for planning

<domain>
## Phase Boundary

실행 가능한 Spring Boot 골격을 세우고, Task 0 API 검증 스파이크로 실제 거래소 응답 필드를 확인해 데이터 모델(4개 테이블)을 잠근다. 수집·조회·관리자 등 후속 기능은 이 위에 얹히며, 이 페이즈는 "앱이 뜨고, DB/캐시가 연결되고, 스키마가 잠긴다"까지만 책임진다.

**잠긴 데이터 모델(설계에서 확정 — planner는 변경 금지):**
- 4 테이블: tracked_item / price_snapshot / game_event / collection_run
- price_snapshot `UNIQUE(tracked_item_id, collected_at)` + 복합 인덱스
- 모든 시간 컬럼 `TIMESTAMPTZ`(UTC): collected_at / fetched_at / created_at / updated_at
- DB=PostgreSQL, 개발 docker-compose postgres, 테스트 Testcontainers PostgreSQL, MySQL 전용 SQL/타입 금지

</domain>

<decisions>
## Implementation Decisions

### Build & Runtime
- **D-01:** Gradle (Groovy DSL) + Java 21 (LTS) + Spring Boot 3.4.x. 모던 Spring 포트폴리오 표준.

### Schema Migration
- **D-02:** **Flyway** 버전드 SQL 마이그레이션으로 스키마를 소유한다. JPA는 `ddl-auto=validate`로 엔티티-스키마 일치만 검증(생성/수정 금지). 4개 테이블·UNIQUE·인덱스·TIMESTAMPTZ는 Flyway 마이그레이션 파일로 정의. "스키마를 규율 있게 손으로 관리한다"를 보이는 의도.

### HTTP Client
- **D-03:** 거래소 API 호출은 **RestClient(Spring 6.1+, 동기)**. 수집이 `@Async` 스레드풀 기반이라 동기 클라이언트가 더 단순하게 맞는다. WebClient(리액티브)·RestTemplate(레거시) 배제.

### Task 0 Spike
- **D-04:** Task 0 API 검증은 **`spike` 프로파일 아래 `@Disabled` 통합 테스트(수동 실행)** 로 실제 `markets/items`를 1회 호출해 응답 구조를 캡처한다. CI에선 실행 안 됨(키 필요). 발견 결과는 phase 디렉터리 아티팩트 + 설계 문서에 기록.
- **D-05:** **품목 매칭 규칙은 Task 0의 산출물**(사전 결정 아님). 안정적 `external_item_id`가 응답에 있으면 `external_item_id` + `display_name`을 저장. 없으면 이름/카테고리/등급/묶음수량 중 식별 조합을 Task 0에서 확정하고 리스크를 문서화한다. (DATA-03 충족 조건.)
- **D-06:** **`avg_price`/`trade_count`는 Task 0 실측 전까지 핵심 모델로 확정하지 않는다.** Task 0에서 제공 확인 후 실제 제공 필드만 사용. 미제공 시 모델·수집 단위 재검토(설계의 Task 0 종료 게이트).

### Claude's Discretion
- 패키지/레이어 구조, `application.yml`/프로파일 세부 구성, 엔티티 매핑 디테일, docker-compose 이미지 버전 핀, Flyway 마이그레이션 파일 분할 — planner/executor 재량.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 설계 / 리뷰 (필수)
- `docs/design/yeonjong-unknown-design-20260619-221517.md` — 승인 + 엔지니어링 리뷰된 설계 문서. 하단 **"엔지니어링 리뷰 반영"** 섹션이 구현 확정 레이어(Data Model Decisions, Database Engine & Environments, Failure Modes, Test Strategy, Phase 시퀀싱). Phase 1은 특히 "Data Model Decisions" + "Database Engine & Environments" + "Task 0 Exit Criteria"를 따른다.
- `docs/reviews/yeonjong-unknown-eng-review-test-plan-20260620-102515.md` — 엔지니어링 리뷰 테스트 플랜(엣지 케이스·통합 테스트 대상).

### 프로젝트 계획
- `.planning/PROJECT.md` — Core Value, 제약(스택/DB/타임라인), Key Decisions 표
- `.planning/REQUIREMENTS.md` — Phase 1 매핑 요구사항 DATA-01~04, DIST-01
- `.planning/ROADMAP.md` (§Phase 1) — 목표 + 5개 성공 기준

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- 없음 — 그린필드(현재 레포에는 CLAUDE.md / README.md / docs/만 존재, src 없음). 이 페이즈가 첫 코드를 만든다.

### Established Patterns
- 아직 없음. 이 페이즈가 persistence 모듈(Phase 2·3·4·5가 공유)과 API 클라이언트 골격의 패턴을 세운다.

### Integration Points
- 이 페이즈가 만드는 것 위에 후속 페이즈가 올라간다: Phase 2(수집)는 RestClient + price_snapshot/collection_run 위에, Phase 3(조회)는 price_snapshot 인덱스 위에, Phase 4(관리자)는 game_event/tracked_item 위에. **공유 persistence 모듈**을 깨끗이 세우는 것이 이 페이즈의 핵심 다운스트림 영향.

</code_context>

<specifics>
## Specific Ideas

- 마이그레이션·스키마는 "포트폴리오에서 규율을 보여준다"가 목적 — Flyway + ddl-auto=validate 조합은 그 의도의 직접 표현.
- Task 0는 코드 작성 전 **최우선 게이트**: 실제 융화재료 품목(예: 아비도스 융화재료)을 `markets/items`로 호출해 필드/요청 포맷/레이트 헤더를 직접 확인.

</specifics>

<deferred>
## Deferred Ideas

None — 논의가 페이즈 스코프 안에 머물렀다. (빌드/마이그레이션/HTTP 클라이언트/Task 0 실행 형태는 전부 "어떻게 만들지"의 명확화였고, 새 기능 추가 없음.)

</deferred>

---

*Phase: 1-Foundation + Task 0*
*Context gathered: 2026-06-20*
