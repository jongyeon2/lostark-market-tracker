---
phase: 06-distribution-docs
verified: 2026-06-25T01:45:00Z
status: passed
score: 4/4 성공 기준 검증됨
---

# Phase 6: Distribution + Docs 검증 보고서

**페이즈 목표:** CI·격리 시드·README 데모 표면으로 리뷰어가 README만 보고 재현할 수 있게 한다.
**검증 시각:** 2026-06-25T01:45:00Z
**상태:** passed
**검증 방식:** 목표 역산(goal-backward) — ROADMAP Phase 6 목표 + 4개 성공 기준 + DIST-02/03/04 요구사항 + 두 PLAN의 must_haves. 오케스트레이터가 인라인으로 수행(이 환경은 백그라운드 서브에이전트가 권한 거부됨).

## 목표 달성

### 관측 가능한 진실 (ROADMAP 성공 기준)

| # | 진실 | 상태 | 증거 |
|---|------|------|------|
| 1 | GitHub Actions CI가 매 푸시에 Testcontainers로 돌고 README에 그린 배지가 뜬다 | ✓ 검증됨 (배지 렌더는 최초 푸시 후) | `.github/workflows/ci.yml`: `name: CI`, `on: push + pull_request`(브랜치 필터 없음), `runs-on: ubuntu-latest`, `actions/setup-java@v4`(temurin 21), `chmod +x ./gradlew`, `./gradlew build --no-daemon`(= 전체 JUnit5/Testcontainers 스위트). `-PdockerApiVersion` 미사용(빌드 기본 1.44, ubuntu-latest 엔진 수용). README 배지 `actions/workflows/ci.yml/badge.svg`가 동일 워크플로를 가리킴. **로컬에서 동일 명령 그린**(아래 SC 전체 빌드). **잔여 수동 단계:** 최초 `git push` 1회로 배지가 실제 green 렌더 → [06-USER-SETUP.md](./06-USER-SETUP.md)에 기록(로컬 검증 불가한 유일 항목). |
| 2 | 시드 프로파일이 합성 데이터(스냅샷 7일+, 이벤트 2개+)를 넣어 타임라인·event-impact가 비어있지 않게 응답한다 | ✓ 검증됨 | `SyntheticDemoData`(`SEED_DAYS = 8` > 7, 10분 케이던스 1152틱/품목; 이벤트 2개) → `SeedDataRunner`(`@Profile("seed")`)가 부팅 시 호출. **테스트 증명** `SyntheticDemoDataIT` (Testcontainers, 2/2 통과): seed() 후 `GET /api/items/{id}/prices` snapshots 비어있지 않음 + `GET /api/items/{id}/event-impact?window=24` ≥1 `status:"ok"` & non-null **non-zero** changeRate. |
| 3 | README가 아키텍처 다이어그램·트레이드오프 설명·curl 예시 3개+·샘플 JSON·로컬 셋업·시드 모드·보존 정책 한 줄을 담는다 | ✓ 검증됨 | `README.md` grep: `mermaid` 다이어그램 1개, curl 6개(≥3), ```json 5개(≥3), `왜 Redis/왜 레이트리밋/왜 @Async` 트레이드오프, `docker compose up` + `--spring.profiles.active=seed` 로컬·시드 셋업, 보존 한 줄("삭제 정책 없이 원본 보존 … 롤업/파티셔닝 v2"). |
| 4 | 리뷰어가 README만 따라 docker-compose up + 시드 모드로 비어있지 않은 event-impact 응답을 재현한다 | ✓ 검증됨 | README 상단 "3단계 재현" 블록: `cp .env.example .env` → `docker compose up -d` → `./gradlew bootRun --args='--spring.profiles.active=seed'` → `curl .../api/items/1/event-impact?window=24`. 이 seed→event-impact "ok" 경로 자체가 `SyntheticDemoDataIT`로 증명됨(SC2). 모든 라우트/필드는 실제 컨트롤러·DTO 대조. |

**점수:** 4/4 진실 검증됨

### 필수 산출물 (must_haves artifacts)

| 산출물 | 상태 | 상세 |
|--------|------|------|
| `.github/workflows/ci.yml` | ✓ 존재 + 실질적 | name CI, push+PR, ubuntu-latest, temurin 21, chmod +x gradlew, `gradlew build`. `permissions: contents: read`; CD/시크릿/매트릭스 0(`grep -niE "deploy|secrets\."` 빈 결과). |
| `seed/SyntheticDemoData.java` | ✓ 존재 + 실질적 | `@Component`(no `@Profile`) `seed()`; 품목별 8일 10분 스냅샷 멱등 삽입(`existsByTrackedItem_IdAndCollectedAt`), `game_event count()==0` 가드 후 2개 이벤트. UTC `Clock` 주입. |
| `seed/SeedDataRunner.java` | ✓ 존재 + 실질적 | `@Component @Profile("seed") @Order(2)` ApplicationRunner → `seed()` 호출 + 카운트만 로깅(요청/시크릿 미로깅). |
| `application-seed.yml` | ✓ 존재 + 실질적 | `collection.initial-delay-ms: 3600000`(@Scheduled 수집기 침묵 → API 키 불필요); datasource/redis는 base 상속. |
| `collect/WatchlistSeeder.java` | ✓ 존재 + 실질적 | `@Order(1)`만 추가 — `@Profile({"dev","seed"})`와 12품목 WATCHLIST·upsert 로직 미변경(grep 확인). |
| `seed/SyntheticDemoDataIT.java` | ✓ 존재 + 실질적 | Testcontainers, RANDOM_PORT, `@ActiveProfiles("test")`: non-empty timeline + event-impact "ok"(non-zero changeRate) + 재시드 멱등(count 불변). 2/2 통과. |
| `README.md` | ✓ 존재 + 실질적 | 배지·mermaid 아키텍처·정직한 트레이드오프·setup/seed·6 curl + 5 JSON·admin X-Admin-Secret 예시·오류 계약·API 한계·보존 한 줄·3단계 재현. |

### 핵심 진실 (must_haves truths) 스폿체크

- **CI = 로컬과 동일 명령:** `./gradlew build`가 컴파일 + 전체 Testcontainers 스위트. 로컬 전체 빌드 **그린: 86 tests, 0 failures, 1 skipped**(@Disabled Task-0 스파이크). Phase 5(84) 대비 +2(이 IT), 회귀 0.
- **스키마/의존성 무변경:** `ls src/main/resources/db/migration` = 3(V1~V3). build.gradle 의존성 추가 0. ddl-auto=validate 그린(전체 빌드 통과).
- **시드 = seed 전용:** 생성 로직은 profile-free `@Component`(단위 테스트 가능), 부팅 트리거만 `@Profile("seed")`. dev/test/prod-default에서 합성 데이터 0. WatchlistSeeder@Order(1) → SeedDataRunner@Order(2) 순서로 활성 품목 선존재.
- **멱등성:** 스냅샷 per-tick `existsBy` 가드 + 이벤트 `count()==0` 가드. IT가 2회 seed() 후 snapshot count 불변·event count=2 단언.
- **staleness 계약 준수 (편차 1건, 의도된 정정):** 이벤트를 10분 그리드에서 **5분 오프셋** 배치 → `EventImpactService` 앵커 타이 규칙상 pre(−5분)/post(+5분)가 **서로 다른** 신선 스냅샷(둘 다 ≤30분) → `status:"ok"` + **실제 non-zero** change_rate. (그리드 정확 배치는 pre==post gap-0 → rate 0이 되어 "실제 change_rate" 목표를 깸.) 06-01-SUMMARY.md에 deviation 기록. IT가 `changeRate.signum() != 0` 단언.
- **시크릿 비노출:** README curl은 `$ADMIN_API_SECRET` 플레이스홀더만(`grep "X-Admin-Secret: [^$]"` 빈 결과). CI는 시크릿 0. 시드/헬스 로깅에 키 없음.
- **라우트/필드 = 실제 코드:** README의 `/api/items`, `/prices`, `/event-impact`, `/api/admin/events` 및 JSON 필드명을 ItemController/PricesController/EventImpactController/Admin*Controller + DTO 레코드(camelCase, Jackson 기본)와 대조 — 발명된 엔드포인트/필드 0.

## 요구사항 추적

| 요구사항 | 상태 | 증거 |
|----------|------|------|
| DIST-02 (CI Testcontainers 빌드+테스트) | ✓ Complete | `.github/workflows/ci.yml` + 로컬 전체 빌드 그린 |
| DIST-03 (격리 시드로 빈 화면 없이 데모) | ✓ Complete | seed 프로파일 + SyntheticDemoDataIT 2/2 |
| DIST-04 (README 데모 표면) | ✓ Complete | README grep 배터리 전부 통과 |

REQUIREMENTS.md 체크박스 DIST-02/03/04 = [x], Traceability `DIST-02..04 | Phase 6 | Complete`.

## 편차 (Deviations)

1건(06-01 Task 2): 데모 이벤트 **그리드 5분 오프셋 배치**. 계획서의 "그리드 정확 배치"는 앵커 타이 규칙상 change_rate 0을 유발 → 계획의 실제 목표("실제 change_rate, insufficient_data 아님")를 달성하도록 정정. 스코프 크리프 없음, 헤드라인 데모 강화. 상세는 06-01-SUMMARY.md.

## 미해결 이슈 / 후속

- **유일 수동 단계:** 최초 `git push origin main`으로 CI 실행 + 배지 green 렌더(06-USER-SETUP.md). 로컬에서 `./gradlew build`는 이미 그린이므로 CI도 그린 예상.
- Phase 6 = 마일스톤 v1.0의 최종 페이즈. 15/15 플랜 완료(100%). 다음: `/gsd-verify-work 6`(대화형 UAT) 또는 `/gsd-complete-milestone`.

---
*Phase: 06-distribution-docs*
*Verified: 2026-06-25 — passed (4/4)*
