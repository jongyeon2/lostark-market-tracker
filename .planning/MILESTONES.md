# Milestones

## v1.0 MVP (Shipped: 2026-06-25)

**Phases completed:** 6 phases, 15 plans, 13 tasks

**Key accomplishments:**

- docker-compose Postgres 16 + Redis 7에 연결되어 부팅되는 Gradle/Java 21/Spring Boot 3.4.1 앱, 공유 Testcontainers 베이스와 실제 컨테이너 위에서 그린인 스모크 통합 테스트.
- 부팅 시 검증되는 JPA 엔티티가 딸린 Flyway 소유 4테이블 스키마, 그리고 실제 HTTP /api/items 라운드트립과 UNIQUE 멱등성·TIMESTAMPTZ UTC 동작을 증명하는 Testcontainers IT.
- 실제 markets API를 한 번 검증: avg_price/trade_count는 일단위 전용(상세 Stats[]), 안정적 Id가 매칭 규칙을 확정, 레이트리밋은 100/min — 모델 잠금 비준, 종료 게이트 PASS.
- 자체 구현 Lua 원자적 Redis 토큰버킷(재시작 복원 가능, fail-closed)과, 모든 HTTP 결과를 구분된 타입드 예외(Auth/RateLimited+RetryAfter/Transient/NonRetryable)로 분류하는 제품화된 LostarkApiClient.
- 활성 품목마다 @Async 가격 fetch를 팬아웃하고 호출당/전체 타임아웃 하에 join하며, 성공당 틱 정규화 collected_at을 공유하는 멱등 스냅샷 하나를 쓰고, 카운트 기반 status로 collection_run을 기록하는 @Scheduled fixedDelay 틱.
- 팬아웃에 연결된 수동 바운드 재시도(max-3, Retry-After 우선 백오프), 신규 호출을 중단하고 run을 AUTH_ERROR로 마킹하는 fatal-auth 수렴, PARTIAL_SUCCESS로의 품목별 실패 격리, 그리고 Flyway V2 summary_message 컬럼 — 모든 마커는 시크릿 프리.
- `GET /api/items/{id}/latest`를 위한 수동 Redis 캐시-어사이드(캐시 히트 시 DB 0회 조회)와 Phase 2 수집기에 연결한 evict-on-write 무효화, 그리고 커스텀 `@RestControllerAdvice` 에러 계약의 404 절반 + active 전용 `/api/items`.
- `GET /api/items/{id}/prices?from=&to=`가 윈도우 내 스냅샷과 겹치는 이벤트를 UTC 두 배열로 반환하며, Phase 5가 재사용할 재사용 가능한 4A `WindowQueryService`가 양끝 포함 경계 시맨틱과 함께 테스트됨.
- `/prices`가 큰 범위를 PostgreSQL `date_trunc` avg 버킷으로 자동 다운샘플하고, 전체 입력 검증 계약(400/404/200-빈)이 커스텀 advice를 확장하며, `GET /api/health/collection`이 시크릿 노출 없이 최신 수집 런을 드러낸다.
- Admin CRUD for game events (full-replace PUT with mutable occurred_at) and watchlist items (soft-delete + reactivate/409 branch) on the locked model, with a V3 UNIQUE constraint and ResourceNotFound/Duplicate/Validation handlers extending the shared error contract — all admin ITs pre-sending X-Admin-Secret for a zero-retrofit gate.
- Hand-rolled X-Admin-Secret OncePerRequestFilter + SecurityFilterChain gating /api/admin/
- `GET /api/items/{id}/event-impact?window=N` — 게임 이벤트별 anchor 단일 스냅샷 delta `change_rate = post/pre − 1`(min_price)을 단일 배치 읽기로 계산해 occurred_at desc 로 반환하는 v1 상관 엔드포인트.
- `EventImpactService` 에 30분 staleness + sufficiency 게이트를 in-place 로 얹어, 양쪽 anchor 가 존재하고 둘 다 fresh 일 때만 `change_rate` 를 산출하고 그 외에는 sparse(null)·stale(시간 보고)을 구분해 `insufficient_data` 로 반환.
- GitHub Actions CI running the full `./gradlew build` Testcontainers suite on every push/PR, plus a `seed` Spring profile that fills 8 days of synthetic 10-min snapshots + 2 grid-anchored demo events (no API key) — proven end-to-end by SyntheticDemoDataIT.
- README.md rewritten into the single reproducible reviewer entry point — CI badge, mermaid architecture diagram, honest Redis/rate-limit/@Async trade-offs, seed-mode setup, ≥3 curl + 5 sample JSON blocks against the real API, and a clone→seed→curl event-impact reproduction.

---

## v1.1 Frontend Demo Dashboard (Shipped: 2026-06-29)

**Phases completed:** 5 phases (7–11), 19 plans

**Delivered:** v1.0의 read API 5종을 백엔드 0줄 변경(Vite 프록시 dev 동일 출처)으로 소비하는 React + TypeScript + Tailwind + Recharts 브라우저 대시보드 3화면을 seed 프로파일 기준 빈 화면 없이 완성 — README의 curl 데모를 클릭 가능한 데모 표면으로 전환.

**Key accomplishments:**

- **Phase 7 (Frontend Foundation):** Vite 6 + React 19 + TS + Tailwind v4 `frontend/` 앱 + `/api`→:8080 dev 프록시(백엔드 무변경) + shadcn/ui(slate/new-york), zod-as-single-source DTO 스키마 5종 + `.parse`-at-boundary 타입드 API 클라이언트 + TanStack Query fetch-on-mount + native-Intl KST 포맷터, React-Router 앱 셸(3탭 내비)·공용 Loading/Empty/Error AsyncBoundary.
- **Phase 8 (Dashboard):** health status 4등급(정상/일부 실패/전체 실패/수집 대기)을 시맨틱 색+한국어 라벨+lucide 배지로 표현하는 StatusBadge, `/api/health/collection`을 전폭 proof-of-life `HealthCard`로, 활성 품목 반응형 카드 그리드 + 품목당 `useLatestPrice` 최신가 요약.
- **Phase 9 (Item Timeline):** 프로젝트 헤드라인 시각화 — UTC 위치/KST 라벨 min_price 라인 위에 eventType별 점선 ReferenceLine 마커를 겹치고 다운샘플 배지·버킷선, URL searchParams 단일 출처 useTimelineParams(기본 30일)·7/30/90일 프리셋, 공유 ItemSelect + 진입 시 자동 선택 + 400/404/200-empty 정직 분기.
- **Phase 10 (Event Impact):** 손수 작성한 shadcn table·`?item=&window=` URL 상태 + ok/insufficient_data를 occurred_at-desc 한 목록에 섞고 null changeRate를 희소/stale 이유+anchor 근거로 치환, 상승/하락 색 구분, 항상 노출되는 비해제형 "상관 ≠ 인과" 배너(과대해석 방지), window 범위 미바운딩으로 백엔드 400 시연 유지.
- **Phase 11 (Demo Surface + Docs):** 3화면 로딩·빈·에러 일관성과 데스크톱 우선 반응형 순회 점검, 스크린샷 캡처 프로토콜 + frontend/README 단일 출처 재현 문서(seed 백엔드 → `npm run dev` → 3화면) + 루트 README 프론트 데모 포인터(기존 curl 섹션 보존).
- **Gap closure 11-04 (UAT Test 3):** `SyntheticDemoData.seed()`가 timeline/impact와 일관된 합성 SUCCESS `collection_run`(시도=성공=활성품목수·실패=0·마커 없음·startedAt=gridNow)을 멱등 적재 — seed 데모 헬스 카드가 영속 볼륨의 과거 키리스 AUTH_ERROR run 대신 '12/12 SUCCESS'를 보임. 읽기 경로·DTO·프론트·스키마 0줄 변경, Testcontainers 회귀 단언으로 고정.

**Deferred to v2:** DEMO-03(Spring 정적 서빙 단일 출처 패키징, 선택/stretch였고 슬립), 관리자 쓰기 UI·실시간 갱신·다크모드/i18n·실배포+프론트 CI·카테고리 베이스라인 시각화(FE-V2-01..05).

---

## v1.2 Item Visual/Data Enrichment (Shipped: 2026-06-30)

**Phases completed:** 3 phases (12–14), 6 plans

**Delivered:** v1.1 데모 대시보드 3화면과 셀렉터에 품목 아이콘·역할 그룹 메타데이터를 입히고, 융화재료 + 큐레이션 딜러/서포터 유물 각인서(최종 15개)를 watchlist·seed에 추가 — **API 키 없이 seed만으로** 시각적으로 풍부한 데모를 재현. enrichment는 수집·캐시·event-impact를 0줄도 건드리지 않는 read-path additive 한 겹.

**Key accomplishments:**

- **Phase 12 (API Spike + Data Lock):** 본인 JWT로 `/markets/options`·`/markets/items` 1회 실측 → 각인서 CategoryCode=40000(leaf)·융화재료=50010, 아이콘 필드 `Icon`·CDN `efui_iconatlas/use/` 확정. 큐레이션 **15개 잠금**(딜러 9 + 서포터 2 + 융화재료 4). 유물 각인서 11종 동일 글리프(`use_9_25.png`)→라벨·배지 식별, 융화재료 4종 구별. findings·커밋 실 키/계정/가격 0건(12-SPIKE-FINDINGS.md 단일 출처).
- **Phase 13 (Backend Enrichment + Seed):** Flyway `V4__add_item_enrichment.sql` nullable icon_url/item_group/role_group(V1–V3 불변, validate 통과) + 4개 read DTO 평면 패스스루(roleGroup ∈ {DEALER,SUPPORT,MATERIAL}|null) + WatchlistSeeder/SyntheticDemoData에 큐레이션 15개·아이콘 상수 베이크(키 없는 재현). 수집/캐시/EventImpactService **0줄**, 전체 Testcontainers 그린.
- **Phase 14 (Frontend Icons + Fallback + Docs):** 공용 `<ItemIcon>`(고정 슬롯 + null/onError 역할색 글리프 fallback, 시프트 0, lucide-react만)·`<RoleBadge>`(solid 한글 배지)·roleGroup(sortByRole) + enrichment 4 zod 스키마. 4화면(Dashboard 카드·셀렉터 역할군 헤더·Timeline 최신가·Impact 정체성) 아이콘·역할 배지 일관 적용, 큐레이션 15개 누락 0. 역할 3색=semantic 색군(rose/emerald/amber). 루트/frontend README 출처·실측·fallback·자산 섹터 서사. 백엔드 src/ 0줄, 신규 npm 의존 0.
- **검증:** Phase 14 UAT **8/8 PASS**(Playwright seed 실측) + 보안 검토 **9위협 closed**(threats_open 0). D-11 스크린샷 3화면 교체.
- **요구사항:** 21/21 충족(SPIKE 5 + ITEM 4 + SEED 4 + ICON 8).

**Quick tasks (7건, 마일스톤 중):** 260630-0rh/gct/g0i/16d(레이아웃 정비) · 260630-em5(dev 첫 수집 initial-delay) · 260630-h16(타임라인 일별 평균 집계) · 260630-lu5(README JSCODE 스타일 재구성).

**Deferred to v2:** FILTER-V2-01(그룹 필터 토글) · GRADE-V2-01(등급별 색상·정렬) · 운명 융화재료·만개(실재/거래량 부족) · OPS-V2 · DEPLOY-V2(라이브 배포) · IMPACT-V2 · SRC-V2 · CFG-V2.

---
