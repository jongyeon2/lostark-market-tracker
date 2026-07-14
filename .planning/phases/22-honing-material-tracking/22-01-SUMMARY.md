---
phase: 22-honing-material-tracking
plan: 01
status: complete
requirements: [MKT-02]
commits: []
---

# 22-01 SUMMARY — 재련 재료 추적 편입 (워치리스트 22→41)

## 무엇을 했나

Phase 21에서 비준한 재련 재료 큐레이션을 워치리스트에 편입해 수집기가 10분 시계열을 쌓게 했다. **Core Value 로직 0줄** — 순수 데이터(워치리스트) 확대. *(22b: 사용자 요청으로 상급재련 [19-20] 2종 추가 실측·편입 → 총 신규 19종.)*

- **`WatchlistSeeder`**: WATCHLIST에 **신규 19종** 추가(22→41). 신규 = 비준 큐레이션 17(19−이미 추적 중이던 파괴/수호석 결정 2) + 22b 상급재련 [19-20] 2.
  - 재련 기본 7 (`category=50010`, `item_group=재련재료`): 운명의 파괴석·수호석(base) · 운명의 돌파석·위대한 돌파석 · 파편 주머니 소/중/대
  - 상급 재련 6 (`category=50020`, `item_group=상급재련`): 용암/빙하의 숨결 · 야금술/재봉술 업화 **[15-18]·[19-20]**(유물)
  - 아크그리드 젬 6 (`category=230000`, `item_group=아크그리드젬`, 영웅): 질서 3(안정/견고/불변) + 혼돈 3(침식/왜곡/붕괴)
  - 전 항목 `role_group=MATERIAL`, 실측 Id·icon(`21-`/`22b-SPIKE`). 클래스 주석 22→41·item_group 목록 갱신.
- **`SyntheticDemoData` 무변경**: `findByActiveTrue()` 동적 로드라 신규 17종이 seed 프로파일에서 자동으로 synthetic snapshot을 받는다(소스 0줄).
- **`PriceCollector`(수집)·캐시·event-impact 무변경**: 수집기도 활성 워치리스트를 읽으므로 신규 17종이 자동으로 10분 틱에 편입.
- **`WatchlistSeederIT` 갱신**: hasSize 22→41, role `MATERIAL 4→23`(DEALER 11·SUPPORT 7 불변), itemGroup isIn에 `상급재련`·`아크그리드젬` 추가, 멱등 count 41, 신규 그룹 샘플 단언 2건(아크그리드젬 67400003·상급재련 66112551).

## 검증

- **`./gradlew build`(Testcontainers Postgres+Redis) BUILD SUCCESSFUL** — 전체 스위트 그린.
- `WatchlistSeederIT`가 증명: (1) 41종 시드 + role 분포(MATERIAL 23/DEALER 11/SUPPORT 7), (2) 신규 재료가 `SyntheticDemoData`로 keyless synthetic snapshot 획득(SEED-02, source 0줄), (3) 멱등(재실행 41 유지).
- **레이트리밋**: 41종/10분 틱 ≪ 100/min(품목당 ~1콜). **DB**: 41×144행/일 ≈ 590만행/년 — 무료 VM 여유.
- **22b [19-20] 실측**: `captureAdvancedHoningTiers()`(DESC 검색)로 `야금술/재봉술 : 업화 [19-20]`(66112553/66112554, 유물) 확인·편입. 별개 변형 "강화 야금술/재봉술 : 업화 [19-20]"(66112555/66112556)도 존재(미편입 — 필요 시 추가).
- **멱등 upsert**(external_item_id): 기존 파괴/수호석 결정·각인서 그대로 유지, 재기동 중복 0.

## 후속 (Phase 23 연결)

신규 재료는 `role_group=MATERIAL`이라 현행 대시보드에선 "재료"로 묶여 21종이 된다. **Phase 23(대시보드 3열)에서 `item_group`(강화재료/재련재료/상급재련/아크그리드젬)으로 세분**하면 자연스럽다 — 이번에 item_group을 그에 맞게 부여해 둠.

## 커밋

- (code) WatchlistSeeder.java(+17종) · WatchlistSeederIT.java(단언 갱신)
- (planning) 22-01-PLAN·SUMMARY · ROADMAP · STATE
