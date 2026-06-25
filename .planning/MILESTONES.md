# Milestones

## v1.0 MVP (Shipped: 2026-06-25)

**Phases completed:** 6 phases, 15 plans, 13 tasks

**Key accomplishments:**

- docker-compose Postgres 16 + Redis 7에 연결되어 부팅되는 Gradle/Java 21/Spring Boot 3.4.1 앱, 공유 Testcontainers 베이스와 실제 컨테이너 위에서 그린인 스모크 통합 테스트.
- 부팅 시 검증되는 JPA 엔티티가 딸린 Flyway 소유 4테이블 스키마, 그리고 실제 HTTP /api/items 라운드트립과 UNIQUE 멱등성·TIMESTAMPTZ UTC 동작을 증명하는 Testcontainers IT.
- 실제 markets API를 한 번 검증: avg_price/trade_count는 일단위 전용(상세 Stats[]), 안정적 Id가 매칭 규칙을 확정, 레이트리밋은 100/min — 모델 잠금 비준, 종료 게이트 PASS.
- 자체 구현 Lua 원자적 Redis 토큰버킷(재시작 복원 가능, fail-closed)과, 모든 HTTP 결과를 구분된 타입드 예외(Auth/RateLimited+RetryAfter/Transient/NonRetryable)로 분류하는 제품화된 LostarkApiClient.
- 활성 품목마다 @Async 가격 fetch를 팬아웃하고 호출당/전체 타임아웃 하에 join하며, 성공당 틱 정규화 collected_at을 공유하는 멱등 스냅샷 하나를 쓰고, 카운트 기반 status로 collection_run을 기록하는 @Scheduled fixedDelay 틱.
- 팬아웃에 연결된 수동 바운드 재시도(max-3, Retry-After 우선 백오프), 신규 호출을 중단하고 run을 AUTH_ERROR로 마킹하는 fatal-auth 수렴, PARTIAL_SUCCESS로의 품목별 실패 격리, 그리고 Flyway V2 summary_message 컬럼 — 모든 마커는 시크릿 프리.
- `GET /api/items/{id}/latest`를 위한 수동 Redis 캐시-어사이드(캐시 히트 시 DB 0회 조회)와 Phase 2 수집기에 연결한 evict-on-write 무효화, 그리고 커스텀 `@RestControllerAdvice` 에러 계약의 404 절반 + active 전용 `/api/items`.
- `GET /api/items/{id}/prices?from=&to=`가 윈도우 내 스냅샷과 겹치는 이벤트를 UTC 두 배열로 반환하며, Phase 5가 재사용할 재사용 가능한 4A `WindowQueryService`가 양끝 포함 경계 시맨틱과 함께 테스트됨.
- `/prices`가 큰 범위를 PostgreSQL `date_trunc` avg 버킷으로 자동 다운샘플하고, 전체 입력 검증 계약(400/404/200-빈)이 커스텀 advice를 확장하며, `GET /api/health/collection`이 시크릿 노출 없이 최신 수집 런을 드러낸다.
- Admin CRUD for game events (full-replace PUT with mutable occurred_at) and watchlist items (soft-delete + reactivate/409 branch) on the locked model, with a V3 UNIQUE constraint and ResourceNotFound/Duplicate/Validation handlers extending the shared error contract — all admin ITs pre-sending X-Admin-Secret for a zero-retrofit gate.
- Hand-rolled X-Admin-Secret OncePerRequestFilter + SecurityFilterChain gating /api/admin/
- `GET /api/items/{id}/event-impact?window=N` — 게임 이벤트별 anchor 단일 스냅샷 delta `change_rate = post/pre − 1`(min_price)을 단일 배치 읽기로 계산해 occurred_at desc 로 반환하는 v1 상관 엔드포인트.
- `EventImpactService` 에 30분 staleness + sufficiency 게이트를 in-place 로 얹어, 양쪽 anchor 가 존재하고 둘 다 fresh 일 때만 `change_rate` 를 산출하고 그 외에는 sparse(null)·stale(시간 보고)을 구분해 `insufficient_data` 로 반환.
- GitHub Actions CI running the full `./gradlew build` Testcontainers suite on every push/PR, plus a `seed` Spring profile that fills 8 days of synthetic 10-min snapshots + 2 grid-anchored demo events (no API key) — proven end-to-end by SyntheticDemoDataIT.
- README.md rewritten into the single reproducible reviewer entry point — CI badge, mermaid architecture diagram, honest Redis/rate-limit/@Async trade-offs, seed-mode setup, ≥3 curl + 5 sample JSON blocks against the real API, and a clone→seed→curl event-impact reproduction.

---
