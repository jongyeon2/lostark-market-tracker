---
phase: 13-backend-enrichment-seed
plan: 01
subsystem: database
tags: [flyway, postgres, jpa, spring-boot, seed, testcontainers]

requires:
  - phase: 12-api-spike-data-lock
    provides: "6필드 큐레이션 15개(external_item_id/display_name/category_code/icon_url/item_group/role_group), CDN base, CategoryCode 40000/50010 잠금"
provides:
  - "tracked_item에 nullable enrichment 3컬럼(icon_url/item_group/role_group) — V4 additive 마이그레이션"
  - "TrackedItem 엔티티 enrichment 매핑(getter + 6-arg 생성자, 3-arg 보존)"
  - "WatchlistSeeder 큐레이션 15개(MATERIAL 4 + DEALER 9 + SUPPORT 2) enrichment 동반 멱등 등록"
  - "WatchlistSeederIT — seed/분포/enrichment/멱등/SEED-02 Testcontainers 증명"
affects: [13-02-read-dto-passthrough, 14-frontend-icon]

tech-stack:
  added: []
  patterns:
    - "read-path additive enrichment: nullable 컬럼만 추가, V1–V3·수집/캐시/event-impact 0줄"
    - "seed 시점 immutable enrichment: setter 없이 6-arg 생성자 + 필드 접근 JPA getter"

key-files:
  created:
    - src/main/resources/db/migration/V4__add_item_enrichment.sql
    - src/test/java/com/lostark/tracker/collect/WatchlistSeederIT.java
  modified:
    - src/main/java/com/lostark/tracker/domain/TrackedItem.java
    - src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java

key-decisions:
  - "enrichment 3컬럼은 전부 NULLABLE — 수집/캐시/event-impact가 요구하지 않는 선택적 표시 메타데이터"
  - "WatchlistSeederIT는 test 프로파일에서 시더를 직접 생성(new WatchlistSeeder)해 프로파일 곡예 없이 완전 통제 + 키 불필요"
  - "SyntheticDemoData 0줄 변경 — findByActiveTrue() 설계가 신규 품목을 자동 흡수(IT가 그 증거)"

patterns-established:
  - "V4 단일 ALTER TABLE 다중 ADD COLUMN(PostgreSQL) — Flyway 체크섬 불변, ddl-auto=validate 그린"
  - "큐레이션 상수를 컴파일타임 List.of(SeedItem)로 전사 — 동적 SQL/외부 입력/키 0건"

requirements-completed: [ITEM-01, ITEM-02, SEED-01, SEED-02, SEED-04]

duration: ~20 min
completed: 2026-06-29
---

# Phase 13 Plan 01: Backend Enrichment + Seed Expansion Summary

**Flyway V4가 tracked_item에 nullable icon_url/item_group/role_group을 additive로 추가하고, TrackedItem이 이를 매핑하며, WatchlistSeeder가 스파이크 검증 큐레이션 15개를 enrichment와 함께 멱등 등록 — 수집/캐시/event-impact는 0줄, Testcontainers IT가 SEED-02까지 증명.**

## Performance

- **Duration:** ~20 min
- **Completed:** 2026-06-29
- **Tasks:** 3
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments
- V4__add_item_enrichment.sql — 단일 ALTER TABLE로 icon_url(VARCHAR 300)/item_group(VARCHAR 50)/role_group(VARCHAR 20)를 전부 nullable 추가. V1–V3 0줄(Flyway 체크섬 불변), ddl-auto=validate 그린.
- TrackedItem — enrichment 3필드 @Column 매핑 + getter 노출 + 6-arg 생성자 추가(기존 3-arg는 6-arg에 null 위임으로 보존, protected 기본 생성자 보존).
- WatchlistSeeder — 미검증 재련 재료 12개(66102101 등)를 큐레이션 15개로 교체(MATERIAL 4 / DEALER 9 / SUPPORT 2), ICON_BASE + 실측 파일명·item_group·role_group을 6-arg 생성자로 멱등 upsert. 키/가격 0건.
- WatchlistSeederIT — Testcontainers(키·네트워크 불필요)에서 15개·분포 4/9/2·표본 enrichment(6861009 use_8_109.png/강화재료/MATERIAL, 65203405 use_9_25.png/각인서/SUPPORT)·시더 멱등·SyntheticDemoData 신규품목 스냅샷(SEED-02)·2회 멱등 단언.

## Task Commits

1. **Task 1: V4 nullable enrichment 컬럼 + TrackedItem 매핑** - `dfce5dd` (feat)
2. **Task 2: WatchlistSeeder 큐레이션 15개 교체** - `6d6ae47` (feat)
3. **Task 3: WatchlistSeederIT Testcontainers 검증** - `82f021d` (test)

## Files Created/Modified
- `src/main/resources/db/migration/V4__add_item_enrichment.sql` - tracked_item nullable enrichment 3컬럼 (V1–V3 불변)
- `src/main/java/com/lostark/tracker/domain/TrackedItem.java` - enrichment 매핑 + 6-arg 생성자 + getter
- `src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java` - 큐레이션 15개 + enrichment 멱등 등록
- `src/test/java/com/lostark/tracker/collect/WatchlistSeederIT.java` - seed/분포/enrichment/멱등/SEED-02 IT

## Decisions Made
- enrichment 컬럼 전부 nullable — 수집/캐시/event-impact 비요구 표시 메타데이터, 순수 additive.
- IT는 시더 빈을 직접 생성(test 프로파일에서 @Profile{dev,seed} 빈 부재) — 통제 + 키 불필요.
- SyntheticDemoData 무변경 — findByActiveTrue() 기반이라 신규 활성 품목 자동 포함.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required. 큐레이션 상수는 12-SPIKE-FINDINGS.md에서 전사하므로 런타임 API 키 불필요.

## Next Phase Readiness
- 데이터 계층 완비 — 13-02(read DTO 패스스루)가 tracked_item enrichment를 4개 read 응답에 노출할 수 있음.
- 불변 가드 유지: 수집/캐시/event-impact 0줄, V1–V3 불변, 시더/합성기 키·가격 0건.

---
*Phase: 13-backend-enrichment-seed*
*Completed: 2026-06-29*
