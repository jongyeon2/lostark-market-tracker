# Requirements: 로스트아크 거래소 시세 수집·분석 파이프라인 — v1.2 Item Visual/Data Enrichment

**Defined:** 2026-06-29
**Core Value:** 레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다 (enrichment는 이를 흔들지 않는 read-path additive 한 겹)

## v1 Requirements

v1.2 마일스톤 범위. 각 항목은 roadmap phase에 매핑된다(Traceability).

### SPIKE — API 실측 + 데이터 잠금 (게이트)

- [ ] **SPIKE-01**: `/markets/options` 응답으로 유물 각인서(CategoryCode=40000 / ItemGrade="유물")와 융화재료의 검색 조건(CategoryCode)을 실측 확인한다.
- [ ] **SPIKE-02**: `/markets/items` 응답의 품목별 아이콘 URL 제공 여부·형태(필드명·CDN 도메인)를 실측해 기록한다.
- [ ] **SPIKE-03**: 융화재료 후보(상급/최상급 오레하 + 아비도스/운명 계열)의 거래 가능 여부·external_item_id·display_name·category·iconUrl을 실측해 포함 목록을 확정한다.
- [ ] **SPIKE-04**: 딜러/서포터 유물 각인서 후보를 실측해 최종 큐레이션 12~20개(item id·iconUrl·각인서 아이콘 구별 여부)를 확정한다.
- [ ] **SPIKE-05**: 아이콘 URL 부재·로딩 실패 시 fallback 전략을 확정하고, findings에 데이터 출처·실측 결과·fallback을 기록하되 실 API 키·계정 식별자·가격 원문은 배제한다.

### ITEM — 백엔드 enrichment 컬럼 + DTO

- [ ] **ITEM-01**: `tracked_item`에 icon_url/item_group/role_group enrichment 컬럼을 Flyway V4(nullable)로 추가한다(V1–V3 마이그레이션 불변).
- [ ] **ITEM-02**: TrackedItem 엔티티가 신규 컬럼을 매핑하고 `ddl-auto=validate`가 통과한다.
- [ ] **ITEM-03**: 4개 read 응답 DTO(item list / latest / timeline / event-impact)에 iconUrl·itemGroup·roleGroup을 노출한다.
- [ ] **ITEM-04**: 수집 스케줄러·Redis 캐시·EventImpactService 계산 로직은 0줄도 변경하지 않는다(회귀 테스트로 단언).

### SEED — seed/watchlist 확장

- [ ] **SEED-01**: WatchlistSeeder가 큐레이션 확정 품목(융화재료 + 딜러/서포터 각인서, 12~20개)을 enrichment(icon_url/item_group/role_group)와 함께 등록한다.
- [ ] **SEED-02**: SyntheticDemoData가 신규 품목의 합성 스냅샷·이벤트를 API 키 없이 재현한다.
- [ ] **SEED-03**: seed 프로파일만으로(키 없이) 신규 품목과 아이콘이 채워진다(브라우저 확인 기준).
- [ ] **SEED-04**: seed/watchlist 코드·문서에 실 API 키·민감정보를 남기지 않는다.

### ICON — 프론트 아이콘 + fallback + docs

- [ ] **ICON-01**: 공용 `<ItemIcon>` 컴포넌트가 iconUrl을 렌더하고 onError 시 역할색 글리프/이니셜 fallback으로 대체한다(고정 슬롯 → 레이아웃 시프트 없음, 아이콘 부재/CDN 차단에도 UI 무파손).
- [ ] **ICON-02**: Dashboard 품목 카드에 아이콘을 표시한다.
- [ ] **ICON-03**: 품목 셀렉터(ItemSelect) 옵션에 아이콘을 표시한다.
- [ ] **ICON-04**: Item Timeline 최신가 카드에 아이콘을 표시한다.
- [ ] **ICON-05**: Event Impact 품목 카드에 아이콘을 표시한다.
- [ ] **ICON-06**: 융화재료/딜러각인/서포터각인 역할 그룹을 배지로 시각 구분한다(필터 컨트롤은 v2).
- [ ] **ICON-07**: 추가 품목(융화재료·큐레이션 각인서)이 Dashboard/Timeline/Event Impact에서 선택 가능하다.
- [ ] **ICON-08**: frontend/README·루트 README에 데이터 출처·API 실측 결과·fallback 전략을 기록한다.

## v2 Requirements

향후 릴리스 보류. 추적하되 현 roadmap 미포함.

### 프론트 enrichment 고도화

- **FILTER-V2-01**: 딜러/서포터/융화재료 그룹 필터 토글 UI (품목 수 증가 시)
- **GRADE-V2-01**: 등급별 색상·정렬 정교화 (등급군 확장 시)

### 기존 v2 백로그 (PROJECT.md 참조, v1.2 범위 밖)

- **OPS-V2** 관측성(Micrometer) · **DEPLOY-V2** 라이브 배포 · **IMPACT-V2** event-impact 고도화 · **SRC-V2** 경매장/보석 소스 확장 · **CFG-V2** 매직넘버 외부화 · **FE-V2-01..04** 관리자 쓰기 UI·실시간 갱신·다크모드/i18n·정적 서빙(DEMO-03)

## Out of Scope

명시적 제외. 스코프 크리프 방지.

| Feature | Reason |
|---------|--------|
| 실서비스급 아이템 검색/관리 UI | 마일스톤 불변 제약 명시 제외 — 큐레이션 watchlist 고정으로 충분 |
| 그룹 필터 컨트롤 | 12~20개 규모엔 과함; 역할 배지로 대체(v2에서 재평가) |
| 프론트에서 Lostark Open API 직접 호출 | 키 노출·CORS·레이트리밋 — 불변 제약 위반. 백엔드 DTO만 소비 |
| 수집/캐시/event-impact 계산 로직 변경 | Core Value(수집 신뢰성) 보호 — enrichment는 read-path additive only |
| 프론트 정적 에셋 번들 아이콘 | "API Icon URL→DB" 채택(사용자 결정); 에셋 수급·번들 부담 회피 |
| 경매장(AUCTIONS)/보석 소스 확장 | SRC-V2 별도 마일스톤 |
| 다크모드 · i18n · 실시간 갱신 | FE-V2 — v1.2 시각 enrichment 범위 밖 |
| 런타임 아이콘 자동 갱신 | spike-then-lock 상수 베이크로 충분(데모); 실시간 갱신은 수집부하·레이트리밋 압박 |

## Traceability

phase 매핑은 roadmap 생성 시 채움.

| Requirement | Phase | Status |
|-------------|-------|--------|
| SPIKE-01 | TBD | Pending |
| SPIKE-02 | TBD | Pending |
| SPIKE-03 | TBD | Pending |
| SPIKE-04 | TBD | Pending |
| SPIKE-05 | TBD | Pending |
| ITEM-01 | TBD | Pending |
| ITEM-02 | TBD | Pending |
| ITEM-03 | TBD | Pending |
| ITEM-04 | TBD | Pending |
| SEED-01 | TBD | Pending |
| SEED-02 | TBD | Pending |
| SEED-03 | TBD | Pending |
| SEED-04 | TBD | Pending |
| ICON-01 | TBD | Pending |
| ICON-02 | TBD | Pending |
| ICON-03 | TBD | Pending |
| ICON-04 | TBD | Pending |
| ICON-05 | TBD | Pending |
| ICON-06 | TBD | Pending |
| ICON-07 | TBD | Pending |
| ICON-08 | TBD | Pending |

**Coverage:**
- v1 requirements: 21 total
- Mapped to phases: 0 (roadmap 대기)
- Unmapped: 21 ⚠️ (roadmap에서 해소)

---
*Requirements defined: 2026-06-29*
*Last updated: 2026-06-29 after initial v1.2 definition*
