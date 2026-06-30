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

## Milestone: v1.1 — Frontend Demo Dashboard

**Shipped:** 2026-06-29
**Phases:** 5 (7–11) | **Plans:** 19

### What Was Built
- v1.0 read API 5종을 **백엔드 0줄 변경**(Vite 프록시 dev 동일 출처)으로 소비하는 React 19 + TS + Tailwind v4 + Recharts 3화면(Dashboard / Item Timeline / Event Impact).
- zod-as-single-source DTO 스키마 + `.parse`-at-boundary 타입드 API 클라이언트 + TanStack Query fetch-on-mount + native-Intl KST 포맷터.
- 헤드라인 시각화 — UTC 위치/KST 라벨 min_price 라인 + eventType별 ReferenceLine 마커 + 다운샘플 배지; event-impact ok/insufficient(희소/stale 이유+anchor 근거) + 상시 "상관≠인과" 배너.
- seed 기준 재현 문서(frontend/README 단일 출처 + 루트 README 포인터 + 스크린샷) + UAT.
- gap closure 11-04 — seed가 합성 SUCCESS collection_run을 멱등 적재해 헬스 카드 12/12 SUCCESS.

### What Worked
- **백엔드 무변경 제약을 프록시로 0-비용 달성** — Vite `server.proxy`로 dev 동일 출처 → CORS·백엔드 변경 회피. `git status`에 `src/` 변경 0줄로 증명.
- **계약을 코드로 강제** — zod 단일 출처 스키마가 백엔드 실측 DTO와 1:1, `.parse`-at-boundary로 드리프트를 런타임에 차단.
- **시각 정직성을 UI에 박음** — UTC instant 위치 / KST 라벨 분리(off-by-9h 가드 연장), 상시 "상관≠인과" 배너·다운샘플 배지로 과대해석/원점-아닌-버킷 명시.
- **gap을 읽기 경로 0줄 변경으로 해소** — UAT Test 3을 seed 데이터 공급 보강만으로 닫아 실데이터 동작 회귀 위험 0.

### What Was Inefficient
- **데모 데이터 일관성을 좁게 봤다 (UAT Test 3)** — seed가 snapshot/event만 심고 collection_run을 누락 → 헬스 카드가 영속 볼륨의 과거 키리스 AUTH_ERROR run을 얼굴로 내걸음. 읽기 경로가 표현할 **모든** 행을 함께 심어야 했는데 한 축만 채워 화면 내적 모순 발생, 사후 gap closure로 해소.
- **(재발) traceability 체크박스 일괄 전환** — 또 마일스톤 클로즈 시점에 Pending→Complete를 몰아서 정리. v1.0 교훈이 아직 습관화 안 됨.
- **shadcn CLI Windows `@` 디렉터리 버그** — `shadcn add`가 블록을 literal `@/` 디렉터리에 잘못 배치 + radix-ui 의존성 추가 → 블록을 `src/components/ui/`에 손수 작성해야 했다(메모리에 기록).

### Patterns Established
- **백엔드 무변경 프론트** — dev 프록시 동일 출처 + 타입드 클라이언트(`.parse`-at-boundary)로 계약 드리프트를 코드로 차단.
- **데모 데이터 일관성 규칙** — 읽기 경로가 그릴 **모든** 테이블(snapshot + event + collection_run)을 함께 심는다. 한 축만 심으면 다른 위젯이 과거 실데이터(stale)를 얼굴로 내건다.
- **UI 정직성 장치** — 상시 배너/배지로 상관≠인과·다운샘플·insufficient 이유를 명시(JSON-as-UI 정직성을 화면으로 연장).

### Key Lessons
1. **데모 데이터는 "읽기 경로가 그릴 모든 행"을 심어라** — 한 테이블만 채우면 다른 위젯이 stale 실데이터를 얼굴로 내건다. seed 일관성은 입력 데이터의 완전성 문제지 읽기 로직 결함이 아니다(UAT Test 3 핵심).
2. **무변경 제약은 프록시+코드 계약으로 0-비용** — zod로 계약을 강제하면 드리프트가 컴파일/런타임에 잡혀 "백엔드 안 건드림"이 말이 아니라 증명이 된다.
3. **(재확인, 미해결) transition마다 traceability 전환** — v1.0에서 배웠는데 v1.1에서 또 어겼다. 다음 마일스톤은 페이즈 SUMMARY 작성 시 즉시 체크박스 전환.

### Cost Observations
- Model mix: 대부분 opus (balanced 프로파일).
- Notable: 백그라운드 서브에이전트 권한 거부 → gap closure(11-04)·검증·마일스톤 클로즈 전부 오케스트레이터 인라인 수행.

---

## Milestone: v1.2 — Item Visual/Data Enrichment

**Shipped:** 2026-06-30
**Phases:** 3 (12–14) | **Plans:** 6

### What Was Built
- **spike-then-lock 데이터 잠금(Phase 12)** — 본인 JWT로 `/markets/options`·`/markets/items` 1회 실측 → 큐레이션 15개·iconUrl CDN·각인서 11종 동일 글리프/융화재료 4종 구별·fallback 전략을 findings로 잠금(런타임 키 불필요).
- **백엔드 enrichment(Phase 13)** — Flyway V4 nullable icon_url/item_group/role_group + 4 read DTO 평면 패스스루 + WatchlistSeeder/SyntheticDemoData 큐레이션 베이크(키 없는 재현). 수집/캐시/event-impact **0줄**.
- **프론트 시각 레이어(Phase 14)** — 공용 `<ItemIcon>`(고정 슬롯 + null/onError 역할색 글리프 fallback, 시프트 0)·`<RoleBadge>`(solid 한글)·roleGroup(sortByRole) + enrichment 4 zod 스키마, 4화면 일관 적용. 루트/frontend README 출처·실측·fallback·자산 섹터 서사.
- **검증** — Phase 14 UAT 8/8 PASS(Playwright seed 실측) + 보안 9위협 closed(threats_open 0).

### What Worked
- **검증된 패턴(spike-then-lock) 재사용** — v1.0 Task 0의 "실측 1회 → 상수 잠금 → 이후 상수만 소비"를 Phase 12가 그대로 재현 → 외부 API 의존을 런타임에서 제거, 데이터 모델 잠금이 매끄러웠다.
- **read-path additive 격리가 Core Value를 코드로 보호** — enrichment를 nullable 컬럼·DTO·시더로만 추가하고 수집/캐시/event-impact를 0줄로 두니, 시각 레이어를 회귀 위험 0으로 얹었다(git file-scope·회귀로 증명).
- **3처리 배지 택소노미** — solid(역할)/tinted(상태)/outline(이벤트)로 처리 방식을 분리해 hue가 겹쳐도 의미가 구별됨(UAT에서 한 화면 3처리 공존 확인).
- **고정 슬롯 + onError 역할색 fallback** — CDN 차단·깨진 URL을 레이아웃 시프트 0으로 흡수, "미완성"이 아닌 의도된 디자인으로 읽힘(Playwright로 15개 강제 실패 실측).
- **시각 phase UAT를 Playwright 자동 검증으로** — 스크린샷 실측으로 체크포인트를 자동 판정, 수동 질문을 최소화.

### What Was Inefficient
- **(3연속) traceability 체크박스 lag** — SPIKE-01..05가 또 클로즈 시점까지 `[ ]`/Pending으로 남아 일괄 정리. v1.0·v1.1에 이어 **세 번째 연속** — 의지로는 안 고쳐지는 구조적 습관 문제로 확정.
- **audit-open false-positive** — quick task가 접두 파일명(`{id}-SUMMARY.md`)을 쓰는데 helper가 bare `SUMMARY.md`만 찾아 완료된 7개를 "missing"으로 오탐. 도구 컨벤션 불일치(클로즈를 막진 않았으나 노이즈).
- **검증용 seed 백엔드 기동이 dev 실데이터를 재시딩** — UAT/스크린샷을 위해 seed로 띄우며 그날 dev 수집분을 덮음. 검증엔 문제없으나 환경 상태 부수효과(재수집 가능).

### Patterns Established
- **spike-then-lock 재사용** — 외부 API 의존을 1회 실측으로 상수화해 런타임 키리스 재현. v1.0 Task 0 → v1.2 Phase 12로 검증됨(재사용 가능 패턴).
- **read-path additive 한 겹** — Core Value(핵심 경로) 0줄 가드 하에 nullable 컬럼·DTO·시더로만 기능을 얹는다.
- **3처리 배지 택소노미** — 의미 축마다 처리(solid/tinted/outline)를 분리해 색 충돌에도 구별.
- **고정 슬롯 + onError fallback** — 외부 자산(CDN) 장애를 시프트 0으로 흡수.

### Key Lessons
1. **검증된 패턴은 재사용이 가장 빠르다** — spike-then-lock을 v1.2가 그대로 재현해 게이트 phase가 매끄러웠다. 패턴 카탈로그(RETROSPECTIVE)가 실제로 비용을 줄인다.
2. **Core Value 보호는 "additive 한 겹 + 0줄 가드"로 증명한다** — enrichment가 핵심 경로를 0줄로 두니 회귀 위험 없이 시각 레이어를 얹었다. "안 건드림"을 코드(diff)로 증명.
3. **(3연속 미해결 → 절차/자동화 필요) traceability lag** — 세 마일스톤 연속 클로즈 시점 일괄 정리. 의지로 안 되니, phase transition·quick 완료 시 체크박스 전환을 **강제하는 절차나 자동화**가 다음 마일스톤의 명시 액션이어야 한다.

### Cost Observations
- Model mix: 대부분 opus (balanced 프로파일).
- Sessions: 다회(컨텍스트 압축 수회 — 타임라인 일별 집계·README 재구성·마일스톤 마감).
- Notable: 백그라운드 서브에이전트 권한 거부 → verify-work(UAT)·secure-phase(보안)·complete-milestone(마감) 전부 오케스트레이터 인라인 수행. Playwright-MCP로 시각 UAT 자동화.

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.0 | 6 | 15 | 초기 MVP — 하드 게이트로 수집 신뢰성 우선, 게이트 통과 후 event-impact 헤드라인 |
| v1.1 | 5 (7–11) | 19 | read-only 프론트 데모 — 백엔드 0줄 변경(Vite 프록시), zod 계약 강제, 정직성 UI 장치 |
| v1.2 | 3 (12–14) | 6 | read-path enrichment 한 겹 — spike-then-lock 데이터 잠금, 백엔드 핵심 0줄, 프론트 아이콘/역할 배지/fallback, UAT+보안 게이트 |

### Cumulative Quality

| Milestone | Tests | Build | Zero-Dep Additions |
|-----------|-------|-------|--------------------|
| v1.0 | 86 (0 failures, 1 skipped) | ✓ green | Flyway V1–V3, build.gradle 의존성 추가 0 (Phase 6 docs/CI) |
| v1.1 | 백엔드 스위트 그린 유지(11-04 IT 회귀 추가) | ✓ green | 백엔드 src 0줄 변경; 프론트 ~2,621 LOC TS 신규 |
| v1.2 | 백엔드 스위트 그린 유지(V4 + enrichment 회귀) | ✓ green | 백엔드 핵심 src 0줄; Flyway V4 nullable 컬럼; 프론트 공용 컴포넌트 추가, 신규 npm 의존 0 |

### Top Lessons (Verified Across Milestones)

1. (v1.0→v1.2 재확인) 코어 밸류는 게이트/가드로 지켜진다 — v1.0은 하드 게이트, v1.2는 "read-path additive + 수집/캐시/event-impact 0줄" 가드로 핵심 경로를 보호하며 시각 레이어를 얹음. 우선순위는 문서가 아니라 게이트/diff로 지켜진다.
2. (v1.0→v1.1 재확인) 데모/시드는 라이브 계약을 준수해야 정직하다 — v1.2 seed도 enrichment를 UNIQUE/멱등 계약 안에서 베이크해 키 없이 재현.
3. (v1.0→v1.1→v1.2 **3연속 미해결**) transition마다 traceability를 전환하라 — 세 마일스톤 연속 클로즈 시점 일괄 정리(v1.2는 SPIKE-01..05). 의지로는 안 고쳐짐이 입증됨 → **다음 마일스톤은 절차/자동화로 강제**(phase SUMMARY/quick 완료 시 체크박스 전환)가 명시 액션.
4. (v1.2 신규) 검증된 패턴 재사용이 가장 빠르다 — spike-then-lock(v1.0 Task 0)을 v1.2 Phase 12가 재현해 게이트가 매끄러웠다. RETROSPECTIVE 패턴 카탈로그가 실제 비용을 줄인다.
