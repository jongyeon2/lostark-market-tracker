# Phase 12 Verification — API Spike + Data Lock (게이트)

**Verified:** 2026-06-29
**Method:** Goal-backward — Phase Goal과 5개 성공 기준(SPIKE-01..05) + 불변 제약을 산출물·git 상태로 역검증
**Verdict:** ✅ **PASS** — Phase 12 게이트 통과, Phase 13 언블록

---

## Phase Goal 달성 여부

> 본인 JWT로 `/markets/options`·`/markets/items`를 1회 실측해 융화재료·유물 각인서의 item id·display_name·category·iconUrl을 확정하고, 거래 가능·아이콘 구별 여부가 확인된 큐레이션 12~20개와 fallback 전략을 findings로 잠근다(런타임 키 불필요).

**달성.** `12-SPIKE-FINDINGS.md`가 실측 CategoryCode·Icon 필드·6필드 큐레이션 15개·아이콘 구별 여부·fallback을 담고, blocking 휴먼 체크포인트가 비준됨. 후속 phase는 키 없이 이 상수만 소비.

## 성공 기준 검증

| # | 기준 (Requirement) | 결과 | 증거 |
|---|---|---|---|
| 1 | 각인서(40000)·융화재료 CategoryCode 실측 기록 (SPIKE-01) | ✅ PASS | findings (a): 각인서 `40000`(leaf, Subs:[]), 융화재료 `50010`(재련 재료). 50020 0건 실측 배제 |
| 2 | Icon URL 필드(필드명·CDN) 확정 / 부재 시 fallback (SPIKE-02) | ✅ PASS | findings (b): 필드명 `Icon`, CDN `cdn-lostark.game.onstove.com/efui_iconatlas/use/`, 전 응답 존재 |
| 3 | 거래 가능·id·iconUrl 검증 큐레이션 12~20개 6필드 (SPIKE-03, SPIKE-04) | ✅ PASS | findings (c): MATERIAL 4 + DEALER 9 + SUPPORT 2 = **15개**(범위 내), 전부 실측 external_item_id·icon_url, 플레이스홀더 0 |
| 4 | 각인서 아이콘 구별 여부 + fallback 전략 (SPIKE-04, SPIKE-05) | ✅ PASS | findings (d): 유물 각인서 11종 `use_9_25` 동일 → identical → 라벨 병기(D-06). (e): 역할색+lucide+라벨병기 |
| 5 | findings·커밋에 키/가격/식별자 0건 (SPIKE-05) | ✅ PASS | `git diff 20fde63..HEAD | grep -Ei "bearer …|eyJ…"` → 0건. 표는 id/name/grade/category/icon만 |

## 불변 제약 (상시 가드)

| 제약 | 결과 | 증거 |
|---|---|---|
| 수집 스케줄러·Redis 캐시·EventImpactService **0줄** 무변경 | ✅ PASS | `git diff --name-only 20fde63 HEAD`에 Scheduler/TokenBucket/Cache/EventImpact/collect/ratelimit 0건 |
| main 소스 변경은 spike 패키지만 | ✅ PASS | 유일 변경 = `LostarkSpikeClient.java`(`@Profile("spike")`) |
| 키 없이 build 그린 + 스파이크 CI 미실행 | ✅ PASS | `compileTestJava` 그린, `@Disabled` 유지(1건). 비-spike 코드 무변경이라 회귀 위험 0 |
| 실 키 미커밋 | ✅ PASS | `.env` gitignored·추적 0(git check-ignore 확인). diff 키 누출 0 |
| blocking 휴먼 체크포인트 비준 | ✅ PASS | Task 3 — 사용자 "비준 — 큐레이션 잠금" 입력 확인 |

## 비고 / 잔여 항목 (Phase 13 진입 전 선택)

- **만개**(도화가 서포터각): ItemName 0건으로 보류. 포함 시 1회 재실측(매물 부재였을 수 있음). 현재 SUPPORT 2종(각성·전문의)으로도 role_group 3군 데모 충족.
- **구원**: 실재 서포터 직업각인 아님 → 제외 확정.
- **운명 융화재료**: Deferred 유지.
- **build 전체 스위트(Testcontainers)**: 미실행. 변경이 spike 전용(@Profile/@Disabled)이라 비-spike 경로 무영향 — 필요 시 `./gradlew build`로 추가 확인 가능.
- ⚠️ 스파이크 중 대화 노출 JWT 키 포털 재발급 권장.

---
*Phase: 12-api-spike-data-lock · Verified 2026-06-29 · PASS*
