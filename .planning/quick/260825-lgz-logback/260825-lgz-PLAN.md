---
quick_id: 260825-lgz
slug: logback
description: Logback 설정(콘솔+RollingFile·gz) + 로그 레벨 정책 수립 및 침묵 지점 로깅 보강
date: 2026-08-25
status: 미채택
---

# PLAN — Logback 설정 + 로그 레벨 정책 (quick-260825-lgz)

> ## ⛔ 미채택 (2026-08-25) — 실행하지 않음
>
> 계획을 세운 뒤 **기존 설계 결정과 충돌한다는 것을 발견**해서 착수하지 않았다.
>
> `docs/specs/2026-07-17-monitoring-alerting-design.md`(설계 확정·사용자 승인 2026-07-17)의
> **비범위**에 이미 이렇게 적혀 있다:
>
> > **Prometheus / Grafana / Loki** — 무료지만 4GB ARM 단일 박스에 과하고, 감시 스택 자체가 운영 부담이 된다
> > **로그 기반 알림**, 상태 페이지, 에스컬레이션·온콜, SMS
>
> 즉 관측성은 이미 설계됐고, 그때 **로그 인프라를 의도적으로 배제**하는 결정이 내려져 있었다.
> 이 PLAN의 Task 1(파일 appender + prod 볼륨 + SSH 조회)은 그 결정을 뒤집는 일이라,
> quick 트랙에 끼워 넣을 게 아니라 별도 설계 판단이 필요하다.
>
> **정정해 둘 것**: 이 PLAN 본문이 근거로 든 것 중 **VM 용량은 반대 근거가 못 된다.**
> 10분 간격 스케줄러라 하루 50~100KB, gz 압축 시 14일치가 1MB 미만이다.
> `totalSizeCap 500MB`(Task 1)는 이 워크로드에 과한 설정이었다. 진짜 쟁점은 용량이 아니라
> **"단일 인스턴스 규모에 로그 인프라가 맞는가"** 와 **"SSH로만 읽는 게 쓸모 있는가"** 였다.
>
> **살아남은 부분**: Task 2(레벨 정책)·Task 3(침묵 지점 로깅)은 로그 목적지와 무관하게 유효하다.
> `log.error` 0건과 `PriceCollector`·`ItemFetchService`·`RedisTokenBucket`의 조용한 예외 삼킴은
> `docker logs`만 봐도 결함이다. 다시 착수한다면 **Task 1을 빼고 2·3만** 하는 것이 맞다.
>
> **더 나은 대안(미실행)**: 알림 이후 원인 규명이 실제로 불편해지면, 파일 로그보다
> **관리자 화면(tailnet `:8081`)에 `collection_run` 실패 이력을 띄우는 쪽**이 낫다 —
> 데이터가 이미 DB에 쌓이고 있고, SSH 불필요, 새 인프라 0.
>
> 면접용 요약은 `STUDY.md` 부록 B에 정리해 뒀다.

## 배경 / 문제

인프런 「로그 관리와 모니터링」 학습 내용의 **1단계 적용**. 강의 7개 축 중 Logback·레벨 정책만
가져오고, MDC·Micrometer·Prometheus/Grafana는 다음 작업으로 분리한다(ELK는 이 프로젝트 규모에
부적합으로 판정 — 단일 VM·단일 인스턴스, CLAUDE.md가 MSA 명시 제외).

실측된 결함 4가지:

1. **`logback-spring.xml`이 없다.** Spring Boot 기본값 = 콘솔만. 파일·롤링·압축 전부 없음.
2. **배포마다 로그 이력이 소실된다.** `.github/workflows/ci.yml`이 머지마다
   `docker compose ... up -d`로 app 컨테이너를 재생성 → `docker logs` 이력이 통째로 날아간다.
   로그 집계 인프라가 없으므로 **장애 후 되짚을 수단이 현재 0**이다.
3. **`log.error`가 0건이다.** 전체 22개 로그가 `debug 12 / info 3 / warn 7`. 사람이 개입해야 하는
   상황과 무시해도 되는 상황이 레벨로 구분되지 않는다.
4. **핵심 경로가 로그 침묵이다.** 106개 java 파일 중 12개만 로깅하며, 수집 파이프라인의
   `PriceCollector` · `ItemFetchService` · `RedisTokenBucket`은 **로그가 0줄**인데 그 안에서
   예외를 조용히 삼킨다:

   | 위치 | 삼키는 예외 | 결과 |
   |---|---|---|
   | `RedisTokenBucket.java:63` | `RuntimeException` | fail-closed → Redis 장애 시 **전 아이템 수집 0건**, 흔적 없음 |
   | `ItemFetchService.java:66,70,73,76` | Auth / RateLimited / Transient / NonRetryable | 아이템 실패 사유가 카운트로만 남음 |
   | `PriceCollector.java:awaitAll` | Timeout / Execution / Interrupted | 전체 타임아웃 백스톱이 조용히 발동 |
   | `PriceCollector.java:settledResult` | `RuntimeException` | 조용히 null |
   | `PriceCollector.java:persistSnapshot` | `DataIntegrityViolationException` | 멱등 스킵(정상) |

5. **(부수) 12개 `log.debug`는 어디서도 출력되지 않는다.** `logging:` 키가 `application-prod.yml`에만
   있고(root: INFO), dev 프로파일엔 없어 기본 INFO → DEBUG 문 전부 사장 상태.

기존 관측 수단인 `CollectionRun` 테이블(counts + `summary_message`)과 `CollectionHeartbeat`
(healthchecks.io 데드맨)는 **유지·보완 대상**이지 대체 대상이 아니다. 로그는 "왜 실패했는가"를,
DB 이력은 "얼마나 성공했는가"를 답한다.

## 결정 (착수 전 확정)

- **로그 목적지 = 콘솔 + 파일(롤링·gz) + prod 볼륨.** `docker logs`(실시간)는 유지하고, 파일은
  named volume에 남겨 **컨테이너 재생성에도 이력이 보존**되게 한다.
- **파일 appender는 dev·prod에서만.** test/seed 프로파일은 콘솔만 — CI가 로그 파일을 남기면 안 된다.
- **폭주 억제 우선.** 아이템 단위(49종 × 144틱/일) 경로는 WARN 이하 + 스택 미포함, 사람이 봐야 하는
  신호는 **틱 요약 1줄**로 승격한다. 강의엔 없는 보강.

## Task 1 — `logback-spring.xml` + 컨테이너 로그 경로

- **files:** `src/main/resources/logback-spring.xml`(신규), `Dockerfile`,
  `docker-compose.prod.yml`, `.gitignore`, `src/main/resources/application-dev.yml`
- **action:**
  - `logback-spring.xml` 신규 작성:
    - `CONSOLE` (ConsoleAppender) — 전 프로파일 공통, `docker logs` 경로 유지.
    - `FILE` (RollingFileAppender + TimeBasedRollingPolicy) — `<springProfile name="dev,prod">`로만
      감싼다. `fileNamePattern`은 `${LOG_DIR}/app-%d{yyyy-MM-dd}.log.gz`, `maxHistory` 14,
      **`totalSizeCap` 500MB**(강의엔 없음 — Oracle Always Free VM 디스크 상한 보호).
    - `LOG_DIR` 기본값 `./logs`, prod는 env로 `/app/logs` 주입.
    - MDC 패턴(`%X{traceId}`)은 **넣지 않는다** — 필터가 없어 항상 빈 `[]`로 찍힌다. 다음 작업에서
      패턴 한 줄만 바꾸면 되도록 주석으로 위치만 표시.
    - `<root level>`을 하드코딩하되, yml의 `logging.level.*` 오버라이드가 계속 이기는지 verify에서 실측.
  - `Dockerfile`: `USER appuser` **앞에** `RUN mkdir -p /app/logs && chown appuser /app/logs` 추가.
    ⚠️ 이게 없으면 Docker가 빈 볼륨을 `root:root`로 초기화 → appuser가 쓰지 못하고 **Logback이 조용히
    실패**한다(앱은 정상 기동하므로 배포 스모크로도 안 잡힌다). 이번 작업 최대 함정.
  - `docker-compose.prod.yml`: app에 `LOG_DIR: /app/logs` env + `volumes: - applogs:/app/logs`,
    최상단 `volumes:`에 `applogs:` 추가. 포트·네트워크 무변경(DEPLOY-03 유지).
  - `.gitignore`: `logs/` 추가(기존 `*.log`는 `.log.gz`를 못 잡는다).
  - `application-dev.yml`: `logging.level.com.lostark.tracker: DEBUG` 추가 — 사장된 12개 DEBUG 문을
    dev에서만 살린다(prod는 `application-prod.yml`의 root INFO 유지).
- **verify:**
  - `./gradlew test` 통과 + 실행 후 `git status --porcelain`이 깨끗(테스트가 로그 파일을 안 남김).
  - dev 기동(포트 8080 리스너 kill 후) → `./logs/` 생성 및 콘솔·파일 양쪽 출력 확인.
  - `docker build` 후 `docker run --rm --entrypoint sh <img> -c 'ls -ld /app/logs && id'`로
    appuser 쓰기 권한 실측.
  - yml 오버라이드 실측: dev에서 DEBUG 문이 실제로 찍히는지 확인.
- **done:** dev/prod는 콘솔+파일(gz 롤링), test/seed는 콘솔만. 컨테이너 로그 디렉터리가 appuser
  소유이고 named volume에 보존된다.

## Task 2 — 로그 레벨 정책 수립 + 기존 22개 전수 교정

- **files:** `docs/logging-policy.md`(신규), `CLAUDE.md`(Conventions 섹션),
  기존 로깅 12개 클래스 중 판정이 바뀌는 파일
- **action:**
  - `docs/logging-policy.md`에 판정 기준을 표로 고정한다:
    - `ERROR` — 사람이 개입해야 함. 자동 복구 불가. 예: 인증 실패로 파이프라인 중단, 틱 전량 실패.
    - `WARN` — 지금은 굴러가지만 신호. fail-open 폴백이 발동했거나 부분 실패.
    - `INFO` — 운영에서 상시 켜두는 주요 흐름. **틱당 몇 줄 이내**로 예산을 명시.
    - `DEBUG` — 개발 진단. dev에서만 켠다.
    - 추가 규칙 2개: (a) **비밀 금지** — API 키·웹훅 URL은 어떤 레벨에도 남기지 않는다
      (기존 D-08/D-14 결정과 동일 선상, `summary_message` 규약을 로그로 확장).
      (b) **폭주 예산** — 아이템 루프 안에서는 WARN 이상 금지, 요약은 틱 단위 1줄.
  - `CLAUDE.md`의 "Conventions" 섹션(현재 미작성)에 3줄 요약 + 위 문서 링크.
  - 기존 22개를 정책표에 대조해 전수 재판정. 현재까지 확인된 승격 후보:
    - `backfill/DetailStatsBackfillRunner.java:90` `log.warn("detail backfill stopped — fatal auth")`
      → **ERROR** (백필 전체 중단 + 키 문제 = 사람 개입 필요)
    - `health/CollectionHeartbeat.java:65` → **WARN 유지** (감시만 꺼지고 수집은 정상 — fail-open 의도와 일치)
    - `cache/LatestPriceCache` DEBUG ×3 → **DEBUG 유지** (폴백이 성공하므로 정책상 DEBUG)
    - 나머지는 실행 시 정책표로 판정하고 SUMMARY에 판정 근거를 남긴다.
- **verify:** `grep -rho "log\.\(trace\|debug\|info\|warn\|error\)" src/main/java | sort | uniq -c`로
  분포 재측정 → `error` ≥ 1건. 정책 문서의 각 레벨에 코드 실례가 최소 1개씩 대응되는지 확인.
- **done:** 레벨 판정 기준이 문서로 존재하고, 기존 22개가 그 기준으로 재판정되어 `log.error` 0건 상태가 해소됨.

## Task 3 — 침묵 지점 로깅 보강 (수집 파이프라인 3개 파일)

- **files:** `src/main/java/com/lostark/tracker/ratelimit/RedisTokenBucket.java`,
  `src/main/java/com/lostark/tracker/collect/ItemFetchService.java`,
  `src/main/java/com/lostark/tracker/collect/PriceCollector.java`
- **action:** Task 2의 정책표를 그대로 적용한다. **동작은 바꾸지 않는다 — 로그만 추가**한다.
  - `RedisTokenBucket:63` → `log.warn`, 예외 **클래스명만**(스택 미포함). 아이템마다 호출되므로
    ERROR로 올리지 않는다. 진짜 신호는 아래 틱 요약이 담당.
  - `ItemFetchService`의 catch 4개 → 실패 사유별 1줄. `AuthApiException`도 여기선 WARN(아이템 단위),
    ERROR 승격은 틱 요약에서. `NOT_FOUND` 분기도 DEBUG 1줄.
  - `PriceCollector`:
    - 틱 시작 `INFO` 1줄(`run`, `items`), 틱 종료 `INFO` 1줄(`run`, `status`, `ok`, `fail`, `marker`).
      10분 간격이므로 하루 288줄 — INFO 예산 내.
    - **`items > 0 && succeeded == 0` → `log.error`** 1줄. 이번 작업에서 ERROR의 1순위 실례이자,
      Redis 장애·인증 실패가 전량 실패로 나타날 때의 유일한 로그 신호.
    - `awaitAll`의 Timeout/Interrupted → `WARN`, ExecutionException → `DEBUG`.
    - `settledResult`의 RuntimeException → `DEBUG`.
    - `persistSnapshot`의 `DataIntegrityViolationException` → `DEBUG`(멱등 스킵은 정상 동작).
- **verify:**
  - `./gradlew test` 전체 통과 — 기존 수집 IT의 카운트·status 단언이 그대로 통과해야 한다
    (동작 무변경 증명).
  - dev 기동 후 틱 1회 관측 → 시작/종료 INFO 2줄이 실제로 찍히는지 확인.
  - Redis만 내린 상태로 틱 1회 → `RedisTokenBucket` WARN + 틱 요약 `ERROR` 1줄 관측(전량 실패 경로 실증).
- **done:** 수집 실패의 사유가 로그로 남고, 전량 실패가 ERROR 1줄로 드러난다. 아이템 루프에서
  ERROR가 발생하지 않아 로그 폭주가 없다.

## 비목표 / 가드

- **MDC/traceId 필터, Micrometer/`micrometer-registry-prometheus`, Prometheus·Grafana·Discord 알림은
  이번 범위 밖**(다음 작업). `logback-spring.xml`에 MDC 자리만 주석으로 표시한다.
- **ELK 미도입** — 단일 VM·단일 인스턴스 규모에 부적합으로 판정. 재검토 대상 아님.
- **Actuator 노출 정책 무변경** — `include: health`, prod `show-details: never` 그대로. 이번 작업은
  actuator를 건드리지 않는다.
- **동작 변경 금지** — 로그 추가만. fail-open/fail-closed 정책, 재시도 정책, 타임아웃, 멱등 처리,
  `CollectionRun` 기록, heartbeat 모두 무변경.
- **비밀 유출 금지** — `LOSTARK_API_KEY`, `ADMIN_API_SECRET`, `COLLECTION_PING_URL`,
  `REDIS_PASSWORD`는 어떤 레벨에도 남기지 않는다. 예외 로깅은 클래스명·사유 코드까지만.
- 프론트엔드·Caddyfile·CI 워크플로 무변경. 배포는 별도(머지 → 이미지 빌드 → 승인).

## 커밋 계획

| # | 대상 | 메시지(안) |
|---|---|---|
| 1 | Task 1 | `feat(logging): logback-spring.xml 도입 — 콘솔+RollingFile(gz), prod 로그 볼륨` |
| 2 | Task 2 | `docs(logging): 로그 레벨 정책 수립 + 기존 로그 레벨 전수 교정` |
| 3 | Task 3 | `feat(logging): 수집 파이프라인 침묵 지점 로깅 보강 (동작 무변경)` |

현재 `main` 브랜치이므로 실행 전 작업 브랜치를 먼저 만든다(`quick/260825-lgz-logback`).
