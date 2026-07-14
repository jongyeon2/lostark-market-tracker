---
gsd_state_version: 1.0
milestone: v1.5
milestone_name: 시세 범위 확장 + UX
status: "v1.5 진행(3/4) — Phase 20(이벤트 +3)·21(재련재료 스파이크)·22(추적 편입) 완료. Phase 22: 신규 19종(재련기본 7·상급재련 6[업화 15-18·19-20]·아크그리드젬 6) WatchlistSeeder 편입(22→41), item_group=재련재료/상급재련/아크그리드젬, role=MATERIAL(4→23). 수집/캐시/event-impact 로직 0줄, SyntheticDemoData 무변경(동적), ./gradlew build 그린. 22b: 사용자 요청 [19-20] DESC 스파이크로 실측·편입. 다음: Phase 23(대시보드 3열 카테고리 레이아웃) — 좌 카테고리(딜러/서포터/재료, 재료는 item_group 세분 후보)/중앙 물품/우 소식 필터, 모바일 칩. UI-SPEC 선행. v1.4 완료·라이브 검증됨."
stopped_at: Phase 22 완료(워치리스트 22→41, [19-20] 포함). 다음: Phase 23(대시보드 3열) — UI-SPEC 선행
last_updated: "2026-07-14T11:30:00.000Z"
last_activity: 2026-07-14 -- Phase 22 완료: 재련 재료 19종 워치리스트 편입(22→41, 상급재련 [19-20] 포함), build 그린
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
**Current focus:** v1.5 시세 범위 확장 + UX **진행(1/4)** — ✅ Phase 20(이벤트 카테고리 +3) 완료. 다음: **Phase 21(재련 재료 스파이크 Stage 0)** — 거래소 재련재료 실측·잠금. (v1.4 CI/CD 자동화는 완료·라이브 검증됨.)

## Current Position

Milestone: v1.5 시세 범위 확장 + UX (Phases 20–24) — 🚀 진행(3/4: Phase 20·21·22 완료)
Next: **Phase 23(대시보드 3열 카테고리 레이아웃)** — 좌 카테고리/중앙 물품/우 소식 필터, 모바일 칩. 재료 21종은 item_group(강화재료/재련재료/상급재련/아크그리드젬) 세분 후보. **UI-SPEC 선행**(`/gsd-ui-phase`). Phase 24(경매장)는 보석만(v1.6).
Status: ✅ Phase 20·21·22 완료. Phase 22: 재련 재료 19종 편입(워치리스트 22→41, MATERIAL 4→23, 상급재련 [15-18]·[19-20]), 수집 로직 0줄, build 그린. 설계: `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md`
Last activity: 2026-07-14 -- Phase 22 완료(재련 재료 추적 편입 22→39). 직전: Phase 21 완료

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
- ✅ **Phase 22(재련 재료 추적 편입) 완료(2026-07-14):** 신규 19종(재련기본 7·상급재련 6[업화 15-18·19-20]·아크그리드젬 6) WatchlistSeeder 편입(22→41), MATERIAL 4→23. SyntheticDemoData 무변경(동적 로드), 수집/캐시/event-impact 0줄. `./gradlew build` 그린. 22b: [19-20] DESC 스파이크로 실측·편입.
- **다음:** **Phase 23(대시보드 3열 카테고리 레이아웃)** — 신규 UI 레이아웃이라 **UI-SPEC 선행**(`/gsd-ui-phase`) → plan → execute. 좌 카테고리 필터(재료 21종은 item_group 세분 후보)/중앙 물품/우 소식, 모바일 칩. gstack `/browse`·`/design-review` QA.
- 불변 제약 상시 가드: 수집/캐시/event-impact/서빙 **로직 0줄** — v1.5의 C(추적 확대)는 워치리스트 **데이터만** 늘림(같은 수집기·레이트리밋·스키마). 경매장(AUCTIONS)은 "현재가 둘러보기"로 한정. 실 시크릿은 VM `.env.prod`에만.
