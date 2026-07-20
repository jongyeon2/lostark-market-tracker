---
quick_id: 260718-jrz
slug: monitoring-alerting
description: 능동 모니터링·알림 구현 — 수집 하트비트(데드맨) + 백업 즉시 /fail
date: 2026-07-18
status: complete
spec: docs/specs/2026-07-17-monitoring-alerting-design.md
commits: [f873010, bd013b5, 9e8f36b, 945e151]
---

# Quick Task 260718-jrz — 능동 모니터링·알림 구현

## 무엇을 만들었나

혼자 운영하는 무료 VM이 **"죽어도 모르는"** 상태를 없애기 위한 능동 감시의 **코드 절반**을 구현했다.
설계 스펙(`docs/specs/2026-07-17-monitoring-alerting-design.md`, 사용자 승인 2026-07-17)의
§7~§9를 코드로 옮겼다. 수집·캐시·서빙 로직은 0줄 변경(Core Value 가드) — 하트비트는 `collectTick()`
끝의 additive fail-open 훅 1줄이다.

- **`CollectionHeartbeat`**(신규, `health` 패키지): 매 수집 틱 결과를 외부 데드맨 스위치(healthchecks.io)에
  핑. **`succeeded` 카운트로 판단**(status 문자열 아님): `>0`→기본 URL, `==0`→`/fail`(즉시 알림).
  빈 ping-url이면 no-op, 핑 실패는 삼킨다(fail-open).
- **`MonitoringConfig`**(신규): 3초 타임아웃 전용 `RestClient` @Bean(`ApiClientConfig` 패턴 미러).
- **`PriceCollector`**(수정): 생성자에 하트비트 1개 추가, `collectionRunRepository.save(run)` **이후**
  `heartbeat.report(succeeded)` 1줄.
- **설정 배선**: `application.yml`(`monitoring.collection.ping-url=${COLLECTION_PING_URL:}` — 빈 기본값=전역
  비활성), `docker-compose.prod.yml`(env 전달), `.env.prod.example`(빈 키+주석, `BACKUP_PING_URL`과 대칭).
- **`backup-db.sh` `die()`**(수정, 선택항목 1): 백업 실패 시 `/fail` 즉시 핑 → 발견이 ~26시간에서 즉시로.

## 핵심 결정 (스펙 근거)

- **`succeeded > 0` 규칙(§4):** status를 안 쓰는 이유는 빈 워치리스트가 `0 == items.size()`로 **거짓 SUCCESS**를
  기록하기 때문. 이건 가상이 아니라 저장소가 이미 아는 버그(`application-prod.yml:6-8`, 기존 우회 `initial-delay-ms:10000`은
  타이밍 레이스). `succeeded > 0`은 결정적 가드라 그 레이스를 감시가 물려받지 않는다.
  → **회귀 테스트로 고정**: 빈 워치리스트에서 run.status는 "SUCCESS"인데 `report(0)`이 호출됨을 단언.
- **예외 객체를 로거에 안 넘김(§5):** ping URL은 시크릿(아는 자가 "정상"을 위조해 알림을 영구 침묵). Spring의
  `ResourceAccessException` 메시지에 URL이 박히므로 **예외 클래스명만 로깅**(`log.warn("... ({})", e.getClass().getSimpleName())`),
  메시지·스택트레이스 금지. `backup-db.sh:78`의 셸 규칙을 Java로 옮긴 것. → **회귀 테스트로 고정**(로그에 URL 부재 단언).
- **검증 가능 패턴 채택(PLAN 대비 편차):** `CollectionHeartbeat`를 `@Component` 단독이 아니라 config @Bean이 빌더를
  주입하는 형태로 구현. request factory를 컴포넌트가 직접 세팅하면 `MockRestServiceServer` 바인딩을 덮어써 단위테스트가
  불가능해진다 — `ApiClientConfig`/`LostarkApiClient`가 이미 쓰는 저장소 표준 분리를 그대로 미러.

## 실행 중 잡은 버그 (안 돌려봤으면 못 잡았을 것)

**증상:** `emptyWatchlistReportsZeroToHeartbeatDespiteFalseSuccessStatus`가 `report(0)` 2회로 실패
(`TooManyActualInvocations`), 격리 실행에서도 재현.

**원인:** `PriceCollectionIT`에는 **`@ActiveProfiles("test")`가 없다**(다른 IT들은 다 있음). 그래서
`collection.initial-delay-ms`가 0으로 떨어지고 **실제 `@Scheduled` 수집기가 부팅 즉시 틱**을 돈다(빈
`tracked_item` → `report(0)`). 하트비트를 `@MockitoBean`으로 교체하면 컨텍스트의 스케줄 빈이 쏜 그 stray
틱까지 검증 카운트에 섞인다.

**수정:** 컨텍스트 빈을 교체하지 않는 **로컬 mock 필드**로 전환(다른 collect IT 2개와 동일 방식). 스케줄 빈은
실제 no-op 하트비트를 쓰고, 수동 collector만 필드 mock을 호출 → 검증이 결정적. (커밋 945e151)

이 잠재 이슈는 백업 quick-260716-h1e가 "안 돌려봤으면 매일 실패했을 버그"를 잡은 것과 같은 성격 — 검증
어서션이 처음으로 스케줄러의 부작용을 드러냈다.

## 검증

- `./gradlew build` **BUILD SUCCESSFUL** (198 통과, 실패/에러 0, 스킵 12).
- 신규/수정 테스트 전부 그린: `CollectionHeartbeatTest` 5/5, `PriceCollectionIT` 5/5, `CollectionResilienceIT` 4/4.
- `bash -n scripts/backup-db.sh` 문법 OK, 저장소 blob은 LF(CR 0).
- 추적 파일에 실 핑/웹훅 URL **0건**(`git grep` hc-ping/hchk/discord webhooks → docs 플레이스홀더 외 없음).

## 라이브 검증 완료 (2026-07-18) ✅

감시는 "알림이 오는 걸 눈으로 봐야" 검증된 것(§10). 운영 VM에서 실증 완료 — 자세한 운영 절차는
런북 §12에 반영됨(`docs/deploy/oracle-vm-runbook.md`).

- ✅ healthchecks 체크 2개 라이브: `lostark-collection-prod`(10분/5분), `lostark-db-backup-prod`(1일/1시간)
- ✅ 배포 후 Java 앱이 실제 collection 핑을 보내는 것 확인, `/actuator/health`={"status":"UP"}
- ✅ Discord 장애·복구 테스트: collection·backup 각각 `/fail`→DOWN, 정상 핑→UP 복구 확인
- ✅ UptimeRobot 5분 `/actuator/health` 감시 + Discord 장애·복구 테스트(`/actuator/nope`→404 DOWN, 복구 UP)
- ✅ `.env.prod`에 `COLLECTION_PING_URL`·`BACKUP_PING_URL`만 저장(chmod 600), Discord 웹훅은 대시보드에만
- ✅ `/security-review` 실행 — §5 유출 경로 관련 신뢰도 8+ 취약점 **0건**(URL 무유출 설계가 도구로도 확인됨)
- 로그 URL 미노출 검사 명령은 런북 §12.5에 상시 절차로 기록

## 사용자 작업 (외부 서비스 — 스펙 §11) — 완료

1. ✅ healthchecks.io 체크 2개(collection 10분/5분, backup 1일/1시간) — Discord Integration 활성화
2. ✅ UptimeRobot 모니터: `https://loaket.kr/actuator/health` 5분, 태그 loaket·production·health
3. ✅ 세 곳 모두 Discord 연결
4. ✅ VM `.env.prod`에 `COLLECTION_PING_URL` 추가 → `app`만 안전 재생성(`--no-deps --force-recreate app`)
5. ✅ **백업 cron 실가동 재확인 완료 (2026-07-19)**: `crontab -l`에 `17 3 * * *` 등록 확인 → `journalctl -u cron`에서 07-17·18·19 **자율 실행** 확인 → `backup.log`에 07-16~19 **4일 연속 업로드 성공(HTTP 200)** → healthchecks `lostark-db-backup-prod` 매일 12:17 KST OK·현재 UP. cron 자율 실행부터 Object Storage 업로드까지 전 구간 실증 — 백업 자동화(런북 §11) 완료.

## 커밋

| 커밋 | 내용 |
|---|---|
| f873010 | CollectionHeartbeat + MonitoringConfig + 테스트 5개 |
| bd013b5 | PriceCollector 배선(report 훅) + IT 3개 정합 + 검증 테스트 2개 |
| 9e8f36b | 설정 배선(application.yml/compose/.env.example) + backup-db.sh die() /fail |
| 945e151 | PriceCollectionIT 로컬 mock 전환(스케줄 startup 틱 오염 차단) |

푸시·배포 완료. 라이브 검증(§ 라이브 검증 완료)·런북 §12 반영 완료.
