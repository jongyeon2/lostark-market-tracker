---
quick_id: 260718-jrz
slug: monitoring-alerting
description: 능동 모니터링·알림 구현 — 수집 하트비트(데드맨) + 백업 즉시 /fail
date: 2026-07-18
spec: docs/specs/2026-07-17-monitoring-alerting-design.md
status: planned
---

# Quick Task 260718-jrz: 능동 모니터링·알림 구현

**설계 스펙:** `docs/specs/2026-07-17-monitoring-alerting-design.md` (사용자 승인 2026-07-17, 선택항목 1=백업 즉시 /fail 포함)

이 계획은 스펙 §7~§9를 코드로 옮긴다. 앱 로직(수집·캐시·서빙)은 건드리지 않는다 — 하트비트는 `collectTick()` 끝의 additive fail-open 훅 1줄이다(Core Value 가드, §2).

## must_haves

**truths**
- `CollectionHeartbeat`는 ping-url이 빈 값이면 HTTP 호출을 하지 않는다 (dev·test·CI 무영향, §7).
- 핑 판단은 `succeeded` 카운트로 한다: `>0`→기본 URL, `==0`→`/fail` (§4).
- 핑 실패는 수집을 절대 깨뜨리지 않는다(fail-open, RuntimeException 삼킴, §7).
- 예외 객체·URL·스택트레이스는 로그에 나가지 않는다 — 클래스명만 (§5).
- `backup-db.sh die()`는 실패 시 `/fail` 핑을 보내되 curl stderr를 버린다(URL 유출 차단, §7).

**artifacts**
- `src/main/java/com/lostark/tracker/health/CollectionHeartbeat.java` (신규)
- `src/test/java/com/lostark/tracker/health/CollectionHeartbeatTest.java` (신규, 테스트 5개)
- `src/main/java/com/lostark/tracker/collect/PriceCollector.java` (수정: 생성자 +1, collectTick() 끝 +1줄)
- `src/main/resources/application.yml` (monitoring.collection.ping-url)
- `docker-compose.prod.yml` (COLLECTION_PING_URL 전달)
- `.env.prod.example` (빈 키 + 주석)
- `scripts/backup-db.sh` (die() /fail 핑)
- 기존 IT 3개 생성자 호출부 수정 + PriceCollector report 검증 테스트

**key_links**
- 스펙 §4 핑 규칙, §5 보안 제약, §7 컴포넌트, §8 설정, §9 테스트
- `PriceCollector.java:127` succeeded 판정, `:133` collectionRunRepository.save(run)
- `ApiClientConfig.java:22-28` RestClient 3초 타임아웃 패턴(미러)
- `backup-db.sh:26` die(), `:94` 성공 핑(2>/dev/null 패턴)
- `application-prod.yml:6-8` 빈 워치리스트 거짓 SUCCESS 버그(근거)

---

## Task 1 — CollectionHeartbeat (TDD: 테스트 먼저)

**name:** 수집 하트비트 컴포넌트 + 단위 테스트 5개

**files:**
- `src/main/java/com/lostark/tracker/health/CollectionHeartbeat.java` (신규)
- `src/test/java/com/lostark/tracker/health/CollectionHeartbeatTest.java` (신규)

**action:**
1. **테스트 먼저 작성**(`CollectionHeartbeatTest`, MockRestServiceServer + LogCaptor/Logback ListAppender):
   - T1: ping-url 빈 값 → `report(3)` 호출해도 서버에 요청 0건(비활성).
   - T2: `succeeded > 0` → 기본 URL(`BASE`)로 GET 1건.
   - T3: `succeeded == 0` → `BASE + "/fail"`로 GET 1건.
   - T4: 서버가 5xx/예외 유발 → `report()`가 예외를 던지지 않음(fail-open).
   - T5: 🔑 실패 유발 후 캡처한 로그 문자열에 ping-url이 **없음**을 단언(§5 회귀 고정).
2. `CollectionHeartbeat` 구현:
   - `@Component`, 패키지 `com.lostark.tracker.health` (CollectionHealthService 옆).
   - 생성자: `CollectionHeartbeat(RestClient.Builder builder, @Value("${monitoring.collection.ping-url:}") String pingUrl)`.
   - 전용 RestClient: `SimpleClientHttpRequestFactory` connect/read 3초(`ApiClientConfig` 미러). baseUrl 미설정 — `report()`에서 절대 URL 사용.
   - `void report(int succeeded)`:
     - `pingUrl`이 blank면 즉시 return(no-op).
     - `String target = succeeded > 0 ? pingUrl : pingUrl + "/fail";`
     - try: `restClient.get().uri(URI.create(target)).retrieve().toBodilessEntity();`
     - catch `RuntimeException e`: `log.warn("collection heartbeat ping failed ({})", e.getClass().getSimpleName());` — **e를 넘기지 않는다**. URL/메시지/스택트레이스 금지.
   - 테스트가 builder를 주입할 수 있게 생성자는 `RestClient.Builder`를 받는다(MockRestServiceServer bindTo 가능).

**verify:** `./gradlew test --tests "com.lostark.tracker.health.CollectionHeartbeatTest"` 그린. T5가 로그에 URL 부재를 단언.

**done:** 5개 테스트 통과, 빈 URL no-op·succeeded 분기·fail-open·로그 무유출 모두 고정됨.

---

## Task 2 — PriceCollector 배선 + 기존 테스트 정합

**name:** collectTick() 끝에 하트비트 훅 1줄 + 생성자 파라미터

**files:**
- `src/main/java/com/lostark/tracker/collect/PriceCollector.java`
- `src/test/java/com/lostark/tracker/collect/PriceCollectionIT.java`
- `src/test/java/com/lostark/tracker/collect/CollectionResilienceIT.java`
- `src/test/java/com/lostark/tracker/read/LatestPriceCacheIT.java`

**action:**
1. `PriceCollector` 생성자에 `CollectionHeartbeat heartbeat` 추가 — `backfillCaptureService` 뒤, `Clock clock` 앞(빈 협력자를 묶음). 필드 저장.
2. `collectTick()` **맨 끝**(`collectionRunRepository.save(run);` **이후**) 1줄: `heartbeat.report(succeeded);`
   - run 기록 저장 후에 호출 → 핑 실패가 run 저장에 영향 못 줌. `succeeded`는 이미 계산된 지역변수.
3. 기존 IT 3곳 `new PriceCollector(...)` 호출부에 헤드비트 인자 삽입:
   - `CollectionResilienceIT`, `LatestPriceCacheIT`: `mock(CollectionHeartbeat.class)` 인라인 주입(무영향).
   - `PriceCollectionIT`: 헤드비트 mock을 필드/지역으로 잡아 신규 검증 테스트에서 `verify(heartbeat).report(<기대 succeeded>)` 단언.
4. `PriceCollectionIT`에 신규 테스트 1개: 성공 N건 틱 후 `report(N)`이 정확히 호출됨을 검증(§9 "mock 하트비트로 succeeded 전달 검증").

**verify:** `./gradlew test --tests "com.lostark.tracker.collect.*" --tests "com.lostark.tracker.read.LatestPriceCacheIT"` 그린. 기존 수집 동작 무변경.

**done:** PriceCollector가 매 틱 succeeded를 하트비트에 넘기고, 기존 IT는 여전히 통과, 신규 검증 테스트가 전달을 고정.

---

## Task 3 — 설정·시크릿 배선 + 백업 즉시 /fail

**name:** application.yml/compose/.env.example + backup-db.sh die() /fail

**files:**
- `src/main/resources/application.yml`
- `docker-compose.prod.yml`
- `.env.prod.example`
- `scripts/backup-db.sh`

**action:**
1. `application.yml`에 블록 추가(빈 기본값 = 전역 비활성):
   ```yaml
   monitoring:
     collection:
       ping-url: ${COLLECTION_PING_URL:}
   ```
2. `docker-compose.prod.yml` `app.environment`에 `COLLECTION_PING_URL: ${COLLECTION_PING_URL:-}` 추가(기존 시크릿 주입 블록 형식).
3. `.env.prod.example`에 `BACKUP_PING_URL` 블록과 대칭인 `COLLECTION_PING_URL=` 빈 키 + 주석(데드맨 설명, 10분 period/5분 grace 권장) 추가.
4. `scripts/backup-db.sh` `die()` 수정: exit 전에 `PING_URL`이 있으면 `${PING_URL%/}/fail`로 즉시 핑.
   - **제약(§7):** curl stderr 반드시 `2>/dev/null`. `PING_URL` 비면 조용히 건너뜀. 핑 실패로 스크립트 흐름 안 바꿈(어차피 die로 exit 1).
   - `PING_URL`은 `die()` 시점에 이미 읽혔을 수도/아닐 수도 있음(초기 검증 die는 PING_URL 읽기 전) → `die()` 내부에서 `${PING_URL:-}` 가드로 미설정 시 skip.

**verify:** `bash -n scripts/backup-db.sh` 문법 OK. `grep -n "COLLECTION_PING_URL" application.yml docker-compose.prod.yml .env.prod.example`로 3곳 배선 확인. `.env.prod.example`·소스에 실 URL 0건.

**done:** prod만 켜지는 빈-기본값 배선 완료, 백업 실패 발견이 ~26h→즉시로 단축, URL 유출 경로 없음.

---

## Task 4 — 빌드 그린 + 검증 게이트

**name:** ./gradlew build + 시크릿 부재 확인

**files:** (없음 — 검증만)

**action:**
1. `./gradlew build` 전체 그린.
2. 추적 파일에 핑/웹훅 URL 문자열 부재 확인(`git grep -i "hc-ping\|hchk\|discord.com/api/webhooks"` → 스펙/런북 플레이스홀더 외 0건).
3. 스펙 §10의 `/security-review`는 **사용자 트리거 게이트**로 SUMMARY에 명시(코드 존재 후 사용자가 실행).

**verify:** `./gradlew build` BUILD SUCCESSFUL. git grep에 실 시크릿 0건.

**done:** 빌드 그린, 커밋에 시크릿 없음, 남은 검증(security-review·라이브 Discord 실증)이 SUMMARY에 사용자 작업으로 이관됨.

---

## 실행 후 (SUMMARY에 기록할 사용자 작업 — 스펙 §11)
1. healthchecks.io 체크 2개: `collection`(10분/5분 grace), `backup`(1일/1시간 grace)
2. UptimeRobot 모니터: `https://loaket.kr/actuator/health` 5분
3. 세 곳 Discord 통합 연결
4. VM `.env.prod`에 `COLLECTION_PING_URL` 실값 → 앱 재기동
5. 백업 cron 실가동 검증(`crontab -l | grep backup`)
6. **`/security-review` 실행 + Discord 실제 알림 수신 확인** — 알림이 오는 걸 보기 전엔 완료 아님(§10)
