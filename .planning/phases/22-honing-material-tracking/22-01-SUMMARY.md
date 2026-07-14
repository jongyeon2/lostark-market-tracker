---
phase: 22-honing-material-tracking
plan: 01
status: complete
requirements: [MKT-02]
commits: []
---

# 22-01 SUMMARY — 재련 재료 추적 편입 (워치리스트 22→39)

## 무엇을 했나

Phase 21에서 비준한 재련 재료 큐레이션을 워치리스트에 편입해 수집기가 10분 시계열을 쌓게 했다. **Core Value 로직 0줄** — 순수 데이터(워치리스트) 확대.

- **`WatchlistSeeder`**: WATCHLIST에 **신규 17종** 추가(22→39). 신규 = 비준 19종 − 이미 추적 중이던 파괴/수호석 결정 2(66102007/66102107, 재추가 안 함).
  - 재련 기본 7 (`category=50010`, `item_group=재련재료`): 운명의 파괴석·수호석(base) · 운명의 돌파석·위대한 돌파석 · 파편 주머니 소/중/대
  - 상급 재련 4 (`category=50020`, `item_group=상급재련`): 용암/빙하의 숨결 · 야금술/재봉술 업화[15-18](유물)
  - 아크그리드 젬 6 (`category=230000`, `item_group=아크그리드젬`, 영웅): 질서 3(안정/견고/불변) + 혼돈 3(침식/왜곡/붕괴)
  - 전 항목 `role_group=MATERIAL`, 실측 Id·icon(`21-SPIKE-FINDINGS`). 클래스 주석 22→39·item_group 목록 갱신.
- **`SyntheticDemoData` 무변경**: `findByActiveTrue()` 동적 로드라 신규 17종이 seed 프로파일에서 자동으로 synthetic snapshot을 받는다(소스 0줄).
- **`PriceCollector`(수집)·캐시·event-impact 무변경**: 수집기도 활성 워치리스트를 읽으므로 신규 17종이 자동으로 10분 틱에 편입.
- **`WatchlistSeederIT` 갱신**: hasSize 22→39, role `MATERIAL 4→21`(DEALER 11·SUPPORT 7 불변), itemGroup isIn에 `상급재련`·`아크그리드젬` 추가, 멱등 count 39, 신규 그룹 샘플 단언 2건(아크그리드젬 67400003·상급재련 66112551).

## 검증

- **`./gradlew build`(Testcontainers Postgres+Redis) BUILD SUCCESSFUL** — 전체 스위트 그린.
- `WatchlistSeederIT`가 증명: (1) 39종 시드 + role 분포(MATERIAL 21/DEALER 11/SUPPORT 7), (2) 신규 재료가 `SyntheticDemoData`로 keyless synthetic snapshot 획득(SEED-02, source 0줄), (3) 멱등(재실행 39 유지).
- **레이트리밋**: 39종/10분 틱 ≪ 100/min(품목당 ~1콜). **DB**: 39×144행/일 ≈ 561만행/년 — 무료 VM 여유.
- **멱등 upsert**(external_item_id): 기존 파괴/수호석 결정·각인서 그대로 유지, 재기동 중복 0.

## 후속 (Phase 23 연결)

신규 재료는 `role_group=MATERIAL`이라 현행 대시보드에선 "재료"로 묶여 21종이 된다. **Phase 23(대시보드 3열)에서 `item_group`(강화재료/재련재료/상급재련/아크그리드젬)으로 세분**하면 자연스럽다 — 이번에 item_group을 그에 맞게 부여해 둠.

## 커밋

- (code) WatchlistSeeder.java(+17종) · WatchlistSeederIT.java(단언 갱신)
- (planning) 22-01-PLAN·SUMMARY · ROADMAP · STATE
