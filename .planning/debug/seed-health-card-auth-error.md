# Debug: seed 모드 대시보드 수집 헬스 카드가 "12/12 실패 · 인증오류"

**발견 경로:** `/gsd-verify-work 11` Test 3 (Dashboard 실데이터로 채워짐) UAT 이슈
**심각도:** major
**상태:** diagnosed (코드 정적 분석으로 근본 원인 확정 — 라이브 재현은 fix 후 verify-work 재실행)

## 증상

seed 프로파일(`--spring.profiles.active=seed`, API 키 불필요)로 기동한 데모에서
Dashboard 수집 헬스 카드가 **"시도 12 · 성공 0 · 실패 12"** + **AUTH_ERROR(인증오류)** 마커로
표시됨. 같은 화면의 timeline/event-impact는 seed 데이터로 가득 차 정상(Test 4·5 pass).
→ Core Value(수집 파이프라인)가 100% 망가진 것처럼 보여 면접관 데모의 신뢰를 깸.

## 근본 원인 (코드 추적)

1. **`SyntheticDemoData.seed()`** (`src/main/java/.../seed/SyntheticDemoData.java`)
   — `PriceSnapshot`(8일치 1152틱/품목) + `GameEvent`(2건)만 적재하고 **`collection_run`은 심지 않음**.
   따라서 헬스 카드가 "seed 데이터가 수집 성공으로 들어왔다"를 표현할 행 자체가 없음.

2. **`CollectionHealthService.latestHealth()`** → `CollectionRunRepository.findTopByOrderByStartedAtDesc()`
   — 프로파일·나이 무관 **최신 collection_run 1건**만 반환. seed가 run을 안 심으므로,
   DB에 존재하는 유일한 run은 **라이브 @Scheduled tick**이 만든 것.

3. **라이브 수집기 키리스 발화** — `PriceCollector.collectTick()`은
   `@Scheduled(initialDelayString="${collection.initial-delay-ms:0}")`.
   - dev/기본 프로파일: `application-dev.yml`에 `collection.initial-delay-ms` 미설정 → **기본 0** → 부팅 즉시 발화.
   - `LOSTARK_API_KEY` 빈 값 → 12개 활성 품목(`WatchlistSeeder` WATCHLIST = **정확히 12개**) 전부 401/403.
   - 결과 run: `itemsAttempted=12, succeeded=0, failed=12, status=FAILED, summaryMessage=AUTH_ERROR`.

4. **영속 볼륨 잔존** — `docker-compose.yml`이 `pgdata:/var/lib/postgresql/data` **명명 볼륨**으로
   Postgres를 영속. → dev/기본 프로파일을 한 번이라도 돌렸거나 seed를 1h+ 띄우면 생긴
   AUTH_ERROR run이 `docker compose down/up` 후에도 살아남아 seed 대시보드의 "최신 run"으로 재등장.

5. **`HealthCard.tsx`** — `시도 {itemsAttempted} · 성공 {itemsSucceeded} · 실패 {itemsFailed}`
   + `SummaryMarker(summaryMessage)` 를 그대로 렌더 → 사용자 보고와 정확히 일치.

> 참고: 완전히 깨끗한 seed 전용 볼륨이라면 헬스 카드는 `NO_RUNS`("아직 수집 실행 기록이 없어요")로
> 뜸 — 실패는 아니지만, timeline/impact는 가득 찬데 헤드라인 수집 헬스만 비어 **데모 내적 모순**.

## 수정 방향 (→ 11-04-PLAN.md)

- **(주)** `SyntheticDemoData.seed()`가 합성 **SUCCESS** `collection_run`을 멱등 적재:
  `attempted=items.size()(=12), succeeded=동일, failed=0, status="SUCCESS", summaryMessage=null,
  startedAt=finishedAt=gridNow`. `startedAt=gridNow`가 최신이라 영속 볼륨의 과거 실패 run을 덮고
  헬스 카드가 녹색 "12/12 성공"으로 표시됨.
- `CollectionRunRepository.existsByStartedAtAndStatus(...)`로 멱등 가드(같은 10분 그리드 재시드 시 중복 금지).
- `SyntheticDemoDataIT`에 회귀 단언: seed 후 latest run = SUCCESS(12/12), 과거 AUTH_ERROR run 선적재 시에도 seed가 최신 SUCCESS로 덮음.
- (선택적 하드닝) seed 프로파일에서 라이브 스케줄러 완전 비활성화 검토 — 1h initial-delay는 일반 데모만 커버.

## 영향 파일
- `src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java` (주 수정)
- `src/main/java/com/lostark/tracker/repository/CollectionRunRepository.java` (멱등 가드 메서드)
- `src/test/java/com/lostark/tracker/seed/SyntheticDemoDataIT.java` (회귀 테스트)
