# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** — Phases 7–11 (shipped 2026-06-29) — [archive](milestones/v1.1-ROADMAP.md)
- ✅ **v1.2 Item Visual/Data Enrichment** — Phases 12–14 (shipped 2026-06-30) — [archive](milestones/v1.2-ROADMAP.md)
- ✅ **v1.3 관리자 콘솔 + 실데이터 라이브 배포** — Phases 15–18 (+17.1~17.4 삽입, 라이브 배포 완료 2026-07-13)
- ✅ **v1.4 CI/CD 자동화** — Phase 19 (shipped 2026-07-14 — 첫 실배포 run #30 `916bd3a`)
- ✅ **v1.5 시세 범위 확장 + UX** — Phases 20–23 완료 (2026-07-14). Phase 24(경매장 보석)=v1.6 후보
- ✅ **v1.6 이벤트 영향 신뢰성** — Phase 25 완료 (2026-07-15) — 수집 이전 이벤트가 "데이터 부족"이던 문제 해소(백필 일평균 폴백)
- ✅ **v1.7 경매장 보석 현재가** — Phase 24·26 완료 (2026-07-15) — v2로 미뤄둔 `SRC-V2-01`(경매장/보석 소스 확장) 실현. **티어4 8~10레벨 보석만**, 별도 `/gems` 페이지
- 🚧 **v1.8 보석 시세 기록 시작** — Phase 27 (착수 2026-07-15) — v1.7이 세운 **"보석 시계열 미기록" 경계를 근거와 함께 넓힌다**. 1시간 폴러로 **기록만 시작**(일별 표·이벤트 영향 표는 데이터 축적 후 별도 phase) + 화면 정정(역할 라벨 제거·레벨 중심 표)

## Phases

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

<details>
<summary>✅ v1.2 Item Visual/Data Enrichment (Phases 12–14) — SHIPPED 2026-06-30</summary>

- [x] **Phase 12: API Spike + Data Lock (게이트)** — 큐레이션 15개·iconUrl CDN·fallback 잠금 (2026-06-29)
- [x] **Phase 13: Backend Enrichment + Seed Expansion** — V4 nullable 컬럼 + 4 DTO 패스스루 + seed 확장 (2026-06-29)
- [x] **Phase 14: Frontend Icons + Fallback + Docs** — 공용 `<ItemIcon>` + 3화면·셀렉터 아이콘·역할 배지 + docs (2026-06-30, UAT 8/8 + 보안 통과)

전체 상세: [milestones/v1.2-ROADMAP.md](milestones/v1.2-ROADMAP.md)

</details>

### 🚧 v1.3 관리자 콘솔 + 실데이터 라이브 배포 (Phases 15–18, +17.1 삽입) — IN PROGRESS

**Goal:** 읽기전용 seed 데모를 → 실데이터로 수집·서빙되고, 관리자가 이벤트·워치리스트를 직접 관리하며, 무료로 라이브 배포된 데모로 승격(배포 직전 보안 검증 게이트 통과). 순서: 관리자 콘솔(15) → 대시보드 카드 개선(16) → 실데이터 전환(17) → 무료 배포+보안(18, 마지막). 15개 요구사항 100% 매핑.

**불변 제약(상시):** 프론트에서 Lostark API 직접 호출 금지 · 실키 코드/문서/로그/커밋 미기재 · 수집/캐시/event-impact는 Core Value(수집 신뢰성) 가드 하에서만.

#### Phase 15: 관리자 콘솔 UI

**Goal**: 기존 백엔드 `/api/admin/*`(X-Admin-Secret 게이트, 백엔드 인증 무변경)을 소비하는 프론트 관리자 콘솔 — 시크릿 입력 로그인 뒤에서 게임 이벤트·워치리스트를 관리하고 수집 상태를 모니터링한다.
**Depends on**: Nothing new (백엔드 admin 엔드포인트는 v1.0에 존재)
**Requirements**: ADMINUI-01, ADMINUI-02, ADMINUI-03, ADMINUI-04, ADMINUI-05, ADMINUI-06
**Success criteria**:
1. 관리자가 시크릿 로그인 후 관리자 작업이 인증되고 새로고침에도 세션이 유지된다
2. 잘못된 시크릿은 거부되고(401) 명확한 오류를 보여준다
3. 관리자가 게임 이벤트를 등록·수정·삭제하고 목록에서 반영을 확인한다
4. 관리자가 워치리스트 품목을 추가·비활성·재활성한다
5. 관리자가 수집 실행 이력을 확인하고, 미로그인 사용자에겐 쓰기 UI가 노출되지 않으며 로그아웃 시 세션이 종료된다

#### Phase 16: 대시보드 카드 개선

**Goal**: 대시보드 품목 카드에서 물품 고유 번호를 제거하고(아이콘·이름·골드·수집시각만), 카드 클릭 시 해당 품목이 선택된 상태로 품목 타임라인으로 딥링크 이동한다.
**Depends on**: Nothing (독립 프론트 변경) — 순서상 15 뒤
**Requirements**: CARD-01, CARD-02
**Success criteria**:
1. 대시보드 카드에 물품 고유 번호가 없고 아이콘·품목명·골드 가격·수집 시각만 표시된다
2. 카드를 클릭하면 해당 품목이 선택된 상태로 타임라인 화면으로 이동한다
3. v1.2 enrichment(아이콘·역할 배지)와 기존 카드 정보는 회귀 없이 유지된다

#### Phase 17: 실데이터 전환

**Goal**: 데모를 seed 합성 데이터에서 실수집(collection_run 기반) 데이터로 전환한다. 실 API 키로 수집기를 구동하고, seed 프로파일은 로컬/테스트 전용으로 격리하며, 초기 미축적 상태를 빈 화면 없이 정직히 처리한다.
**Depends on**: Nothing structural (수집기는 v1.0에 존재)
**Requirements**: REALDATA-01, REALDATA-02, REALDATA-03
**Success criteria**:
1. 실 API 키로 수집기가 구동되어 실데이터가 축적된다(collection_run SUCCESS)
2. 3화면이 실수집 데이터를 표시하고 seed 프로파일은 로컬/테스트 전용으로만 남는다
3. 데이터 미축적 초기 상태에서도 빈 화면 없이 '수집 중/데이터 없음'을 정직히 표시한다
4. 수집/캐시/event-impact 핵심 경로가 회귀 없이 그린(Core Value 가드)

#### Phase 17.1: 데모 최종 폴리시 — 큐레이션 갱신 + 대시보드 재구성 (INSERTED)

**Goal**: 라이브 배포 전 데모 품질을 다듬는다 — 대시보드 정보 배치를 클라이언트/관리자 관점으로 정리하고(수집 헬스 카드 관리자 이관), 각인·재료 큐레이션을 현행 로아 티어4 메타로 갱신하며, 대시보드를 카테고리별 세로 섹션으로 재구성하고, event-impact 주의 문구를 이해하기 쉽게 개선한다. 큐레이션 변경은 Phase 12 spike-then-lock 패턴으로 API 데이터를 재검증하며 수집/캐시/event-impact 핵심 경로는 0줄(Core Value 가드).
**Depends on**: Phase 15(관리자 콘솔 — 헬스 카드 이관 대상), 16(대시보드 카드), 17(실데이터 — watchlist/seed 큐레이션 기반); Phase 12 spike-then-lock 패턴 재사용
**Requirements**: POLISH-01, POLISH-02, POLISH-03, POLISH-04, POLISH-05
**Success criteria**:
1. 대시보드(클라이언트 화면)에서 수집 헬스 카드가 제거되고, 수집 파이프라인 상태는 관리자 콘솔에서 'API 상태'로만 확인된다
2. 각인 큐레이션이 현행 유효각인(딜러 11 + 서포터 7)으로 갱신되고, 각 각인이 실 API Id/Icon으로 아이콘·라벨 표시된다
3. 재료 큐레이션이 티어4 기준으로 갱신된다 — 운명의 파괴석/수호석 결정 노출, 융화재료는 아비도스 계열만, 상급·최상급 오레하 융화재료 미노출
4. 대시보드가 바둑판 그리드에서 카테고리별 세로 섹션(각인 → 재료, 섹션 내 위→아래)으로 재구성된다
5. event-impact의 상관≠인과 주의 문구가 번역투 없이 이해하기 쉬운 주의사항 표현으로 개선된다
6. (Core Value 가드) 수집/캐시/event-impact 핵심 경로 회귀 없음 — 큐레이션 변경은 watchlist/seed 데이터 층에 한정
**Plans**: 4 plans

Plans:
- [x] 17.1-01-PLAN.md — 신규 큐레이션 10개 실 API 스파이크 (POLISH-02/03)
- [x] 17.1-02-PLAN.md — 큐레이션 22개 재잠금: WatchlistSeeder + roleGroup 라벨 (POLISH-02/03)
- [x] 17.1-03-PLAN.md — 대시보드 정보 재배치: 헬스카드 이관 + 세로 섹션 재구성 (POLISH-01/04)
- [x] 17.1-04-PLAN.md — event-impact 주의 문구 개선 (POLISH-05)

#### Phase 17.2: 대시보드 뉴스 패널 — 로아 이벤트·공지 (INSERTED)

**Goal**: 대시보드 물품 카드를 좁히며 생긴 우측 여백에 로스트아크 진행중 이벤트·공지사항을 표 형식으로 노출한다. 로아 공식 `/news/events`·`/news/notices` API를 저빈도 폴러(~6h) + Redis 캐시로 서빙(`GET /api/news`)하며, 수집/가격 캐시/event-impact 핵심 경로는 0줄(Core Value 가드) — 완전 독립 read-path. 쿠폰코드는 범위 밖(공식 API 부재 → 이후 관리자 수동 입력).
**Depends on**: Phase 15(관리자 콘솔 — 향후 쿠폰 수동 입력 확장 지점), 16·17.1(대시보드 카드·레이아웃), Phase 12 spike-then-lock 패턴(뉴스 응답 필드 실측); 로아 공식 API 키 재사용
**Requirements**: NEWS-01, NEWS-02, NEWS-03
**Success criteria**:
1. 로아 공식 `/news/events`·`/news/notices`에서 진행중 이벤트·공지를 저빈도 폴러로 수집해 Redis에 캐시하고 `GET /api/news`로 서빙한다 (프론트 직접 호출 금지·키 서버 env only)
2. 대시보드 우측 사이드바에 이벤트·공지가 표 형식으로 표시되고(모바일에서는 물품 아래 세로 스택), 항목 클릭 시 로아 공식 페이지가 새 탭으로 열린다
3. 뉴스 소싱이 실패하거나 아직 갱신 전이어도 대시보드가 빈 화면 없이 로딩/빈/에러를 정직히 표시하고 마지막 캐시를 유지한다
4. (Core Value 가드) 수집(`PriceCollector`)·가격 캐시·event-impact 로직 0줄 — 뉴스는 독립 패키지·독립 스케줄·독립 Redis 키
**설계 스펙**: `docs/superpowers/specs/2026-07-06-dashboard-news-panel-design.md`

#### Phase 17.3: 쿠폰 관리자 등록 — 대시보드 쿠폰 섹션 (INSERTED)

**Goal**: 관리자가 콘솔에서 로스트아크 쿠폰(코드·보상·만료일)을 등록·수정·삭제하면 PostgreSQL에 영속되고, 대시보드 뉴스 패널의 '쿠폰' 섹션에 미만료 쿠폰만 만료임박순(≤6)으로 표시되며(코드 원클릭 복사), 만료 쿠폰은 공개 목록에서 자동 제외된다. Phase 17.2(NEWS)에서 defer된 "관리자 수동 입력"을 실현한다. 뉴스(휘발성 Redis)와 달리 관리자 입력이라 PostgreSQL 영속. 수집/가격 캐시/event-impact 핵심 경로는 0줄(Core Value 가드) — 쿠폰은 독립 도메인.
**Depends on**: Phase 15(관리자 콘솔·admin CRUD 패턴 — `AdminEventController`/`EventSection` 재사용), Phase 17.2(뉴스 패널 — 쿠폰 섹션 배치 대상)
**Requirements**: COUPON-01, COUPON-02, COUPON-03
**Success criteria**:
1. 관리자가 쿠폰(코드·보상·만료일)을 등록·수정·삭제하고 PostgreSQL에 영속한다 (X-Admin-Secret 게이트, `/api/admin/coupons`)
2. 대시보드 뉴스 패널에 미만료 쿠폰이 만료임박순(≤6)으로 표시되고 코드를 원클릭 복사할 수 있다
3. 만료 쿠폰은 공개 `GET /api/coupons`에서 자동 제외되고, 없거나 로딩 실패해도 대시보드가 정직히 표시한다
4. (Core Value 가드) 수집·가격 캐시·event-impact 로직 0줄, 프론트 로아 직접 호출 0 — 쿠폰은 독립 도메인·PostgreSQL 영속
**스펙**: `.planning/phases/17.3-coupon-admin/17.3-SPEC.md`

#### Phase 17.4: 타임라인 gap 백필 — 일별 Stats (INSERTED)

**Goal**: 서버 off로 생긴 품목 타임라인의 수집 공백을, 로스트아크가 제공하는 **일별 평균가**로 별도 "일별 평균" 시리즈로 정직하게 백필해 어떤 기간을 봐도 시세가 연속으로 보이게 한다. 소스 2종 병행: (1) 매 수집 시 리스트 응답의 `YDayAvgPrice`(전일 평균, **각인서 포함 전 품목**, 추가 호출 0·무료)를 저장해 going-forward 일별 평균을 쌓고, (2) 상세 API `Stats[].AvgPrice`(최근 **14일**, 재료만 채워짐)로 재료의 과거 gap을 소급 채운다. `price_snapshot`(Core Value 가드)은 무변경, 별도 `item_daily_stats` 테이블에 additive 적재하며, 타임라인은 실측 min_price(실시간 최저 호가)와 백필 일평균(거래 평균가)을 시각적으로 구분한다(한 라인 혼합 금지). 14일 초과 다일 gap은 API에도 없어 복구 불가 → Phase 18 상시 배포(연속 수집)가 근본 해결이고 백필은 그 보완이다.
**Depends on**: Phase 2(수집 파이프라인·LostarkApiClient·레이트리밋), Phase 3(타임라인 read/차트), Phase 12(상세 API 스파이크 패턴)
**Requirements**: BACKFILL-01, BACKFILL-02, BACKFILL-03, BACKFILL-04
**Success criteria**:
1. 매 수집 시 리스트 응답의 `YDayAvgPrice`(전 품목·무료)를 `item_daily_stats`에 멱등 upsert해 일별 평균 시리즈를 going-forward로 축적한다 (실 수집 min_price·price_snapshot 무변경)
2. 서버 기동 시 + 일 1회, 상세 API `Stats[]`로 재료의 최근 14일 일별 평균을 소급 upsert해 과거 gap을 채운다
3. 품목 타임라인이 백필 일별 평균을 연속 라인으로 표시하고 실측 min_price는 그 위에 시각적으로 구분해 표식한다(정직성)
4. 수집/캐시/event-impact 핵심 경로 0줄(Core Value 가드), price_snapshot 스키마 무변경(additive 신규 테이블만)
**스파이크**: `.planning/phases/17.4-timeline-gap-backfill/17.4-SPIKE-FINDINGS.md` (실 API 검증: 상세 Stats 14일·재료만 / 리스트 YDayAvgPrice 전 품목·무료 / 각인서도 거래 활발)

#### Phase 18: 무료 라이브 배포 + 보안 검증 (마지막)

**Goal**: 무료 호스팅에 배포해 공개 URL로 데모 3화면 + 관리자 콘솔을 서빙한다. 프론트는 정적/단일 출처로 서빙하고, 실 키·시크릿은 서버 env only, 배포 직전 보안 검증 게이트를 통과한다. 무료 타깃(항상무료 VM vs 무료 PaaS)은 착수 시 리서치로 결정.
**Depends on**: Phase 15, 16, 17 (완성된 앱을 배포) — 마일스톤 마지막
**Requirements**: DEPLOY-01, DEPLOY-02, DEPLOY-03, DEPLOY-04
**Success criteria**:
1. 공개 URL로 데모 3화면과 관리자 콘솔에 접근된다
2. 프론트가 배포 환경에서 서빙되고 백엔드와 동일 출처로 API·관리자 시크릿 경로가 동작한다
3. 실 API 키·관리자 시크릿이 서버 env로만 주입되고 코드/문서/로그/커밋에 미기재된다
4. 배포 직전 보안 게이트 통과 — 관리자 쓰기 보호·API 키 미노출·DB/Redis 포트 비공개·HTTPS·CORS 정책
**배포 결정(대화형 디스커션 확정)**: Oracle Cloud Always Free VM 한 대 · docker-compose 단일 박스(app+postgres+redis+caddy) · Caddy 자동 HTTPS + 정적 서빙(동일 출처→CORS 소멸) · DuckDNS 도메인 · **수동 배포**(CD 자동화는 v2). 상세: `.planning/phases/18-free-deploy-security/18-CONTEXT.md`
**Plans**: 5 plans

Plans:
- [x] 18-01-PLAN.md — 앱 컨테이너화(비루트 Dockerfile) + prod 프로파일(actuator never) + WatchlistSeeder prod (DEPLOY-01/04) ✅ compileJava 그린
- [x] 18-02-PLAN.md — Caddy 리버스 프록시 + 프론트 정적 서빙(동일 출처·자동 HTTPS·SPA fallback) (DEPLOY-02) ✅ npm build 그린 (정정: web 컨텍스트=frontend/, Caddyfile→frontend/Caddyfile)
- [x] 18-03-PLAN.md — docker-compose.prod.yml(보안 네트워킹·포트 미공개·redis requirepass) + .env.prod.example (DEPLOY-03/04) ✅ compose valid + ./gradlew build 그린
- [x] 18-04-PLAN.md — 배포 런북(Oracle VM·DuckDNS·방화벽 이중개방·systemd) — 수동 배포 (DEPLOY-01/03) ✅ 문서 산출 (Task3 실제 VM 배포는 사용자 수동 대기)
- [x] 18-05-PLAN.md — 배포 직전 보안 검증 게이트(6항목 체크리스트·go/no-go) (DEPLOY-03) ✅ 정적 4/4 PASS (라이브 2항목 배포 후 대기)

**실행 상태**: 저장소 산출물 5/5 완료·검증 그린. **라이브 배포는 사용자 수동**(Oracle VM 계정 필요) — 런북 `docs/deploy/oracle-vm-runbook.md` + 게이트 `docs/deploy/security-checklist.md` 준비됨. **라이브 배포 완료(2026-07-13)** — https://lostark-tracker.duckdns.org, 보안 게이트 6/6 + 게이트 밖 하드닝(CSP/헤더·SSH /32) 통과.

### ✅ v1.4 CI/CD 자동화 (Phase 19) — SHIPPED 2026-07-14

**Goal:** Phase 18에서 **수동으로 남겨둔 배포를 자동화**한다 — `main` 머지 → CI(백엔드+프론트 테스트) 그린 → GHCR 이미지 빌드·푸시 → Tailscale로 VM에 SSH해 pull+재기동+스모크테스트까지 자동. VM에서 빌드하지 않고(4GB ARM 부담 제거) **불변 이미지(SHA 태그)**로 배포·롤백한다. Phase 18에서 "CD 자동화는 v2"로 미뤘던 항목의 실현. **첫 실배포 성공: run #30(`916bd3a`, 2026-07-14) — 전 구간 실사 검증.**

**불변 제약(상시):** 실 시크릿(`.env.prod`)은 VM에만 · CI는 앱/레지스트리 시크릿 미접근(패키지 **private** + VM 1회 `docker login`) · 수집/캐시/event-impact/서빙 로직 **0줄**(순수 배포 파이프라인) · **GitHub-hosted 러너 사용**(self-hosted 미사용).

#### Phase 19: 자동 CI/CD 파이프라인

**Goal**: `main` push 시 CI 그린이면 GHCR에 app·web 이미지를 빌드·푸시하고, Tailscale SSH로 VM에 자동 배포(pull→재기동→스모크)한다. 배포 승인=자동(CI 게이트), 접근=Tailscale(공개 SSH 개방 불필요).
**Depends on**: Phase 18 (컨테이너화 `Dockerfile`/`frontend/Dockerfile` · `docker-compose.prod.yml` · Caddy · systemd · 런북 — 그 산출물을 자동화 대상으로)
**Requirements**: CICD-01, CICD-02, CICD-03, CICD-04
**Success criteria**:
1. `main` push → 백엔드(`./gradlew build`) + 프론트(`npm ci && npm run build`, `tsc`) CI가 **게이트**로 동작하고, 실패 시 배포되지 않는다
2. CI 그린 시 `app`·`web` 이미지가 GHCR에 `sha-<커밋>` + `latest` 태그로 푸시된다 (`GITHUB_TOKEN`, 앱 시크릿 미사용)
3. 러너가 **Tailscale**로 VM에 접속해 새 이미지를 pull하고 재기동하며, **공개 SSH 개방 없이** 배포된다(방화벽 /32 유지 또는 SSH 완전 폐쇄)
4. 배포 후 **스모크 테스트**(`/actuator/health`·`/api/health/collection` 200)가 통과해야 성공 처리되고, 실패 시 loud fail + 이전 SHA 태그로 롤백 가능
5. (가드) `.env.prod`은 VM에만·CI 미노출, 수집/캐시/event-impact/서빙 로직 0줄

**Requirements 정의(CICD):**
- **CICD-01** CI 게이트 — 백엔드+프론트 테스트가 배포 전 게이트로 동작
- **CICD-02** 이미지·레지스트리 — `app`·`web` 이미지를 GHCR에 `sha`+`latest` 태그 푸시(VM 빌드 제거)
- **CICD-03** 자동 배포 — Tailscale SSH로 VM pull+재기동(공개 SSH 불필요), `main` 한정
- **CICD-04** 배포 검증·롤백 — 스모크 테스트 게이트 + SHA 태그 롤백 경로

**분할:**
- [x] **19-01** GHCR 이미지화 — compose `build`→`image` + CI 이미지 빌드·GHCR push job + 프론트 CI 게이트 (실행 완료 2026-07-13, `940901b`·`97d44b6`, actionlint 통과)
- [x] **19-02** 배포 job — Tailscale SSH → pull+재기동+스모크 (게이트 `DEPLOY_ENABLED`, 실행 완료 2026-07-13, `c5cde94`). 사전조치 완료 후 `DEPLOY_ENABLED=true` 전환 → **첫 실배포 성공(run #30, `916bd3a`, 2026-07-14)**
- [x] **19-03** 운영 문서 — 런북 운영 Runbook(배포·상태·로그·health·롤백·장애진단·복구) · README 자동 배포 · `systemd`/compose `--build` 제거 · 첫 실배포 반영 (실행 완료 2026-07-14)

**Plans**: 3/3 ✅ (19-01·19-02·19-03 실행 완료 · 라이브 배포 활성·검증)

### ✅ v1.5 시세 범위 확장 + UX (Phases 20–23) — COMPLETE (2026-07-14). Phase 24(경매장)=v1.6 후보

**Goal:** 사용자가 실제로 쓰는 **"스펙업 재료" 중심으로 시세 커버리지를 넓히고**, 대시보드를 maplanet식 **3열(좌 카테고리 / 중앙 물품 / 우 소식)**로 재구성하며, 이벤트 상관 분석 표현력을 **이벤트 카테고리 확장**으로 높인다. 설계: `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`.

**불변 제약(상시):** 수집/캐시/event-impact **로직 0줄**(C의 추적 확대는 워치리스트 **데이터만**) · 실키 서버 env only · 프론트 Lostark 직접 호출 금지 · 경매장(AUCTIONS) 진입은 "**현재가 둘러보기**"로 한정(거래소 시계열 수집 무오염).

**분할(Phase):**
- [x] **Phase 20** 이벤트 카테고리 +3 (EVT-01) — `NEW_CLASS`/`NEW_RAID`/`GENERAL_PATCH` additive(enum+zod+마커색+폼). DB 마이그레이션 불필요. 차원술사(7/8) 상관 기록. **완료 2026-07-14** (dataviz 검증 색, 백엔드 IT+프론트 build 그린)
- [x] **Phase 21** 재련 재료 스파이크 Stage 0 (MKT-01) — **완료 2026-07-14**(`21-SPIKE-FINDINGS.md`): 거래소 카탈로그 실측 + **큐레이션 비준(19종 잠금)**. 50010 기본(파괴/수호석 base+결정·돌파석 둘 다·파편 소중대) · 50020 추가(숨결·상급재련 업화[15-18]) · **230000 아크그리드젬=거래소, 영웅 6종**. 아크그리드젬=경매장 아님 발견.
- [x] **Phase 22** 재련 재료 추적 확대 (MKT-02) — **완료 2026-07-14**: 재련기본 7·상급재련·아크그리드젬 6 WatchlistSeeder 편입. 아크그리드젬 포함(거래소). **수집/캐시/event-impact 로직 0줄**, SyntheticDemoData 무변경(동적 로드). `./gradlew build` 그린. 22b: [19-20] DESC 스파이크로 실측·편입. **quick-260714 교정**: 업화 계열(일반 재련 보조)·숨결(상급·일반 겸용)을 `재련보조`로 재분류하고, 진짜 상급재련 전용 재료=장인의 야금술/재봉술 1~4단계 8종 신규 편입 → **워치리스트 22→49, MATERIAL 31, item_group 6종**(상급재련=장인 책 8, 재련보조=숨결 2+업화 4; WatchlistSeederIT 49종 증명).
- [x] **Phase 23** 대시보드 3열 카테고리 레이아웃 (UX-01, UX-02) — **완료 2026-07-14**: 2열→3열(좌 CategoryNav 2단계 그룹 필터 / 중앙 물품 / 우 소식), 모바일 상단 가로 칩. 신규 `categories.ts`(taxonomy 단일 출처)+`CategoryNav.tsx`(반응형 무상태), `DashboardPage` 3열 grid+필터 상태. ItemCard/NewsPanel 무변경, 기존 디자인 토큰 재사용(신규 0), **Core Value 0줄**. `npm run build` 그린 + 라이브 Playwright QA 통과(데스크톱 3열·필터·모바일 칩·빈 카테고리 숨김). UI-SPEC: `23-UI-SPEC.md`.
- [~] **Phase 24** 경매장 통합 (MKT-03) — **v1.7로 이관·착수**(2026-07-15). 보석만·티어4 8~10레벨로 스코프 확정. 아래 v1.7 섹션 참조.

### ✅ v1.6 이벤트 영향 신뢰성 (Phase 25) — COMPLETE (2026-07-15)

**Goal:** 이벤트 영향 페이지가 **이미 가진 데이터를 쓰게** 한다. 타임라인은 `price_snapshot`(10분 min 호가)과 `item_daily_stats`(백필 일평균)를 둘 다 읽지만 `EventImpactService`는 전자만 읽는다 — 그래서 **수집 시작(2026-07-10) 이전 이벤트는 구조적으로 영원히 "데이터 부족"**이다. 사용자 제보(2026-07-15)로 발견.

**실측 근거:** `GET /api/items/9/event-impact?window=24`(유물 타격의 대가 각인서 / 차원술사 출시 7/8) → `preAnchorAt`·`postAnchorAt` 모두 `null`(희소). 같은 품목 타임라인 백필에는 **7/7 32,628 → 7/8 45,877(+40.6%) → 7/14 40,483(정상화)**가 그대로 있다. 사용자가 화면에서 본 스파이크를 영향 페이지만 못 본 것.

- [x] **Phase 25** event-impact 백필 폴백 앵커 (IMPACT-V2-01) — **완료 2026-07-15**: 스냅샷 앵커가 있으면 지금처럼 min 기준으로 계산하고, **없을 때만** 일별 평균으로 폴백해 `일별 평균 기준`이라고 정직하게 표기. 지표 semantics(min 호가 vs avg 체결)를 절대 섞지 않는 게 핵심. 수집/캐시 **0줄**, N+1 회피 유지. 라이브 실증: `/api/items/10/event-impact?window=24` → `ok`·`changeRate 0.4061`·`DAILY_AVG`, 화면 "비교 가능 · 일별 평균 기준 · +40.6% · 32,629→45,878 G". `EventImpactBackfillAnchorIT` 7/7(스냅샷 우선·소스 우선순위·KST 일자).

**Requirements 정의:**
- **EVT-01** 이벤트 카테고리 확장 — 신규 캐릭터/레이드/일반 패치 3종 additive(백엔드 enum + 프론트 레전드·폼·마커색)
- **MKT-01** 재련 재료 카탈로그 스파이크 — 거래소 실측·잠금(품목·카테고리·아이콘) + 아크그리드젬 API 위치
- **MKT-02** 재련 재료 추적 확대 — 잠근 재료 워치리스트 편입, 10분 시계열 축적, Core Value 로직 0줄
- **MKT-03** (v1.6) 경매장 보석·아크그리드젬 현재가 둘러보기 — AUCTIONS 온디맨드+캐시, 시계열 미기록
- **UX-01** 대시보드 3열 카테고리 필터 레이아웃 — 좌 카테고리/중앙 물품/우 소식, 필터 상호작용
- **UX-02** 모바일 반응형 — 카테고리 상단 칩 탭, 물품/소식 세로 스택

**Plans**: 4/4 (✅ Phase 20·21·22·23 완료 · Phase 24는 v1.7로 이관·착수)

### ✅ v1.7 경매장 보석 현재가 (Phase 24, 26) — COMPLETE (2026-07-15)

**Goal:** v1.0 REQUIREMENTS가 v2로 미뤄둔 **`SRC-V2-01`(경매장(AUCTIONS)/보석 소스 확장)을 실현**한다 — Phase 18이 `DEPLOY-V2-01`을, Phase 19가 `CD-V2-01`을 실현한 것과 같은 방식. 스코프는 사용자 확정(2026-07-15)대로 **티어4 보석 8·9·10레벨만**, 클래스 무관. 보석은 **"현재가 둘러보기"(온디맨드+캐시)로 한정**하고 **시계열로 기록하지 않는다** — 거래소 10분 수집(Core Value)을 오염시키지 않기 위해서다.

**스코프 경계 진화(명기):** PROJECT.md는 "데이터 소스 = 거래소(MARKETS)만, 경매장/보석은 v2"로 명시한다. 이 마일스톤은 **그 경계를 의도적으로 넓히는 것**이므로 별도 마일스톤으로 분리해 기록한다. 아크그리드젬은 Phase 21 스파이크에서 **거래소(230000)로 판명**나 Phase 22에서 이미 추적 편입됨 → **v1.7은 보석만**.

**불변 제약(상시):** 실 JWT는 서버 env only(문서·로그·커밋 미기재) · 프론트 Lostark 직접 호출 금지 · 거래소 수집(`price_snapshot`)/캐시/event-impact **로직 0줄** · 보석은 시계열 미기록(DB 폭증·레이트리밋 잠식 금지).

#### Phase 24: 경매장 보석 카탈로그 스파이크 (Stage 0 게이트)

**Goal**: 경매장(AUCTIONS) API를 **실호출로 검증**해, 티어4 8~10레벨 보석의 실 카탈로그(품목·Id·아이콘)와 **"현재가"의 정의**를 잠근다. 이 프로젝트는 경매장 API를 한 번도 호출한 적이 없고, 거래소(MARKETS)와 요청 shape·레이트리밋·응답 구조가 다르다 — 실측 전엔 데이터 모델도 화면도 확정하지 않는다(Task 0 / Phase 12 / Phase 21의 spike-then-lock 계승).
**Depends on**: Phase 21(`21-SPIKE-FINDINGS` — 아크그리드젬=거래소 판명으로 이 phase 스코프가 보석만으로 축소됨), Phase 2(`LostarkApiClient`·레이트리밋 예산), `spike` 프로파일 하네스(`LostarkSpikeClient`/`MarketsApiSpikeTest`)
**Requirements**: GEM-01
**Status**: ✅ 실행 완료 2026-07-15 — `24-SPIKE-FINDINGS.md` 잠금(보석 6종 · 현재가=`min(BuyPrice)` · 레이트리밋 버킷 공유). 🚦 휴먼 비준 후 GEM-02.
**Success criteria**:
1. `GET /auctions/options` · `POST /auctions/items`가 **기존 JWT 키로 실호출 200**을 반환하고, 응답의 레이트리밋 헤더로 **거래소 수집과 버킷을 공유하는지** 판정된다 (공유면 수집 예산 잠식 위험이므로 구현 phase의 캐시 TTL·호출 빈도 설계 입력이 된다 — Core Value 가드)
2. **티어4 8·9·10레벨 보석이 실제로 무엇이며 몇 종인지** 실측으로 확정된다 — 도메인 가정 금지(Phase 21에서 "아크그리드젬=경매장" 가정이 실측으로 뒤집힌 선례)
3. **"현재가"의 정의가 잠긴다** — `BuyPrice`(즉시구매) / `BidStartPrice`(입찰시작) / `AuctionInfo` 필드의 실제 semantics를 응답으로 확인하고, 어느 것을 "현재가"로 표시할지 근거와 함께 결정한다(설계 스펙 미해결 질문)
4. 표시에 필요한 필드(Id·Name·Icon·GradeQuality 등)와 CDN 아이콘 URL 패턴, 페이징·정렬(`SortCondition`) 동작이 기록된다
5. 산출물 `24-SPIKE-FINDINGS.md`가 실측 근거 + 잠금 결정 + exit-gate를 담고, **공개 메타데이터만** 기록한다(가격 원문·키 미기재)
6. (Core Value 가드) 수집/캐시/event-impact **로직 0줄** — 스파이크 코드는 `spike` 프로파일 한정이라 일반 실행·CI에 미개입, 프로덕트 UI 0줄

#### Phase 26: 보석 현재가 둘러보기 — `/gems`

**Goal**: Phase 24가 잠근 티어4 보석 6종(겁화·작열 × 8/9/10레벨)의 **최저 즉시구매가**를 백엔드가 온디맨드로 조회·캐시해 서빙하고, 신규 `/gems` 페이지에서 보여준다. 보석은 **시계열로 기록하지 않는다** — 거래소 10분 수집(Core Value)과 레이트리밋 버킷을 공유하므로(24 실측) 캐시가 필수다.
**Depends on**: Phase 24(`24-SPIKE-FINDINGS` — 카탈로그·"현재가" 정의·버킷 공유·`Id` 부재 잠금), Phase 3(read API + Redis 캐시 패턴), Phase 17.2(뉴스 = 독립 read-path + 캐시 + keep-on-failure 선례), Phase 7(라우팅·AppLayout)
**Requirements**: GEM-02
**Status**: ✅ 완료 2026-07-15 — 라이브 6/6 OK. 실버그 1건 잡음: 보석이 공유 토큰버킷을 우회해 429 → 같은 버킷 편입 후 해소.
**비준 완료(사용자 2026-07-15)**: (a) 보석 6종 카탈로그, (b) **현재가 = `min(BuyPrice)`(최저 즉시구매가)**, (c) 배치 = **별도 페이지 `/gems`**(상단내비 4번째). 대시보드 3열·`CategoryNav`·`TrackedItem` 모델 **무변경** — 보석은 `Id`가 없고 타임라인이 없어 `ItemCard`(카드 전체가 `/timeline?item=` 링크)의 어포던스가 성립하지 않기 때문.
**Success criteria**:
1. `GET /api/gems`가 보석 6종의 최저 즉시구매가를 서빙한다 — 계열(겁화/작열)·레벨(8/9/10)·아이콘·가격·갱신시각 포함, 프론트는 이 백엔드 계약만 소비(로아 직접 호출 금지)
2. 응답이 **Redis 캐시**로 서빙되어 페이지뷰마다 경매장을 호출하지 않는다 — 버킷을 거래소 수집과 공유하므로(24 §H2) 캐시 미스에만 6콜
3. 즉시구매 매물이 없는 보석(전 매물 `BuyPrice`=null)은 **가격 없음으로 정직 표기** — 값을 지어내지 않는다(`insufficient_data`·쿠폰 `startsAt` null과 같은 원칙)
4. `/gems` 페이지가 계열별로 6종을 표시하고, 로딩/빈/에러를 빈 화면 없이 정직히 처리하며, 소싱 실패 시 마지막 캐시를 유지한다(17.2 keep-on-failure 선례)
5. (Core Value 가드) 수집(`PriceCollector`)·가격 캐시·event-impact·`price_snapshot` **0줄**, 마이그레이션 0 — 보석은 독립 read-path·시계열 미기록

**Requirements 정의(GEM):**
- **GEM-01** 경매장 보석 카탈로그 스파이크 — AUCTIONS 실호출로 인증·레이트리밋 공유 여부·티어4 8~10레벨 보석 실 카탈로그·"현재가" 필드 semantics를 실측·잠금(findings 산출) ✅ Phase 24
- **GEM-02** 보석 현재가 둘러보기 — 온디맨드 조회 + Redis 캐시 + `/gems` 화면. 시계열 미기록, Core Value 0줄 → Phase 26

### 🚧 v1.8 보석 시세 기록 시작 (Phase 27) — IN PROGRESS (착수 2026-07-15)

**Goal:** 보석 가격을 **1시간마다 기록하기 시작**하고, `/gems` 화면의 사실오류(역할 라벨)를 고치며 레벨 중심 표로 재구성한다. 일별 시세 표와 이벤트 영향 표는 **이 phase에서 만들지 않는다** — 채울 과거가 없기 때문이다(아래 §백필 불가). 기록을 **오늘 켜는 것**이 그 표들의 유일한 전제조건이라 먼저 켠다.

**🔑 스코프 경계 진화(명기):** v1.7은 "보석은 **시계열 미기록**(DB 폭증·레이트리밋 잠식 금지)"을 불변 제약으로 세웠다. 이 마일스톤은 **그 제약을 의도적으로 해제**하므로 별도 마일스톤으로 분리해 기록한다(v1.7이 PROJECT.md의 "거래소만" 경계를 넓힐 때와 같은 방식). 해제 근거는 **주기를 1시간으로 잡으면 두 우려가 모두 성립하지 않는다**는 실측 산수다:

| | 하루 행/콜 | `price_snapshot` 대비 | 분당 콜 (한도 100) |
|---|---|---|---|
| 거래소 수집 (49품목 × 10분) | ~7,056 | 100% | ~4.9 |
| **보석 기록 (6종 × 1시간)** | **144** | **2%** | **0.1** |

즉 v1.7의 제약은 "시계열 기록 자체"가 아니라 **"수집 예산을 잠식하는 빈도의 기록"**을 막으려던 것이었고, 1시간 주기는 그 선 아래에 있다. 조건이 달라졌으므로 제약도 근거와 함께 갱신한다.

**🔑 백필 불가 — 표를 지금 만들지 않는 이유:** 거래소 품목은 `GET /markets/items/{id}`의 `Stats[]`(일평균 ~14일)로 과거를 소급할 수 있었다(Phase 17.4). **보석은 그 경로가 없다** — 경매장 응답에 `Id`가 없고(Phase 24 H5) 경매장엔 히스토리 엔드포인트 자체가 없다. 합성 데이터는 이미 배제한 길이다(dev DB 합성 seed 삭제, 실데이터만). 따라서:
- **일별 시세 표**: 기록 시작일부터 1행/일로만 자란다.
- **이벤트 영향 표**: `EventImpactService`는 이벤트 전·후 데이터가 **둘 다** 있어야 `ok`다 → 등록된 이벤트는 전부 과거라 **전 행 "데이터 부족"**. 첫 의미 있는 행은 기록 시작 이후 **새 이벤트가 열려야** 나온다.
- Phase 25가 존재한 이유(이벤트 영향이 영원히 "데이터 부족")와 같은 상황이지만, **이번엔 폴백할 소스가 없다.** 그래서 표는 데이터가 쌓인 뒤 별도 phase로 계획한다.

**불변 제약(상시):** 실 JWT는 서버 env only · 프론트 Lostark 직접 호출 금지 · 거래소 수집(`PriceCollector`/`price_snapshot`)·가격 캐시·event-impact **로직 0줄**(보석 기록은 **독립 테이블·독립 폴러**, additive 마이그레이션만) · 보석 호출은 수집기와 **같은 토큰 버킷**에서 예산을 얻는다(Phase 26에서 우회가 실버그였음).

#### Phase 27: 보석 시세 기록 시작 + `/gems` 정정

**Goal**: 보석 6종의 최저 즉시구매가를 **1시간마다 독립 테이블에 기록**하기 시작하고(일별 표·이벤트 표의 전제조건), `/gems` 화면에서 **근거 없는 역할 라벨을 제거**하고 레벨 중심 표로 재구성한다. 헤드라인 현재가의 **온디맨드 캐시(≤5분·무방문 0콜)는 그대로 유지**한다 — 기록 폴러와 화면 서빙은 다른 일이다.
**Depends on**: Phase 24(`24-SPIKE-FINDINGS` — 카탈로그·`min(BuyPrice)` 정의·`Id` 부재), Phase 26(`GemService`/`LostarkAuctionClient`/`GemCatalog`/`/gems`), Phase 17.4(`item_daily_stats` = 독립 시계열 테이블 + additive 마이그레이션 선례), Phase 2(`RedisTokenBucket` 공유 버킷)
**Requirements**: GEM-03, GEM-04
**사용자 결정(2026-07-15)**: (a) 기록 주기 = **1시간마다**(하루 1회는 그날 우연히 올라온 매물 하나에 값이 좌우되고, 이벤트 영향이 "어제 vs 오늘"로 뭉개짐), (b) 이번 범위 = **라벨·표 수정 + 기록 켜기**까지 — 일별 표·이벤트 표는 축적 후.
**Success criteria**:
1. 보석 6종의 최저 즉시구매가가 **1시간마다 독립 테이블에 적재**된다 — 수집기와 **같은 토큰 버킷**에서 예산을 얻고, 재기동에 멱등하며, 폴러 실패가 화면을 죽이지 않는다
2. **답을 받은 것만 기록한다** — 즉시구매 매물이 없으면 가격 null로 "물어봤고 없었다"를 남기고, 호출 실패·레이트리밋은 **행을 남기지 않는다**(행 부재 = 못 물어봄). 값을 지어내지 않는다
3. `/gems`에서 **딜러/서포터 라벨이 제거**된다 — 측정된 적 없는 도메인 추측이었다(Phase 24 findings 문서도 같이 정정). 겁화/작열은 값이 다른 별개 아이템이므로 구분은 유지
4. 화면이 **레벨 중심 표**(행=레벨 8/9/10, 열=겁화/작열)로 재구성되어 6개 값이 3행에 들어가고 계열 간 비교가 즉시 된다
5. **헤드라인 현재가는 회귀 없음** — 여전히 방문 시 ≤5분, 무방문 시 경매장 호출 0. 기록 폴러가 캐시를 덮어써 화면을 묵은 값으로 만들지 않는다
6. (Core Value 가드) 수집(`PriceCollector`)·가격 캐시·event-impact·`price_snapshot` **0줄** — 보석 기록은 독립 테이블·독립 폴러·additive 마이그레이션(V9)만

**Requirements 정의(GEM 추가):**
- **GEM-03** 보석 시세 기록 — 1시간 폴러가 6종 최저 즉시구매가를 독립 테이블에 멱등 적재, 공유 토큰버킷 준수, 답받은 것만 기록 → Phase 27
- **GEM-04** `/gems` 정정 — 근거 없는 역할 라벨 제거(문서 포함) + 레벨 중심 표 재구성, 헤드라인 신선도 회귀 0 → Phase 27

**Plans**: 2 plans

Plans:
- [ ] 27-01-PLAN.md — 보석 시세 기록 시작: V9 `gem_price_snapshot` + `GemPriceFetcher` 추출 + 1시간 폴러 (GEM-03)
- [ ] 27-02-PLAN.md — `/gems` 정정: 역할 라벨 제거(문서 포함) + 레벨 중심 표 (GEM-04)

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–6 (Foundation → Distribution) | v1.0 | 15/15 | Complete | 2026-06-25 |
| 7–11 (Frontend Foundation → Demo Surface) | v1.1 | 19/19 | Complete | 2026-06-29 |
| 12–14 (API Spike → Frontend Icons) | v1.2 | 6/6 | Complete | 2026-06-30 |
| 15. 관리자 콘솔 UI | v1.3 | 4/4 | Complete    | 2026-07-03 |
| 16. 대시보드 카드 개선 | v1.3 | 1/1 | Complete    | 2026-07-03 |
| 17. 실데이터 전환 | v1.3 | 3/3 | Complete   | 2026-07-03 |
| 17.1 데모 최종 폴리시 (INSERTED) | v1.3 | 4/4 | Complete   | 2026-07-06 |
| 18. 무료 라이브 배포 + 보안 검증 | v1.3 | 5/5 | Complete — 라이브 배포 완료 | 2026-07-13 |
| 19. 자동 CI/CD 파이프라인 | v1.4 | 3/3 | Complete — 자동 배포 활성·검증(run #30 `916bd3a`) | 2026-07-14 |
| 20. 이벤트 카테고리 +3 | v1.5 | 1/1 | Complete | 2026-07-14 |
| 21. 재련 재료 스파이크 (Stage 0) | v1.5 | 1/1 | Complete — 큐레이션 비준(19종 잠금) | 2026-07-14 |
| 22. 재련 재료 추적 확대 | v1.5 | 1/1 | Complete — 워치리스트 22→49 (quick-260714 교정) | 2026-07-14 |
| 23. 대시보드 3열 레이아웃 | v1.5 | 1/1 | Complete — 3열+필터+모바일 칩, 라이브 QA 통과 | 2026-07-14 |
| 25. event-impact 백필 폴백 앵커 | v1.6 | 1/1 | Complete — 차원술사×타격의 대가 데이터 부족→+40.6% | 2026-07-15 |
| 24. 경매장 보석 카탈로그 스파이크 | v1.7 | 1/1 | Complete — 보석 6종 잠금 · 현재가=최저 즉시구매가 · 버킷 공유 확정 (비준 완료) | 2026-07-15 |
| 26. 보석 현재가 둘러보기 `/gems` | v1.7 | 1/1 | Complete — 6종 최저 즉시구매가 · 공유 토큰버킷 편입(429 해소) | 2026-07-15 |
| 27. 보석 시세 기록 시작 + `/gems` 정정 | v1.8 | 0/2 | Planned — 1시간 폴러(기록만) + 역할 라벨 제거 + 레벨 중심 표 | — |

**v1.3 Coverage:** v1.3 requirements 20 total · 매핑 **20/20 ✓** (ADMINUI 6 + CARD 2 + REALDATA 3 + POLISH 5 + DEPLOY 4)

_v1.0/v1.1/v1.2 상세는 milestones/ 아카이브. v1.3(Phases 15–18, +17.1~17.4) 라이브 배포 완료(2026-07-13). **v1.4 CI/CD 자동화 (Phase 19) 완료(2026-07-14)** — 수동 배포를 GHCR+Tailscale 무인 파이프라인으로 전환, 첫 실배포 run #30(`916bd3a`) 성공. **v1.5 시세 범위 확장 + UX (Phases 20–23) 완료(2026-07-14)** — 스펙업 재료 커버리지 확대 + 대시보드 3열 + 이벤트 카테고리. 설계 스펙 `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`. **v1.6 이벤트 영향 신뢰성 (Phase 25) 완료(2026-07-15)**. **v1.7 경매장 보석 현재가 (Phase 24·26) 완료(2026-07-15)** — v2로 미뤄둔 `SRC-V2-01` 실현, 티어4 8~10레벨 보석만·현재가 둘러보기. 현재 활성: **v1.8 보석 시세 기록 시작 (Phase 27, 착수 2026-07-15)** — v1.7의 "시계열 미기록" 경계를 실측 산수(1시간 주기 = 수집의 2%)를 근거로 해제하고 **기록만 먼저 켠다**. 보석은 `Id`가 없어 **백필이 불가능**하므로(경매장에 히스토리 엔드포인트 부재) 일별 표·이벤트 영향 표는 데이터가 쌓인 뒤 별도 phase. https://lostark-tracker.duckdns.org._
