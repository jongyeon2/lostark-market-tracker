# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — MVP

**Shipped:** 2026-06-25
**Phases:** 6 | **Plans:** 15 | **Tasks:** 13

### What Was Built
- 신뢰 가능한 10분 수집 파이프라인 — @Scheduled fixedDelay 병렬 팬아웃 + 자체 Lua 원자적 Redis 토큰버킷(재시작 복원·fail-closed) + 상한 재시도/부분 실패 격리 + collection_run 실행 이력.
- 시계열 영속화 — Flyway 소유 4테이블(price_snapshot UNIQUE 멱등, TIMESTAMPTZ UTC), JPA ddl-auto=validate 일치, Task 0 실측으로 모델 잠금.
- 조회 API + 캐시 — latest Redis 캐시-어사이드(evict-on-write), prices 타임라인(스냅샷+이벤트), date_trunc 다운샘플, 입력 검증 계약, /health/collection.
- 관리자 표면 — X-Admin-Secret 게이트 뒤 이벤트/품목 CRUD (V3 UNIQUE, 공유 에러 계약).
- 헤드라인 event-impact — change_rate(min_price delta) + 30분 staleness/sufficiency 가드로 sparse/stale을 insufficient_data로 구분.
- 배포 표면 — 전체 Testcontainers CI + API 키 없는 seed 프로파일 + 리뷰어가 README만으로 재현하는 데모 표면.

### What Worked
- **수집 신뢰성 우선 + 하드 게이트** — "다른 게 실패해도 수집·저장·서빙은 동작" 코어 밸류를 2주차 말 하드 게이트로 강제. 게이트 통과 후에야 event-impact를 올려 헤드라인이 나쁜 데이터 위 장식이 되지 않게 함.
- **재사용 가능한 WindowQueryService (4A)** — Phase 3에서 만든 양끝 포함 윈도우 쿼리를 Phase 5 event-impact가 그대로 재사용 → N+1 제거, 경계 시맨틱 일관.
- **프로파일-프리 생성기 + 얇은 @Profile 러너** — seed 로직을 단위 테스트 가능한 @Component로 두고 부팅 트리거만 @Profile("seed")로 분리 → Testcontainers IT가 프로파일 곡예 없이 seed→타임라인→event-impact "ok"를 증명.
- **계약을 우회하지 않고 준수** — 데모 시드도 라이브 UNIQUE 키 + staleness 계약을 그대로 따름(이벤트 5분 오프셋 배치로 실제 non-zero change_rate 산출).

### What Was Inefficient
- **요구사항 체크박스 전환 누락** — COLL/DATA/DIST-01 요구사항이 페이즈 완료 후에도 REQUIREMENTS.md에서 `[ ]`로 남아 마일스톤 클로즈 시점에 일괄 정리해야 했다. 페이즈 transition마다 traceability를 갱신했으면 클로즈가 더 깔끔했을 것.
- **CI 액션 버전 핀 노후** — actions/checkout@v4, setup-java@v4가 Node 20 deprecation 경고를 유발(UAT Test 7에서 발견). 처음부터 최신 메이저(@v5)를 핀했으면 회피 가능했다.

### Patterns Established
- Generator/Runner split: 프로파일-프리 @Component(테스트 가능) + 얇은 @Profile ApplicationRunner(@Order 명시).
- 멱등 시드: per-key `existsBy` 가드 + 집계 `count()==0` 가드로 재실행 안전.
- 리뷰어-페이싱 README: badge → 3단계 재현 → 아키텍처 → 정직한 트레이드오프 → setup/seed → curl+JSON → 한계/보존. JSON-as-UI(프론트엔드 없이 curl+샘플 JSON으로 헤드라인 판매).
- 시크릿-프리 로깅/마커: collection_run·헬스·시드 어디에도 키/시크릿 미노출, README는 env 플레이스홀더만.

### Key Lessons
1. **코어 밸류를 게이트로 강제하라** — "수집 신뢰성 우선"을 하드 게이트로 만들어 헤드라인 기능(event-impact)이 단단한 데이터 위에만 올라가게 했다. 우선순위는 문서가 아니라 게이트로 지켜진다.
2. **데모는 계약을 준수해야 진짜다** — 시드가 UNIQUE·staleness 계약을 우회하면 데모 숫자가 거짓이 된다. 5분 오프셋 배치처럼 계약 안에서 의미 있는 데이터를 만들어야 헤드라인이 정직하다.
3. **transition마다 traceability를 갱신하라** — 요구사항 체크박스/traceability를 페이즈 완료 시 즉시 전환하면 마일스톤 클로즈가 일괄 정리 작업 없이 끝난다.
4. **상관 ≠ 인과를 응답·문서에 박아라** — event-impact는 시점 상관까지만. 과대 주장을 피하는 문구가 포트폴리오의 신뢰도를 높인다.

### Cost Observations
- Model mix: 대부분 opus (balanced 프로파일 — planner opus, checker sonnet).
- Notable: 이 환경은 백그라운드 서브에이전트가 권한 거부 → 검증·진단·수정을 오케스트레이터가 인라인 수행.

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.0 | 6 | 15 | 초기 MVP — 하드 게이트로 수집 신뢰성 우선, 게이트 통과 후 event-impact 헤드라인 |

### Cumulative Quality

| Milestone | Tests | Build | Zero-Dep Additions |
|-----------|-------|-------|--------------------|
| v1.0 | 86 (0 failures, 1 skipped) | ✓ green | Flyway V1–V3, build.gradle 의존성 추가 0 (Phase 6 docs/CI) |

### Top Lessons (Verified Across Milestones)

1. (v1.0) 코어 밸류는 게이트로 지켜진다 — 다음 마일스톤에서 재검증 예정.
2. (v1.0) 데모/시드는 라이브 계약을 준수해야 정직하다 — 다음 마일스톤에서 재검증 예정.
