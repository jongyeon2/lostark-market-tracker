# Phase 15: 관리자 콘솔 UI - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-01
**Phase:** 15-관리자 콘솔 UI
**Areas discussed:** 시크릿 로그인 & 세션 지속, 콘솔 진입 & 미로그인 가드, CRUD 폼 UX & 파괴적 작업, 백엔드 무변경 텐션(수집 이력 + 재활성화)

---

## 시크릿 로그인 & 세션 지속

### 시크릿 저장 위치
| Option | Description | Selected |
|--------|-------------|----------|
| sessionStorage | 탭 세션 동안 유지(새로고침 OK), 탭 종료 시 소멸 → 노출 창 최소 | ✓ |
| localStorage | 재방문까지 유지(편의 ↑) but XSS 노출 창 큼, 1인 데모엔 과함 | |
| You decide | 빌더 재량 | |

### 로그인 검증 방식
| Option | Description | Selected |
|--------|-------------|----------|
| GET /api/admin/events probe | 부작용 없는 read로 200/401 판정, 성공 시 이벤트 목록 확보 | ✓ |
| 낙관적 진입 후 첫 쓰기에서 검증 | 입력만 받고 진입, 첫 CRUD 401이면 거부 | |
| You decide | 빌더 재량 | |

### 세션 중 401 처리
| Option | Description | Selected |
|--------|-------------|----------|
| 전역 401 인터셉트 → 자동 로그아웃 | 시크릿 폐기 + 로그인 화면 복귀 + 안내 | ✓ |
| 화면별 ErrorState로만 표시 | 세션 유지, 무효 시크릿으로 로그인 상태 잔존 | |
| You decide | 빌더 재량 | |

**User's choice:** sessionStorage · GET /api/admin/events probe · 전역 401 인터셉트 자동 로그아웃
**Notes:** 백엔드 STATELESS·전용 verify 엔드포인트 없음이 결정 배경. 포트폴리오 honesty 서사(스토리지 트레이드오프)로 연결.

---

## 콘솔 진입 & 미로그인 가드

### 콘솔 배치
| Option | Description | Selected |
|--------|-------------|----------|
| 신규 /admin 라우트 | 데모 3화면과 분리, 공개 nav 미노출 | ✓ |
| TopNav에 '관리자' 탭 추가 | 발견성 ↑ but 공개 nav에 관리자 노출 | |
| You decide | 빌더 재량 | |

### 미로그인 가드
| Option | Description | Selected |
|--------|-------------|----------|
| 전체 게이트 → 로그인 폼만 렌더 | 쓰기 UI DOM 자체 미렌더, ADMINUI-06 문구 정확 부합 | ✓ |
| 쓰기 UI 렌더 + 버튼 disable | DOM에 쓰기 UI 잔존, 문구와 약한 충돌 | |
| You decide | 빌더 재량 | |

### 콘솔 내부 구조
| Option | Description | Selected |
|--------|-------------|----------|
| 단일 페이지 + 섹션/탭 | 이벤트·워치리스트·수집 상태 응집, 상태 관리 단순 | ✓ |
| 하위 라우트 분리 | /admin/events 등, URL 딥링크 but 소규모엔 과함 | |
| You decide | 빌더 재량 | |

**User's choice:** 신규 /admin 라우트 · 전체 게이트(로그인 폼만) · 단일 페이지 섹션/탭

---

## CRUD 폼 UX & 파괴적 작업

### 폼 패턴
| Option | Description | Selected |
|--------|-------------|----------|
| 인라인 폼/섹션 내 | 단일 페이지와 일관, shadcn dialog 미설치 회피 | ✓ |
| 모달 다이얼로그 | 집중 폼 but dialog 블록 수작업 추가 필요(Windows CLI 버그) | |
| You decide | 빌더 재량 | |

### occurredAt 입력
| Option | Description | Selected |
|--------|-------------|----------|
| KST 입력 → UTC 변환 | formatKst 가드의 쓰기 역방향, off-by-9h 방지 | ✓ |
| UTC 직접 입력 | 단순하나 입력 혼란(로아는 KST 사고) | |
| You decide | 빌더 재량 | |

### 파괴적 작업 보호
| Option | Description | Selected |
|--------|-------------|----------|
| 확인 단계(인라인/다이얼로그) | 이벤트 삭제·품목 비활성 실행 전 확인 | ✓ |
| 즉시 실행 + undo 토스트 | 매끄럽지만 undo/toast 구현 복잡 | |
| You decide | 빌더 재량 | |

### mutation 후 갱신
| Option | Description | Selected |
|--------|-------------|----------|
| invalidateQueries → refetch | TanStack Query 표준, 서버 상태 단일 출처 | ✓ |
| 낙관적 업데이트 | 반응성 ↑ but 롤백·불일치 복잡, 소규모엔 과함 | |
| You decide | 빌더 재량 | |

**User's choice:** 인라인 폼 · KST→UTC 변환 · 확인 단계 · invalidateQueries

---

## 백엔드 무변경 텐션(수집 이력 + 재활성화)

### 수집 이력 (ADMINUI-05)
| Option | Description | Selected |
|--------|-------------|----------|
| 기존 엔드포인트 재사용 — 최신 상태 카드 1개 | 백엔드 0줄, '최신 수집 모니터링'으로 해석 | ✓ |
| 소규모 read 엔드포인트 추가 | GET /api/admin/collection-runs?limit=N 이력 리스트 | |
| You decide | 빌더 재량 | |

### 재활성화 (ADMINUI-04)
| Option | Description | Selected |
|--------|-------------|----------|
| 워크플로우 우회 — externalItemId 재입력 POST | 백엔드 0줄 but 비활성 목록 못 봄, UX 약함 | |
| 소규모 read 엔드포인트 추가 — GET /api/admin/items | active+inactive 전체 목록, '재활성' 버튼, 요구 UX 부합 | ✓ |
| You decide | 빌더 재량 | |

### 신규 엔드포인트 배치·가드
| Option | Description | Selected |
|--------|-------------|----------|
| /api/admin/* 아래 + read-only + 핵심 0줄 | 기존 X-Admin-Secret 게이트 재사용, Core Value 가드 | ✓ |
| 둘 다 우회 선택 — 백엔드 완전 0줄 | 해당 없음 | |
| You decide | 빌더 재량 | |

**User's choice:** 수집 이력=기존 재사용(최신 카드) · 재활성화=신규 GET /api/admin/items read-only · 신규 엔드포인트는 admin 게이트 뒤 read-only, 핵심 0줄
**Notes:** 혼합 결과 — health는 기존 소비(0줄), items는 소규모 read 추가. 인증·수집·캐시·event-impact 로직은 0줄. "요구 충실도 + Core Value 가드를 함께 지키는 최소 확장" 서사.

---

## Claude's Discretion

- 로그인 폼 시각·카피·password 입력·Enter 제출·실패 문구
- 성공/오류 피드백 메커니즘(토스트 미설치 → 인라인/경량 자체 구현)
- 이벤트 PUT 수정 폼 기존값 프리필, EventType 셀렉트 라벨
- 품목 추가 폼(externalItemId/displayName/category) 배치
- /admin 셸 재사용 vs 별도 셸, 섹션 vs 탭 렌더
- KST↔UTC 변환·zod admin 스키마·API 클라이언트 함수 시그니처 구현 위치
- sessionStorage 추상화(훅/컨텍스트)·X-Admin-Secret 첨부 지점
- GET /api/admin/items 경로·정렬·응답 형태(TrackedItemResponse 재사용 권장)·서비스/리포지토리 메서드명

## Deferred Ideas

- 수집 이력 리스트(최근 N건) — v2/후속 후보(GET /api/admin/collection-runs)
- 풀 유저/권한 모델 — AUTH-V2
- 성공/오류 토스트 시스템 고도화 — FE-V2 후보
- 실시간 자동 갱신 — FE-V2-01
- 대시보드 카드 개선(Phase 16) · 실데이터 전환(Phase 17) · 배포/보안(Phase 18)
