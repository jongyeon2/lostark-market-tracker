---
gsd_state_version: 1.0
milestone: v1.8
milestone_name: 보석 기록 + 대시보드 통합
status: "**v1.5 완료(4/4)** — Phase 20(이벤트 +3)·21(재련재료 스파이크)·22(추적 편입)·23(대시보드 3열) 완료 + quick-260714 교정. 워치리스트 **49종**: 강화재료 2·재련재료 9·상급재련 8(장인 야금술/재봉술 1~4단계)·재련보조 6(숨결 2+업화 4)·아크그리드젬 6·각인서 18(item_group 6종, role=MATERIAL 31/DEALER 11/SUPPORT 7). **Phase 23**: 대시보드 2열→3열(좌 CategoryNav 2단계 그룹 필터 / 중앙 물품 / 우 소식), 신규 CategoryNav+categories.ts, ItemCard/NewsPanel 무변경, 기존 토큰 재사용(신규 0). 라이브 Playwright QA 통과(데스크톱 3열·필터·모바일 칩·빈 카테고리 숨김). 수집/캐시/event-impact 로직 0줄. Phase 24(경매장 보석)=v1.6 후보. v1.4 완료·라이브 검증됨."
stopped_at: v1.8 완료(Phase 27 기록 폴러+라벨 정정, Phase 28 대시보드 통합). 라이브 검증 완료·전부 미푸시 — 사용자 방침대로 v1.7~v1.8을 1회 push로 배포 예정(V9 마이그레이션 포함). ⚠️ 보고사항 유지: 공유 토큰버킷(용량90+리필90/분)이 서버 한도 100/분 초과 가능 — Phase 2부터의 성질, 미수정
last_updated: "2026-07-15T17:10:00.000Z"
last_activity: 2026-07-15 -- v1.8 완료. Phase 27: 보석 1시간 기록(V9)+역할 라벨 제거, 라이브 6행·재기동 멱등 실증. Phase 28: /gems·이벤트영향 페이지를 대시보드로 흡수(화면 4→2). 검증이 모바일 겹침을 잡음 — 구버전 대조로 "이름 실종=기존 버그 / 겹침=내 차트링크" 갈라내고 함께 수정
progress:
  total_phases: 13
  completed_phases: 10
  total_plans: 27
  completed_plans: 27
  percent: 77
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-07-01 for v1.3 milestone)

**Core value:** 레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다
**Current focus:** 🚧 **v1.8 보석 시세 기록 시작 (Phase 27, 착수 2026-07-15)** — 보석 가격을 **1시간마다 기록만** 시작하고 `/gems`의 사실오류(역할 라벨)를 고친다. 일별 표·이벤트 영향 표는 **이번에 만들지 않는다**(아래 §백필 불가). 직전 v1.7(24·26)·v1.6(25)·v1.5(20–23) 완료.

## Current Position

Milestone: v1.8 보석 기록 + 대시보드 통합 — ✅ **완료(Phase 27·28, 4/4 plans)**. 직전 v1.7(Phase 24·26) 완료
Next: **사용자가 1회 push로 배포** — v1.7(24·26) + v1.8(27·28)이 한꺼번에 나간다. **V9 마이그레이션 포함**(신규 테이블만 추가하는 additive라 기존 데이터 무영향, 롤백 시 구버전은 이 테이블을 무시). 이후 후보: 보석 일별 표·이벤트 영향 표(GEM-05/06 — 기록이 쌓인 뒤에만 의미).

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
- Phase 17.2 inserted after Phase 17.1: 대시보드 뉴스 패널 (로아 공식 이벤트·공지, 배포 앞) — 설계 스펙 docs/specs/2026-07-06-dashboard-news-panel-design.md (쿠폰은 이후 관리자 수동 입력)
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
| 260720-lk6 | **이벤트 영향 필터·정렬·개수 제한(서버) + 중앙 폭 확대**. 사용자 지적: 물품 선택 시 등록된 **모든** 이벤트 영향이 떠서 이벤트가 쌓이면 끝없이 스크롤. 🔑 **스크롤은 증상, 병목은 페이로드**: `EventImpactItem` 11필드(타임스탬프3+제목)≈250~400B → 1만 건이면 응답 **3~4MB**, 게다가 프론트가 `EventImpactTable`+`EventImpactCards`를 **둘 다 DOM에** 넣음(CSS로 하나만 숨김)=서브트리 2만. → **클라 필터는 절반만 해결**(3~4MB 받아놓고 거름)이라 **서버에서 자름**(사용자 결정). 계약: `types`(전체 기본)·`sort`(`occurred_desc`/`occurred_asc`)·`limit`(50, 상한 200) + 응답에 **`totalCount`**(필터 후·limit 전). 🔑 `totalCount ≠ events.size()` — 그 구분이 없으면 잘린 화면과 전부 보여준 화면을 구별 못 하고 '더 보기' 끝을 모름, 화면엔 항상 "전체 N건 중 M건". 🔑 **화이트리스트 400**(조용한 fallback 금지): 모르는 sort를 기본값으로 흘리면 화면은 "오래된순"인데 목록은 최신순 그대로(`MarketSearchService` 규율 동일); 잘못된 타입을 버리면 "그 종류엔 데이터 없음"이라는 **다른 주장**이 됨. 검증은 기존대로 **400이 404보다 먼저**. `findAllByOrderByOccurredAtDesc()` **삭제**(유일 호출자를 바꿨는데 남기면 "전량 조회" 경로가 살아 같은 실수 재발); 타입 미지정 시 전체 enum 전달로 nullable 분기 제거, `Page.getTotalElements()`가 totalCount 무료. 🔑 **N+1 회피(D-09)는 유지+개선** — 필터·limit을 계산 **전에** 적용해 `[min,max]`가 좁아져 단일 스냅샷 범위 조회가 더 작아지고 루프도 최대 limit개. 프론트: 종류 칩7(색·라벨은 `EVENT_MARKERS` 재사용 — 차트 마커·범례·배지와 같은 색이어야 함), "전체" 칩 없음(선택 없음==전체, 두면 "전체+로아ON" 모순 조합 발생), 최신/오래된순, `더 보기(+50)`, 필터·정렬 변경 시 limit 리셋(거래소 검색이 1페이지로 가는 것과 동일 논리), `placeholderData`로 깜빡임 방지. 칩 한 줄(피드백): 기본 크기로 1920에서 21px 부족→'일반 패치'만 2줄, 실측(가용862) px-2.5 +7 / +gap-1.5 +19 / **+text-xs +98** → 앞 둘은 창 조금만 줄여도 재접힘이라 글자까지 축소; **`koLabel`은 미변경**(공유 문자열이라 타임라인 범례까지 바뀜), ⚠️ ~1670px 미만은 여전히 2줄. 폭: 좌우 20→18rem, max-w 100→110rem(중앙 800→**1024px**@1920), 테이블 자체 스크롤 시작점 뷰포트 ~1570→~1505px. 검증: gradlew build 그린(신규 IT 7건), npm build 그린, 라이브 400 4/4, **필터 A/B**(`[로아ON]`→빈 결과 / `[로아ON,신규캐릭터출시]`→행 복귀), 가로스크롤 1920·1440·375 없음, 콘솔 에러 0. ⚠️ **미검증(정직)**: dev DB 이벤트 1건이라 `더 보기`·정렬 변화는 화면 확인 불가 — 합성 이벤트 주입은 "dev는 실데이터만" 결정 위반이라 안 함, 대신 IT로 고정(5건·limit=2→events2/total5·잘린 2건이 최신 2건). ⚠️ **별건: 내가 만든 회귀 수정(3090bdb)** — 375px에서 페이지 전체 가로스크롤 17px, 원인은 **다크모드(jkx)에서 넣은 테마 토글**(숨기면 377→360 정확히 일치), **이미 배포된 상태**였음 → `<sm` nav gap 16→8px. ⚠️ **320px는 미해결**(토글 빼도 20px 넘침 = 토글 이전 문제, 오버플로 메뉴 같은 반응형 네비 필요) | 2026-07-20 | 3090bdb, 510673b, c723a98, 9eee26a | [260720-lk6-event-impact-filter](./quick/260720-lk6-event-impact-filter/) |
| 260720-jkx | **다크모드**(프론트만·백엔드 0줄) — `index.css`가 "Light single theme only (dark mode is FE-V2-03)"로 미뤄뒀던 항목, 프로젝트 마지막 기능. 🔑 **싸게 끝난 이유**: 색을 전부 CSS 변수로 소비하는 구조라 대부분이 `.dark` 블록 하나로 따라옴 — 착수 전 하드코딩 색 전수조사 결과 **5곳뿐**(차트 grid/tick/시리즈2색·`impactFormat` 3색·`eventMarkers` 8색·클래스SVG 30개·`--grade-*`), 작업 후 재검사 **잔존 0건**. ①`@custom-variant dark` — Tailwind v4의 `dark:`는 기본이 미디어쿼리라 수동 토글이 안 먹음, `<html class="dark">` 기반으로 전환 ②`.dark` 토큰(shadcn 표준 slate, 사용자 결정) — **면 관계를 라이트와 동일 유지**(라이트 bg=slate-50/card=흰색 = 카드가 더 밝음 → 다크도 bg-950/card-900; 뒤집으면 같은 컴포넌트가 테마마다 다른 깊이로 읽힘) ③**FOUC 차단**: `index.html` `<head>` 인라인 스크립트가 첫 페인트 전 확정(React 안에서 하면 번들 로드까지 라이트가 먼저 그려짐), localStorage 접근 실패 try/catch ④`ThemeToggle` 해/달 2단계 — 첫 방문 OS 추종, 누르면 기억, **선택 후엔 OS가 안 덮음**; 판정 로직은 인라인 스크립트에만 두고 토글은 `<html>` 클래스를 **읽기만**(중복되면 화면과 아이콘이 반대가 됨) ⑤클래스 아이콘 `dark:invert`(공식 SVG가 `fill="#222222"` **단색**이라 반전이 정확히 #DDDDDD; mask-image/흰색 세트 추가보다 싸고 `<img>` 유지라 onError 폴백 생존) ⑥`eventMarkers`·차트 hex→`var(--*)`인데 **소비처 수정 0**(SVG stroke·inline style borderColor/backgroundColor 전부 CSS 변수를 그대로 받음) ⑦역할 알약은 다크에서 밝히고 글자 반전(진한 알약이 어두운 배경에 묻힘). 🔑 **함정 회피**: `impactFormat.ts`의 3 hex를 `--up`/`--down`으로 "정리"할 뻔했는데 **의미 축이 정반대**였다(이 화면은 국내 거래소 관례 상승=빨강·하락=파랑, `--up`은 초록). 그대로 바꿨으면 라이트 색이 뒤집힘 → 전용 `--change-up/down/flat` 신설, 라이트 값은 기존 hex 그대로. 🔑 **라이브 QA가 잡은 것**: 네이티브 스크롤바가 다크에서 밝게 남음 — CSS 변수를 안 보고 브라우저가 `color-scheme`으로 칠하기 때문. 계산·빌드로는 안 잡히고 실화면에서만 보이는 종류 → `:root{color-scheme:light}`/`.dark{color-scheme:dark}`. 검증: build 그린, **대비 전량 실측**(본문19.3·muted7.9·primary4.9·등급6.8~10.3·등락6.5~7.0·이벤트6.6~9.6, 전 항목 AA 통과), Playwright 라이트/다크 4라우트 육안 확인, **라이트 원상 유지**(라이트 토큰 0값 변경으로 보장), 콘솔 에러 0(경고 2건은 기존 Radix Select, 무관). 스크린샷에서 쿠폰 `복사` 버튼이 흰색으로 보여 버그를 의심했으나 계산된 스타일 측정 결과 정상(저해상도 착시) — 코드 미수정. ⚠️ `/admin`은 별도 셸이라 같은 토큰을 쓰지만 정밀 QA 미실시. 🔑 **후속(푸시 후 발견) — 운영 CSP 위반 2건**: ①폰트 4개가 `font-src 'self'`에 차단(Vite 기본 `assetsInlineLimit` 4KB가 작은 서브셋을 `data:` URI로 인라인 — **기존 버그**, 빌드CSS data:font 4개 = 콘솔오류 4건 일치) → vite.config에서 폰트 인라인 금지(4→0, 폰트파일 15→19) ②**이 작업이 넣은** FOUC 차단 인라인 스크립트가 `script-src 'self'`에 차단 → `public/theme-init.js`로 분리(동일 출처). 배포됐다면 저장된 다크 설정이 새로고침마다 무시됐을 것. 🔑 **둘 다 로컬 재현 불가**(dev는 인라인 안 하고 CSP 헤더는 운영 Caddy에만) — build·tsc·대비계산 전부 통과함. 재발 방지: **운영 Caddyfile과 동일한 CSP 헤더로 dist를 로컬 서빙 + Playwright** 절차로 검증(/avatar·/dashboard 콘솔 에러 0, 새로고침 후 첫 페인트에 dark 적용 실측). 두 건 모두 CSP를 넓히지 않고(=`data:`·`unsafe-inline` 추가 안 함) 정책을 지키는 쪽으로 수정 | 2026-07-20 | e0f9d4c, 72e5816, 7127536 | [260720-jkx-dark-mode](./quick/260720-jkx-dark-mode/) |
| 260720-giw | **모험의 서 대륙별 분류 + 등급색**(이 프로젝트에서 **백엔드를 건드린 첫 UI 작업**). 모험의 서는 대륙마다 달성도를 채우는 수집품 묶음인데 화면은 이름 검색뿐이라 "루테란 서부 채우려면 뭐가 얼마인지"를 못 봤다. 🔑 **API가 분류를 안 준다**: `/markets/options`의 `100000 모험의 서`는 Subs 빈 배열·대륙 필터 없음·이름에도 대륙 표시 없음(실측). 웹 조사 전부 실패(공식 게임가이드는 대륙 이름조차 없음·나무위키 403·인벤·INTY). → **사용자가 인게임을 직접 보며 노션 정리**가 유일 출처. 🔑 **그대로 안 믿고 거래소 전량(14페이지)과 대조**: 20대륙×7=140 == `TotalCount` 140, 중복·누락 0, 136개 문자열 동일, 4개만 표기차(루페온 시상→**신**상 / 태양 소금→태양소금 / 사피라 연구 일지→연구일지 / 엘조**원**→엘조**윈**)인데 남은 미매칭 4개와 **1:1 대응**이라 해석 여지 0 → 거래소 표기를 정본으로. `tomes.ts` 작성 후 **재대조 통과**(양방향 차집합 공집합). **백엔드**: 한 대륙 7개가 14페이지에 흩어져 페이지 계약으론 불가 → `getAdventureAll()`이 totalCount까지 순회 후 Redis 전량 캐시(TTL 10분), `/api/market/adventure`는 파라미터 없이 전량 반환, `search()` 무변경(아바타용). **부수효과가 더 좋음** — 옛 계약은 페이지 넘길 때마다 1콜 → 이제 **10분에 14콜 상한 고정**, 페이징 제거. 신규 IT 5건 전부 **종료조건이 load-bearing**(페이지당 토큰 1개라 안 멈추는 루프 = 수집기 예산 잠식): 전페이지 순회 / 2번째 0콜 / 도달불가 totalCount에서 50페이지 정지 / 빈 페이지 정지 / **중간 고갈 시 중단·부분캐시 없음**. **프론트**: `tomes.ts` 신설(매핑을 서버가 아닌 프론트에 — `categories.ts`·`classes.ts`와 같은 "API가 안 주는 정적 도메인 표" 성격), 좌측 대륙 네비(지도 글리프+`전체`140+20대륙 개수), 검색·정렬·필터 **전부 로컬** → 대륙 전환 시 요청 0. 표에 없는 아이템은 드롭 대신 `미분류` leaf(대륙 추가일에 조용히 사라지는 것 방지, 보이는 것 자체가 갱신 신호). 정렬 시 **가격 null은 방향 무관 항상 뒤로**(0으로 치면 "제일 싼 물건"으로 올라와 값 없음을 0원이라 지어냄). **등급색**: `--grade-*` 5종 신설 — **"신규 색 0" 원칙을 명시적으로 뒤집음**(로아 유저는 초록/파랑/보라/주황을 등급으로 즉시 읽음). 🔑 게임 색 그대로 안 씀(게임=검은 배경, 앱=흰 카드) — **일반은 게임에서 흰색이라 안 보여 중립 회색으로 대체**(유일하게 색상 변경), 나머지는 같은 계열 어둡게. **AA 실측**: 일반7.24/고급4.79/희귀6.41/영웅6.67/전설4.95 (#FFFFFF·#F8FAFC 양쪽). 등급 매핑도 픽셀 짐작 아님 — 스크린샷 9개를 API `Grade`와 대조 **9/9 일치**. 색은 **배지에만**(이름까지 칠했더니 행마다 본문색이 달라져 산만, 라이브 QA). 아바타·모험의 서 `<h1>`+부제 제거(상단 네비가 이미 현재 화면을 말함). 검증: `./gradlew build` 그린(203테스트), npm build 그린, **라이브 재기동 후 `pageSize=140 totalCount=140 items=140`·거래소 140과 집합 일치 True**. ⚠️ **별건**: `NewsControllerIT` 픽스처가 `endDate=2026-07-20T06:00:00` 하드코딩이라 **오늘 06:00 KST를 넘기며 터짐**(내 변경과 무관, news 파일 0줄 수정) → 날짜 미루기는 타이머 재장전이라 **실행시각 상대(now(KST)±1일)**로 교체; 나머지 하드코딩 날짜는 `Clock.fixed`·명시 `NOW`와 함께 쓰여 안전 확인. ⚠️ **등급색 확산 금지**(고급 초록=`--up`, 희귀 파랑=accent와 근접 — 거래소 행은 링크 아니고 등락 텍스트도 없어 두 의미가 한 화면에서 안 만나는 게 유일한 논거). ⚠️ `/api/market/adventure`는 **계약 파괴 변경** — 배포 시 백/프론트 동시 필요 | 2026-07-20 | 2debf60, bf5f308, 3459d7f, ad2c728 | [260720-giw-adventure-continents](./quick/260720-giw-adventure-continents/) |
| 260720-fn3 | **아바타 직업 바둑판 + 페이지 배치 재구성**(프론트만·백엔드 0줄) — 드롭다운 교체로 시작해 라이브 QA로 페이지 전체 배치까지 커짐. 🔑 **조사 결론: 공식 API는 직업 아이콘도 직업군 분류도 안 준다**(`/markets/options`의 `Classes`는 한글 문자열 30개뿐, 실측; CDN 경로 추측 4종 전부 404). 그래서 공식 사이트 자산에서 원본 추출 — 아이콘 파일명은 `2018/obt/assets/css/pc.css`의 `.icon--*` 배경경로, 직업군↔클래스는 `assets/js/pc.js`의 `{root_ko,root_en,list:[{name_ko,name_en}]}`. 추출 30개가 **라이브 API 30개와 집합 완전일치**(스크립트 검증, `기타` 그룹 0). 파일명 불규칙 — souleater(언더바X)/dimension_master(O)/가디언나이트=`dragon_knight.svg`. 공식 11 root(성별분리)를 **성별만 병합해 7그룹**(전사6·무도가6·헌터5·마법사4·암살자4·스페셜리스트4·가디언나이트1). ①공식 SVG 30개 `public/class-icons/` **번들**(핫링크 아님 — API가 준 URL이 아니라 사이트 내부자산이라 예고없이 이동 가능) ②`classes.ts` 신설, `groupClasses()`가 **API 응답을 단일 출처**로 삼고 표에 없는 신규 직업은 드롭 대신 `기타` 그룹(출시일에 그 직업만 조용히 선택불가 되는 게 최악) ③shadcn `Select` 폐기 → 직업군 블록(3열=6인그룹 정확히 2줄)을 `sm:2→lg:3→xl:4→2xl:5`로 가로배열, `Card`로 감싸고 그룹라벨 `border-b` ④**필터 전부 상단 통합** — 좌측 11rem 부위 기둥 제거, 부위 칩(`flex-wrap`, 가로스크롤 아님)+검색+정렬을 카드 하나로 → 위→아래가 사용순서(직업→부위→검색→결과)와 일치 ⑤시세 **2열**(`MarketResultList`에 `className` 주입구, 기본 `space-y-2` 유지 → 모험의 서 무영향). 🔑 **"박스가 넓다"는 폭 상한이 아니라 폭 활용으로 뒤집음**(원인은 카드가 아니라 타일 160px에 아이콘 24px = 내용이 폭을 못 채움). 대가: 부위 `lg:sticky` 포기(페이지당 10개=2열 5줄이라 스크롤 중 전환 드묾). 아이콘 5개 `fill="white"`→`#222222`(사용자 지목과 실제 파일 정확히 일치). 검증: build 그린, SVG 30개 전부 200+`<svg` 시작·md5중복 0·slug↔파일 30↔30, 사용자 QA "매우 만족". ⚠️ SVG가 `#222222` 고정 — 다크모드(FE-V2-03) 시 `mask-image`+`currentColor` 전환 필요 | 2026-07-20 | 70b7487, d909974 | [260720-fn3-avatar-class-grid](./quick/260720-fn3-avatar-class-grid/) |
| 260719-wl0 | **대시보드 헤더/네비 다듬기**(프론트만·백엔드 0줄) — 배포 대시보드 라이브 QA 폴리시. ①헤더 좌우 이동(페이지 길이에 따라 스크롤바 유무로 가용폭 ±15px→`mx-auto` 중앙정렬이 라우트마다 튐) → `index.css`에 `html{scrollbar-gutter:stable}`. ②보석 단독 leaf(그룹 헤더 없어 아이콘 못 받던)에 lucide `Gem`(재료 `Hammer` 선례). ③공지사항을 우측 소식박스→좌측으로 분리(`NoticeRail` export, 자체 useNews+AsyncBoundary D-07 격리; React Query 캐시공유로 좌·모바일 1요청). 우측 "로스트아크 소식" 제목 제거 + 상단 이중패딩(Card `py-6`+CardContent `pt-6`=48px) 정리(쿠폰 위로), 공지 제목 `line-clamp-1`. 🔑 **라이브 QA 수렴**: 좌측을 우측과 동일 20rem·p-6 흰 카드로 → 다시 카테고리/공지 **별개 흰 카드 2개**(구분선 제거, "한 박스에 둘"이 불편 피드백). sticky 소유를 `CategoryNav`→`DashboardPage`로 이관(필터+공지 한 블록 고정→겹침 방지), 카드는 `lg:*` 전용(모바일은 칩 edge-to-edge + 공지는 NewsPanel 하단 카드). 좌우 20rem 대칭으로 좁아진 중앙(이벤트 영향 테이블 754px 가로스크롤) 보전 위해 `max-w` **90rem→100rem**(중앙 ~800px, TopNav 동기화). 컬럼 간격 `lg:gap-12`. 검증: `tsc -b`+vite build 그린, 사용자 라이브 QA "매우 만족"(무스크롤은 화면 ~1560px+ 보장). 미푸시 | 2026-07-19 | 3ae1e60, a710297 | [260719-wl0-dashboard-nav-polish](./quick/260719-wl0-dashboard-nav-polish/) |
| 260718-jrz | **능동 모니터링·알림 — 수집 하트비트(데드맨) + 백업 즉시 /fail**(코드 절반; 사이트·TLS·Discord 라우팅은 사용자 외부작업 §11). 혼자 운영 무료 VM의 "죽어도 모른다"를 없앤다. 신규 `CollectionHeartbeat`(health 패키지): 매 수집 틱을 healthchecks.io 데드맨에 핑 — **`succeeded` 카운트로 판단**(status 아님). `PriceCollector`는 `collectionRunRepository.save(run)` **이후** `heartbeat.report(succeeded)` 1줄(additive fail-open, 수집 로직 0줄). 🔑 **핵심결정1**: `succeeded>0`→기본 URL / `==0`→`/fail`. status를 안 쓰는 건 **빈 워치리스트가 `0==items.size()`로 거짓 SUCCESS**를 기록하기 때문 — 저장소가 이미 아는 버그(`application-prod.yml:6-8`, 기존 우회 `initial-delay-ms:10000`은 타이밍 레이스)를 결정적 가드로 대체. 🔑 **핵심결정2(보안)**: ping URL은 시크릿(아는 자가 "정상" 위조→알림 영구침묵). `ResourceAccessException` 메시지에 URL이 박히므로 **예외 클래스명만 로깅**(메시지·스택트레이스 금지) — `backup-db.sh:78` 셸 규칙을 Java로. 둘 다 회귀 테스트로 고정. `MonitoringConfig`가 3초 전용 RestClient(@Bean, `ApiClientConfig` 미러 → MockRestServiceServer 단위테스트 가능). 설정: `application.yml` 빈 기본값=전역 비활성(dev/test/CI 무영향, prod만), compose·`.env.prod.example` 배선. **선택항목1**: `backup-db.sh die()`에 `/fail` 즉시 핑(발견 ~26h→즉시, `${PING_URL:-}` 가드+curl `2>/dev/null` URL 유출 차단). 🔑 **실행 중 버그**: `PriceCollectionIT`에 `@ActiveProfiles("test")` 누락→initial-delay 0→**실제 스케줄러가 부팅 즉시 빈 틱**→`@MockitoBean` 하트비트에 stray `report(0)` 섞임(TooManyActualInvocations). **로컬 mock 필드**로 전환해 차단(다른 collect IT 2개와 동일). 검증: build 그린(198통과/0실패), 신규 테스트 Heartbeat 5·PriceCollection 5·Resilience 4 전부, 추적파일 실 URL 0건. ⚠️ **미완(완료조건)**: 배포 후 **핑 도착+Discord 실제 알림 수신**을 눈으로 봐야 검증됨(§10, 초록불만으론 거짓안심), `/security-review` 실행. 앱 수집 로직 0줄. 미푸시 | 2026-07-18 | f873010, bd013b5, 9e8f36b, 945e151 | [260718-jrz-monitoring-alerting](./quick/260718-jrz-monitoring-alerting/) |
| 260717-i2l | **도메인 이관 커밋 #2 — 전환 확정**(loaket.kr 정착) — 2단계 컷오버 완료. 커밋 #1 배포로 loaket.kr 라이브·인증서·www 301 확인 후, 옛 duckdns 병행 서빙 종료 + 운영 도메인 참조 정착. **운영 5파일 교체**: `frontend/Caddyfile`(duckdns 임시 블록 **제거** → apex 공개 서빙 + www→apex 301 + :8081 tailnet만; (public) 스니펫 /admin 404·보안헤더 유지), `ci.yml`(스모크 URL 2줄 L174·176→loaket.kr), `.env.prod.example`, `runbook`(§3 DuckDNS 절차→가비아 A레코드 @·www 재작성 + SITE_ADDRESS·검증 curl 3곳), `README`(라이브 링크 5곳). **`.planning/** 16파일의 duckdns는 역사 기록이라 보존**(교체 안 함 — 그 시점 사실·결정). VM `.env.prod`는 이미 loaket.kr(무변경), DuckDNS 크론 실제 제거는 배포 후 사용자 작업. 검증: `caddy fmt` clean(Write 끝개행 누락→fmt --overwrite 정규화)+`validate` Valid(loaket.kr), `ci.yml` yq 4잡 파싱, tracked 파일 `lostark-tracker.duckdns.org` 잔존 **0**. 앱 코드 0줄. ⏸️ **커밋 생성·push 대기**(사용자 검토 후 지시) | 2026-07-17 | 0a5ce22 | [20260717-domain-cutover-c2](./quick/20260717-domain-cutover-c2/) |
| 260717-gvl | **도메인 이관 커밋 #1 — 듀얼 서빙 Caddyfile**(loaket.kr) — 2단계 컷오버의 1단계. `(public)` 스니펫으로 admin /404 + 보안헤더 7종을 묶어 `{$SITE_ADDRESS}`(신규 apex loaket.kr)와 `lostark-tracker.duckdns.org`에 **동일 적용**, `www.{$SITE_ADDRESS}`→apex **301 permanent**. duckdns 병행 서빙으로 신규 도메인 인증서 발급 실패에도 사이트 안 죽는 **롤백 여유**(전환 확정 시 커밋 #2에서 삭제). 🔑 배포 메커니즘상 Caddyfile은 web 이미지에 구워지고(`frontend/Dockerfile:13`) SITE_ADDRESS는 VM `.env.prod`(`compose:82`) → **하드 스톱**: SITE_ADDRESS가 duckdns인 채 배포되면 하드코딩 duckdns 블록과 **호스트 중복→Caddy 미기동**(사이트 전체 중단). 로컬 `caddy validate`(docker)로 결정적 증명 — loaket.kr=`Valid configuration`(exit 0) / duckdns=`ambiguous site definition`(exit 1). ci.yml 스모크 URL·README·runbook·.env.prod.example 도메인 교체는 **커밋 #2**로 미룸. 앱 코드 0줄. ⏸️ **커밋 생성·push 대기** — 사용자가 VM `.env.prod` `SITE_ADDRESS=loaket.kr` 변경 확인 후 push | 2026-07-17 | daa2f88 | [20260717-domain-cutover-c1](./quick/20260717-domain-cutover-c1/) |
| 260716-o5k | **아바타·모험의 서 실시간 시세 검색 페이지 2개**(거래소 카테고리 확장) — 인게임 거래소를 본떠 검색으로 시세를 찾는다. **저장 없는 실시간 조회**(GemService 패턴: 온디맨드+공유 토큰버킷+Redis 5분 캐시), 시계열 미저장·새 테이블 0·마이그레이션 0. 이벤트 상관 대상이 아니고 직업당 ~9k 아바타 저장은 순수 부하라 온디맨드가 맞음. 백엔드: `market` 패키지(searchMarket·MarketSearchService·MarketController) + `/api/market/{classes,adventure,avatar}`. 🔑 **정렬 화이트리스트가 핵심** — 로스트아크 API가 잘못된 Sort를 200으로 조용히 무시하므로(실측) min_price/recent_price×asc/desc만 받고 400. avatar class 필수·부위 화이트리스트. 프론트: /adventure(검색+정렬+페이지)·/avatar(직업 드롭다운30+부위 사이드바10+검색+정렬), 디바운스 300ms, TanStack+zod, 기존 토큰 재사용. 🔑 **라이브 검증에서 버그 2개** 잡음(안 돌려봤으면 못 잡음): ①응답 PascalCase 누출(@JsonProperty가 직렬화까지 PascalCase→zod 거부) → **@JsonAlias**로 입력만 별칭·출력 camelCase, IT에 원본 JSON 키 단언 추가 ②같은 id 한 페이지 중복(다른 가격 등록)→React key 충돌, index 조합. 검증: IT 17개 통과(build 그린, max_connections=300으로 커넥션 소진 완화), 라이브(로컬 새 백엔드 8095)로 직업선택→부위전환→검색→정렬방향→페이지 실동작·375px·콘솔 에러 0. 미배포(배포 백엔드는 옛 버전). 커밋 3개 | 2026-07-16 | fd78e2e | [20260716-market-search-pages](./quick/20260716-market-search-pages/) |
| 260716-nfh | **Loaket 브랜딩**(헤더 워드마크 + 파비콘) — 프로젝트명 Loaket(Lostark+Market) 확정. 헤더 "로스트아크 시세 트래커" 16px 텍스트 → **Loaket 워드마크**(Fredoka·그라데이션 초록 #4ade80→#15803d·30px) + 부제, 56→72px + 상하패딩. 사용자가 초안(카트로고+볼드) 실물 보고 조정: 로고 이미지 제거(카트는 탭 파비콘으로만), 폰트는 **후보 3종 실제 렌더 비교 후 선택**(추측 금지 원칙). 🔑 CSP `font-src 'self'`라 외부 폰트 CDN 불가 → Inter가 이미 쓰던 @fontsource self-host 선례 따라 @fontsource/fredoka 번들(woff2 16.5KB, 런타임 네트워크 0). 그라데이션은 background-clip:text+transparent+단색 fallback. 파비콘: 원본 1254px PNG(저장소 미포함)를 Pillow로 32/180 리사이즈, 낡은 📈 favicon.svg 제거. 검증(라이브 1440+375): 헤더 72px·Fredoka 적용·그라데이션·브랜드→/, 375px 회귀 없음(겹침·가로스크롤 없음·부제 <sm 숨김), build 그린. 프론트만·백엔드 0줄 | 2026-07-16 | 0668826 | [20260716-loaket-branding](./quick/20260716-loaket-branding/) |
| 260716-k35 | **/admin을 tailnet 전용으로** — 구글 세이프브라우징이 사이트를 사회공학(피싱)으로 분류했고 방아쇠가 **평판 없는 무료 DDNS 위의 공개 비밀번호 폼**이었다. 도메인 이전 전에 제거(안 그러면 방아쇠를 들고 이사). Caddy 리스너 2개: `{$SITE_ADDRESS}`는 `/admin`·`/api/admin/*` **404**(403 아님 — 403은 존재를 알림), `http://:8081`은 tailnet 전용 전부. `(site)` 스니펫으로 공유해 갈라짐 방지. 🔑 **최대 위험 가정을 실측**: `/api/admin/*`이 `/api/*`보다 먼저 잡히는가 — 틀리면 "막은 줄 알고 뚫림". 목 백엔드+실제 Caddy로 확인 → 공개 `/api/admin/events` **404**, `/api/items` **200 프록시**. 그 과정에 `handle`이 경로 인자를 **하나만** 받는 것도 발견(원래 문법은 파싱 실패). 포트는 0.0.0.0 bind — tailnet IP 직접 bind는 재부팅 시 Docker가 Tailscale보다 먼저 떠서 **Caddy 통째 미기동 → 공개 사이트까지 죽음**. 3겹 방화벽(OCI 8081 미추가=실질 차단 + DOCKER-USER + INPUT). **런북 정정 2건(내 오판)**: `iptables -L`은 `in` 인터페이스를 안 보여줘 `-i lo`가 "전체 허용"으로 오독됨(`-v` 필수 명시, 실측 1번=tailscale0:22·5번=lo로 정상) / "80·443 INPUT은 Docker엔 무의미"라 단정했으나 카운터 148·89로 0이 아님(userland-proxy가 INPUT을 탐) → 양쪽에 넣어야 함. 한계: 관리자 JS는 번들에 남음(경로만 차단, 다만 크롤러는 폼 못 보고 API가 404라 동작 전부 실패). 검증: 헤더 7종·Server 제거·404 본문 0바이트·경계값 과잉차단 없음·validate/fmt·포트 80/443/8081. 미검증: 실제 배포·iptables·OCI(사용자). 앱 코드 0줄 | 2026-07-16 | 9ab4bc4 | [20260716-admin-tailnet-only](./quick/20260716-admin-tailnet-only/) |
| 260716-jk4 | 런북 §11 백업 문서를 **실제 운영 상태에 맞게 정정 7건**(문서 1파일·코드 0줄·운영 무변경) — OCI 구축이 실제로 끝나며 내 문서 오류가 드러남. 백업 문서가 틀린 건 백업이 없는 것과 비슷한 위험(재구축·장애 복원 때 틀린 길로 감). ①경로 5곳 `~/lostark`→`/opt/lostark-price-tracker`(§5·§9·§10과 통일) — §11.4 표가 **틀린 경로를 확인하라고 시키고 있었음** ②수명주기 **규칙 2개**(latest+previous)로 재기술 — 버저닝 ON이면 최신 삭제가 이전 버전으로 내려앉을 뿐이라 두 번째 없인 계속 쌓임(연 ~126MB, 과장 금지). 실수명 30일 아닌 **최대 60일**(~21MB) 명시 ③**IAM 정책** 신설 — 없으면 규칙이 Enabled로 보이는데 조용히 미실행 ④복원에 **`--exit-on-error`** — pg_restore는 에러 무시하고 종료코드 0으로 끝날 수 있어 일부 복원본을 통과시킴 ⑤cron 패키지 기본 미설치 ⑥행 수 **드리프트 경고 + 실측 8행 표**(price_snapshot·collection_run은 증가가 정상) ⑦PAR 만료 2028-07-16 / 알림 2028-06-16. 검증: 틀린 경로 잔존 0건, 7건 매치, git 전체 이력 토큰 0건 | 2026-07-16 | 1eee3ef | [20260716-runbook-backup-corrections](./quick/20260716-runbook-backup-corrections/) |
| 260716-h1e | DB 자동 백업 메커니즘(스크립트+문서) — 되돌릴 수 없는 유일한 자산이 PG 시계열(보석은 이력 API 부재로 영구 소실·가격은 ~2주 한계·이벤트/쿠폰은 수기)이라 **PG만** 백업(Redis=캐시·Caddy=재발급 제외). `pg_dump -Fc` 무중단 → **쓰기 전용 버킷 PAR** + curl PUT(OCI CLI·키를 VM에 안 둠 → VM 털려도 기존 백업 못 읽고 못 지움) → 성공 시에만 ping(**데드맨 스위치**). 보관은 서버측 수명주기 30일 — 스크립트에 삭제 로직 없음. 🔑 `pg_restore -l /dev/stdin` 검증이 **정상 덤프까지 거부**(커스텀 포맷은 seek 필요, 파이프 불가) → 컨테이너 내 실파일 검사로 수정, 안 돌려봤으면 매일 백업 실패했을 버그. 검증(dev 실데이터): 일회용 PG16 복원 후 **8테이블 COUNT(*) 전부 일치**(n_live_tup 추정치는 9109로 오보·실제 6914), 잘림/0바이트/쓰레기 전부 거부, 목 PAR로 전 구간 통과, **404 시 종료1·PUT 0·ping 0**(실패를 성공으로 보고 안 함), PAR URL 로그 미출현, shellcheck 0. 미검증: 실제 OCI 업로드·ping·cron(사용자 설정 후). 앱 코드 0줄 | 2026-07-16 | e9c00e2 | [20260716-db-backup-object-storage](./quick/20260716-db-backup-object-storage/) |
| 260716-e8k | 카테고리 sticky 가림 + 물품 박스 8칸(배포 후 피드백 2건) — 증상은 "각인서가 스크롤을 안 따라온다"였으나 **따라오는데 불투명 상단바 밑에 깔린** 것: `TopNav`가 `sticky top-0 h-14`(56) `z-40`인데 nav는 `lg:top-6`(24)에 멈춰 h2(24~51px)가 100% 가림 → `lg:top-20`(80=56+24). z-index로 덮지 않음(nav가 상단바를 가리는 새 버그가 됨). 박스는 `lg:h-[468px]`(6칸) → **`lg:max-h-[628px]`**(8칸=8×68+7×12): 고정이면 6개짜리(재련보조·아크그리드젬)에 160px 빈칸 → **사용자 결정: 내용만큼 축소**. 상수 `ITEM_BOX_HEIGHT`→`ITEM_BOX_MAX_HEIGHT` 및 "fixed-height" 주석 정정. 검증: 6개 카테고리 전수(11/7/11/8/6/6 → 628/548/628/628/468/468, **잘린 카드 0**, 8칸 경계 정확), 헤더 가림 0px, 375px 회귀 없음(둘 다 `lg:` 전용), build 그린. 프론트 CSS만·Core Value 0줄 | 2026-07-16 | 8dfc06f | [20260716-dashboard-sticky-box-height](./quick/20260716-dashboard-sticky-box-height/) |
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
- 🚀 **v1.5 시세 범위 확장 + UX 진행(1/4, 2026-07-14):** 설계 스펙(`9cd8393`) + 로드맵 Phase 20~24 세팅. ✅ **Phase 20(이벤트 +3) 완료** — NEW_CLASS/NEW_RAID/GENERAL_PATCH additive, 백엔드 IT+프론트 build 그린, 마커색 dataviz 7색 CVD PASS. 남은: 21(재료 스파이크) → 22(재료 추적) → 23(대시보드 3열). Phase 24(경매장 보석)는 v1.6 후보. 설계: `docs/specs/2026-07-14-v1.5-market-scope-ux-design.md`.
- ✅ **Phase 21(재련 재료 스파이크) 실행 완료(2026-07-14):** `21-SPIKE-FINDINGS.md` — 거래소 카탈로그 실측(50010 기본·50020 추가/상급재련·230000 아크그리드젬), Id·아이콘 잠금. 🔑 **아크그리드젬=거래소**(경매장 아님) → Phase 24는 보석만.
- ✅ **Phase 22(재련 재료 추적 편입) 완료(2026-07-14) + quick-260714 교정:** 재련기본 7·상급재련·아크그리드젬 6 WatchlistSeeder 편입. SyntheticDemoData 무변경(동적 로드), 수집/캐시/event-impact 0줄. `./gradlew build` 그린. 22b: [19-20] DESC 스파이크로 실측·편입. **🔧 quick-260714 교정(로아 유저 지적):** 업화 계열은 일반 재련 성공률 보조, 숨결(용암/빙하)은 상급·일반 재련 겸용 → **둘 다 `재련보조`로 재분류**. 진짜 상급재련 전용 재료=**장인의 야금술/재봉술 1~4단계 8종**(스파이크 실측 편입) → **워치리스트 22→49, MATERIAL 31, item_group 6종**(상급재련=장인 책 8, 재련보조=숨결 2+업화 4).
- **다음:** **Phase 23(대시보드 3열 카테고리 레이아웃)** — 신규 UI 레이아웃이라 **UI-SPEC 선행**(`/gsd-ui-phase`) → plan → execute. 좌 카테고리 필터(재료 31종은 item_group 6종[강화재료/재련재료/상급재련/재련보조/아크그리드젬/각인서] 세분 후보)/중앙 물품/우 소식, 모바일 칩. gstack `/browse`·`/design-review` QA.
- 불변 제약 상시 가드: 수집/캐시/event-impact/서빙 **로직 0줄** — v1.5의 C(추적 확대)는 워치리스트 **데이터만** 늘림(같은 수집기·레이트리밋·스키마). 경매장(AUCTIONS)은 "현재가 둘러보기"로 한정. 실 시크릿은 VM `.env.prod`에만.
