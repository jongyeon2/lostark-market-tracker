# Architecture Research

**Domain:** 기존 read 경로에 품목 enrichment를 additive로 관통시키기
**Researched:** 2026-06-29
**Confidence:** HIGH (기존 아키텍처 위 얇은 한 겹)

## Standard Architecture

핵심 원칙: enrichment는 **수집·캐시·event-impact 계산을 건드리지 않는 read-path additive 한 겹**이다. 데이터는 `tracked_item`의 신규 컬럼에서 시작해 DTO를 타고 프론트 `ItemIcon`까지 단방향으로 흐른다.

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                       Frontend (React 19)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │Dashboard │  │ Timeline │  │EventImpact│ │ItemSelect│     │
│  │  카드     │  │ 최신가카드 │  │ 품목카드   │ │ 옵션      │     │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘     │
│       └─────── 공용 <ItemIcon iconUrl roleGroup/> ──────┘    │
│                  (zod DTO의 iconUrl 소비, onError fallback)   │
├─────────────────────────────────────────────────────────────┤
│                     Backend (Spring Boot)                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Read API DTO (item list / latest / timeline /        │    │
│  │  event-impact) ← iconUrl·itemGroup·roleGroup 추가     │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  TrackedItem 엔티티 ← icon_url/item_group/role_group  │    │
│  └─────────────────────────────────────────────────────┘    │
│  [수집 스케줄러·Redis 캐시·EventImpactService = 무변경]       │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL: tracked_item (+V4 nullable enrichment 컬럼)     │
│  Seed: SyntheticDemoData / WatchlistSeeder (스파이크 상수)    │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| V4 마이그레이션 | enrichment 컬럼 추가(nullable) | `ALTER TABLE tracked_item ADD COLUMN icon_url text, item_group text, role_group text` |
| TrackedItem 엔티티 | 신규 컬럼 매핑 | 필드 3개 추가, getter만 |
| Read DTO | iconUrl/itemGroup/roleGroup 노출 | 기존 DTO 매퍼에 필드 추가(계산 로직 무변경) |
| WatchlistSeeder | 큐레이션 품목 + enrichment 등록 | 스파이크 캡처 상수 테이블 |
| SyntheticDemoData | 신규 품목 스냅샷·아이콘 재현 | 기존 합성 로직에 신규 item 포함 |
| `<ItemIcon>` (프론트) | 아이콘 렌더 + fallback | `<img onError>` → 역할색 lucide 글리프 |

## Recommended Project Structure

```
src/main/resources/db/migration/
└── V4__add_item_enrichment.sql      # icon_url/item_group/role_group (nullable)

src/main/java/.../item/
├── TrackedItem.java                 # +3 필드
├── dto/ItemResponse.java 외         # +iconUrl/itemGroup/roleGroup
└── seed/WatchlistSeeder.java        # 큐레이션 상수(스파이크 출처)

frontend/src/
├── components/ItemIcon.tsx          # 신규 공용 컴포넌트(아이콘+fallback)
├── lib/schemas.ts                   # zod DTO에 옵셔널 필드 추가
└── (Dashboard/Timeline/EventImpact)에서 <ItemIcon> 소비
```

### Structure Rationale

- **V4만 추가, V1–V3 불변:** Flyway 체크섬 안정.
- **`<ItemIcon>` 단일 컴포넌트:** 4곳(3화면+셀렉터)이 같은 fallback 규칙을 공유 → 일관성·단일 수정점.
- **seed의 상수 테이블:** 스파이크 findings를 코드 상수로 한 번만 잠금 → 키 없이 재현.

## Architectural Patterns

### Pattern 1: Spike-then-Lock (v1.0 Task 0 재사용)

**What:** 실 API를 한 번 호출해 item id·iconUrl·category를 확정하고, 그 값을 코드 상수로 잠근 뒤 런타임은 상수로만 동작.
**When to use:** 런타임이 키 없이 재현돼야 하지만 데이터는 실 API에서 와야 할 때.
**Trade-offs:** 아이콘/가격이 "스냅샷 시점" 고정(데모엔 OK, 실서비스엔 부적합 — 명시 제외 범위).

### Pattern 2: Additive Read-Path Enrichment

**What:** 기존 계산·수집 경로는 손대지 않고, 응답 DTO에 옵셔널 필드만 덧댐.
**When to use:** Core Value(수집 신뢰성)를 흔들면 안 될 때.
**Trade-offs:** 신규 컬럼이 nullable이라 과거 행/미배포 프론트와도 호환(점진 배포 안전).

### Pattern 3: Graceful Image Fallback

**What:** `<img onError>`로 깨진 아이콘을 역할색 글리프/이니셜로 치환, 고정 크기 슬롯으로 레이아웃 시프트 차단.
**When to use:** 외부 CDN·신규 품목 등 아이콘 부재가 상시 가능할 때.
**Trade-offs:** 글리프가 실제 아이콘만큼 식별적이진 않음(허용 가능).

## Data Flow

### Request Flow

```
[사용자: 품목 선택/화면 진입]
    ↓
[React 컴포넌트] → [TanStack Query] → [/api/items... read 엔드포인트]
    ↓                                        ↓
[<ItemIcon> 렌더] ← [zod .parse(iconUrl?)] ← [DTO 매퍼 + TrackedItem.icon_url]
    ↓
[onError 시 fallback 글리프]
```

### Key Data Flows

1. **Enrichment 적재:** 스파이크 findings → WatchlistSeeder 상수 → `tracked_item.icon_url` → DTO → 프론트.
2. **Fallback:** iconUrl null 또는 img 로드 실패 → 역할색 글리프(데이터 흐름 중단 없이 표현만 대체).

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 12~20 품목(현 범위) | 단일 watchlist 상수로 충분, 인덱스 변경 불필요 |
| 수백 품목(가정) | 그룹 필터·페이지네이션 필요(v2) — 현 범위 밖 |

## Anti-Patterns

### Anti-Pattern 1: 수집/캐시 경로에 아이콘 fetch 끼워넣기

**What people do:** 스케줄러 틱에서 가격과 함께 아이콘을 갱신.
**Why it's wrong:** Core Value(수집 신뢰성)를 흔들고 레이트리밋을 더 압박, 불변 제약 위반.
**Do this instead:** 아이콘은 enrichment 상수/스파이크 1회 캡처. 수집은 가격만.

### Anti-Pattern 2: 프론트에서 Lostark API 직접 호출해 아이콘 획득

**What people do:** 브라우저에서 `/markets/items` 호출.
**Why it's wrong:** 키 노출·CORS·레이트리밋 — 불변 제약 정면 위반.
**Do this instead:** 백엔드 DTO `iconUrl`만 소비.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Lostark `/markets/items` | Phase 0 스파이크 1회(백엔드/로컬, 본인 키) | item id·iconUrl·grade·category 캡처 후 상수 잠금 |
| Lostark CDN(`cdn-lostark...`) | 프론트 `<img src>` 핫링크 | 핫링크 차단/장애 대비 onError fallback 필수 |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| TrackedItem ↔ Read DTO | 매퍼 필드 추가 | 계산 로직 무변경, 필드 패스스루만 |
| Backend DTO ↔ Frontend | zod 옵셔널 필드 | 점진 배포 안전(미배포 시 undefined) |

## Sources

- 기존 코드베이스 구조(`.planning/phases/03-read-api-cache`, `07-frontend-foundation`) — DTO·zod 경계 패턴(HIGH)
- v1.0 Task 0 findings — spike-then-lock 선례(HIGH)
- 사용자 브리프 — 무변경 제약·enrichment 범위

---
*Architecture research for: additive item enrichment*
*Researched: 2026-06-29*
