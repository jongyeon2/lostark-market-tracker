# Feature Research

**Domain:** 게임 마켓 대시보드 품목 시각/데이터 enrichment
**Researched:** 2026-06-29
**Confidence:** HIGH (기능 범위가 브리프로 잘 정의됨)

## Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| 품목 아이콘 표시 | 마켓/거래소 UI는 텍스트만 있으면 "미완성"으로 보임 — 로아 유저는 아이콘으로 품목을 식별 | LOW | `<img src=iconUrl>` + 고정 슬롯 |
| 아이콘 로딩 실패 fallback | 외부 CDN·신규 품목은 항상 깨질 수 있음 → UI 무파손이 기본기 | LOW | `onError` → 역할색 글리프/이니셜 |
| 품목 그룹 구분(융화재료/딜러각인/서포터각인) | 12~20개가 섞이면 무엇이 무엇인지 모름 | LOW | 그룹 배지(필터는 v2) |
| seed만으로 재현 | 면접관이 키 없이 데모 → 아이콘이 비면 데모가 깨짐 | MEDIUM | 스파이크 캡처 상수를 seed에 베이크 |

### Differentiators (Competitive Advantage)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| 큐레이션된 딜러/서포터 공용 각인서 | "전체 각인서 덤프"가 아니라 도메인 지식 기반 선별 — 로아 유저의 안목을 증명 | MEDIUM | 후보에서 API 실거래 확인된 것만 확정 |
| role_group 기반 시각 구분 | 금융 마켓의 "섹터/자산군"에 대응 — 포트폴리오 서사 강화 | LOW | dealer/support/material enum |
| 융화재료(고변동 강화재) 포함 | PROJECT.md 도메인 지식("융화재료가 이벤트 시점 최고변동")을 데이터로 실증 | MEDIUM | event-impact 화면과 결합 시 서사 완성 |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| 실서비스급 아이템 검색/관리 UI | "더 많은 품목을 자유롭게" | 범위 폭증·수집 부하·레이트리밋 압박, 마일스톤 불변 제약에 명시 제외 | 큐레이션 watchlist 고정 |
| 딜러/서포터/재료 필터 컨트롤 | "필터 있으면 편함" | 12~20개 규모엔 과함 — 배지 구분으로 충분 | 그룹 배지(v2에서 필터 재평가) |
| 프론트에서 실시간 아이콘 fetch | "항상 최신 아이콘" | API 키 노출·CORS·레이트리밋 — 불변 제약 위반 | 백엔드 DTO `iconUrl` |
| 전체 각인서 일괄 등록 | "완전성" | 노이즈·수집부하, 포트폴리오 안목 희석 | 핵심 공용만 큐레이션 |

## Feature Dependencies

```
[Phase 0: API 스파이크 — item id/iconUrl/category 실측 + 모델 잠금]
    └──requires──> [백엔드 enrichment 컬럼 + DTO]
                       └──requires──> [seed/watchlist 확장(스파이크 상수)]
                                          └──requires──> [프론트 아이콘 렌더 + fallback]

[프론트 아이콘] ──enhances──> [Dashboard / Timeline / Event Impact 3화면]
[그룹 배지] ──depends──> [role_group/item_group enrichment]
```

### Dependency Notes

- **enrichment 컬럼이 스파이크를 요구:** iconUrl·item id가 실측 전엔 미확정 → 스파이크가 하드 게이트(v1.0 Task 0 동형).
- **seed 확장이 enrichment를 요구:** seed가 새 품목·아이콘을 심으려면 컬럼·DTO가 먼저 존재.
- **프론트가 DTO를 요구:** 프론트는 백엔드 변경 0 원칙대로 `iconUrl` DTO만 소비.

## MVP Definition

### Launch With (v1.2)

- [ ] **API 스파이크 + findings 문서** — iconUrl/item id/category 실측, fallback 전략 확정 (게이트)
- [ ] **enrichment 컬럼 + DTO** — icon_url/item_group/role_group, 4개 read 응답에 노출
- [ ] **seed/watchlist 확장** — 융화재료 + 큐레이션 각인서(12~20개), 아이콘 베이크
- [ ] **3화면 아이콘 + fallback** — Dashboard/Timeline/Event Impact + 셀렉터
- [ ] **docs** — 데이터 출처·실측 결과·fallback 전략 기록

### Add After Validation (v1.x)

- [ ] 그룹 필터 컨트롤 — 품목 수가 늘어 배지만으로 부족해질 때
- [ ] 등급 색상 정교화 — 더 많은 등급군이 들어올 때

### Future Consideration (v2+)

- [ ] 실서비스급 아이템 검색/관리 UI — 명시 제외
- [ ] 경매장/보석 소스 확장(SRC-V2) — 별도 마일스톤

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| API 스파이크/모델 잠금 | HIGH | LOW | P1 |
| enrichment 컬럼 + DTO | HIGH | MEDIUM | P1 |
| seed/watchlist 확장 | HIGH | MEDIUM | P1 |
| 3화면 아이콘 + fallback | HIGH | MEDIUM | P1 |
| 그룹 배지 | MEDIUM | LOW | P1 |
| 그룹 필터 | LOW | MEDIUM | P3 |

**Priority key:** P1=v1.2 필수 / P2=가능하면 / P3=v2 이후

## Competitor Feature Analysis

| Feature | LOALAB/유사 시세 사이트 | kloa/loatool | Our Approach |
|---------|------------------------|--------------|--------------|
| 품목 아이콘 | 공식 CDN 아이콘 핫링크 | 동일 | 백엔드 DTO 경유 + fallback(핫링크 깨짐 방어) |
| 품목 범위 | 전체 거래소 덤프 | 전체 | **큐레이션**(포트폴리오 안목 서사) |
| 그룹/역할 구분 | 카테고리 트리 | 카테고리 | dealer/support/material 역할 그룹(금융 섹터 비유) |

## Sources

- https://lo4.app/markets/inscriptions (LOALAB 유물 각인서 시세) — 큐레이션·아이콘 관행 참고
- https://kloa.gg / https://loatool.taeu.kr — 아이콘 표시 관행
- 사용자 브리프(v1.2) — 기능 범위·후보 품목 목록
- PROJECT.md Context — 융화재료 도메인 지식

---
*Feature research for: item visual/data enrichment*
*Researched: 2026-06-29*
