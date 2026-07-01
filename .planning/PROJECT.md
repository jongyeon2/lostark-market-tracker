# 로스트아크 거래소 시세 수집·분석 파이프라인 (Lostark Price Tracker)

## What This Is

레이트리밋이 걸린 로스트아크 거래소(MARKETS) Open API에서 시세를 주기적으로 수집해 시계열로 적재하고, Redis 캐시 계층으로 서빙하며, 관리자가 등록한 주요 게임 이벤트와 가격 변동을 시점 상관시키는 백엔드 데이터 파이프라인이다. 신입 백엔드 개발자의 **설명 가능한 엔지니어링 실력**을 증명하기 위한 포트폴리오 프로젝트로, 도메인은 게임이지만 구조는 금융 마켓 데이터 파이프라인과 동일하다. 대상 청중은 신입 백엔드 채용 면접관과 본인(로아 유저)이다.

## Core Value

**레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다.** 다른 모든 게 실패해도 이 수집·저장·서빙 파이프라인은 동작해야 한다. event-impact(이벤트 상관)는 그 위에 얹는 헤드라인 기능이며, 수집 신뢰성이 흔들리면 상관 분석은 나쁜 데이터 위 장식 수학이 된다.

## Current Milestone: v1.3 관리자 콘솔 + 실데이터 라이브 배포

**Goal:** 읽기전용 seed 데모를 → 실데이터로 수집·서빙되고, 관리자가 이벤트·워치리스트를 직접 관리하며, 무료로 라이브 배포된 데모로 승격한다(배포 직전 보안 검증 게이트 통과). Phase 15부터 연속 번호.

**Target features:**
- **관리자 콘솔 UI** (먼저) — 시크릿 입력 로그인 게이트 + 이벤트 CRUD + 워치리스트 품목 관리 + 수집 상태 모니터링. 기존 `/api/admin/*` 소비, 백엔드 인증 무변경.
- **대시보드 카드 개선** — 물품 고유 번호 제거(아이콘·이름·골드 가격·수집 시각 유지) + 카드 클릭 시 해당 품목 타임라인으로 딥링크 이동(품목 pre-select).
- **실데이터 전환** — 프론트 데모를 seed→실수집 데이터 기준으로(seed 프로파일은 로컬/테스트 전용 유지). 조기 배포로 시계열·event-impact를 시간에 걸쳐 축적.
- **무료 라이브 배포 + 보안 검증** (마지막) — 무료 타깃 리서치 결정 + 프론트 서빙 방식(정적/단일 출처, 보류됐던 DEMO-03 재활성 후보) + 배포 직전 보안 게이트(공개 URL 관리자 쓰기·API 키 노출·CORS·DB/Redis 포트·HTTPS).

**핵심 결정 (new-milestone):** 관리자 인증 = 기존 X-Admin-Secret 재사용(시크릿 입력 로그인, 백엔드 인증 무변경) · 데모 = 실데이터만(조기 배포 후 축적, seed는 로컬/테스트 전용) · 무료 배포 타깃 = 배포 phase 착수 시 리서치로 결정.

**v2 후보(잔여 보류):** 그룹 필터 토글(FILTER-V2) · 등급별 색상/정렬 정교화(GRADE-V2) · 관측성(OPS-V2, Micrometer) · event-impact 고도화(IMPACT-V2) · 경매장/보석 소스 확장(SRC-V2) · 매직넘버 외부화(CFG-V2) · 프론트 잔여 고도화(FE-V2: 실시간 갱신·다크모드/i18n).

**불변 제약(상시):** 프론트에서 Lostark API 직접 호출 금지 · API key는 백엔드 env에서만 · 실키를 코드/문서/로그/커밋에 미기재 · 수집/캐시/event-impact 변경은 Core Value(수집 신뢰성) 가드 하에만.

## Current State

**Shipped:**
- ✅ **v1.0 MVP** (2026-06-25) — 신뢰 가능한 10분 수집 파이프라인 + Redis 캐시 read API + event-impact + 관리자 CRUD + CI·seed·README 데모 표면 (Phases 1–6, 24/24 요구사항). [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** (2026-06-29) — v1.0 read API를 백엔드 0줄 변경(Vite 프록시 dev 동일 출처)으로 소비하는 React + TS + Tailwind + Recharts 3화면(Dashboard / Item Timeline / Event Impact)을 seed 기준 빈 화면 없이 재현. README의 curl 데모를 클릭 가능한 데모 표면으로 전환 (Phases 7–11, 필수 20/20; DEMO-03 정적 서빙은 v2 강등). [archive](milestones/v1.1-ROADMAP.md)
- ✅ **v1.2 Item Visual/Data Enrichment** (2026-06-30) — v1.1 3화면·셀렉터에 품목 아이콘·역할 배지(딜러/서포터/융화재료)·역할군 정렬을 입히고 큐레이션 15개를 watchlist·seed에 추가. enrichment는 수집·캐시·event-impact 0줄 변경의 read-path additive 한 겹이며 API 키 없이 seed만으로 아이콘까지 재현. 공용 `<ItemIcon>` onError 역할색 글리프 fallback(레이아웃 시프트 0) (Phases 12–14, 21/21 요구사항, UAT 8/8 + 보안 9위협 closed). [archive](milestones/v1.2-ROADMAP.md)

**현재 코드 상태:** 백엔드 Java ~5,790 LOC(main 67 + test 23 파일) — v1.2는 백엔드 핵심 src 0줄(enrichment는 nullable 컬럼·엔티티·DTO·시더만). 프론트 TypeScript ~2,800 LOC(`frontend/src`) — v1.2에서 공용 `<ItemIcon>`/`<RoleBadge>`/roleGroup + enrichment 4 zod 스키마 추가, 신규 npm 의존 0. Spring Boot 3.4 / Java 21 / PostgreSQL 16 (Flyway V1–V4) / Redis 7 + Vite 6 / React 19 / Tailwind v4 / Recharts / shadcn(slate·new-york) / zod / TanStack Query. 전체 Testcontainers 스위트 그린.

**현재 상태:** v1.2 마감(2026-06-30) — v1.0/v1.1/v1.2 전부 shipped. 다음 마일스톤 미정(`/gsd-new-milestone`으로 정의, Phase 15부터). 보류 중 v2 후보는 위 "Next Milestone" 참조.

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
- ✓ 시각 enrichment (품목 아이콘 + 역할 배지[딜러/서포터/융화재료] + 역할군 정렬 + onError 역할색 글리프 fallback[시프트 0], 큐레이션 15개, API 키 없는 seed 재현, 백엔드 read-path additive 0줄) — v1.2 (Phase 12–14, SPIKE/ITEM/SEED/ICON 전체 21/21)

### Active

<!-- v1.3 관리자 콘솔 + 실데이터 라이브 배포 — REQUIREMENTS.md에서 REQ-ID로 정의(Phase 15부터). -->

**v1.3 관리자 콘솔 + 실데이터 라이브 배포** (진행 중). 요구사항은 이 마일스톤의 `REQUIREMENTS.md`에 REQ-ID로 정의된다:
- 관리자 콘솔 UI — 시크릿 입력 로그인 + 이벤트 CRUD + 워치리스트 품목 관리 + 수집 상태 모니터링
- 대시보드 카드 개선 — 물품 고유 번호 제거 + 카드 클릭 시 품목 타임라인 딥링크
- 실데이터 전환 — 프론트 데모를 seed→실수집 데이터 기준으로(조기 배포 후 축적)
- 무료 라이브 배포 + 사전 보안 검증 게이트

**v2 백로그(보류):** 그룹 필터 토글(FILTER-V2-01) · 등급별 색상/정렬 정교화(GRADE-V2-01) · 관측성(OPS-V2) · event-impact 고도화(IMPACT-V2) · 소스 확장(SRC-V2) · 매직넘버 외부화(CFG-V2) · 프론트 잔여 고도화(FE-V2: 실시간 갱신·다크모드/i18n).

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
| spike-then-lock 재사용 + 아이콘 출처=API Icon URL→DB 적재 (v1.2) | 실 API 1회 실측으로 데이터 잠금(런타임 키 불필요, v1.0 Task 0 동형); 번들 대신 DB 적재로 에셋 수급·번들 부담 회피 | ✓ Good — 큐레이션 15개·iconUrl CDN 잠금, seed만으로 아이콘 재현 |
| enrichment = read-path additive only, 수집/캐시/event-impact 0줄 (v1.2) | Core Value(수집 신뢰성) 보호 — 시각 한 겹이 핵심 경로를 오염하지 않게 | ✓ Good — Phase 13/14 백엔드 핵심 0줄, 회귀 그린 |
| 역할색=semantic 색군(rose/emerald/amber) + 역할 배지 solid 처리 (v1.2) | accent(blue-600)와 분리; solid/tinted/outline 3처리로 역할·상태·이벤트 배지 구별 | ✓ Good — Phase 14 UAT에서 한 화면 3처리 구별 확인 |
| 각인서 11종 동일 글리프 → 라벨·배지 식별(fallback 미대체), 그룹 필터 v2 강등 (v1.2) | 아이콘만으론 각인서 구분 불가; 15개 규모엔 필터 과함 | ✓ Good — 실아이콘+한글 라벨+역할 배지로 식별, 셀렉터 그룹 헤더로 대체 |

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
*Last updated: 2026-07-01 — v1.3 관리자 콘솔 + 실데이터 라이브 배포 마일스톤 착수 (`/gsd-new-milestone`). Current Milestone/Active 갱신; 요구사항·로드맵 정의 진행 (Phase 15부터).*
