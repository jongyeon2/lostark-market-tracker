# 능동 모니터링 · 알림 (사이트 · 수집 · 백업) — 설계 스펙

**작성:** 2026-07-17
**상태:** 설계 확정 (구현 대기) — 사용자 승인 2026-07-17
**대상:** 운영 유지보수 (마일스톤 외 quick 트랙 — 백업 데드맨 quick-260716-h1e의 후속)

## 1. 개요 / 목표

혼자 운영하는 무료 VM에서 **"죽어도 모른다"를 없앤다.** 지금 이 시스템은 조용히 죽을 수 있다 — 사이트가 내려가도, 수집이 멈춰도, 백업이 실패해도 내가 우연히 들여다볼 때까지 아무도 알려주지 않는다. 수집이 멈춘 시간은 **영구 손실**이다(상세 `Stats[]` 소급은 ~2주가 한계, 보석은 이력 API 부재로 애초에 소급 불가 — Phase 24 §H5).

세 신호를 **Discord로 능동 통지**한다: 사이트 다운, 수집 정지·전량 실패, 백업 실패.

**이 설계의 핵심 원칙: 거짓 안심을 만들지 않는다.** 감시가 "정상"이라고 말할 때는 정말로 정상이어야 한다. 초록불이 거짓말하면 감시가 없느니만 못하다 — 없으면 최소한 의심이라도 한다.

## 2. 범위 / 비범위

**In scope**
- 수집 하트비트: `PriceCollector` 성공 시 healthchecks.io 데드맨 핑 / 전량 실패 시 즉시 `/fail`
- 백업 실패 시 즉시 `/fail` 핑 (`scripts/backup-db.sh` — 기존 성공 핑의 대칭)
- 사이트·TLS 외부 능동 감시 (UptimeRobot 무료 → Discord)
- 알림 라우팅: healthchecks.io·UptimeRobot → **Discord 웹훅**
- 런북 문서화 (체크 생성·Discord 연결·오탐 대응·핑 URL 재발급)

**Out of scope (비범위)**
- **Prometheus / Grafana / Loki** — 무료지만 4GB ARM 단일 박스에 과하고, 감시 스택 자체가 운영 부담이 된다
- **`PARTIAL_SUCCESS` 알림** — 오탐의 원천 (§4 참조)
- 로그 기반 알림, 상태 페이지, 에스컬레이션·온콜, SMS
- 수집/캐시/event-impact/서빙 **로직 변경** — Core Value 가드. 하트비트는 `collectTick()` 끝의 **additive fail-open 훅** 1줄이며 수집 판단에 개입하지 않는다
- `management.endpoint.health.show-details` 변경 — **불필요**(§6 참조)

## 3. 아키텍처 — 신호 3개, 도구 3개

| 신호 | 잡는 것 | 방식 | 도구 | 탐지 지연 |
|---|---|---|---|---|
| 사이트 · TLS | Caddy·VM·앱·DB 다운, 인증서 만료 | 외부 능동 프로브 | UptimeRobot 무료 | 5분 |
| 수집 | 스케줄러 정지, 전량 실패(키 만료 등) | 데드맨 핑 + 즉시 `/fail` | healthchecks.io | 15분 / 즉시 |
| 백업 | 덤프·업로드 실패, PAR 만료 | 데드맨 핑 + 즉시 `/fail` | healthchecks.io | 25시간 / 즉시 |

**두 메커니즘의 겹침은 의도적이다.** 외부 프로브는 빠르고 독립적인 시점(우리 인프라가 통째로 죽어도 살아있음)이고, 데드맨은 느리지만 파이프라인 **내부**를 본다. 하나가 실패해도 다른 하나가 남는다.

### 사이트 프로브 대상: `https://loaket.kr/actuator/health` 하나

Spring이 DB·Redis 컴포넌트가 죽으면 **HTTP 503**을 반환하므로, 단순 상태코드 감시만으로 Caddy·TLS·앱·DB·Redis를 한 번에 덮는다. 키워드 감시 불필요(무료 티어 기능 의존도 낮춤).

### 데드맨이 필요한 이유 — 폴링으로는 못 잡는 것

`/api/health/collection`은 **최신 `collection_run` 행의 status를 그대로** 반환한다(`CollectionHealthService.latestHealth()`). 따라서 **스케줄러가 통째로 죽으면 마지막 `SUCCESS` 행이 영원히 남아 "겉보기 정상"**이 된다. 외부에서 이 엔드포인트를 키워드 폴링하는 방식은 이 실패를 **원리적으로 못 잡는다**. 침묵을 감지하는 데드맨만이 잡는다.

## 4. 🔑 핵심 결정: 핑 규칙은 `succeeded` 카운트로 판단한다

`collectTick()`은 `SUCCESS` / `PARTIAL_SUCCESS` / `FAILED`로 끝난다. 핑은 **status 문자열이 아니라 `succeeded` 카운트**로 가른다.

| 조건 | 동작 | 의미 |
|---|---|---|
| `succeeded > 0` | 기본 URL 핑 | 파이프라인 살아있음 |
| `succeeded == 0` | `/fail` 핑 → **즉시** Discord | 파이프라인 고장 |
| (핑 없음 = 침묵) | 데드맨 15분 후 Discord | 아예 안 돌고 있음 |

**status를 쓰지 않는 이유 — 거짓 안심 엣지.** 현재 코드는 `succeeded == items.size()`로 SUCCESS를 판정한다(`PriceCollector:127`). **워치리스트가 비면 `0 == 0`이라 SUCCESS가 기록된다.** 즉 아무것도 수집하지 않으면서 "건강함" 핑을 보내게 된다. `succeeded > 0` 기준은 전량 실패와 이 엣지를 **한 규칙으로** 덮는다.

이건 가상의 엣지가 아니다. **저장소가 이미 이 버그를 알고 있다** — `application-prod.yml:6-8`:

> `WatchlistSeeder`(ApplicationRunner)가 첫 스케줄 tick 전에 워치리스트를 upsert하도록 첫 tick을 지연. 안 그러면 빈 `tracked_item`에 첫 수집이 돌아 `items_attempted=0`로 **잘못 SUCCESS 기록됨**.

기존 대응은 `initial-delay-ms: 10000`, 즉 **타이밍 레이스에 건 우회**다 — 시더가 10초보다 느리면(느린 디스크·콜드 부팅·마이그레이션 동반) 같은 거짓 SUCCESS가 다시 난다. `succeeded > 0`은 타이밍에 의존하지 않는 **결정적 가드**이므로, 감시가 그 레이스를 물려받지 않는다.

**배포 오탐 없음:** 첫 틱이 부팅 후 ~10초라 재배포로 인한 수집 공백은 데드맨 허용치(10분 period + 5분 grace = 15분) 안에 들어온다.

**`PARTIAL_SUCCESS`에 알림을 보내지 않는 것도 결정이다.** 아이템 하나가 상장폐지되거나 일시적 429를 맞을 때마다 새벽에 알림이 오면, 사람은 감시를 끈다. 꺼진 감시는 없는 감시다. 대신 `AUTH_ERROR`(키 만료 — **이미 실제로 겪은 실패**, 2026-06-30 Blocker)는 전량 실패라 `/fail`로 즉시 잡힌다.

**명시적 한계:** 절반이 계속 실패하는 **완만한 저하는 이 설계가 잡지 않는다.** 그건 `/api/health/collection`과 대시보드가 담당하고, 실제로 문제가 되면 그때 임계값 기반 감시를 붙인다(YAGNI).

## 5. 🔑 보안 제약: 예외 객체를 로거에 넘기지 않는다

**핑 URL은 시크릿이다.** 이 URL을 아는 자는 "살아있음"을 위조해 **알림을 영구히 침묵**시킬 수 있다 — 감시 시스템의 최악 실패인 거짓 침묵이다.

Spring의 `RestClient`는 연결 실패 시 `ResourceAccessException`을 던지는데, **그 메시지에 요청 URL이 그대로 박힌다**:

```
I/O error on GET request for "https://hc-ping.com/<uuid>": Connection timed out
```

따라서 기존 fail-open 패턴(`BackfillCaptureService:54`)의 `log.warn("...", item.getId(), e)`처럼 **`e`를 로거에 넘기면 시크릿이 로그에 남는다.** 스택트레이스도 금지다(첫 줄에 메시지가 들어간다).

이건 이 저장소가 **이미 아는 규칙**이다. `scripts/backup-db.sh:78`:

> `curl -f`는 실패 메시지에 URL을 실을 수 있어 쓰지 않고, http_code만 직접 받는다.

셸에서 지킨 규칙을 Java로 옮기며 놓치지 않는다. **규칙: `CollectionHeartbeat`는 예외 클래스명만 로깅한다** — `log.warn("collection heartbeat ping failed ({})", e.getClass().getSimpleName())`. **테스트로 고정한다**(§9).

| 자산 | 보관 위치 | 저장소 유입 |
|---|---|---|
| `COLLECTION_PING_URL` | VM `.env.prod`만 | ❌ (`.env.prod.example`엔 **빈 값**) |
| `BACKUP_PING_URL` | VM `.env.prod`만 | ❌ (기존) |
| **Discord 웹훅 URL** | healthchecks·UptimeRobot 대시보드만 | ❌ **코드·설정 어디에도 없음** |

**수용 리스크:** VM 셸을 얻은 자는 `docker inspect`·`/proc/<pid>/environ`으로 핑 URL을 볼 수 있다. 기존 `LOSTARK_API_KEY`·`ADMIN_API_SECRET`과 동일한 자세이므로 새 리스크가 아니다.

## 6. 확인된 사실 (검증 완료 — 추측 아님)

- `https://loaket.kr/actuator/health` → **`{"status":"UP"}`** (2026-07-17 실측). `application-prod.yml:22`가 `show-details: never`로 base의 `always`를 오버라이드하고 있어 **상세 노출 없음** → 하드닝 불필요.
- `show-details: never`여도 컴포넌트 DOWN 시 **HTTP 503은 그대로** 나가므로 프로브 설계는 유효하다.
- 저장소에 커밋된 시크릿 **0건** — 런북의 핑/PAR URL은 전부 플레이스홀더(`<uuid>`, `<토큰>`).
- `.gitignore`가 `.env.*`를 막고 `!.env.prod.example`만 예외 → 실 시크릿 커밋 경로 없음.
- 앱 컨테이너는 이미 로스트아크 API로 아웃바운드 → **hc-ping.com에 새 방화벽 구멍 불필요**. 인바운드 포트 추가 0.

## 7. 컴포넌트

### 신규 — `com.lostark.tracker.health.CollectionHeartbeat`

`CollectionHealthService` 옆(같은 패키지 = 수집 건강 관심사).

| 항목 | 내용 |
|---|---|
| 책임 | 수집 틱 결과를 외부 데드맨에 알린다. 그 외 아무것도 안 한다 |
| 인터페이스 | `void report(int succeeded)` |
| 의존 | 전용 `RestClient`(3초 connect/read — `ApiClientConfig` 방식 미러), `monitoring.collection.ping-url` |
| **비활성** | ping-url이 **빈 값이면 HTTP 호출 자체를 하지 않는다**(no-op) |
| **fail-open** | `RuntimeException` 삼킴 — 핑 실패가 수집을 절대 깨뜨리지 않는다 |
| **로깅** | 예외 클래스명만. **URL·예외 메시지·스택트레이스 금지**(§5) |

### 수정 — `PriceCollector`

생성자 파라미터 1개 추가 + `collectTick()` **맨 끝** 1줄(`collectionRunRepository.save(run)` **이후** — 핑 실패가 run 기록에 영향을 못 주게).

> 기존 테스트가 생성자를 직접 호출하므로 mock 주입이 필요하다(컴파일 영향 있음).

### 수정 — `scripts/backup-db.sh`

`die()`에 `/fail` 핑 추가. 발견 시간이 ~26시간 → **즉시**로 줄어든다.

**제약:** `curl`의 stderr를 반드시 버린다(`2>/dev/null`) — 안 그러면 실패 메시지로 URL이 cron 메일에 샌다. 기존 성공 핑(94행)이 이미 그렇게 하고 있다. `PING_URL`이 비어 있으면(= 아직 미설정) 조용히 건너뛴다.

## 8. 설정 · 시크릿 배선

```yaml
# application.yml — 빈 기본값 = 전역 비활성. dev·test·CI는 아무 설정 없이 무영향, prod만 켜진다.
monitoring:
  collection:
    ping-url: ${COLLECTION_PING_URL:}
```

- `docker-compose.prod.yml` `app.environment`에 `COLLECTION_PING_URL` 전달
- `.env.prod.example`에 **빈 키 + 주석**(기존 `BACKUP_PING_URL` 블록 형식 따름)
- VM `.env.prod`에 실값 — **사용자 작업**

## 9. 테스트

`CollectionHeartbeat` 단위 테스트:

1. ping-url이 **빈 값 → HTTP 호출 자체가 없음**(비활성)
2. `succeeded > 0` → **기본 URL** 호출
3. `succeeded == 0` → **`/fail`** 호출
4. HTTP가 예외를 던져도 **삼켜짐**(fail-open — 호출자에게 전파 안 됨)
5. 🔑 **핑 URL이 로그에 안 나온다** — 실패를 유발한 뒤 캡처한 로그에 URL 문자열이 **없음**을 단언(§5 제약을 회귀로 고정)

`PriceCollector`: mock 하트비트로 `succeeded` 전달 검증. 기존 수집 테스트는 무영향이어야 한다.

## 10. 검증 게이트

- `./gradlew build` 그린
- **`/security-review` 실행** — 하트비트 코드가 존재하는 상태에서. 수동 검토가 찾은 §5 유출 경로를 도구가 독립적으로 확인하는지가 교차 검증
- 배포 후 라이브 실증:
  - healthchecks 대시보드에 **실제 핑 도착** 확인(10분 주기)
  - **Discord로 실제 알림 수신** 확인 — 체크를 수동 일시정지/`/fail` 호출로 유발. **알림이 실제로 오는 걸 보기 전엔 완료가 아니다**(백업 quick-260716-h1e가 "안 돌려봤으면 매일 실패했을 버그"를 잡은 것과 같은 이유)
  - VM 로그에 핑 URL 미출현 확인

## 11. 사용자 작업 (외부 서비스 — 코드 밖)

1. healthchecks.io 체크 2개 생성: `collection`(period 10분 / grace 5분), `backup`(period 1일 / grace 1시간)
2. UptimeRobot 모니터 1개: `https://loaket.kr/actuator/health`, 5분
3. 세 곳 모두 **Discord 통합** 연결 (healthchecks·UptimeRobot 무료 플랜 모두 Discord 지원 확인됨)
4. VM `.env.prod`에 `COLLECTION_PING_URL` 추가 → 앱 재기동
5. **백업 cron 실가동 검증** — `crontab -l | grep backup` (quick-260716-h1e에서 미검증으로 남은 항목)

## 12. 채택하지 않은 대안

| 대안 | 기각 사유 |
|---|---|
| 외부에서 `/api/health/collection` 키워드 폴링 (앱 코드 0) | **죽은 스케줄러를 원리적으로 못 잡는다**(§3) — Core Value를 감시하는 데 구멍이 치명적 |
| GitHub Actions 스케줄 폴러 (staleness 검사, 앱 코드 0) | staleness로 죽은 스케줄러는 잡지만, **GitHub이 60일 무활동 시 스케줄 워크플로를 자동 비활성화** → 감시가 조용히 멈춘다. 감시 도구 자신이 조용히 죽는 건 이 설계가 없애려는 바로 그 실패 |
| Prometheus + Grafana + Alertmanager | 4GB ARM 단일 박스에 과함. 감시 스택 운영이 본체 운영보다 무거워진다 |

## 13. 왜 이 설계가 이 프로젝트에 맞는가 (포트폴리오 관점)

데드맨 스위치는 금융 마켓 데이터 파이프라인의 표준 감시 도구이며, 이 프로젝트의 프레이밍과 정합한다. 그리고 **새 패턴이 아니다** — `BackfillCaptureService`가 이미 "수집 성공에 올라타는 additive fail-open 훅"이고, `backup-db.sh`가 이미 데드맨이다. 이 설계는 **기존 패턴을 한 번 더 적용**하며, 그 과정에서 저장소가 셸에 적어둔 시크릿 규칙을 Java로 옮긴다.
