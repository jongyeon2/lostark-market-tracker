---
quick_id: 260811-w1h
slug: add-jeonyul-honing-aux-materials
description: 신규 재련보조 '전율' 4종(야금술·재봉술 × [12-15]/[16-19]) 워치리스트 편입 — 거래소 스파이크 실측 후 WatchlistSeeder에 추가
date: 2026-08-11
status: complete
commits: [ed131c6]
---

# SUMMARY — 신규 재련보조 '전율' 재련재료 편입 (quick-260811-w1h)

WatchlistSeeder 49 → **53** (재련보조 6 → 10, MATERIAL 31 → 35). 마이그레이션 0, 프론트 변경 0.

## 무엇을 했나

2026-08-05 벨가르딘 그림자 레이드로 나온 신규 **일반 재련 보조 재료 '전율'**을 거래소에서 실측하고
워치리스트에 편입해 10분 시계열 수집 대상에 넣었다.

| external_item_id | display_name | grade | icon | item_group |
|---|---|---|---|---|
| 66112561 | 야금술 : 전율 [12-15] | 고대 | use_12_218.png | 재련보조 |
| 66112562 | 야금술 : 전율 [16-19] | 고대 | use_12_218.png | 재련보조 |
| 66112564 | 재봉술 : 전율 [12-15] | 고대 | use_12_219.png | 재련보조 |
| 66112565 | 재봉술 : 전율 [16-19] | 고대 | use_12_219.png | 재련보조 |

## 스파이크 (거래소 /markets/items 50020, 2026-08-11, HTTP 200)

- `LOSTARK_API_KEY`(.env)로 이름검색 '전율' DESC+ASC 실측. **Docker 꺼져 있어** Spring/Testcontainers 스파이크
  테스트 대신 동일 계약의 stdlib 직접 호출 스크립트로 캡처(공개 메타데이터 Id/Name/Grade/Icon만, 가격·키 미출력).
- 재현용 provenance는 `MarketsApiSpikeTest.captureAwakenHoningTiers()`(@Disabled 상속)로 코드에 잠금.
- **아이콘 공유 확인**: 야금술 전율 = use_12_218(업화와 동일), 재봉술 전율 = use_12_219(업화와 동일) → 라벨로 구분.
- **강화 업화[19-20] 66112555/66112556** 재확인 = 2026-07-14 이미 실측된 기존 미편입 변형 → "전율만 신규"대로 제외.

## 🔑 핵심 판단 — 마이그레이션을 쓰지 않았다

앞서 "insert-only라 Flyway 필수"로 계획했으나, 스키마 확인 후 정정했다. WatchlistSeeder는
`findByExternalItemId(...).orElseGet(save)` — **신규 external_item_id는 다음 기동 시 dev·prod 모두 자동 insert**된다.
V7이 마이그레이션을 쓴 건 *기존* 융화재료 행의 item_group을 바꾸는(seeder가 안 건드리는) 재분류였고,
코드베이스에 아이템 insert용 마이그레이션은 전무하다 — 모든 아이템 추가는 seeder를 통한다. 신규 편입에
마이그레이션은 중복·비관용적이라 넣지 않았다. (memory `watchlist-seeder-insert-only`는 *메타 변경*에 한한 규칙.)

## 변경 파일

| 파일 | 변경 |
|---|---|
| `WatchlistSeeder.java` | 재련보조에 전율 4종 + 클래스 주석 개수(49→53, 31→35, 재련보조 6→10, 아이콘 공유 목록에 전율) |
| `WatchlistSeederIT.java` | hasSize 49→53, MATERIAL 31→35, 재련보조 6→10, count ×2 49→53, 전율 샘플 단언 1건 |
| `MarketsApiSpikeTest.java` | `captureAwakenHoningTiers()` 재현 스파이크 메서드(@Disabled) |

## 검증

- ✅ `./gradlew compileTestJava` 통과(EXIT=0) — 문법·타입 로컬 확인.
- 🚦 **Docker 꺼짐 → Testcontainers 통합테스트(WatchlistSeederIT) 로컬 미실행.** 최종 게이트는 **PR CI**
  (러너 Docker로 53·재련보조 10 단언 실행) — postcss 선례와 동일.
- 배포 후 loaket.kr에서 전율 4종이 재련보조 카테고리에 아이콘과 함께 노출되는지 육안 확인(사용자 몫).

## 후속

- 미머지/미배포: 브랜치 `quick/260811-w1h-jeonyul-materials`. PR 올려 CI 그린 확인 후 머지 → 배포(승인).
- **Task 3**(타임라인 좌측 카테고리 레일) = 별도 quick 태스크로 진행 예정.
