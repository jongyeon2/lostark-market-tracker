---
quick_id: 260715-nav
slug: nav-category-taxonomy
date: 2026-07-15
mode: quick-full (--validate)
description: 대시보드 카테고리 교정 — 각인서 구분선/아이콘 + 강화재료 폐지·재련재료 이관 (요청 1번·2번)
must_haves:
  truths:
    - "운영 DB의 기존 융화재료 행이 실제로 재련재료로 옮겨진다 — 시더는 insert-if-absent라 소스 수정만으론 운영에 아무 일도 일어나지 않는다."
    - "강화재료 leaf는 사라지고, 아비도스 융화 재료 2종은 재련재료를 눌렀을 때 나온다 — 어느 leaf에도 안 걸려 증발하면 안 된다."
    - "각인 그룹 헤더는 '각인서'로 읽히고, 헤더 바로 아래 구분선이 그룹과 leaf를 시각적으로 분리한다."
    - "각인서 헤더의 아이콘은 데이터에서 파생된다(그룹 첫 아이템의 iconUrl) — 하드코딩 URL 금지."
    - "재료 그룹은 아이콘이 없다 — 아이템마다 아이콘이 달라 대표 아이콘이 성립하지 않는다."
  artifacts:
    - src/main/resources/db/migration/V7__move_fusion_material_to_refine_group.sql
    - src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java
    - src/test/java/com/lostark/tracker/collect/WatchlistSeederIT.java
    - frontend/src/features/dashboard/categories.ts
    - frontend/src/features/dashboard/CategoryNav.tsx
  key_links:
    - src/main/resources/db/migration/V6__add_item_daily_stats.sql  # 직전 마이그레이션 번호
    - frontend/src/features/_shared/roleGroup.ts
---

# Quick 260715-nav — 대시보드 카테고리 교정 (요청 1번 + 2번)

## 배경 (실측)

**시더는 기존 행을 절대 갱신하지 않는다.** `WatchlistSeeder.run()`은
`findByExternalItemId(...).orElseGet(insert)` — "Phase 4 admin CRUD가 나중 편집을 소유"하기 때문에
의도적으로 insert-if-absent다. 따라서 시더 소스의 `item_group`만 고치면 **운영에는 아무 일도 일어나지 않고**,
프론트에서 `강화재료` leaf만 사라져 **아비도스 2종이 어느 leaf에도 안 걸려 조용히 증발**한다.

운영 실측(2026-07-15): 아비도스 2종은 `id=1(6861012)`, `id=2(6861013)`로 `item_group='강화재료'` 상태.
dev DB에는 추가로 **시더에 없는 레거시 오레하 융화 재료 2종**(`6861009`, `6861011`)이 같은 그룹에 남아 있다
(과거 seed 잔재 — insert-only라 지워진 적이 없다).

## 결정

**마이그레이션은 id가 아니라 그룹 기준**: `WHERE item_group='강화재료'`.
`강화재료`라는 카테고리 자체를 폐지하는 것이 목표이므로 "그 그룹에 있는 걸 전부 옮긴다"가 정확하다.
- 운영: 아비도스 2종 이동 → `강화재료` 0건 → leaf 자동 소멸(deriveCategories 빈-숨김).
- dev: 레거시 오레하 2종도 함께 이동 → dev에서도 `강화재료` leaf 소멸. 오레하도 융화 재료이므로
  "융화재료는 재련재료 소속"이라는 이번 결정과 일관된다.
- 멱등: 재실행해도 대상 0건.

## Tasks

### Task 1 — Flyway V7 + 시더 + IT (백엔드)

**files:**
- `src/main/resources/db/migration/V7__move_fusion_material_to_refine_group.sql` (신규)
- `src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java` (수정)
- `src/test/java/com/lostark/tracker/collect/WatchlistSeederIT.java` (수정)

**action:**
1. V7: `UPDATE tracked_item SET item_group='재련재료' WHERE item_group='강화재료';` +
   왜 필요한지(시더 insert-only) 주석.
2. 시더: 아비도스 2행 `강화재료`→`재련재료`, 헤더 주석 갱신(재련재료 9→11, 그룹 6종→5종).
3. IT: `isIn(...)`에서 `강화재료` 제거, 아비도스 샘플의 기대 itemGroup을 `재련재료`로,
   그룹 카운트 갱신.

**verify:** `./gradlew build` 그린
**done:** 신규 DB든 기존 DB든 `강화재료` 0건, 아비도스 2종이 `재련재료`.

### Task 2 — 각인서 헤더(구분선 + 아이콘) + 강화재료 leaf 제거 (프론트)

**files:**
- `frontend/src/features/dashboard/categories.ts` (수정)
- `frontend/src/features/dashboard/CategoryNav.tsx` (수정)

**action:**
1. `categories.ts`: `MATERIAL_GROUPS`에서 `'강화재료'` 제거. `CategoryGroup` `'각인'`→`'각인서'`.
   `groupIconUrl(items, group)` 신설 — `CATEGORY_DEFS`로 그룹 소속을 판정해 첫 매칭 아이템의
   `iconUrl` 반환. 각인서만 대표 아이콘이 성립(18종 동일 아이콘)하므로 재료는 `null`.
2. `CategoryNav.tsx`: `GROUP_ORDER` `['각인서','재료']`. 그룹 헤더에 `border-b`(헤더 바로 아래 구분선) +
   아이콘(`groupIconUrl`이 null이 아니면 `<img>`, alt=""는 옆 텍스트가 라벨을 이미 제공).
   `items` prop 추가(아이콘 파생에 필요).
3. `DashboardPage.tsx`: `CategoryNav`에 `items={sorted}` 전달.

**verify:** `npm run build` 그린
**done:** 좌측 nav = "각인서 [아이콘]" + 구분선 + 딜러/서포터, "재료" + 구분선 + 재련재료(11 포함 아비도스).

### Task 3 — 검증 체크포인트

**name:** 빌드 + 라이브 렌더 검증
**action:** `./gradlew build`·`npm run build` 그린 확인 후 백엔드 재기동(V7 적용 + 시더 재실행)하고
Playwright로 1280px 렌더 확인 — 각인서 헤더 아이콘·구분선, 강화재료 leaf 부재, 재련재료 클릭 시
아비도스 노출, 한글 깨짐 없음. 빌드/기동 오류 시 로그 분석 후 수정.
**verify:** Playwright 스냅샷 + 스크린샷 + `psql`로 item_group 실황
**done:** 의도대로 렌더 + DB 실황 일치.

## Core Value 가드

수집·캐시·event-impact **0줄**. V7은 `tracked_item.item_group` 재분류(표시용 메타)만 — 가격 데이터·
스키마 구조 무변경.
