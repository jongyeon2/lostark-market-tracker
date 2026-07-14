# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** — Phases 7–11 (shipped 2026-06-29) — [archive](milestones/v1.1-ROADMAP.md)
- ✅ **v1.2 Item Visual/Data Enrichment** — Phases 12–14 (shipped 2026-06-30) — [archive](milestones/v1.2-ROADMAP.md)
- ✅ **v1.3 관리자 콘솔 + 실데이터 라이브 배포** — Phases 15–18 (+17.1~17.4 삽입, 라이브 배포 완료 2026-07-13)
- ✅ **v1.4 CI/CD 자동화** — Phase 19 (shipped 2026-07-14 — 첫 실배포 run #30 `916bd3a`)
- 🚀 **v1.5 시세 범위 확장 + UX** — Phases 20–24 (진행 중, 착수 2026-07-14)

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

### 🚀 v1.5 시세 범위 확장 + UX (Phases 20–24) — IN PROGRESS (착수 2026-07-14)

**Goal:** 사용자가 실제로 쓰는 **"스펙업 재료" 중심으로 시세 커버리지를 넓히고**, 대시보드를 maplanet식 **3열(좌 카테고리 / 중앙 물품 / 우 소식)**로 재구성하며, 이벤트 상관 분석 표현력을 **이벤트 카테고리 확장**으로 높인다. 설계: `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`.

**불변 제약(상시):** 수집/캐시/event-impact **로직 0줄**(C의 추적 확대는 워치리스트 **데이터만**) · 실키 서버 env only · 프론트 Lostark 직접 호출 금지 · 경매장(AUCTIONS) 진입은 "**현재가 둘러보기**"로 한정(거래소 시계열 수집 무오염).

**분할(Phase):**
- [x] **Phase 20** 이벤트 카테고리 +3 (EVT-01) — `NEW_CLASS`/`NEW_RAID`/`GENERAL_PATCH` additive(enum+zod+마커색+폼). DB 마이그레이션 불필요. 차원술사(7/8) 상관 기록. **완료 2026-07-14** (dataviz 검증 색, 백엔드 IT+프론트 build 그린)
- [x] **Phase 21** 재련 재료 스파이크 Stage 0 (MKT-01) — **완료 2026-07-14**(`21-SPIKE-FINDINGS.md`): 거래소 카탈로그 실측 + **큐레이션 비준(19종 잠금)**. 50010 기본(파괴/수호석 base+결정·돌파석 둘 다·파편 소중대) · 50020 추가(숨결·상급재련 업화[15-18]) · **230000 아크그리드젬=거래소, 영웅 6종**. 아크그리드젬=경매장 아님 발견.
- [x] **Phase 22** 재련 재료 추적 확대 (MKT-02) — **완료 2026-07-14**: 신규 17종(재련기본 7·상급재련 4·아크그리드젬 6) WatchlistSeeder 편입(22→39). 아크그리드젬 포함(거래소). **수집/캐시/event-impact 로직 0줄**, SyntheticDemoData 무변경(동적 로드). `./gradlew build` 그린(WatchlistSeederIT 39종·MATERIAL 21 증명).
- [ ] **Phase 23** 대시보드 3열 카테고리 레이아웃 (UX-01, UX-02) — 좌 카테고리 필터/중앙 물품/우 소식, 모바일 상단 칩. **UI-SPEC 선행**.
- [ ] **(v1.6 후보) Phase 24** 경매장 통합 (MKT-03) — **보석(gems)만** 현재가 둘러보기(새 AUCTIONS 클라이언트). *(아크그리드젬은 거래소라 Phase 22로 이동 — 스파이크 발견.)* 스코프 경계 진화 → 별도 마일스톤에서 결정.

**Requirements 정의:**
- **EVT-01** 이벤트 카테고리 확장 — 신규 캐릭터/레이드/일반 패치 3종 additive(백엔드 enum + 프론트 레전드·폼·마커색)
- **MKT-01** 재련 재료 카탈로그 스파이크 — 거래소 실측·잠금(품목·카테고리·아이콘) + 아크그리드젬 API 위치
- **MKT-02** 재련 재료 추적 확대 — 잠근 재료 워치리스트 편입, 10분 시계열 축적, Core Value 로직 0줄
- **MKT-03** (v1.6) 경매장 보석·아크그리드젬 현재가 둘러보기 — AUCTIONS 온디맨드+캐시, 시계열 미기록
- **UX-01** 대시보드 3열 카테고리 필터 레이아웃 — 좌 카테고리/중앙 물품/우 소식, 필터 상호작용
- **UX-02** 모바일 반응형 — 카테고리 상단 칩 탭, 물품/소식 세로 스택

**Plans**: 3/4 (✅ Phase 20·21·22 완료 · 23 대기 · Phase 24는 v1.6 후보)



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
| 22. 재련 재료 추적 확대 | v1.5 | 1/1 | Complete — 워치리스트 22→39 | 2026-07-14 |
| 23. 대시보드 3열 레이아웃 | v1.5 | 0/1 | Planned | — |
| 24. 경매장 통합(보석·아크그리드젬) | v1.6? | — | Deferred (스코프 경계 진화) | — |

**v1.3 Coverage:** v1.3 requirements 20 total · 매핑 **20/20 ✓** (ADMINUI 6 + CARD 2 + REALDATA 3 + POLISH 5 + DEPLOY 4)

_v1.0/v1.1/v1.2 상세는 milestones/ 아카이브. v1.3(Phases 15–18, +17.1~17.4) 라이브 배포 완료(2026-07-13). **v1.4 CI/CD 자동화 (Phase 19) 완료(2026-07-14)** — 수동 배포를 GHCR+Tailscale 무인 파이프라인으로 전환, 첫 실배포 run #30(`916bd3a`) 성공. 현재 활성: **v1.5 시세 범위 확장 + UX (Phases 20–24, 착수 2026-07-14)** — 스펙업 재료 커버리지 확대 + 대시보드 3열 + 이벤트 카테고리. 설계 스펙 `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`. 순서: Phase 20(이벤트) → 21(재료 스파이크) → 22(재료 추적) → 23(대시보드). https://lostark-tracker.duckdns.org._
