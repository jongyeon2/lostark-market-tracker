---
gsd_state_version: 1.0
milestone: v1.8
milestone_name: 보석 기록 + 대시보드 통합
status: "**v1.5 완료(4/4)** — Phase 20(이벤트 +3)·21(재련재료 스파이크)·22(추적 편입)·23(대시보드 3열) 완료 + quick-260714 교정. 워치리스트 **49종**: 강화재료 2·재련재료 9·상급재련 8(장인 야금술/재봉술 1~4단계)·재련보조 6(숨결 2+업화 4)·아크그리드젬 6·각인서 18(item_group 6종, role=MATERIAL 31/DEALER 11/SUPPORT 7). **Phase 23**: 대시보드 2열→3열(좌 CategoryNav 2단계 그룹 필터 / 중앙 물품 / 우 소식), 신규 CategoryNav+categories.ts, ItemCard/NewsPanel 무변경, 기존 토큰 재사용(신규 0). 라이브 Playwright QA 통과(데스크톱 3열·필터·모바일 칩·빈 카테고리 숨김). 수집/캐시/event-impact 로직 0줄. Phase 24(경매장 보석)=v1.6 후보. v1.4 완료·라이브 검증됨."
stopped_at: v1.8 Phase 27 완료(기록 폴러 + 라벨 정정), 라이브 실증 완료·미푸시. 다음 = Phase 28 UI-SPEC(대시보드 통합). 사용자 방침 = 28까지 다 끝내고 1회 push로 배포. ⚠️ 보고사항 유지: 공유 토큰버킷(용량90+리필90/분)이 서버 한도 100/분 초과 가능 — Phase 2부터의 성질, 미수정
last_updated: "2026-07-15T17:10:00.000Z"
last_activity: 2026-07-15 -- Phase 27 완료: 보석 1시간 기록 폴러(V9 gem_price_snapshot) + 역할 라벨 제거. 라이브: 6행 기록·재기동 멱등(0 rows inserted)·캐시 미오염 실증. IT 16/16. grep 검증이 GemDtos javadoc 누락을 잡아냄
progress:
  total_phases: 13
  completed_phases: 9
  total_plans: 25
  completed_plans: 25
  percent: 69
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-07-01 for v1.3 milestone)

**Core value:** 레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다
**Current focus:** 🚧 **v1.8 보석 시세 기록 시작 (Phase 27, 착수 2026-07-15)** — 보석 가격을 **1시간마다 기록만** 시작하고 `/gems`의 사실오류(역할 라벨)를 고친다. 일별 표·이벤트 영향 표는 **이번에 만들지 않는다**(아래 §백필 불가). 직전 v1.7(24·26)·v1.6(25)·v1.5(20–23) 완료.

## Current Position

Milestone: v1.8 보석 기록 + 대시보드 통합 — Phase 27 ✅ **완료(2/2)**, Phase 28 **UI-SPEC 대기**. 직전 v1.7(Phase 24·26) 완료
Next: **Phase 28 UI-SPEC**(대시보드 통합) → 계획 → 실행 → **그 뒤 사용자가 1회 push로 배포**(사용자 방침: main push = 자동 배포이므로 다 끝내고 한 번에).

**Phase 27 잠금**: `gem_price_snapshot`(V9) — 키 = **(series, level, hour_slot)**, `tracked_item` FK 없음(보석은 Id 없음). `recorded_at`=실측 순간 / `hour_slot`=절삭(멱등 키 전용). **답받은 것만 기록**: 매물없음=`min_buy_price` null 행 / 실패·레이트리밋=**행 없음**(행 부재 = 못 물어봄) → status 컬럼 불요. `ON CONFLICT DO NOTHING`(먼저 온 표본이 이김 — 각 표본은 그 순간의 사실이라 나중 값이 더 옳지 않다). 폴러 1시간(`gem.poll-interval-ms`), test·seed 프로파일은 initial-delay를 밀어 실호출 차단. **`GemPriceFetcher`가 토큰버킷 획득의 유일한 집**(Phase 26 실버그 재발 자리 제거). 폴러는 `gem:latest`를 **안 건드림** — 서빙은 여전히 ≤5분·무방문 0콜.
**라이브 실증**: 부팅 기록 6행(slot 12:00Z) → 같은 시간대 재기동 `0 rows inserted`·6행 유지(멱등). 기록 12:52:37 vs API `updatedAt` 12:54:00 = 캐시 미오염. 429 0건. IT 16/16(`GemServiceIT` 9/9는 **무수정** 통과 = fetcher 추출이 서빙 무변경).

### 🔑 사용자 재설계 (2026-07-15) — Phase 28

사용자가 화면 구조를 다시 잡았다: **흩어진 3화면을 대시보드 하나로 모은다.** `/gems`는 6개 값 보여주자고 페이지 하나를 썼고, `이벤트 영향`은 페이지를 옮겨가 셀렉터로 품목을 또 골라야 했다.

- **`/gems` 제거** → 좌측 카테고리에 `보석` 추가, 6종 카드 일괄(레벨 하위 카테고리 없음), 목록 위 "현재 보석 시세값"
- **`이벤트 영향` 페이지 제거** → 물품 카드 클릭 시 **중앙 하단**에 영향 카드(`EventImpactCards` 재활용)
- **중앙 = 높이 고정 스크롤 박스**(우측 소식 패널 첫 이벤트 카드 아래까지 정렬), 모바일은 고정높이 해제
- **`ItemCard`에 차트 링크 신설** — 카드 클릭이 영향 선택으로 바뀌므로 타임라인 진입 보존. `품목 타임라인` 페이지 **유지**
- **보석 카드는 안 눌린다** — 과거 가격이 없어 이벤트 영향 계산 불가 → 갈 곳이 없다(가짜 어포던스 금지, Phase 26 `/gems` 행과 같은 판단)
- 27-02의 "레벨 중심 표"는 **폐기** — 28이 페이지째 지우므로 만들자마자 버려질 작업이었다
⚠️ **v1.7은 아직 미푸시**(사용자가 직접 push) — 마이그레이션 없음, Caddy SPA fallback으로 `/gems` 설정 불요, CSP 무변경. Phase 27은 **V9 마이그레이션이 생기므로** 배포 시 성격이 달라진다.

### v1.8이 뒤집는 것 (근거 있는 경계 진화)

v1.7은 "보석 **시계열 미기록**(DB 폭증·레이트리밋 잠식 금지)"을 불변 제약으로 세웠다. v1.8은 **주기를 근거로 그 제약을 해제**한다 — 막으려던 건 기록 자체가 아니라 *수집 예산을 잠식하는 빈도의 기록*이었다:

| | 하루 콜/행 | `price_snapshot` 대비 | 분당 콜 (한도 100) |
|---|---|---|---|
| 거래소 수집 (49품목×10분) | ~7,056 | 100% | ~4.9 |
| 보석 5분 폴러 (v1.7이 거부) | 1,728 | 24% | 1.2 |
| **보석 1시간 폴러 (v1.8)** | **144** | **2%** | **0.1** |

🔑 **백필 불가 — 표를 지금 안 만드는 이유:** 보석은 `Id`가 없고(24 §H5) 경매장에 히스토리 엔드포인트가 없어 거래소의 `Stats[]` 소급(Phase 17.4)에 **해당하는 경로가 존재하지 않는다**. 합성 데이터는 배제된 길(dev 합성 seed 삭제, 실데이터만). 따라서 일별 표는 기록 시작일부터 1행/일로 자라고, 이벤트 영향 표는 **등록된 이벤트가 전부 과거라 전 행 "데이터 부족"**이다(첫 유효 행은 기록 시작 이후 새 이벤트가 열려야 나옴). Phase 25와 같은 상황이지만 **이번엔 폴백 소스가 없다** → 표는 축적 후 별도 phase(GEM-05/06 후보).

🔑 **사용자 도메인 정정(2026-07-15):** 보석은 딜러/서포터로 나눠 쓰지 않는다 — 레벨로 사서 실링으로 원하는 스킬에 돌려 낀다. `겁화(딜러)`/`작열(서포터)` 라벨은 **Phase 24가 측정한 적 없는 추측**이 findings 표 → `GemCatalog.SERIES_DEALER` → 화면으로 전파된 것(같은 스파이크가 `Level` 가정을 뒤집었다고 자랑한 문서에서). 27-02가 **문서 원본부터** 제거한다. 단 겁화/작열은 값이 다른 별개 아이템이라 구분 자체는 유지.
**Phase 26 잠금**: `GET /api/gems` → 보석 6종 최저 즉시구매가(`min(BuyPrice)`), Redis `gem:latest` TTL 5분 **캐시-어사이드**(폴러 없음 → 무방문 시 경매장 호출 0). 행 상태 4값 `OK`/`NO_BUYOUT`/`RATE_LIMITED`/`FETCH_FAILED` — 가격 없으면 값을 지어내지 않고 행 단위 문구. `/gems` 행은 **링크 아님**(보석은 Id·시계열 없음 → 갈 곳 없음).
🔑 **실버그(수정 완료)**: 보석이 프로젝트 공유 `RedisTokenBucket`("one API key, one bucket" D-03)을 우회해 첫 라이브에서 429(겁화 3 OK/작열 3 실패). 같은 버킷 편입 후 6/6 OK. IT 9/9.
⚠️ **미수정 보고사항**: 버킷 용량 90 + 리필 90/분 → 가득 찬 상태면 1분 최대 180콜 가능한데 서버 한도 100/분. 리미터를 지켜도 429 가능(dev 기동 시 수집49+백필49 동시 발화로 더 쉬움). Phase 2부터의 성질이고 수집기는 Retry-After로 흡수 → **Core Value 코드라 손대지 않음**, 별도 판단 필요.

## Performance Metrics

**Velocity:**

- Total plans completed: 30
- Average duration: —
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 03 | 3 | - | - |
| 04 | 2 | - | - |
| 05 | 2 | - | - |
| 08 | 3 | - | - |
| 09 | 5 | - | - |
| 10 | 4 | - | - |
| 15 | 4 | - | - |
| 16 | 1 | - | - |
| 17.3 | 3 | - | - |

**Recent Trend:**

- Last 5 plans: —
- Trend: —

*Updated after each plan completion*

## Accumulated Context

### Roadmap Evolution

- Phase 17.1 inserted after Phase 17: 데모 최종 폴리시: 큐레이션 갱신·대시보드 재구성 (배포 앞) (URGENT)
- Phase 17.2 inserted after Phase 17.1: 대시보드 뉴스 패널 (로아 공식 이벤트·공지, 배포 앞) — 설계 스펙 docs/superpowers/specs/2026-07-06-dashboard-news-panel-design.md (쿠폰은 이후 관리자 수동 입력)
- Phase 17.4 inserted after Phase 17.3: 타임라인 gap 백필 (일별 Stats) — 서버 off 수집 공백을 일별 평균가로 백필. discuss 완료: 소스 2종(리스트 YDayAvgPrice 전 품목·무료 going-forward + 상세 Stats 재료 14일 소급), 백필 연속라인+실측 표식, 기동+일1회, 항상 표시. 각인서도 거래 활발(초기 "거래 없음" 결론 정정). findings/CONTEXT 17.4-* (URGENT)
- Phase 19 added (v1.4 CI/CD 자동화, 2026-07-13): 자동 CI/CD 파이프라인 — Phase 18에서 v2로 미룬 CD 실현. 결정 확정(GHCR 이미지 빌드 + Tailscale SSH 자동배포 · CI 그린 시 자동). 분할 19-01(GHCR 이미지화)·19-02(배포 job)·19-03(운영 문서). slug 정정: gsd-sdk가 `ci-cd`로 truncate→dir `19-cicd-pipeline`으로 리네임

### Decisions

Decisions are logged in PROJECT.md Key Decisions table. Recent decisions affecting current work:

- 설계: 접근법 B (A→B 단계화) + event-impact 헤드라인 (office-hours 승인, plan-eng-review clean)
- 1A: 틱 내 병렬 팬아웃 + allOf().join() → 분산 락 제거 (Phase 2)
- DATA: price_snapshot UNIQUE(tracked_item_id, collected_at) 멱등 + TIMESTAMPTZ UTC (Phase 1)
- DB: PostgreSQL 확정 (개발 docker-compose, 테스트 Testcontainers)
- 게이트: 2주차 말 하드 게이트 통과 시에만 Phase 5(event-impact) 진행, 슬립 시 v2 강등
- [Phase ?]: 01-01: Docker api.version=1.44 핀 (엔진 29.x가 docker-java 기본 v1.32 거부); Testcontainers 공유 베이스 PostgresRedisContainers 확립
- [Phase ?]: 01-02: 4테이블 Flyway V1 잠금 + JPA ddl-auto=validate 일치; OffsetDateTime<->TIMESTAMPTZ UTC; UNIQUE(item,collected_at) 멱등 DATA-01~04 IT 증명
- [Phase ?]: 01-03 Task0(확정): avg_price/trade_count는 일단위 제공(상세 Stats) → min_price 유지·avg_price V2 추가·trade_count per-tick 제외; 매칭 external_item_id=API Id+display_name; 레이트 100/min 확정
- [Phase 12]: 12-01(확정): 각인서 CategoryCode=40000(leaf), 융화재료=50010(재련 재료); Icon 필드명=Icon, CDN=cdn-lostark.game.onstove.com/efui_iconatlas/use/; 큐레이션 15개 잠금(융화재료 4 MATERIAL + 딜러 9 + 서포터 2[각성·전문의] DEALER/SUPPORT); 유물 각인서 아이콘 동일(use_9_25)→라벨병기(D-06); 만개 보류(0건)·구원 제외(실재 아님)·운명 융화재료 Deferred
- [Phase 14]: 14-01(확정): eventImpactSchema는 실제 백엔드 EnrichedEventImpactResponse **평면**({itemId,window,iconUrl,itemGroup,roleGroup,events}) — CONTEXT/UI-SPEC 중첩 enrichment 서술 부정확(코드가 진실, src 확인); roleGroup=z.enum 3값 nullable로 boundary loud-fail(D-06); 역할 3색=semantic 색군(rose-600/emerald-700/amber-700, accent blue-600과 분리, D-01); RoleBadge는 ui/badge.tsx 0줄 className 오버라이드; ItemIcon 고정 슬롯 + null/onError 역할색 글리프(ScrollText/FlaskConical/Package) 시프트 0, lucide-react만
- [Phase 14]: 14-02: ICON-05는 ImpactPage 정체성 영역(LatestPriceCard) 1회로 충족·EventImpactCards 0줄(D-04 zone 분리); 셀렉터 sortByRole 후 SECTIONS filter(null→기타), 큐레이션 누락 0(ICON-07) · 14-03: README findings 요약+링크(단일출처 12-SPIKE-FINDINGS, D-10), 스크린샷 캡처 수동 위임(D-11), 코드 diff 0

### Pending Todos

None yet.

### Blockers/Concerns

None (2026-06-30: `.env` `LOSTARK_API_KEY` 무효(401) 이슈는 키 재발급·교체로 해소 — `collection_run` SUCCESS 15/15, 실데이터 적재 확인)

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260715-g98 | 재료 그룹 망치 아이콘 + 쿠폰 시작일~만료일 렌더(사용자 후속 2건) — 쿠폰은 `starts_at` 컬럼 자체가 없어 **Flyway V8** 세로 슬라이스(엔티티/DTO/서비스/어드민 폼/렌더). **nullable** 결정: 기존 쿠폰엔 시작일이 없고 NOT NULL은 없는 날짜를 지어내는 것 → 없으면 `~ 만료일` 폴백. `@AssertTrue isPeriodOrdered`로 역순 400(렌더가 순서를 전제). 어드민 시작일 선택 입력(빈 값→null, max=만료일). 재료는 대표 게임 아이콘이 성립 안 해(품목별 상이) lucide Hammer 글리프 — 각인서(CDN 이미지)와 `groupIconUrl`/`GROUP_GLYPH` 한 규칙. 검증: IT +5·build 그린·라이브 API 두 경로·Playwright. Core Value 0줄 | 2026-07-15 | 89f7da6, ee3ec22 | [260715-g98-coupon-period-material-icon](./quick/260715-g98-coupon-period-material-icon/) |
| 260715-nav | 대시보드 카테고리 교정(요청 1·2번) — **강화재료 그룹 폐지**: 융화재료는 재련에 필수라 재련재료로 통합(재련기본 9+융화 2=11). 핵심은 **Flyway V7** — 시더가 insert-if-absent라 소스만 고치면 운영 기존 행(아비도스 id=1,2)이 안 옮겨져 어느 leaf에도 안 걸린 채 증발한다. V7은 id가 아닌 `WHERE item_group='강화재료'` 기준(레거시 행 포함·멱등). `WatchlistSeederIT`에 item_group 카운트 단언 신설(doesNotContainKey 강화재료 + 재련재료 11). 각인 그룹→"각인서", 헤더에 border-b 구분선 + 대표 아이콘(`groupIconUrl` 데이터 파생, 각인서 18종 동일 아이콘이라 성립 / 재료는 null). 검증: build 그린·dev DB 실측(강화재료 0건, v7)·Playwright(재련재료 클릭→아비도스 노출). Core Value 0줄 | 2026-07-15 | 5d43d6f, 877e606 | [260715-nav-category-taxonomy](./quick/260715-nav-category-taxonomy/) |
| 260715-eeg | 소식 패널 교정(요청 3·4번) — **만료 이벤트 필터 실버그 수정**: `fetchEvents()`가 endDate asc(종료임박순)+6건 cap만 하고 만료를 안 걸러, 7/8 종료 이벤트 2건이 7/15 운영 최상단 점유(만료가 cap 잠식→진행중 이벤트 축출). `NewsEvent.isOngoingAt(nowKst)` 술어 신설(KST 벽시계, 경계 inclusive, null/파싱불가 유지) + filter를 sorted/limit 앞에, `getLatest()`에도 재적용(TTL 12h·폴링 6h 구멍 차단). "진행중 이벤트"→"진행중인 이벤트", 쿠폰 행 reward 표시 제거(DB/어드민 유지). `NewsServiceIT` fixture 시한폭탄(2026-07-20) 제거. 검증: TDD 6케이스·build 그린·로컬 API 실측(만료 2건↓·진행중 2건↑)·Playwright 라이브. Core Value 0줄 | 2026-07-15 | 50136b1, 3871c16 | [260715-eeg-news-panel-fix](./quick/260715-eeg-news-panel-fix/) |
| 260714-sxn | 재련 재료 분류 교정(업화·숨결 → `재련보조`) + 진짜 상급재련=장인의 야금술/재봉술 1~4단계 8종 스파이크(captureRefineMasterBooks, DESC/ASC 50020) 실측·편입 → 워치리스트 41→49, MATERIAL 31, item_group 6종(상급재련=장인 책 8 전용, 재련보조=숨결 2+업화 4). 도메인 오분류(로아 유저 지적) 교정. Core Value 로직 0줄, `./gradlew build` 그린 | 2026-07-14 | 2c96665 | [260714-sxn-refine-material-reclassify](./quick/260714-sxn-refine-material-reclassify/) |
| 260713-mur | WatchlistSeederIT 시점 경계 flaky 수정 — SyntheticDemoData.seed()가 벽시계 now()로 gridNow 재계산, 첫 seed(~25k인서트 수십초)와 둘째가 10분 경계 넘으면 멱등성 깨짐. 테스트에서 Clock.fixed(UTC)로 수동 생성해 결정적 통과. prod 0줄, 로컬 그린 | 2026-07-13 | a0ed5be | [260713-mur-watchlistseederit-flaky-clock](./quick/260713-mur-watchlistseederit-flaky-clock/) |
| 260713-h3s | README 클라이언트 친화 리라이트 v2 — glz 위에 2층 구조 확립(가시 ~80줄 + <details> 접힌 깊이, 375→206줄). 라이브 링크·타임라인 딥링크·배포 사이트 실제 스크린샷 3장 교체(impact 빈 상태=insufficient_data 정직 캡션), 스택 3중복→표 1곳, API 데모 4→1+표. 코드 0줄(문서+이미지) | 2026-07-13 | 3e996b2 | [260713-h3s-readme-client-rewrite](./quick/260713-h3s-readme-client-rewrite/) |
| 260713-glz | README 리라이트 — 상단 히어로/소개 논문체→평이한 '무엇을·왜'(리크루터 20초 이해), 신규 '어떻게 만들었나(AI 협업)' 섹션(Claude Code+GSD 명시하되 설계결정 주도권·검증게이트·설명가능성 프레이밍, '바이브코딩' 금지), 중복 정리(상관≠인과 3→1·기술스택 2블록→1·seed/dev 압축), stale 사실 조정. 하단 깊이 보존. 문서만 | 2026-07-13 | dc4d7b4 | [260713-glz-readme-rewrite](./quick/260713-glz-readme-rewrite/) |
| 260713-g9d | 배포 런북(oracle-vm-runbook) 하드닝 반영 — §4 SSH /32 제한 강화(유동 IP 주의·키 전용 인증), §8에 배포된 보안 헤더 7종 목록(CSP·Permissions-Policy·COOP)+curl 검증+CSP 근거(unsafe-eval 미포함·COEP 제외) 추가. 문서만, 코드 0줄 | 2026-07-13 | 0e783ad | [260713-g9d-runbook-post-deploy-hardening](./quick/260713-g9d-runbook-post-deploy-hardening/) |
| 260713-e1o | Caddy CSP + 보안 헤더 보강(Permissions-Policy·COOP) — 18-05 게이트 밖 하드닝 후속. 라이브 실측(Playwright page.route로 후보 CSP 주입)으로 origin 매핑 검증: script-src 'self'(unsafe-eval 없음, eval은 라이브러리 무해 프로브), style-src 'unsafe-inline'(Recharts), img-src에 onstove CDN. caddy validate 통과. Core Value 0줄 | 2026-07-13 | e347b65 | [260713-e1o-caddy-csp-security-headers](./quick/260713-e1o-caddy-csp-security-headers/) |
| 260707-usn | 대시보드 카드(ItemCard) 가격 용어를 차트와 통일 — 라벨 없던 🪙 minPrice에 "최저가" 라벨 추가 + "최신가 수집 중"→"최저가 수집 중" | 2026-07-07 | cdb1cc1 | [260707-usn-dashboard-card-price-label](./quick/260707-usn-dashboard-card-price-label/) |
| 260707-uly | 프론트 고객친화 UI — 차트 범례/툴팁 용어 순화(개발자 용어 "백필·일평균(거래가)"/"실측 최저호가"→"평균 거래가"/"최저가", 2지표 구분 유지) + 없던 파비콘 📈 SVG 추가 | 2026-07-07 | bf37c9d | [260707-uly-ui](./quick/260707-uly-ui/) |
| 260707-tzj | 각인서 백필 버그 수정 — getItemDetail이 상세 배열 details[0](귀속 거래1회·Stats 0)만 반환하던 것을 총 TradeCount 최대 원소 선택으로, 소급 러너를 재료 한정→전 활성 품목(각인서 포함)으로 확대. 각인서도 상세 14일 소급됨. Core Value 경로 0줄 | 2026-07-07 | 9e492b3 | [260707-tzj-getitemdetail-detailstatsbackfillrunner](./quick/260707-tzj-getitemdetail-detailstatsbackfillrunner/) |
| 260707-fkp | 데모 3화면(대시보드·품목 타임라인·이벤트 영향) 페이지 제목 h1 제거 — 진입 시 컨트롤·카드·차트 바로 렌더 | 2026-07-07 | daa7c24 | [260707-fkp-remove-page-titles](./quick/260707-fkp-remove-page-titles/) |
| 260706-ohg | 대시보드 카드 컴팩트화(py-6→py-3 + max-w-3xl 폭 제한) + 골드 🪙 이모티콘 (웹 라이브 피드백) | 2026-07-06 | 161018d | [260706-ohg-dashboard-card-compact-gold](./quick/260706-ohg-dashboard-card-compact-gold/) |
| 260630-0rh | Timeline·Impact LatestPriceCard 긴 품목명 세로 잘림 수정 (가로 한 줄 + 제목 축소) | 2026-06-30 | ec0bc97 | [260630-0rh-timeline-impact-latestpricecard](./quick/260630-0rh-timeline-impact-latestpricecard/) |
| 260630-16d | Phase 14 카드/셀렉터 비율 조정 3건 (대시보드 카드 축소 · LatestPriceCard container-type 확장 · 셀렉터 트리거 폭) | 2026-06-30 | e18e714 | [260630-16d-phase-14-3](./quick/260630-16d-phase-14-3/) |
| 260630-em5 | dev 첫 수집 타이밍 버그 수정 (collection.initial-delay-ms 추가 — 첫 틱 빈 워치리스트 헛돎 0→15). 별개로 .env API 키 무효(401) 발견 → Blocker | 2026-06-30 | 6fafc25 | [260630-em5-dev-collection-initial-delay](./quick/260630-em5-dev-collection-initial-delay/) |
| 260630-g0i | ItemSelect 트리거 카테고리 라벨만 표기 (긴 품목명 잘림 해소, w-40, 목록·LatestPriceCard 품목명 유지) | 2026-06-30 | 6dcb7ce | [260630-g0i-itemselect](./quick/260630-g0i-itemselect/) |
| 260630-gct | LatestPriceCard 가로 한 줄 레이아웃 (아이콘+이름+배지·골드·시간 균등 gap 24px, w-fit) | 2026-06-30 | c5e1373 | [260630-gct-latestpricecard](./quick/260630-gct-latestpricecard/) |
| 260630-h16 | 품목 타임라인 일별 평균 최저가 집계 표기 (프론트 KST 일별 가중평균, '버킷 평균·1일' 배지 재사용, dot 밀도 기준화로 희소 일별 가시화, 백엔드 무변경) | 2026-06-30 | 137ed0a | [260630-h16-timeline-daily-buckets](./quick/260630-h16-timeline-daily-buckets/) |
| 260630-lu5 | README JSCODE 스타일 재구성 (친근 인트로 상단 + 엔지니어링 깊이 하단 보존, 프로젝트 구조·Developer 섹션 추가, curl/JSON·스크린샷·설계결정 무손실, 코드 0줄) | 2026-06-30 | 68dc55b | [260630-lu5-readme-jscode](./quick/260630-lu5-readme-jscode/) |

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-07-07 -- Phase 17.4 executed inline (4/4 plans)
Stopped at: Phase 17.4 complete + verified (VERIFICATION.md PASS)
Resume file: .planning/phases/17.4-timeline-gap-backfill/17.4-VERIFICATION.md

## Operator Next Steps

- ✅ **Phase 19 (v1.4 CI/CD 자동화) 완료(2026-07-14):** 3/3 plans 인라인 실행. `main` push → CI(백엔드+프론트 게이트) → arm64 이미지 GHCR push → Tailscale SSH로 VM pull+재기동 → 공개 HTTPS 스모크까지 **무인 배포**. **첫 실배포 성공: run #30(`916bd3a`)** — VM `sha-916bd3a` 4컨테이너 Up, `/actuator/health`=UP. systemd/compose `--build` 제거(GHCR pull 전용), 운영 Runbook(배포·상태·로그·health·롤백·장애진단·GHCR/Tailscale/SSH 복구) 정리. Core Value 가드 0줄(순수 배포 파이프라인).
- ⚠️ **후속 보안(문서만 남김):** 공개 SSH 22 폐쇄(OCI Ingress `/32` 제거)는 **Windows Tailscale 클라이언트로 운영자 SSH 실검증 후**. 검증 전엔 비상 복구 경로 유지 위해 열어둠. 런북 §10.9.
- 🚀 **v1.5 시세 범위 확장 + UX 진행(1/4, 2026-07-14):** 설계 스펙(`9cd8393`) + 로드맵 Phase 20~24 세팅. ✅ **Phase 20(이벤트 +3) 완료** — NEW_CLASS/NEW_RAID/GENERAL_PATCH additive, 백엔드 IT+프론트 build 그린, 마커색 dataviz 7색 CVD PASS. 남은: 21(재료 스파이크) → 22(재료 추적) → 23(대시보드 3열). Phase 24(경매장 보석)는 v1.6 후보. 설계: `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`.
- ✅ **Phase 21(재련 재료 스파이크) 실행 완료(2026-07-14):** `21-SPIKE-FINDINGS.md` — 거래소 카탈로그 실측(50010 기본·50020 추가/상급재련·230000 아크그리드젬), Id·아이콘 잠금. 🔑 **아크그리드젬=거래소**(경매장 아님) → Phase 24는 보석만.
- ✅ **Phase 22(재련 재료 추적 편입) 완료(2026-07-14) + quick-260714 교정:** 재련기본 7·상급재련·아크그리드젬 6 WatchlistSeeder 편입. SyntheticDemoData 무변경(동적 로드), 수집/캐시/event-impact 0줄. `./gradlew build` 그린. 22b: [19-20] DESC 스파이크로 실측·편입. **🔧 quick-260714 교정(로아 유저 지적):** 업화 계열은 일반 재련 성공률 보조, 숨결(용암/빙하)은 상급·일반 재련 겸용 → **둘 다 `재련보조`로 재분류**. 진짜 상급재련 전용 재료=**장인의 야금술/재봉술 1~4단계 8종**(스파이크 실측 편입) → **워치리스트 22→49, MATERIAL 31, item_group 6종**(상급재련=장인 책 8, 재련보조=숨결 2+업화 4).
- **다음:** **Phase 23(대시보드 3열 카테고리 레이아웃)** — 신규 UI 레이아웃이라 **UI-SPEC 선행**(`/gsd-ui-phase`) → plan → execute. 좌 카테고리 필터(재료 31종은 item_group 6종[강화재료/재련재료/상급재련/재련보조/아크그리드젬/각인서] 세분 후보)/중앙 물품/우 소식, 모바일 칩. gstack `/browse`·`/design-review` QA.
- 불변 제약 상시 가드: 수집/캐시/event-impact/서빙 **로직 0줄** — v1.5의 C(추적 확대)는 워치리스트 **데이터만** 늘림(같은 수집기·레이트리밋·스키마). 경매장(AUCTIONS)은 "현재가 둘러보기"로 한정. 실 시크릿은 VM `.env.prod`에만.
