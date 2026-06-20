# 로스트아크 거래소 시세 수집·분석 파이프라인 (Lostark Price Tracker)

## What This Is

레이트리밋이 걸린 로스트아크 거래소(MARKETS) Open API에서 시세를 주기적으로 수집해 시계열로 적재하고, Redis 캐시 계층으로 서빙하며, 관리자가 등록한 주요 게임 이벤트와 가격 변동을 시점 상관시키는 백엔드 데이터 파이프라인이다. 신입 백엔드 개발자의 **설명 가능한 엔지니어링 실력**을 증명하기 위한 포트폴리오 프로젝트로, 도메인은 게임이지만 구조는 금융 마켓 데이터 파이프라인과 동일하다. 대상 청중은 신입 백엔드 채용 면접관과 본인(로아 유저)이다.

## Core Value

**레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다.** 다른 모든 게 실패해도 이 수집·저장·서빙 파이프라인은 동작해야 한다. event-impact(이벤트 상관)는 그 위에 얹는 헤드라인 기능이며, 수집 신뢰성이 흔들리면 상관 분석은 나쁜 데이터 위 장식 수학이 된다.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

(None yet — ship to validate)

### Active

<!-- Current scope. 상세 REQ-ID는 REQUIREMENTS.md. -->

- [ ] 10분 주기 시세 수집 파이프라인 (스케줄러 + 레이트리밋 + 부분 실패 처리)
- [ ] 시계열 영속화 (price_snapshot, UNIQUE 멱등, UTC TIMESTAMPTZ, collection_run 실행 이력)
- [ ] 조회 API (latest 캐시, prices 타임라인=스냅샷+이벤트, 큰 범위 다운샘플)
- [ ] 관리자 이벤트/품목 CRUD (시크릿 인증)
- [ ] event-impact (이벤트 전후 변화율, 충분성/staleness 가드) — **2주차 말 하드 게이트 통과 조건부**
- [ ] 운영 가시성 (/health/collection)
- [ ] 배포 산출물 (docker-compose, CI=Testcontainers, 격리 시드, README 데모 표면)

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
| 접근법 B (A→B 단계화) + C(event-impact)를 헤드라인으로 | 학습 목표(스케줄러·캐시·레이트리밋)가 곧 포트폴리오 차별점; B가 셋을 정면으로 다루는 유일안 | — Pending |
| 틱 내 병렬 팬아웃 + allOf().join(), 분산 락 제거 (1A) | fixedDelay가 동기 틱을 이미 직렬화; @Async fire-and-forget이 그 보장을 깨므로 await로 유지 → 단일 인스턴스에 분산 락 불필요 | — Pending |
| price_snapshot UNIQUE(tracked_item_id, collected_at) + 틱 정규화 collected_at | 재시도/중복 틱의 중복 행 방지(멱등성) | — Pending |
| collection_run 테이블 | /health/collection의 출처 + 구멍이 수집 실패인지 프로세스 다운인지 구분 | — Pending |
| DB = PostgreSQL (MySQL 아님) | TIMESTAMPTZ/시계열 범위 쿼리; Choice에서 MySQL 경험했으니 이번엔 Postgres 경험 | — Pending |
| 2주차 말 하드 게이트 (T-A) | 신뢰 수집+조회가 안 서면 event-impact를 v2로 강등 — 수집 신뢰성 우선 | — Pending |
| 관리자 엔드포인트 시크릿 인증 게이트 (6A) | 공개 레포/데모에서 인증 없는 쓰기 엔드포인트는 감점 | — Pending |
| 레이트리밋·@Async를 의도적 학습 쇼케이스로 유지 | MVP 규모엔 과중하지만 학습 목표 자체 — README에 정직히 서술 (Codex "과중" 지적은 합의된 트레이드오프) | — Pending |

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
*Last updated: 2026-06-20 after initialization*
