# Phase 4: Admin + Events - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-23
**Phase:** 4-Admin + Events
**Areas discussed:** 인증 게이트 구현 방식, 품목 삭제 의미론, 이벤트 수정(PUT) 형태, 관리자 응답·404 계약

---

## 인증 게이트 구현 방식

### 공유 시크릿 검증 메커니즘

| Option | Description | Selected |
|--------|-------------|----------|
| 헤더 시크릿 + 커스텀 필터 | `OncePerRequestFilter`가 요청 헤더(예: X-Admin-Secret)를 env 시크릿과 비교, `SecurityFilterChain`에 등록. 나머지 표면 permitAll. '단일 공유 시크릿'에 가장 정확히 대응하고 필터 체인 이해를 보이는 면접 서사 | ✓ |
| HTTP Basic (in-memory) | `http.httpBasic()` + env에서 온 단일 in-memory 유저. 코드 최소·표준이지만 username이 의미 없는 단일 공유 계정이라 약간 어색 | |

**User's choice:** 헤더 시크릿 + 커스텀 필터

### 인증 실패 401 응답 본문

| Option | Description | Selected |
|--------|-------------|----------|
| 기존 에러 JSON 계약과 통일 | 커스텀 `AuthenticationEntryPoint`가 `{timestamp,status,error,message}` 형태로 401 반환. Phase 3 `ApiExceptionHandler`와 일관 — 계약 통일 쇼케이스 (성공기준은 401 — 403 아님) | ✓ |
| Spring Security 기본 401 | 별도 본문 구성 없이 프레임워크 기본 401. 코드는 적지만 읽기 API의 4xx 에러 형태와 불일치 | |

**User's choice:** 기존 에러 JSON 계약과 통일
**Notes:** 보안 필터가 디스패처 앞단이라 `@RestControllerAdvice`가 못 잡으므로 entry point에서 직접 직렬화 필요. 시크릿/헤더값 등 민감정보 미노출.

---

## 품목 삭제 의미론

### DELETE 의미론

| Option | Description | Selected |
|--------|-------------|----------|
| Soft delete (active=false) | active=false 표시 → 컬렉터 폴링 제외 + `GET /api/items`에서 사라짐. price_snapshot 이력·FK·Phase 5 event-impact 근거 보존, 멱등. active 플래그·findByActiveTrue 이미 존재 | ✓ |
| Hard delete (행 제거) | tracked_item 행 실제 삭제. price_snapshot FK 제약 위반 또는 cascade 삭제 필요 — 시계열 이력·event-impact 근거 소실 위험 | |

**User's choice:** Soft delete (active=false)
**Notes:** V1 스키마에서 `price_snapshot.tracked_item_id` FK에 ON DELETE CASCADE 없음을 확인 — hard delete는 제약 위반이 되므로 soft delete가 정합적.

### 기존 공개 POST /api/items 처리

| Option | Description | Selected |
|--------|-------------|----------|
| /api/admin/items로 이전 | `ItemController` 공개 POST 제거, 품목 생성은 게이트 뒤 `AdminItemController`로 이동. 설계와 일치, 공개 표면은 읽기 전용 | ✓ |
| 공개 POST 유지 + admin에도 추가 | 둘 다 둠. 표면 중복·인증 우회 경로 잔존(포트폴리오 감점 위험) | |

**User's choice:** /api/admin/items로 이전

---

## 이벤트 수정(PUT) 형태

| Option | Description | Selected |
|--------|-------------|----------|
| 전체 교체 (모든 필드 필수) | POST와 동일 DTO로 모든 필드 교체. 참 HTTP PUT 의미·테스트 단순. occurred_at 변경 허용(잘못 입력한 시점 정정) → 타임라인·event-impact 앵커에 반영. description nullable 유지 | ✓ |
| 부분 수정 (PATCH 유사) | 보낸 필드만 갱신, 나머지 유지. 유연하지만 코드 증가, '무변경 vs null로 지움' 모호. MVP엔 과함 | |

**User's choice:** 전체 교체 (모든 필드 필수)

---

## 관리자 응답·404 계약

### DELETE 성공 응답 코드

| Option | Description | Selected |
|--------|-------------|----------|
| 204 No Content | 본문 없이 204. REST 표준, 멱등 soft-delete와 자연스러움 | ✓ |
| 200 OK + 바디 | 200에 삭제된 리소스/메시지 반환. 본문이 필요한 경우이지만 MVP엔 불필요 | |

**User's choice:** 204 No Content

### 중복 external_item_id POST 동작 (현재 DB UNIQUE 없음, 앱 코드만 유일 가정)

| Option | Description | Selected |
|--------|-------------|----------|
| 활성이면 409, 비활성이면 재활성화 | external_item_id 조회: active면 409 Conflict; soft-deleted면 active=true로 재활성화. '삭제 후 재추가' 자연 지원, 시드 멱등과 조화. V3 UNIQUE 마이그레이션 권장 | ✓ |
| 항상 409 Conflict | 존재하면(활성/비활성 무관) 409. 단순하지만 비활성 품목 재추가 막혀 수동 정리 필요 | |
| 항상 새 행 insert | 중복 허용. external_item_id 중복 행 → 컬렉터·조회 모호, findByExternalItemId Optional 깨짐 | |

**User's choice:** 활성이면 409, 비활성이면 재활성화
**Notes:** V1 스키마에서 `external_item_id`가 NOT NULL이지만 DB UNIQUE 미설정 확인 — 재활성화 의미론을 안전하게 강제하려면 V3 `UNIQUE(external_item_id)` 마이그레이션 권장.

---

## Claude's Discretion

- 시크릿 env 변수명·헤더명 정확값, `application.yml` 노출 방식 및 미설정 시 기동 정책
- 패키지/레이어 구조(`web.admin`, 컨트롤러 분리 vs 단일), 관리자 서비스 계층 분리 여부
- 이벤트 요청 DTO(`GameEventRequest`) 형태, 재활성화 응답 201 vs 200
- `EventNotFoundException` 신설 vs `ItemNotFoundException` 일반화 택1
- V3 `UNIQUE(external_item_id)` 마이그레이션 파일 분할·이름
- `GET /api/admin/events` 정렬 순서

## Deferred Ideas

- HTTP Basic / 풀 유저·역할·권한 모델 — v2(AUTH-V2-01)
- 이벤트 PATCH(부분 수정) — v2
- 감사 로그·created_by·변경 이력 — 인증 모델 없음, 설계상 생략
- 기간형 이벤트(start/end) 모델 — v2(EVT-V2-01)
- 관리자 표면 운영 메트릭(쓰기 감사·레이트리밋) — v2
