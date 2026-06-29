# Project Research Summary

**Project:** 로스트아크 거래소 시세 수집·분석 파이프라인 — v1.2 Item Visual/Data Enrichment
**Domain:** 게임 마켓 대시보드 품목 시각/데이터 enrichment(기존 파이프라인 위 additive 한 겹)
**Researched:** 2026-06-29
**Confidence:** HIGH (기존 스택·제약에서 직접 도출) / MEDIUM (Lostark Icon 필드·정확 CategoryCode — Phase 0 스파이크 확정)

## Executive Summary

v1.2는 **신규 인프라·의존성 없이** v1.0(백엔드)·v1.1(프론트) 위에 품목 메타데이터(아이콘·그룹·역할)를 read-path additive로 한 겹 얹는 마일스톤이다. 도메인은 "게임 마켓 시세"지만 구조는 금융 마켓의 "자산에 섹터/심볼 메타 붙이기"와 동형이라 서사 비용이 낮다. 가장 가치 있는 작업은 새 코드가 아니라 **실 API 실측(Phase 0 스파이크)으로 item id·iconUrl·category를 확정하고 그 값을 코드 상수로 잠그는 것**이며, 이는 v1.0 Task 0에서 이미 성공한 spike-then-lock 패턴의 재사용이다.

권장 접근: (1) 스파이크로 모델·큐레이션 목록·fallback 전략을 잠근다 → (2) `tracked_item`에 nullable enrichment 컬럼(V4)을 추가하고 4개 read DTO에 패스스루하며 seed/watchlist를 스파이크 상수로 확장한다 → (3) 프론트는 백엔드 0줄 변경 원칙대로 DTO의 `iconUrl`만 소비하는 공용 `<ItemIcon>`(onError fallback)로 3화면+셀렉터를 입힌다.

핵심 리스크는 기술이 아니라 **규율**이다: ① 실 API 키/응답 원문이 seed·문서·커밋에 새지 않게, ② 수집·캐시·event-impact 계산을 0줄도 건드리지 않게, ③ seed가 키 없이 아이콘까지 재현되게(상수 베이크) — 이 셋이 무너지면 Core Value(수집 신뢰성)와 검증 기준(키 없는 재현)이 깨진다.

## Key Findings

### Recommended Stack

신규 런타임 의존성 0. 유일한 스키마 변경은 `V4__add_item_enrichment.sql` 한 장(nullable 컬럼 3개). 프론트는 lucide(fallback 글리프)·Tailwind(고정 슬롯)만 기존에서 재사용.

**Core technologies (전부 기존):**
- Flyway V4(additive): icon_url/item_group/role_group — `ddl-auto=validate`와 1:1
- zod 옵셔널 DTO 필드: `.parse`-at-boundary가 타입 안전 + 점진 배포 안전(미배포 시 undefined)
- React `<img onError>` + lucide: 외부 라이브러리 없이 graceful fallback

### Expected Features

**Must have (table stakes):**
- 품목 아이콘 표시(3화면+셀렉터) — 마켓 UI 기본기
- 아이콘 로딩 실패 fallback — UI 무파손
- 융화재료/딜러각인/서포터각인 그룹 배지 — 12~20개 식별성
- seed만으로 재현 — 키 없는 데모

**Should have (competitive):**
- 큐레이션된 딜러/서포터 공용 각인서 — "전체 덤프"가 아닌 도메인 안목
- role_group 시각 구분 — 금융 섹터 비유로 포트폴리오 서사 강화

**Defer (v2+):**
- 그룹 필터 컨트롤(배지로 충분), 실서비스급 검색/관리 UI(명시 제외), 소스 확장(SRC-V2)

### Architecture Approach

enrichment는 `tracked_item` 신규 컬럼 → Read DTO → 프론트 `<ItemIcon>`까지 단방향 read-path. 수집 스케줄러·Redis 캐시·EventImpactService는 무변경. seed는 스파이크 findings를 상수로 잠가 키 없이 재현.

**Major components:**
1. V4 마이그레이션 + TrackedItem 필드 — enrichment 적재면
2. Read DTO 매퍼(4종) — iconUrl/group 패스스루(계산 무변경)
3. WatchlistSeeder/SyntheticDemoData — 큐레이션 품목·아이콘 상수
4. 프론트 `<ItemIcon>` — 단일 fallback 규칙 공유점

### Critical Pitfalls

1. **키/응답 원문 유출** — 공개 메타데이터만 발췌, 커밋 전 JWT grep (Phase 0)
2. **seed 아이콘 부재** — 스파이크 iconUrl을 seeder 상수로 베이크 (seed phase)
3. **수집/계산 회귀** — enrichment는 read-path additive only, 해당 파일 diff 0 (전 백엔드 phase)
4. **Flyway 체크섬 깨짐** — V1–V3 불변, 신규 V4 nullable (enrichment phase)
5. **비거래/잘못된 id** — API 실측 확인된 품목만 확정 (Phase 0)

## Implications for Roadmap

연속 번호. v1.1이 Phase 11에서 종료 → **v1.2는 Phase 12부터** 시작. 제안 구조 3 phase:

### Phase 12: API 실측 스파이크 + 데이터 잠금 (게이트)
**Rationale:** iconUrl·item id·category가 실측 전엔 미확정 → 모든 후속의 선행 게이트(v1.0 Task 0 동형).
**Delivers:** `/markets/options`·`/markets/items` 실응답 기반 FINDINGS 문서 — iconUrl 제공 여부, 각인서(40000)·융화재료 CategoryCode, 거래 가능·item id 확인된 큐레이션 12~20개 확정, fallback 전략 확정, 각인서 아이콘 구별 여부.
**Addresses:** 큐레이션 각인서·융화재료 후보 검증.
**Avoids:** 비거래/잘못된 id, 키 유출, 각인서 동일 아이콘.

### Phase 13: 백엔드 Enrichment + Seed/Watchlist 확장
**Rationale:** 스파이크가 데이터를 잠근 직후, 프론트가 소비할 DTO·seed가 존재해야 함.
**Delivers:** V4 nullable 컬럼(icon_url/item_group/role_group) + TrackedItem 필드 + 4개 read DTO 패스스루 + WatchlistSeeder/SyntheticDemoData에 신규 품목·아이콘 상수.
**Uses:** Flyway/JPA/zod 계약.
**Avoids:** Flyway 체크섬·수집 회귀·seed 아이콘 부재.
**Note:** 플랜이 커지면 (13a 컬럼+DTO)/(13b seed 확장)으로 분할 가능.

### Phase 14: 프론트 아이콘 + Fallback + Docs
**Rationale:** 백엔드 DTO가 준비된 뒤 표시 계층 — 백엔드 0줄 변경.
**Delivers:** 공용 `<ItemIcon>`(onError fallback) + Dashboard/Timeline/Event Impact 카드·셀렉터 아이콘·그룹 배지 + frontend/README·루트 README에 데이터 출처·실측 결과·fallback 전략 기록.
**Avoids:** 레이아웃 시프트·깨진 아이콘 노출.

### Phase Ordering Rationale

- 스파이크가 데이터를 잠그기 전엔 컬럼·seed를 못 만든다(하드 의존) → Phase 12 선행.
- 프론트는 DTO 소비자라 백엔드 enrichment 뒤(Phase 13 → 14).
- 무변경 가드는 매 phase verification에서 수집/캐시/event-impact diff 0으로 단언.

### Research Flags

깊은 plan-phase 리서치가 필요한 곳:
- **Phase 12:** 실 API 응답 형태(특히 융화재료 CategoryCode·각인서 아이콘 구별)는 키로 직접 호출해야만 확정 — plan/execute에서 사용자 키 실행 단계 필요.

표준 패턴이라 리서치 생략 가능:
- **Phase 13:** Flyway additive·DTO 패스스루는 프로젝트 기존 패턴.
- **Phase 14:** React img fallback·기존 `<ItemSelect>` 확장은 v1.1 패턴 재사용.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | 전부 기존, 신규 의존성 0 |
| Features | HIGH | 브리프로 범위 명확 |
| Architecture | HIGH | 기존 read-path 위 얇은 한 겹 |
| Pitfalls | HIGH | 프로젝트 제약에서 직접 도출 |

**Overall confidence:** HIGH (단, Phase 12 실측 전까지 Icon 필드·CategoryCode·큐레이션 확정값은 MEDIUM)

### Gaps to Address

- **Lostark `Icon` 필드 실재·형태:** 온라인 부분 확인, Phase 12에서 실응답으로 확정.
- **융화재료 CategoryCode:** 각인서=40000은 확인, 융화재료(상급/최상급 오레하)는 50000대 추정 → 실측 필요.
- **아비도스/운명 융화재료 포함 여부:** 실거래 품목명·id 확인 후 결정(브리프 지시).

## Sources

### Primary (HIGH confidence)
- 기존 코드베이스·PROJECT.md Key Decisions — spike-then-lock, 무변경 원칙, zod DTO 경계
- `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — 동일 API 모델 잠금 선례

### Secondary (MEDIUM confidence)
- https://www.inven.co.kr/board/lostark/4821/104361 — 각인서 CategoryCode=40000, ItemGrade="유물"
- https://developer-lostark.game.onstove.com/ — 공식 포털(스파이크 1차 출처)
- https://lo4.app/markets/inscriptions, https://kloa.gg — 아이콘 표시·큐레이션 관행

### Tertiary (LOW confidence)
- 융화재료 CategoryCode 50010/50020 — 커뮤니티 추정, Phase 12 실측 필요

---
*Research completed: 2026-06-29*
*Ready for roadmap: yes*
