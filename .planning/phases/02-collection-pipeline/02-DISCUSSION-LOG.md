# Phase 2: Collection Pipeline - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-21
**Phase:** 2-Collection Pipeline
**Areas discussed:** Watchlist Bootstrap, Rate Limiter, Collection Fan-out, Run Recording & Status (+ Error Handling / collected_at locked via user free-text)

---

## Watchlist Bootstrap

| Option | Description | Selected |
|--------|-------------|----------|
| 데이터 시더(ApplicationRunner) | 기동 시 큐레이션 실제 워치리스트를 external_item_id 기준 멱등 upsert; Phase 4 CRUD가 위에 얹음 | ✓ |
| Flyway 시드 마이그레이션 | V2__seed_watchlist.sql로 행 삽입; 단순하나 D-02(스키마≠데이터)와 충돌 | |
| 빈 상태 + Phase 4 위임 | 비면 no-op; 데모/성공기준 시연이 Phase 4 이후로 밀림 | |

**User's choice:** 데이터 시더(ApplicationRunner)
**Notes:** 구성 규모 후속 질문 → **고변동 큐레이션 소수(~10-20개)** 선택(융화재료/재련재료 등; Task 0 확인 Id + 도메인 지식). 대안: 2-3 카테고리 혼합 / 카테고리 통꺼(33개).

---

## Rate Limiter

| Option | Description | Selected |
|--------|-------------|----------|
| 직접 구현 Redis 토큰버킷 | lazy refill + Lua 원자성; 재시작 시 Redis에서 토큰 복원(성공기준 2 직접 증명); 학습 쇼케이스 | ✓ |
| Bucket4j (Redis 백엔드) | 검증 라이브러리, 코드 적음; 단 '재시작 복원'이 라이브러리 뒤로 숨음 | |
| You decide | 트레이드오프 문서화 후 planner 결정 | |

**User's choice:** 직접 구현 Redis 토큰버킷
**Notes:** 헤더 동기화 후속 질문 → **순수 client-side(헤더는 관측만)** 선택. 버킷=단일 진실원, x-ratelimit는 로그/health용, 429 Retry-After만 기존 경로. 대안: 적극 reconcile(복잡·경합 위험).

---

## Collection Fan-out

| Option | Description | Selected |
|--------|-------------|----------|
| 품목별 ItemName 호출 | 품목마다 CategoryCode+ItemName 1콜, 응답을 Id로 대조; 1콜=1품목 단순; ~10-20콜/틱 | ✓ |
| 카테고리별 list 페이징 | 카테고리만 페이징해 응답에서 워치리스트 Id 추출; COLL-02 문자적 정렬; 페이징 로직 필요 | |

**User's choice:** 품목별 ItemName 호출
**Notes:** 동시성 수단 → **Spring @Async + allOf().join()**(설계 1A + 학습 목표; 대안 Java 21 virtual threads / You decide). 타임아웃 → **보수적 고정값 per-call ~5s / join ~60-90s**(join < 틱; 대안 planner 재량-제약만 잠금). ItemName 부분일치라 Id 정확 대조 필수, detail엔 CurrentMinPrice 없어 list 강제(Task 0).

---

## Run Recording & Status

| Option | Description | Selected |
|--------|-------------|----------|
| 시작 생성(RUNNING) → 종료 업데이트 | 틱 시작 시 RUNNING 행 생성, 종료 시 카운트/status 업데이트; 프로세스 다운 가시화; 쓰기 2회 | ✓ |
| 종료 시 1행 기록 | 종료 때만 1행 insert; 단순하나 중간 다운 흔적 없음 | |

**User's choice:** 시작 생성(RUNNING) → 종료 업데이트
**Notes:** status 어휘 RUNNING→SUCCESS/PARTIAL_SUCCESS/FAILED(카운트 기반). summary_message(AUTH_ERROR/429 마커)는 신규 Flyway V2 컬럼으로 추가.

---

## Error Handling & collected_at (user free-text, locked)

마무리 질문("더 파고들 vs CONTEXT 정리")에 사용자가 자유서술로 정책을 직접 확정하고 CONTEXT 정리를 지시:

- **401/403:** fatal auth, 재시도 없음, 새 호출 중단·in-flight 실패 수렴, succeeded=0→FAILED / 일부성공 후 auth→PARTIAL_SUCCESS, summary_message=AUTH_ERROR, /health에 키/민감정보 노출 금지.
- **429:** Retry-After 우선, 없으면 지수 백오프, max3 후 품목 실패(스냅샷 미저장), failed+summary 기록.
- **5xx/timeout:** transient, 품목 단위 지수 백오프 max3, 초과 시 해당 품목만 실패, 일부=PARTIAL_SUCCESS/전건=FAILED.
- **그 외 4xx(400/404):** 재시도 없음, 품목 실패, 스냅샷 미저장, 사유 로그.
- **collected_at:** run 시작 UTC를 분 단위 truncate, run 전 품목 공유, fetched_at은 실제 호출시각 별도, UNIQUE가 논리 tick 중복 차단, 재시도 불변, 수동 재실행 충돌은 기본 skip(planner가 skip/update 명시).
- **프로세스:** 정책을 테스트 가능 단위로 분할, 명시 테스트 케이스 포함; GSD=source of truth, Superpowers=TDD+코드리뷰 규율로만.

---

## Claude's Discretion

패키지/레이어 구조, TaskExecutor 풀/큐, Lua 스크립트 세부, 토큰버킷 키 네이밍/TTL, 시더 품목 정확 목록 및 test-프로파일 비활성 방식, 백오프 base/jitter, status 어휘 미세조정, 중복 충돌 skip vs update 최종 선택.

## Deferred Ideas

- avg_price(YDayAvgPrice) → 별도 V2 마이그레이션 (Phase 2 아님)
- trade_count 일별 통계 테이블 → Phase 5/v2
- Micrometer 관측성 카운터 → v2
- 매직 넘버 @ConfigurationProperties 외부화 → v2
- 레이트리밋 헤더 적극 reconcile → 보류(client-side 선택)
