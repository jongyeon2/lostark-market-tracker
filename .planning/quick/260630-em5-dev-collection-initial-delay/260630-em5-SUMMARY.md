---
quick_id: 260630-em5
slug: dev-collection-initial-delay
status: complete
date: 2026-06-30
files_modified:
  - src/main/resources/application-dev.yml
---

# Quick 260630-em5: dev 프로파일 첫 수집 타이밍 버그 수정 Summary

**dev 프로파일에서 `@Scheduled` 수집기가 `WatchlistSeeder`(ApplicationRunner)보다 먼저 시작돼 첫 틱이 빈 워치리스트로 헛돌던(items_attempted=0) 버그를 `collection.initial-delay-ms: 10000`로 수정. `collection_run`이 0→15로 회복됨을 런타임 검증. (별개로 `.env` API 키가 무효(401)임을 발견 — 본 태스크 범위 밖, 키 재발급 필요.)**

## 근본 원인 (측정 기반)

- dev는 `collection.initial-delay-ms`를 오버라이드하지 않아 `PriceCollector` 기본값 `0` 적용.
- Spring 생명주기상 `@Scheduled` 스케줄러(`finishRefresh`)가 `WatchlistSeeder`(`callRunners`)보다 먼저 시작 → initial-delay=0이라 첫 틱이 시더보다 먼저 실행 → `tracked_item` 빈 상태 → `items.size()=0` → `0==0`이라 SUCCESS로 오기록 → `price_snapshot` 0건.
- seed는 `initial-delay-ms: 3600000`이라 이 버그가 가려져 있었음.

## 수정

- `src/main/resources/application-dev.yml`에 최상위 `collection.initial-delay-ms: 10000` 추가 (주석 포함). 첫 틱만 10초 지연, `fixedDelay`(10분) 유지. dev 한정 — seed/test(1h)·기본 무변경. Java·DB 무변경.

## 검증 (런타임)

- 재시작 후 `collection_run` 결과:
  - 01:20:55 (수정 전) `items_attempted=0`, SUCCESS — 버그 재현
  - 01:30:55 / 01:36:26 (수정 후) **`items_attempted=15`** — 시더가 먼저 돌아 워치리스트 15개가 채워진 뒤 첫 틱 실행됨 → **타이밍 버그 해소 확인**
- `tracked_item`(active)=15 유지.

## 별개 발견 — `.env` API 키 무효 (본 태스크 범위 밖, Blocker)

- 위 두 틱은 `items_failed=15`, `summary_message=AUTH_ERROR`. `.env` 키(길이 632)로 거래소 API를 **직접 호출해도 401**. JWT 구조 정상(3 segments)·`iss=ludy.game.onstove.com`·`exp` 없음(무기한)인데도 거부 → **키가 폐기/무효화된 상태**.
- 그래서 `price_snapshot` 0건 → 프론트 `/latest`가 데이터 없음으로 404(콘솔 404의 정체).
- **해결: 로스트아크 개발자 포털에서 키 재발급 → `.env`의 `LOSTARK_API_KEY` 교체(bearer 없이·공백 없이) → dev 재시작.** 키 교체 후 첫 틱에서 `items_succeeded=15`·실제 시세 적재가 기대됨.

## 최종 검증 (키 교체 후, 2026-06-30 01:49)

키 재발급·`.env` 교체 후 `collection_run` **SUCCESS 15/15**(`items_succeeded=15`, AUTH_ERROR 해소). 실제 시세 적재 확인 — 유물 원한 각인서 145,000G·돌격대장 145,000G·예리한 둔기 137,500G·아드레날린 130,799G 등 합성 범위(1k~5k) 완전 이탈. 프론트 렌더링 정상(사용자 확인).

## 결과

dev 실시간 수집의 첫 틱 헛돎(구조적 버그) 수정·검증 완료 + 무효 키 교체로 전체 파이프라인(수집→저장→서빙→렌더)이 실데이터로 정상 동작함을 확인.
