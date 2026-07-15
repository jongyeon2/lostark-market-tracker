---
gsd_state_version: 1.0
milestone: v1.6
milestone_name: 이벤트 영향 신뢰성
status: "**v1.5 완료(4/4)** — Phase 20(이벤트 +3)·21(재련재료 스파이크)·22(추적 편입)·23(대시보드 3열) 완료 + quick-260714 교정. 워치리스트 **49종**: 강화재료 2·재련재료 9·상급재련 8(장인 야금술/재봉술 1~4단계)·재련보조 6(숨결 2+업화 4)·아크그리드젬 6·각인서 18(item_group 6종, role=MATERIAL 31/DEALER 11/SUPPORT 7). **Phase 23**: 대시보드 2열→3열(좌 CategoryNav 2단계 그룹 필터 / 중앙 물품 / 우 소식), 신규 CategoryNav+categories.ts, ItemCard/NewsPanel 무변경, 기존 토큰 재사용(신규 0). 라이브 Playwright QA 통과(데스크톱 3열·필터·모바일 칩·빈 카테고리 숨김). 수집/캐시/event-impact 로직 0줄. Phase 24(경매장 보석)=v1.6 후보. v1.4 완료·라이브 검증됨."
stopped_at: Phase 25(event-impact 백필 폴백 앵커) 완료 + 사용자 요청 5건 전부 반영(quick-260715-eeg·nav 포함). 다음: push로 배포 반영(V7 포함) 또는 Phase 24(경매장 보석) 착수 결정
last_updated: "2026-07-15T02:40:00.000Z"
last_activity: 2026-07-15 -- Phase 25 완료(event-impact 백필 폴백 앵커) — 차원술사×타격의 대가 "데이터 부족"→+40.6%. 사용자 요청 5건 전부 반영
progress:
  total_phases: 12
  completed_phases: 8
  total_plans: 23
  completed_plans: 23
  percent: 67
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-07-01 for v1.3 milestone)

**Core value:** 레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다
**Current focus:** ✅ **v1.6 이벤트 영향 신뢰성(Phase 25) 완료** — 이벤트 영향이 백필 일평균으로 폴백해, 수집 시작(7/10) 이전 이벤트가 구조적으로 "데이터 부족"이던 문제 해소. v1.5(20–23)도 완료.

## Current Position

Milestone: v1.6 이벤트 영향 신뢰성 (Phase 25) — ✅ 완료(1/1). 직전 v1.5(Phases 20–23) 완료. Phase 24(경매장 보석)=미착수 후보
Next: **push로 배포 반영** — CI가 프론트 빌드→GHCR 재배포. 이번 배포엔 **Flyway V7**(강화재료→재련재료 이관)이 포함되므로 운영 DB의 아비도스 2종이 재련재료로 옮겨진다. 이후 Phase 24(경매장 보석) 착수 여부 결정.
Status: ✅ **Phase 25 완료** — event-impact가 스냅샷 없으면 백필 일평균으로 폴백(`anchorSource=DAILY_AVG`, 화면 "일별 평균 기준"), min(호가)/avg(체결) 혼합 금지·기존 ok 행 값 불변. 워치리스트 item_group 5종(강화재료 폐지→재련재료 11, V7). v1.5 4/4 완료 + quick-260714 교정. Phase 23(대시보드 3열) 코드+라이브 Playwright QA 통과(데스크톱 3열·필터·모바일 칩). 워치리스트 **49**(MATERIAL 31/DEALER 11/SUPPORT 7), item_group 6종. 수집 로직 0줄. 설계: `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`
Last activity: 2026-07-15 -- quick-260715-g98 완료(재료 망치 아이콘 + 쿠폰 시작일~만료일, V8). 같은 날: Phase 25(event-impact 백필 폴백 — 차원술사×타격의 대가 "데이터 부족"→+40.6%), quick-260715-eeg(소식 패널 만료 필터), quick-260715-nav(강화재료 폐지→재련재료 V7). 사용자 요청 5+2건 전부 반영. 다음: 사용자가 직접 push(배포 시 **V7·V8 마이그레이션** 실행) 또는 Phase 24(경매장 보석) 착수 결정

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
