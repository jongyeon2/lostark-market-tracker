# 로스트아크 거래소 시세 수집·분석 파이프라인 (Lostark Price Tracker)

## What This Is

레이트리밋이 걸린 로스트아크 거래소(MARKETS) Open API에서 시세를 주기적으로 수집해 시계열로 적재하고, Redis 캐시 계층으로 서빙하며, 관리자가 등록한 주요 게임 이벤트와 가격 변동을 시점 상관시키는 백엔드 데이터 파이프라인이다. 신입 백엔드 개발자의 **설명 가능한 엔지니어링 실력**을 증명하기 위한 포트폴리오 프로젝트로, 도메인은 게임이지만 구조는 금융 마켓 데이터 파이프라인과 동일하다. 대상 청중은 신입 백엔드 채용 면접관과 본인(로아 유저)이다.

## Core Value

**레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다.** 다른 모든 게 실패해도 이 수집·저장·서빙 파이프라인은 동작해야 한다. event-impact(이벤트 상관)는 그 위에 얹는 헤드라인 기능이며, 수집 신뢰성이 흔들리면 상관 분석은 나쁜 데이터 위 장식 수학이 된다.

## Current State

**Shipped:**
- ✅ **v1.0 MVP** (2026-06-25) — 신뢰 가능한 10분 수집 파이프라인 + Redis 캐시 read API + event-impact + 관리자 CRUD + CI·seed·README 데모 표면 (Phases 1–6, 24/24 요구사항). [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** (2026-06-29) — v1.0 read API를 백엔드 0줄 변경(Vite 프록시 dev 동일 출처)으로 소비하는 React + TS + Tailwind + Recharts 3화면(Dashboard / Item Timeline / Event Impact)을 seed 기준 빈 화면 없이 재현. README의 curl 데모를 클릭 가능한 데모 표면으로 전환 (Phases 7–11, 필수 20/20; DEMO-03 정적 서빙은 v2 강등). [archive](milestones/v1.1-ROADMAP.md)

**현재 코드 상태:** 백엔드 Java ~5,790 LOC(main 67 + test 23 파일) · 프론트 TypeScript ~2,621 LOC(`frontend/src`). Spring Boot 3.4.1 / Java 21 / PostgreSQL 16 / Redis 7 + Vite 6 / React 19 / Tailwind v4 / Recharts / shadcn(slate·new-york) / zod / TanStack Query. 전체 Testcontainers 스위트 그린.

**다음 마일스톤(v1.2):** 미정 — `/gsd-new-milestone`로 범위 정의. 후보: 관측성(OPS-V2, Micrometer) · 라이브 배포(DEPLOY-V2) · event-impact 고도화(IMPACT-V2) · 소스 확장(SRC-V2) · 매직넘버 외부화(CFG-V2). 백엔드 포트폴리오 관점 권장 묶음 = 관측성 + 라이브 배포(프로덕션 readiness 한 겹).

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- ✓ 시계열 영속화 (price_snapshot UNIQUE 멱등, UTC TIMESTAMPTZ, collection_run 실행 이력) — v1.0 (Phase 1, DATA-01..04)
- ✓ 10분 주기 시세 수집 파이프라인 (스케줄러 + 레이트리밋 + 부분 실패 처리) — v1.0 (Phase 2, COLL-01..05)
- ✓ 조회 API (latest 캐시, prices 타임라인=스냅샷+이벤트, 큰 범위 다운샘플) — v1.0 (Phase 3, API-01..05)
- ✓ 운영 가시성 (/health/collection) — v1.0 (Phase 3, OPS-01)
- ✓ 관리자 이벤트/품목 CRUD (시크릿 인증) — v1.0 (Phase 4, ADMIN-01..03)
- ✓ event-impact (이벤트 전후 변화율, 충분성/staleness 가드) — v1.0 (Phase 5, IMPACT-01..02; 2주차 말 하드 게이트 통과)
- ✓ 배포 산출물 (docker-compose, CI=Testcontainers, 격리 시드, README 데모 표면) — v1.0 (Phase 1/6, DIST-01..04)
- ✓ 프론트 데모 대시보드 3화면 (Dashboard: collection health·활성 품목·최신가 / Item Timeline: min_price 라인차트+eventType 마커+다운샘플 / Event Impact: 전후 변화율+희소/stale 구분+"상관≠인과" 상시 배너) — v1.1 (Phase 7–11, FND/DASH/TIME/IMPCT-전체 + DEMO-01..02)
- ✓ 백엔드 무변경 데모 표면 (Vite 프록시 dev 동일 출처, seed 기준 재현, frontend/README+루트 README+스크린샷, UTC→KST 표시 가드) — v1.1 (Phase 7/11)

### Active

<!-- 다음 마일스톤 범위. 현재 활성 REQUIREMENTS.md 없음(마일스톤 사이) — /gsd-new-milestone에서 생성. -->

**다음 마일스톤(v1.2) 미정.** v1.0(24/24)·v1.1(필수 20/20) 모두 배포·검증 완료(Validated 참조). `/gsd-new-milestone`로 범위를 정의한다. v2 후보(승격 가능):

- [ ] 관측성 — Micrometer 카운터(429 / skipped tick / failed item / cache hit·miss) + Actuator (OPS-V2) ★ 권장
- [ ] 데모 배포 — Railway/Fly/Render + 프론트 CI (DEPLOY-V2) ★ 권장
- [ ] event-impact 고도화 — 카테고리 베이스라인 대비 초과상승률, median/스무딩 (IMPACT-V2)
- [ ] 소스 확장 — 경매장(AUCTIONS)/보석 (SRC-V2)
- [ ] 매직넘버 `@ConfigurationProperties` 외부화 (CFG-V2)
- [ ] 프론트 고도화 — 관리자 쓰기 UI, 실시간 갱신 (FE-V2-01..03)
- [ ] DEMO-03 — Spring 정적 서빙 단일 출처 패키징 (v1.1에서 슬립, FE-V2-04)

### Out of Scope

<!-- Explicit boundaries. -->

- 별도 프론트엔드 UI — README의 curl + 샘플 JSON으로 헤드라인 기능을 판다 (백엔드 포트폴리오)
- 카테고리 베이스라인 초과상승률 비교 — v2 (MVP는 단일 품목 전후 변화율까지)
- 자동 뉴스 파싱 · 이벤트 자동 추출 · 알림 — v2
- 경매장(AUCTIONS) · 보석 · 캐릭터 분석 — v2
- 기간형 이벤트(start/end) — v2 (MVP는 단일 occurred_at; ±N시간 윈도우 계산이 단일 시점 전제)
- 롤업 · 파티셔닝 · 보존 삭제 정책 · 다중 인스턴스 HA — v2
- 풀 유저/권한 모델 — v2 (MVP는 시크릿 게이트 한 겹)
- MSA · Kafka · Spring Batch — 1개월/단일 인스턴스/첫 스케줄러·캐시 학습에 과한 고도

## Context

- **개발자 경험:** Spring Boot + JPA 자주 사용(CRUD 능숙). **스케줄러·캐시·레이트리밋은 처음** → 학습 곡선이 곧 포트폴리오 차별점과 일치.
- **선행 설계:** 이 프로젝트는 `/office-hours`(승인) → `/plan-eng-review`(8개 결정 + Codex 외부검토 흡수, clean)를 거친 설계 문서를 기반으로 한다. 원본: `~/.gstack/projects/test/yeonjong-unknown-design-20260619-221517.md` (하단 "엔지니어링 리뷰 반영" 섹션이 구현 확정 레이어).
- **도메인 지식:** 강화 수단인 융화재료처럼 골드가 많이 드는 고변동 품목이 로아온·시즌 종료·대형 업데이트 시점에 시세 변동이 가장 심하다 — 검색으로 못 얻는, 이 프로젝트 차별점의 출처.
- **DB 선택 배경:** 기존 Choice 프로젝트에서 MySQL을 경험 → 이번엔 PostgreSQL로 시계열 스냅샷 저장 + 복합 인덱스 설계를 경험.
- **상관 ≠ 인과:** 이벤트-가격은 "시점상 겹친다(상관)"이지 "이벤트가 가격을 올렸다(인과)"가 아니다. 응답 문구/README를 거기에 맞추고 과대 주장하지 않는다.
- **현재 상태 (v1.0 shipped, 2026-06-25):** Java ~5,790 LOC (main 67 파일 + test 23 파일). Spring Boot 3.4.1 / Java 21 / PostgreSQL 16 / Redis 7, Flyway V1–V3, 전체 빌드 그린(86 tests, 0 failures, 1 skipped=@Disabled Task-0 스파이크). 6 phases / 15 plans / 약 5일. 잔여: CI 배지 Node 20 deprecation 경고는 액션 @v5 상향으로 정리(커밋 03584e0) — 다음 push에서 annotation 클린 최종 확인.

## Constraints

- **Timeline**: 약 1개월 (단일 개발자)
- **Tech stack**: Java / Spring Boot / JPA / Redis / **PostgreSQL** — MySQL 전용 SQL·타입 미사용
- **DB 환경**: 개발 = docker-compose `postgres` 컨테이너, 테스트 = Testcontainers PostgreSQL (로컬·CI 동일 메커니즘)
- **제외 기술**: MSA · Kafka · Spring Batch (명시적 제외)
- **API**: 로스트아크 개발자 포털 JWT 키(재발급 가능), `Authorization: bearer {token}`, 레이트리밋 키당 분당 100회(정확 수치 Task 0 확인)
- **데이터 소스**: 거래소(MARKETS)만. 경매장/보석은 v2
- **착수 게이트**: Task 0(API 검증 스파이크) 전엔 데이터 모델 미확정 — 특히 avg_price/trade_count 실제 제공 여부

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 접근법 B (A→B 단계화) + C(event-impact)를 헤드라인으로 | 학습 목표(스케줄러·캐시·레이트리밋)가 곧 포트폴리오 차별점; B가 셋을 정면으로 다루는 유일안 | ✓ Good — v1.0 전 범위 배포, event-impact가 헤드라인으로 동작 |
| 틱 내 병렬 팬아웃 + allOf().join(), 분산 락 제거 (1A) | fixedDelay가 동기 틱을 이미 직렬화; @Async fire-and-forget이 그 보장을 깨므로 await로 유지 → 단일 인스턴스에 분산 락 불필요 | ✓ Good — Phase 2 수집기 배포, 부분 실패 격리 동작 |
| price_snapshot UNIQUE(tracked_item_id, collected_at) + 틱 정규화 collected_at | 재시도/중복 틱의 중복 행 방지(멱등성) | ✓ Good — Testcontainers IT가 멱등성 증명 |
| collection_run 테이블 | /health/collection의 출처 + 구멍이 수집 실패인지 프로세스 다운인지 구분 | ✓ Good — /health/collection 배포 |
| DB = PostgreSQL (MySQL 아님) | TIMESTAMPTZ/시계열 범위 쿼리; Choice에서 MySQL 경험했으니 이번엔 Postgres 경험 | ✓ Good — TIMESTAMPTZ UTC + date_trunc 다운샘플 활용 |
| 2주차 말 하드 게이트 (T-A) | 신뢰 수집+조회가 안 서면 event-impact를 v2로 강등 — 수집 신뢰성 우선 | ✓ Good — 게이트 통과로 Phase 5 진행 |
| 관리자 엔드포인트 시크릿 인증 게이트 (6A) | 공개 레포/데모에서 인증 없는 쓰기 엔드포인트는 감점 | ✓ Good — X-Admin-Secret OncePerRequestFilter 배포 |
| 레이트리밋·@Async를 의도적 학습 쇼케이스로 유지 | MVP 규모엔 과중하지만 학습 목표 자체 — README에 정직히 서술 (Codex "과중" 지적은 합의된 트레이드오프) | ✓ Good — README에 정직히 서술 |
| 자체 Lua 원자적 Redis 토큰버킷 (Bucket4j 대신) | 재시작 후 토큰 복원 + fail-closed를 직접 제어; 레이트리밋 학습 목표에 부합 | ✓ Good — Phase 2 (COLL-03) 배포 |
| min_price 유지 + avg_price/trade_count v2 강등 | Task 0 실측: avg_price/trade_count는 일단위 전용(상세 Stats[]), per-tick 미제공 | ✓ Good — 모델 잠금 비준 (DATA-03) |
| 데모 이벤트 10분 그리드 5분 오프셋 배치 | 앵커 타이 규칙상 그리드 정확 배치는 pre==post → change_rate 0; 오프셋이 실제 non-zero 산출 | ✓ Good — Phase 6, SyntheticDemoDataIT가 non-zero 단언 |
| CI = 로컬과 동일 `./gradlew build` 단일 ubuntu job (CD는 v2) | 리뷰어가 로컬에서 돌리는 것과 동일 명령; CD/시크릿/매트릭스는 MVP 과중 | ✓ Good — 전체 Testcontainers 스위트 그린. CI 액션은 @v5로 상향(Node 24, deprecation 정리) |
| 프론트 = Vite 프록시(백엔드 무변경) + Recharts + shadcn/zod/TanStack Query (v1.1) | dev 동일 출처로 CORS·백엔드 변경 회피; 선언형 차트, zod 단일 출처 DTO·`.parse`-at-boundary로 타입 안전 | ✓ Good — v1.1 3화면 배포, 백엔드 src 0줄 변경 |
| UTC 데이터 유지 + KST 표시 (off-by-9h 가드를 UI까지 연장, v1.1) | 차트 위치는 UTC instant, 라벨만 KST → 시각 오프셋 버그 차단 | ✓ Good — 타임라인/임팩트 시각 일관(human-verified) |
| seed 데모에 합성 SUCCESS collection_run 멱등 적재 (11-04 gap closure) | seed가 snapshot/event만 심어 헬스 카드가 영속 볼륨의 과거 키리스 AUTH_ERROR run을 표시 — UAT Test 3 gap | ✓ Good — startedAt=gridNow가 stale run 덮음, 읽기 경로·DTO·프론트 0줄 변경, Testcontainers 회귀 고정 |
| DEMO-03(정적 서빙) 슬립 → v2 | 단일 출처 패키징은 명시적 stretch였고 Vite 프록시로 데모 충분 | — Pending — v2(FE-V2-04)에서 재평가 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-29 — v1.1 Frontend Demo Dashboard 마일스톤 종료 (Phases 7–11, 필수 20/20; DEMO-03 v2 강등)*
