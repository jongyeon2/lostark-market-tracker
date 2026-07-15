---
quick_id: 260715-nav
status: passed
verified: 2026-07-15
method: build + DB 실측(psql) + Playwright 라이브 렌더
---

# Quick 260715-nav VERIFICATION

**결과: ✅ PASSED**

## must_haves 대조

| truth | 검증 | 결과 |
|---|---|---|
| 기존 융화재료 행이 실제로 재련재료로 옮겨진다(시더 소스 수정만으론 불가) | Flyway 로그 `Migrating schema "public" to version "7"` → `now at version v7`. `psql` 실측: `강화재료` **0건** | ✅ 실증 |
| 강화재료 leaf는 사라지고 아비도스는 재련재료에서 나온다(증발 금지) | Playwright: nav에 강화재료 부재. **재련재료 클릭 → 아비도스 융화 재료 + 상급 아비도스 융화 재료 최상단 노출** | ✅ 실증 |
| 각인서 헤더 + 헤더 아래 구분선 | Playwright 스크린샷 — "각인서" 헤더에 `border-b` rule, 그 아래 딜러/서포터 leaf | ✅ |
| 아이콘은 데이터 파생(하드코딩 금지) | `groupIconUrl`이 `CATEGORY_DEFS`로 그룹 소속 판정 후 첫 매칭 아이템의 `iconUrl` 반환. 코드에 CDN URL 리터럴 없음 | ✅ |
| 재료 그룹은 아이콘 없음 | `ICONIC_GROUPS = ['각인서']` → 재료는 `null`. 스크린샷상 "재료" 헤더에 아이콘 없음 | ✅ |

## 빌드/테스트

- `./gradlew build` → **BUILD SUCCESSFUL** (V7 + `WatchlistSeederIT` 신규 item_group 카운트 단언 포함).
- `cd frontend && npm run build` → tsc -b + vite **그린**.

## DB 실황 (dev, V7 적용 후)

| item_group | count |
|---|---|
| 각인서 | 19 |
| 재련재료 | 13 |
| 상급재련 | 8 |
| 아크그리드젬 | 6 |
| 재련보조 | 6 |

`강화재료` 없음. dev가 운영(재련재료 11 · 각인서 18)보다 많은 건 시더에 없는 **레거시 행**
(오레하 융화 재료 2 · 정밀 단도 각인서 1) 때문 — insert-only 시더가 지운 적이 없어서이며, 운영엔 없다.
`WatchlistSeederIT`가 시더 산출물 기준으로 재련재료 **11**을 고정하므로 운영 기대치는 회귀 방어된다.

## 라이브 렌더 (Playwright, 1280px)

- 좌측 nav: **각인서 [아이콘]** ─구분선─ 딜러 각인 12 / 서포터 각인 7, **재료** ─구분선─ 재련재료 13 /
  상급재련 8 / 재련보조 6 / 아크그리드젬 6.
- **강화재료 leaf 부재** 확인.
- **재련재료 클릭 → 아비도스 2종 노출** 확인.
- 한글 깨짐 없음.
- 증거: `quickA-nav-taxonomy.png`, `quickA-refine-with-abidos.png`

## 비고

- 백엔드 기동 exit 1은 포트 8080 고아 프로세스(이전 TaskStop이 Gradle 래퍼만 종료) — 코드 무관.
  정리 후 정상 기동, SUMMARY 편차 참조.
- **배포 시 주의**: 운영 반영은 push → CI → 재배포로 V7이 실행돼야 완성된다. V7 없이 프론트만 나가면
  아비도스 2종이 증발한다(같은 커밋에 함께 있으므로 정상 배포에선 발생 불가).

**Quick A 완료 → Phase 24(event-impact 백필 폴백 앵커) 착수 가능.**
