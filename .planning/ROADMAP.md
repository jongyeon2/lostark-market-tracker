# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** — Phases 7–11 (shipped 2026-06-29) — [archive](milestones/v1.1-ROADMAP.md)
- 🚧 **v1.2 Item Visual/Data Enrichment** — Phases 12–14 (정의됨 2026-06-29)

## Overview (v1.2)

v1.1이 세운 데모 대시보드 3화면(Dashboard / Item Timeline / Event Impact)과 셀렉터에 **품목 아이콘·역할 그룹 메타데이터**를 입히고, **융화재료 + 큐레이션된 딜러/서포터 유물 각인서**(12~20개)를 watchlist·seed 데모에 추가한다. enrichment는 수집·캐시·event-impact 계산을 **0줄도 건드리지 않는 read-path additive 한 겹**이며, **API 키 없이 seed만으로** 아이콘까지 재현돼야 한다. 순서: 실 API로 데이터를 잠그는 스파이크(12, 게이트) → 백엔드 enrichment 컬럼·DTO·seed 확장(13) → 프론트 아이콘·fallback·docs(14). 아이콘 출처는 **API `Icon` URL을 DB에 적재**(스파이크 캡처 상수), 프론트는 `<img onError>` fallback으로 핫링크 장애를 흡수한다.

---

## Phases (v1.0 — SHIPPED)

<details>
<summary>✅ v1.0 MVP (Phases 1–6) — SHIPPED 2026-06-25</summary>

- [x] **Phase 1: Foundation + Task 0** — 2026-06-20
- [x] **Phase 2: Collection Pipeline** — 2026-06-22
- [x] **Phase 3: Read API + Cache** — 2026-06-23
- [x] **Phase 4: Admin + Events** — 2026-06-24
- [x] **Phase 5: Event Impact (게이트 조건부)** — 2026-06-24
- [x] **Phase 6: Distribution + Docs** — 2026-06-25

전체 상세: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v1.1 Frontend Demo Dashboard (Phases 7–11) — SHIPPED 2026-06-29</summary>

- [x] **Phase 7: Frontend Foundation** — 2026-06-25
- [x] **Phase 8: Dashboard** — 2026-06-25
- [x] **Phase 9: Item Timeline** — 2026-06-26
- [x] **Phase 10: Event Impact** — 2026-06-26
- [x] **Phase 11: Demo Surface + Docs** — 2026-06-27

전체 상세: [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md)

</details>

## Phases (v1.2 — Item Visual/Data Enrichment)

**Phase Numbering:** v1.1의 마지막 phase(11)에 이어 12부터 연속 번호.

- [x] **Phase 12: API Spike + Data Lock (게이트)** — `/markets/options`·`/markets/items` 실측으로 iconUrl/item id/category·fallback·큐레이션 목록 잠금 ✅ (큐레이션 15개, 만개 보류)
- [ ] **Phase 13: Backend Enrichment + Seed Expansion** — V4 nullable 컬럼 + 4개 DTO 패스스루 + seed/watchlist 확장(키 없는 재현)
- [ ] **Phase 14: Frontend Icons + Fallback + Docs** — 공용 `<ItemIcon>` + 3화면·셀렉터 아이콘·역할 배지 + docs

## Phase Details

### Phase 12: API Spike + Data Lock (게이트)

**Goal**: 본인 JWT 키로 `/markets/options`·`/markets/items`를 1회 실측해 융화재료·유물 각인서의 item id·display_name·category·**iconUrl 제공 여부**를 확정하고, 거래 가능·아이콘 구별 여부가 확인된 **큐레이션 12~20개**와 **fallback 전략**을 findings 문서로 잠근다. 이후 phase는 이 상수만 소비한다(런타임은 키 불필요). v1.0 Task 0의 spike-then-lock 패턴 재사용.
**Depends on**: Nothing (v1.2 첫 phase) — 실행 시 로컬 env에 본인 JWT 필요(1회 실측, 런타임 아님)
**Requirements**: SPIKE-01, SPIKE-02, SPIKE-03, SPIKE-04, SPIKE-05

**완료 조건 (Success Criteria — 무엇이 TRUE여야 하나):**

  1. `/markets/options` 응답으로 유물 각인서(CategoryCode=40000 / ItemGrade="유물")와 융화재료의 CategoryCode가 findings에 **실측값**으로 기록된다
  2. `/markets/items` 응답에 품목별 아이콘 URL 필드가 존재하는지(필드명·CDN 도메인)가 findings에 확정되고, 없으면 fallback 경로가 정해진다
  3. 융화재료(상급/최상급 오레하 + 아비도스/운명 확인분) + 딜러/서포터 각인서 후보가 거래 가능·item id·iconUrl로 검증되어, 최종 큐레이션 **12~20개** 목록(각인서 아이콘 구별 여부 포함)이 findings에 확정된다
  4. 아이콘 부재·로딩 실패 시 fallback 전략이 findings에 문서화된다
  5. findings 문서·커밋에 실 API 키·계정 식별자·가격 원문이 **없다**(공개 메타데이터만)

**검증 방법 (Verification):**

  - 로컬 env JWT로 `/markets/options`·`/markets/items` 호출 → 아이콘 URL·id·grade·category를 마스킹해 findings에 발췌 기록
  - findings의 큐레이션 12~20개가 전부 실측 id·iconUrl을 가짐 — 추정/플레이스홀더 0건
  - `git diff`/grep으로 findings·커밋에 `bearer`/JWT/계정 식별자 0건 확인
  - 각인서 iconUrl이 서로 구별되는지 확인(동일하면 라벨 병기를 fallback 결정에 반영)

**사용자 확인 포인트 (User confirmation):**

  - 큐레이션 목록(어떤 융화재료·어떤 각인서)이 도메인 안목에 맞는지 직접 확인
  - 아비도스/운명 융화재료 포함/제외 최종 결정

### Phase 13: Backend Enrichment + Seed Expansion

**Goal**: 스파이크가 잠근 데이터를 받아 `tracked_item`에 nullable enrichment 컬럼(Flyway V4)을 추가하고 4개 read DTO에 패스스루하며, WatchlistSeeder/SyntheticDemoData에 큐레이션 품목·아이콘 상수를 베이크해 **키 없이 재현**되게 한다. 수집·캐시·event-impact는 **0줄 변경**.
**Depends on**: Phase 12
**Requirements**: ITEM-01, ITEM-02, ITEM-03, ITEM-04, SEED-01, SEED-02, SEED-03, SEED-04

**완료 조건 (Success Criteria):**

  1. Flyway `V4__add_item_enrichment.sql`이 icon_url/item_group/role_group(**nullable**)을 추가하고 부팅 시 `ddl-auto=validate`가 통과한다(V1–V3 마이그레이션 불변)
  2. 4개 read 응답(item list / latest / timeline / event-impact)이 iconUrl·itemGroup·roleGroup을 포함한다
  3. WatchlistSeeder가 큐레이션 12~20개를 enrichment와 함께 등록하고, SyntheticDemoData가 신규 품목의 합성 스냅샷을 키 없이 멱등 적재한다
  4. seed 프로파일만으로 신규 품목·아이콘 URL이 DB·응답에 채워진다(`curl`로 iconUrl 확인)
  5. 수집 스케줄러·Redis 캐시·EventImpactService 소스 diff가 **0줄**이고 전체 Testcontainers 스위트가 그린이다

**검증 방법 (Verification):**

  - `./gradlew build` 전체 그린(Testcontainers), V4 적용 후 `validate` 통과
  - seed 기동 후 `curl /api/items`·`/latest`·`/prices`·`/event-impact` 응답에 iconUrl·group 필드 존재 + 값이 스파이크 상수와 일치
  - `git diff --stat`으로 Scheduler/TokenBucket/Cache/EventImpactService 변경 0 확인
  - seed 2회 실행 시 중복 품목·스냅샷 없음(기존 UNIQUE 멱등 가드 유지)

**사용자 확인 포인트 (User confirmation):**

  - 응답 DTO 필드명(iconUrl/itemGroup/roleGroup)·role_group enum 값이 프론트 zod 스키마와 합의된 형태인지 확인

### Phase 14: Frontend Icons + Fallback + Docs

**Goal**: 백엔드 0줄 변경 원칙대로 DTO의 iconUrl·group을 소비하는 공용 `<ItemIcon>`(onError fallback)으로 Dashboard/Timeline/Event Impact·셀렉터에 아이콘과 역할 배지를 입히고, 데이터 출처·실측 결과·fallback 전략을 docs에 기록한다.
**Depends on**: Phase 13
**Requirements**: ICON-01, ICON-02, ICON-03, ICON-04, ICON-05, ICON-06, ICON-07, ICON-08

**완료 조건 (Success Criteria):**

  1. 공용 `<ItemIcon>`이 iconUrl을 고정 슬롯에 렌더하고 onError 시 역할색 글리프/이니셜로 대체한다(레이아웃 시프트 없음)
  2. Dashboard 품목 카드·ItemSelect 셀렉터·Timeline 최신가 카드·Event Impact 품목 카드에 아이콘이 표시된다
  3. 융화재료/딜러각인/서포터각인 역할 그룹이 배지로 구분된다(필터 컨트롤은 v2)
  4. 추가 품목(융화재료·큐레이션 각인서)이 Dashboard/Timeline/Event Impact에서 선택 가능하다
  5. seed만으로(키 없이) 3화면에 아이콘이 뜨고, CDN 차단/아이콘 부재에도 UI가 안 깨지며, `npm run build`가 통과한다. frontend/README·루트 README에 출처·실측·fallback이 기록된다

**검증 방법 (Verification):**

  - seed 백엔드 + `npm run dev` → 3화면+셀렉터에 아이콘·역할 배지 표시(브라우저 확인)
  - devtools offline 또는 iconUrl 손상 → fallback 글리프로 대체되고 레이아웃 유지 확인
  - `npm run build`(tsc 포함) 통과, `git status`에 백엔드 `src/` 변경 0
  - 루트/frontend README만 따라 클린 재현 시 아이콘까지 뜨는지 확인

**사용자 확인 포인트 (User confirmation):**

  - 아이콘·역할 배지가 3화면에서 일관되고 식별적인지 직접 확인
  - fallback이 "미완성"으로 안 보이는지 확인

## Progress

**Execution Order (v1.2):**
Phase 12 → Phase 13 → Phase 14 (선형 — 각 phase가 직전 산출물에 하드 의존: 스파이크 상수 → 백엔드 DTO/seed → 프론트 소비)

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 12. API Spike + Data Lock | v1.2 | 0/? | Not started | — |
| 13. Backend Enrichment + Seed | v1.2 | 0/? | Not started | — |
| 14. Frontend Icons + Fallback + Docs | v1.2 | 0/? | Not started | — |

*Plan 수는 `/gsd-plan-phase [N]`에서 확정.*

## Requirement Coverage (v1.2)

| Phase | Requirements | Count |
|-------|--------------|-------|
| 12. API Spike + Data Lock | SPIKE-01..05 | 5 |
| 13. Backend Enrichment + Seed | ITEM-01..04, SEED-01..04 | 8 |
| 14. Frontend Icons + Fallback + Docs | ICON-01..08 | 8 |

**Coverage:** v1 requirements 21 total · mapped 21 · unmapped 0 ✓

---

_v1.0/v1.1 상세는 milestones/ 아카이브, 마일스톤 종료 시 v1.2도 milestones/v1.2-ROADMAP.md로 아카이브._
