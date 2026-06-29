---
phase: 12-api-spike-data-lock
plan: 01
subsystem: api
tags: [lostark-markets-api, spike, restclient, data-lock, curation, icon-url, category-code]

requires:
  - phase: 01-foundation-task-0
    provides: LostarkSpikeClient/MarketsApiSpikeTest spike-then-lock infra, leaf-CategoryCode rule, Icon field observation, rate-limit headers
provides:
  - 실측 CategoryCode (각인서 40000 leaf, 융화재료 50010 재련 재료)
  - Icon URL 필드 확정 (필드명 Icon, CDN cdn-lostark.game.onstove.com/efui_iconatlas/use/)
  - 6필드 큐레이션 15개 (융화재료 4 MATERIAL + 딜러 9 DEALER + 서포터 2 SUPPORT), 전부 실측 external_item_id/icon_url
  - 각인서 아이콘 identical(등급 내 동일) → 라벨 병기 결정 + fallback 전략
affects: [13-backend-enrichment-seed, 14-frontend-icons-fallback-docs]

tech-stack:
  added: []
  patterns: [spike-then-lock 재사용, CategoryCode 파라미터화 검색, 콘솔출력→마스킹 findings 캡처]

key-files:
  created:
    - .planning/phases/12-api-spike-data-lock/12-SPIKE-FINDINGS.md
  modified:
    - src/main/java/com/lostark/tracker/spike/LostarkSpikeClient.java
    - src/test/java/com/lostark/tracker/spike/MarketsApiSpikeTest.java

key-decisions:
  - "각인서 40000은 그 자체가 leaf(Subs:[]); 융화재료는 50010(재련 재료)에서 검색됨 — 50020 0건 실측"
  - "각인서 아이콘은 등급 내 동일(유물=use_9_25)이라 라벨 병기로 식별(D-06); 융화재료는 아이콘 서로 구별됨"
  - "큐레이션 15개 잠금(융화재료 4 + 딜러 9 + 서포터 2). 만개 0건 보류, 구원 제외(실재 각인 아님) — 휴먼 비준"

patterns-established:
  - "스파이크 결과 캡처: @Disabled 테스트를 JUnit DisabledCondition 비활성화로 1회 실행 → build/test-results XML의 system-out에서 마스킹 발췌(raw 디스크 미덤프)"
  - "ItemName 부분검색으로 각인서 유물 등급 행만 골라 큐레이션 잠금"

requirements-completed: [SPIKE-01, SPIKE-02, SPIKE-03, SPIKE-04, SPIKE-05]

duration: ~55min
completed: 2026-06-29
---

# Phase 12: API Spike + Data Lock Summary

**실 markets API 1회 실측으로 각인서(40000)·융화재료(50010) CategoryCode와 Icon 필드를 확정하고, 6필드 큐레이션 15개(role_group 3군)·라벨병기·fallback을 키 없이 재현 가능한 findings로 잠금**

## Performance

- **Duration:** ~55 min (사용자 로컬 JWT 스파이크 실행 2회 포함)
- **Completed:** 2026-06-29
- **Tasks:** 3 (auto 2 + blocking 휴먼 체크포인트 1)
- **Files modified:** 3 (client + test + findings)

## Accomplishments
- **CategoryCode 실측:** 각인서 `40000`(leaf, Subs 없음), 융화재료 `50010`(재련 재료); 50020은 0건으로 배제
- **Icon 필드 확정:** 필드명 `Icon`, CDN `cdn-lostark.game.onstove.com/efui_iconatlas/use/` — 모든 응답에 존재
- **6필드 큐레이션 15개:** 융화재료 4(상급/최상급 오레하·아비도스·상급 아비도스, MATERIAL) + 딜러 9 + 서포터 2(각성·전문의), 전부 실측 `external_item_id`·`icon_url`
- **아이콘 구별:** 유물 각인서 11종 전부 `use_9_25.png` 동일 → 라벨 병기(D-06); 융화재료 4종은 아이콘 구별됨
- **키-안전:** findings·커밋에 JWT/가격/식별자 0건(grep 확인), 스파이크 `@Disabled`로 CI 미실행

## Task Commits

1. **Task 1: spike 클라이언트·테스트 확장 (/markets/options + 카테고리 파라미터화)** - `2784345` (feat)
2. **Task 2 (code): 각인서 이름검색 + 융화재료 50010 잠금 + @Disabled 복원** - `7d60d39` (test)
3. **Task 2 (findings): CategoryCode/Icon + 15개 6필드 큐레이션 잠금** - `bb243a4` (docs)
4. **Task 3: blocking 휴먼 체크포인트 — "비준 — 큐레이션 잠금"** (코드 산출물 없음, 큐레이션 비준)

## Files Created/Modified
- `src/main/java/com/lostark/tracker/spike/LostarkSpikeClient.java` - `getMarketOptions()` GET + `searchMarketItems(int,String)` 오버로드 추가(기존 메서드·키 정규화 보존)
- `src/test/java/com/lostark/tracker/spike/MarketsApiSpikeTest.java` - 옵션 조회 + 각인서(40000)/융화재료(50010) 검색 + Id/Name/Grade/Icon·아이콘 구별 출력
- `.planning/phases/12-api-spike-data-lock/12-SPIKE-FINDINGS.md` - 6필드 큐레이션 표·CategoryCode·Icon·fallback·키안전

## Decisions Made
- **각인서 큐레이션 방식 보정:** 40000 전체검색(CURRENT_MIN_PRICE ASC)은 저가 전설 각인서만 잡혀 D-02 균형 큐레이션에 부적합 → 딜러/서포터 핵심 각인서를 `ItemName`으로 지정 검색해 유물 등급 행을 잠금
- **만개 보류 / 구원 제외:** 만개(도화가 서포터각)는 ItemName 검색 0건 → 보류(휴먼 비준 시 재실측 가능). 구원은 실재 서포터 직업각인 아님(각성/만개/전문의 3종뿐) → 제외
- **운명 융화재료:** 범위 밖 유지(Deferred)

## Deviations from Plan
None - 플랜의 spike-then-lock 흐름을 그대로 따름. 각인서 이름검색 추가는 D-02(딜러/서포터 균형) 충족을 위한 계획된 검색 방식 구체화이며, planner가 명시한 "융화재료는 ItemName으로 후보명 질의" 패턴을 각인서에도 적용한 것(스코프 내).

## Issues Encountered
- **사용자 첫 실행 401 Unauthorized:** JWT 중간 공백/줄바꿈 + Gradle 데몬의 옛 env 캐시가 원인 → 새 셸에서 순수 JWT 한 줄 설정 + `--stop`/`--no-daemon`으로 해소(Task 0 401 선례와 동일 계열)
- **통과 테스트의 stdout 미표시:** Gradle이 성공 테스트의 System.out을 콘솔에 안 보임 → `build/test-results/.../*.xml`의 `<system-out>`에서 직접 추출(복사 불필요)

## User Setup Required
실측 1회에 한해 로컬 env `LOSTARK_API_KEY`(본인 JWT) 필요했고 완료됨. **이후 Phase 13·14는 키 불필요**(findings 상수만 소비).
⚠️ 스파이크 중 대화에 노출된 JWT 키는 **포털에서 재발급 권장**(.env는 gitignored·추적 0이라 git 유출은 없음).

## Next Phase Readiness
- **Phase 13 언블록:** 6필드 표(external_item_id/display_name/category_code/icon_url/item_group/role_group)를 V4 컬럼·TrackedItem·4개 read DTO·WatchlistSeeder/SyntheticDemoData에 전사 가능
- **Phase 14 언블록:** Icon URL·라벨병기·역할색 fallback·role_group(DEALER/SUPPORT/MATERIAL) 배지 계약 확정
- **잔여 도메인 결정:** 만개 포함 여부(필요 시 1회 재실측), 운명 융화재료(현재 보류) — Phase 13 착수 전 선택 가능

---
*Phase: 12-api-spike-data-lock*
*Completed: 2026-06-29*
