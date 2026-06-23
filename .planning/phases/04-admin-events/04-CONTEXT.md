# Phase 4: Admin + Events - Context

**Gathered:** 2026-06-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1에서 잠근 `game_event`·`tracked_item` 위에 **공유 시크릿 인증 게이트 뒤의 관리자 CRUD 계층**을 올린다 — 이벤트(`POST/GET/PUT/DELETE /api/admin/events`)와 워치리스트 품목(`POST/DELETE /api/admin/items`). (ADMIN-01..03)

이 페이즈는 "시크릿 없이 `/api/admin/**`는 401, 시크릿 있으면 관리자가 이벤트·품목을 CRUD하고 그 변경이 공개 읽기 표면(`GET /api/items`)·컬렉터·타임라인에 즉시 반영된다"까지 책임진다. **백엔드 전용 — 프론트엔드 UI는 명시적 제외.** event-impact(Phase 5)는 후속이지만 이 페이즈가 만드는 `game_event`를 소비한다.

**선행 페이즈에서 잠긴 것(planner 변경 금지):** 4테이블 모델 / `game_event`(event_type 4종, 단일 `occurred_at`, `created_by` 없음 — 인증 모델 없음) / `tracked_item`(`external_item_id` NOT NULL·**DB UNIQUE 미설정**, `active` 기본 TRUE) / `price_snapshot.tracked_item_id` FK는 **ON DELETE CASCADE 없음** / 모든 시간 `TIMESTAMPTZ`(UTC) / 에러 계약 `{timestamp,status,error,message}` via `@RestControllerAdvice` / Flyway 포워드 + `ddl-auto=validate` / DB=PostgreSQL, 테스트=Testcontainers / `GET /api/items`·`/latest`·`/prices`·`/health/collection`은 이미 존재(공개 읽기 표면).

</domain>

<decisions>
## Implementation Decisions

### 인증 게이트 (ADMIN-03 · 설계 6A / T8)
- **D-01:** **공유 시크릿 검증 = 헤더 기반 커스텀 `OncePerRequestFilter`.** 요청 헤더(예: `X-Admin-Secret`)를 env 시크릿과 상수시간 비교하고 `SecurityFilterChain`에 등록. 나머지 표면(`GET /api/items`, `/api/items/{id}/latest`, `/api/items/{id}/prices`, `/api/health/collection`, actuator)은 **permitAll**. ⚠ `spring-boot-starter-security` 추가 즉시 전체 엔드포인트가 기본 잠금되므로 **공개 읽기 API permitAll 명시는 필수**(회귀 방지). HTTP Basic·풀 유저/역할 모델 배제 — 단일 공유 시크릿 한 겹(권한 모델은 v2).
- **D-02:** **인증 실패 = 401(403 아님) + 커스텀 `AuthenticationEntryPoint`가 기존 `{timestamp,status,error,message}` JSON 계약으로 본문 작성.** 시크릿 누락/불일치 모두 401. 보안 필터는 디스패처보다 앞단이라 `@RestControllerAdvice`(`ApiExceptionHandler`)가 못 잡으므로 entry point에서 직접 직렬화해 계약을 통일. **응답·로그에 시크릿/헤더값 등 민감정보 절대 미노출.**
- 엔드포인트(설계 잠금): `POST/GET/PUT/DELETE /api/admin/events`, `POST/DELETE /api/admin/items` — 전부 게이트 뒤.

### 품목 관리 (ADMIN-02)
- **D-03:** **`DELETE /api/admin/items/{id}` = soft delete(`active=false`).** 컬렉터 `findByActiveTrue` 폴링에서 제외되고 `GET /api/items`에서 즉시 사라짐(성공기준 3). `price_snapshot` FK(ON DELETE CASCADE 없음)·시계열 이력·Phase 5 event-impact 근거를 보존. 멱등(이미 비활성 품목도 204). hard delete 배제(FK 위반/이력 소실 위험).
- **D-04:** **기존 공개 `POST /api/items`(walking-skeleton 잔재) 제거 → `POST /api/admin/items`로 이전.** 품목 생성/삭제는 인증 게이트 뒤 신규 `AdminItemController`로 이동. 공개 표면은 **읽기 전용(GET만)**. `ItemController`의 `@PostMapping` 핸들러 삭제.
- **D-05:** **중복 `external_item_id` POST = 조회 후 분기 — 활성이면 409 Conflict, soft-deleted(비활성)면 `active=true` 재활성화**(요청의 `displayName`/`category`도 갱신). '삭제 후 재추가'를 자연 지원하고 `WatchlistSeeder` upsert(멱등)와 충돌 없음. **V3 마이그레이션으로 `UNIQUE(external_item_id)` 추가 권장** — 앱은 이미 유일 가정(`findByExternalItemId` Optional, 시드 upsert)인데 DB가 미강제. 항상-insert(중복 행)는 배제.

### 이벤트 관리 (ADMIN-01)
- **D-06:** **`PUT /api/admin/events/{id}` = 전체 교체(모든 필드 필수, POST와 동일 요청 DTO).** 보낸 본문으로 `event_type`/`title`/`occurred_at`/`description` 전부 치환. **`occurred_at` 변경 허용**(관리자가 잘못 입력한 시점 정정) — Phase 3 타임라인 겹침(`from ≤ occurred_at ≤ to`)·Phase 5 event-impact 윈도우 앵커에 직접 반영됨. PATCH식 부분 수정 배제(MVP 과함, '무변경 vs null로 지움' 모호).
- **D-07:** **`created_at`/`updated_at`은 `GameEvent` 엔티티 `@PrePersist`/`@PreUpdate`가 이미 UTC로 채움**(성공기준 2) — 컨트롤러/서비스가 직접 손대지 않음. `created_by`는 없음(인증 모델 없음, 설계 확정).

### 공통 응답·검증 계약 (ADMIN-01..03)
- **D-08:** **DELETE 성공 = 204 No Content(본문 없음)**, 이벤트·품목 공통. REST 표준, 멱등 soft-delete와 자연. **POST = 201 Created + 생성 리소스 바디.**
- **D-09:** **없는 리소스 대상 PUT/DELETE → 404, 기존 에러 계약(`{timestamp,status,error,message}`)으로 통일.** 이벤트용 not-found 신호는 `EventNotFoundException` 신설 또는 `ItemNotFoundException` 일반화(`ResourceNotFoundException`) 중 택1해 `ApiExceptionHandler`에 매핑. 엔티티 대신 **요청 DTO 바인딩**(`TrackedItemRequest` 패턴)으로 `id`/`active` mass-assignment 차단.
- **D-10:** **요청 검증** = 이벤트: `event_type` 필수(enum 4종), `title` `@NotBlank`, `occurred_at` 필수; 품목: `externalItemId`/`displayName` `@NotBlank`(기존 `TrackedItemRequest` 재사용). `@Valid` 위반은 400으로 기존 에러 계약에 맞춰 반환 — 현재 `ApiExceptionHandler`에 `MethodArgumentNotValidException` 핸들러가 **없으므로 신규 추가 필요**.

### Claude's Discretion
- 시크릿 env 변수명(예: `ADMIN_API_SECRET`)·헤더명(예: `X-Admin-Secret`) 정확값; `application.yml` 노출 방식(`${ADMIN_API_SECRET:}` placeholder, 커밋 금지 — `LOSTARK_API_KEY` 패턴과 동일); 미설정 시 기동 정책.
- 패키지/레이어 구조(`web.admin` 패키지, `AdminEventController`/`AdminItemController` 분리 vs 단일), 관리자 서비스 계층 분리 여부.
- 이벤트 요청 DTO 신설 형태(`GameEventRequest` record); 재활성화(D-05) 응답 201 vs 200 정확값.
- `EventNotFoundException` 신설 vs `ItemNotFoundException` 일반화 중 택1(D-09).
- V3 `UNIQUE(external_item_id)` 마이그레이션 채택 시 파일 분할·이름(V2 다음 번호).
- `GET /api/admin/events` 정렬(예: `occurred_at` desc); `GameEventRepository` CRUD는 `JpaRepository` 기본 메서드로 충분(추가 쿼리 불요 — window finder는 Phase 3가 이미 추가).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 설계 / 리뷰 (필수 — 구현 확정 레이어)
- `docs/design/yeonjong-unknown-design-20260619-221517.md` — Phase 4 핵심: **§관리자 엔드포인트**(`POST/PUT/DELETE /api/admin/events`, `POST/DELETE /api/admin/items`, **단일 공유 시크릿/Basic auth, 유저 모델 없음**), **확정 8결정 중 6A(관리자 시크릿 인증)**, **§Implementation Tasks T8**(`/api/admin/**` 시크릿 게이트 = Spring Security + env; 검증: 시크릿 없이 401/403, 있으면 200), **game_event 모델**(event_type 4종, 단일 `occurred_at`, `created_by` 생략), 풀 유저/권한 모델 v2 강등.
- `docs/reviews/yeonjong-unknown-eng-review-test-plan-20260620-102515.md` — 테스트 플랜: **관리자 인증 401/200**.

### 선행 데이터 모델·스키마 (필수 — 사실 근거)
- `src/main/resources/db/migration/V1__init_schema.sql` — `tracked_item`(`external_item_id` VARCHAR(100) NOT NULL·**UNIQUE 미설정**, `active` BOOLEAN 기본 TRUE), `game_event`(`event_type`/`title`/`occurred_at` NOT NULL, `description` TEXT, `created_at`/`updated_at`), `price_snapshot.tracked_item_id` FK **REFERENCES tracked_item(id) — ON DELETE CASCADE 없음**(→ soft delete 근거 D-03).
- `.planning/phases/01-foundation-task-0/01-CONTEXT.md` — Flyway 포워드 + `ddl-auto=validate`(D-02); DTO 바인딩으로 mass-assignment 차단.
- `.planning/phases/03-read-api-cache/03-CONTEXT.md` — 에러 계약 `{timestamp,status,error,message}` `@RestControllerAdvice`(D-10), UTC `OffsetDateTime` 직렬화(D-11), `/api/items` active 필터, `GameEventRepository`·`WindowQueryService`. **이벤트 `occurred_at` 변경(D-06)은 timeline 겹침(D-05)·event-impact 앵커에 영향.**

### 프로젝트 계획
- `.planning/PROJECT.md` — Key Decisions(**6A 관리자 시크릿 인증**), Out of Scope(풀 유저/권한 모델 v2 = 시크릿 게이트 한 겹).
- `.planning/REQUIREMENTS.md` — **ADMIN-01..03**.
- `.planning/ROADMAP.md` (§Phase 4) — 목표 + 3개 성공 기준 + 2개 plan 분할(04-01 이벤트/품목 CRUD 컨트롤러+서비스 / 04-02 Spring Security 공유 시크릿 게이트).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`src/main/java/com/lostark/tracker/web/ItemController.java`** — 기존 **공개 `@PostMapping` 제거 대상**(D-04); `GET /api/items`(active 필터)·`/{id}/latest`는 유지. 품목 POST/DELETE는 신규 `AdminItemController`로.
- **`src/main/java/com/lostark/tracker/web/error/ApiExceptionHandler.java` + `ApiErrorResponse` + `ItemNotFoundException`/`InvalidRequestException`** — 401 entry point(D-02)·404(D-09)·400 검증(D-10)이 따를 에러 계약. **`MethodArgumentNotValidException` 핸들러 신규 추가 필요**(현재 없음).
- **`src/main/java/com/lostark/tracker/web/dto/TrackedItemRequest.java`** — 품목 POST 요청 DTO 재사용(`@NotBlank externalItemId/displayName`, `id`/`active` 비노출). 이벤트는 `GameEventRequest` 신설.
- **`repository/{TrackedItemRepository, GameEventRepository}`** — `TrackedItemRepository.findByExternalItemId`(D-05 재활성화 분기)·`findByActiveTrue` 재사용; `GameEventRepository`는 `JpaRepository` CRUD 기본 메서드로 충분(window finder는 Phase 3가 이미 추가).
- **`domain/{GameEvent, TrackedItem, EventType}`** — 엔티티 그대로 사용. `GameEvent` `@PrePersist`/`@PreUpdate`가 `created_at`/`updated_at` UTC 자동 처리(D-07); `TrackedItem.setActive`(soft delete), `setDisplayName`/`setCategory`(재활성화 갱신).
- **`src/test/java/com/lostark/tracker/support/PostgresRedisContainers.java`** — Testcontainers 공유 베이스, Phase 4 인증 401/200·CRUD·soft delete 통합테스트 재사용.

### Established Patterns
- **`spring-boot-starter-security` 미도입** → `build.gradle`에 신규 추가(implementation). 추가 즉시 전체 엔드포인트 기본 잠금 → **공개 읽기 표면 permitAll 명시 필수**(D-01).
- **Flyway 포워드 + `ddl-auto=validate`**: D-05의 `UNIQUE(external_item_id)`는 **V3 마이그레이션**(V2 다음 번호). 그 외 스키마 변경 없음(엔티티 기존 컬럼만 사용 → CRUD는 마이그레이션 불요).
- `application.yml`: 시크릿은 `${ADMIN_API_SECRET:}` 같은 env placeholder(커밋 금지), `LOSTARK_API_KEY` 패턴과 동일.

### Integration Points
- **소비:** 신규 `AdminItemController`가 `TrackedItem.active` 토글 → 컬렉터(Phase 2 `findByActiveTrue` 폴링)·`GET /api/items`(Phase 3)에 즉시 반영. `AdminEventController` CRUD → `game_event` → Phase 3 타임라인 겹침·Phase 5 event-impact 윈도우.
- **보안 필터는 모든 web 표면 앞단:** 공개 읽기 API(Phase 3)·actuator가 permitAll로 열려야 회귀 없음.
- **Phase 5(event-impact)가 `game_event`를 소비** → 이벤트 `occurred_at` 정정(D-06)이 앵커 시각에 영향.

</code_context>

<specifics>
## Specific Ideas

- 단일 공유 시크릿 한 겹 = "공개 레포/데모에서 인증 없는 쓰기 엔드포인트는 감점"(PROJECT 6A) 방지. 풀 권한 모델은 의도적 v2.
- soft delete(D-03)는 "삭제해도 시계열·event-impact 근거가 남는다"는 데이터 파이프라인 정합성의 직접 표현.
- 401(403 아님) + 기존 에러 JSON 계약 통일(D-02) = 면접에서 "보안 계층도 API와 동일한 응답 계약을 따른다"를 설명하는 신호.

</specifics>

<deferred>
## Deferred Ideas

- **HTTP Basic / 풀 유저·역할·권한 모델** — v2(AUTH-V2-01). MVP는 시크릿 게이트 한 겹.
- **이벤트 PATCH(부분 수정)** — v2. MVP는 전체 교체(D-06).
- **감사 로그·`created_by`·변경 이력** — 인증 모델 없음, 설계상 생략.
- **기간형 이벤트(start/end) 모델** — v2(EVT-V2-01). MVP는 단일 `occurred_at`.
- **관리자 표면 운영 메트릭(쓰기 감사·레이트리밋 등)** — v2.

None 외 위 항목은 모두 명시적 v2/설계 제외로 회귀 추적용.

</deferred>

---

*Phase: 4-Admin + Events*
*Context gathered: 2026-06-23*
