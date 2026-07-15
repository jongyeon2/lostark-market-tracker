---
quick_id: 260715-nav
status: complete
date: 2026-07-15
commits: [5d43d6f, 877e606]
---

# Quick 260715-nav SUMMARY — 대시보드 카테고리 교정 (요청 1번 + 2번)

## 무엇을 했나

**1. 강화재료 폐지 → 재련재료 통합 (요청 2번)** — 융화재료는 재련에 반드시 들어가므로 별도 카테고리로
분리할 이유가 없다. 재련기본 9 + 융화 2 = **재련재료 11**.

이 작업의 핵심은 **Flyway V7**이다. `WatchlistSeeder`는 `findByExternalItemId(...).orElseGet(insert)` —
"Phase 4 admin CRUD가 나중 편집을 소유"하므로 **기존 행을 절대 갱신하지 않는다**. 아비도스 융화 재료는
운영 DB에 `id=1(6861012)`, `id=2(6861013)`로 이미 존재하므로, 시더 소스만 고쳤다면 운영에는 아무 일도
일어나지 않고 프론트에서 `강화재료` leaf만 사라져 **두 품목이 어느 leaf에도 안 걸린 채 대시보드에서
조용히 증발**했을 것이다.

- `V7__move_fusion_material_to_refine_group.sql`: `UPDATE ... WHERE item_group='강화재료'`.
  **id가 아니라 group 기준** — 폐지 대상이 카테고리 자체이므로, 시더에 없는 레거시 행까지 함께 옮겨
  어느 환경에서도 그룹이 확실히 비고, 재실행해도 대상 0건이라 멱등하다.
- `WatchlistSeeder`: 아비도스 2행 `재련재료`, 헤더/javadoc 주석 갱신.
- `WatchlistSeederIT`: **item_group 카운트 단언 신설** — `doesNotContainKey("강화재료")` + 재련재료 11 ·
  상급재련 8 · 재련보조 6 · 아크그리드젬 6 · 각인서 18. 대시보드 재료 leaf가 `item_group`에서 그대로
  파생되므로 이 카운트가 곧 사용자가 보는 카테고리다.

**2. 각인서 헤더: 구분선 + 아이콘 (요청 1번)** — 그룹과 leaf가 구분선 없이 붙어 있어 같은 층위로 읽혔다.

- `CategoryGroup` `'각인'` → `'각인서'`(실제 품목이 각인서).
- 그룹 헤더에 `border-b` + 각인서 대표 아이콘.
- `groupIconUrl(items, group)`: 아이콘을 **로드된 데이터에서 파생**(그룹 첫 멤버의 `iconUrl`) — CDN URL
  하드코딩이 아니라 시드를 자동으로 따라간다. 각인서만 대표 아이콘이 성립하는 이유는 18종이 전부 같은
  `use_9_25.png`(등급 글리프)를 쓰기 때문이고, 재료는 반대로 품목마다 아이콘이 달라 어느 멤버도 나머지를
  대표할 수 없다 → 재료 그룹은 `null`.
- `MATERIAL_GROUPS`에서 `'강화재료'` 제거, `DashboardPage`가 `items={sorted}` 전달.

## 발견 (계획에 없던 것)

**dev DB 레거시 행** — dev에는 시더에 없는 오레하 융화 재료 2종(`6861009`, `6861011`)이 `강화재료`에 남아
있었다(과거 seed 잔재, insert-only라 지워진 적 없음). 운영에는 없다. group 기준 마이그레이션이라 이들도
함께 재련재료로 이동했고, 오레하도 융화 재료이므로 이번 결정과 일관된다. 그래서 **dev는 재련재료 13,
운영은 11**이 된다(각인서도 dev 19 / 운영 18 — 레거시 정밀 단도 1종).

## 검증

- `./gradlew build` **BUILD SUCCESSFUL** (V7 + 시더 IT 포함 전체 스위트).
- `npm run build` (tsc -b + vite) **그린**.
- **dev DB 실측** (`psql`): Flyway `now at version v7`, `강화재료` **0건**, 재련재료 13 · 상급재련 8 ·
  재련보조 6 · 아크그리드젬 6 · 각인서 19.
- **Playwright 라이브(1280px)**: 좌측 nav = "각인서 [아이콘]" + 구분선 + 딜러 12/서포터 7,
  "재료" + 구분선 + 재련재료 13/상급재련 8/재련보조 6/아크그리드젬 6. **강화재료 leaf 부재** 확인.
  **재련재료 클릭 → 아비도스 융화 재료·상급 아비도스 융화 재료가 최상단 노출** (요청 2번 충족).
  한글 깨짐 없음. 스크린샷 `quickA-nav-taxonomy.png`, `quickA-refine-with-abidos.png`.

## 편차 (기동 오류 → 분석·수정)

백엔드 재기동이 exit 1로 실패했다. 로그 분석 결과 **포트 8080 already in use** — 앞선 `TaskStop`이 Gradle
래퍼만 종료하고 fork된 `java.exe`가 살아남은 탓이었다. 코드 문제가 아니며, 고아 프로세스를 정리 후 정상
기동했다. Flyway는 웹서버 바인딩 이전에 실행되므로 V7 자체는 그 시도에서 이미 적용됐다.

## Core Value 가드

수집·캐시·event-impact **0줄**. V7은 `tracked_item.item_group`(표시용 메타) 재분류만 — 가격 데이터·스키마
구조 무변경.

## 커밋

- `5d43d6f` — V7 + WatchlistSeeder + WatchlistSeederIT
- `877e606` — categories.ts + CategoryNav.tsx + DashboardPage.tsx
