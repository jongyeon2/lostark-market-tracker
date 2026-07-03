# Phase 17: 실데이터 전환 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-03
**Phase:** 17-real-data-transition
**Areas discussed:** 17↔18 경계 & 축적 시작, 빈 상태 정직성 수준, seed 격리 강도 & 볼륨 위생, README/데모 서사 재편

---

## 17↔18 경계 & 축적 시작

### 실데이터 '축적' 수준 (REALDATA-01 충족 방식)

| Option | Description | Selected |
|--------|-------------|----------|
| 모드 전환 + 로컬 수집 검증 | 실데이터 기준으로 전환(프로파일·EmptyState·seed 격리·문서) + 실키 로컬 1회 구동해 collection_run SUCCESS·스냅샷 검증. 축적·배포는 18 | ✓ |
| 로컬 실키 상시 수집 시작 | 실키 인스턴스를 상시 구동해 지금부터 시계열 축적(배포는 18) | |
| You decide | Claude 재량 | |

**User's choice:** 모드 전환 + 로컬 수집 검증
**Notes:** REALDATA-01을 로컬에서 증명(collection_run SUCCESS + 실스냅샷). 시간에 걸친 상시 축적·공개 배포는 Phase 18로 경계 유지 → CONTEXT D-01.

### 라이브 실행 프로파일

| Option | Description | Selected |
|--------|-------------|----------|
| dev 재사용, prod는 18로 연기 | dev가 이미 실수집이므로 dev로 전환·검증. prod 프로파일은 Phase 18 신설 | ✓ |
| prod/live 프로파일 지금 신설 | 배포 지향 프로파일을 미리 생성 | |
| You decide | Claude 재량 | |

**User's choice:** dev 재사용, prod는 18로 연기
**Notes:** 지금 새 프로파일을 만들면 Phase 18과 겹침 → CONTEXT D-02.

---

## 빈 상태 정직성 수준

### '수집 중' vs '데이터 없음' 구분

| Option | Description | Selected |
|--------|-------------|----------|
| collection_run 상태 기반 구분 | GET /api/health/collection 근거로 '수집 중'(SUCCESS·희소) vs '데이터 없음'(NO_RUNS/실패) 구분. HealthCard 이미 소비 | ✓ |
| 단일 '수집 중' copy 통일 | 3화면 모두 한 문구로 통일 | |
| You decide | Claude 재량 | |

**User's choice:** collection_run 상태 기반 구분
**Notes:** REALDATA-03 '수집 중/데이터 없음' 문구와 정합, 기존 /health/collection 재사용 → CONTEXT D-03.

### 3화면 빈 상태 문구

| Option | Description | Selected |
|--------|-------------|----------|
| 화면별 맞춤 문구 | 각 화면 빈 조건에 맞춘 문구(AsyncBoundary per-screen override), Impact는 기존 sparse/stale 가드 위에 | ✓ |
| 공유 문구 + 최소 오버라이드 | 공유 기본 문구 실데이터로 교체 + Impact만 override | |
| You decide | Claude 재량 | |

**User's choice:** 화면별 맞춤 문구
**Notes:** AsyncBoundary가 이미 per-screen override 지원. EmptyState seed 하드코딩 copy 교체 포함 → CONTEXT D-04/D-05.

---

## seed 격리 강도 & 볼륨 위생

### seed 데이터 오염 방지

| Option | Description | Selected |
|--------|-------------|----------|
| 프로파일 격리 유지 + 깨끗한 볼륨에서 시작 | @Profile("seed") 유지(REALDATA-02), 실데이터는 seed 미주입으로 자연 격리 + 검증 전 볼륨 초기화. 핵심 0줄 | ✓ |
| 런타임 가드 추가(seed 감지/거부) | 실데이터 프로파일에서 seed 마커 감지·거부 코드 추가 | |
| You decide | Claude 재량 | |

**User's choice:** 프로파일 격리 유지 + 깨끗한 볼륨에서 시작
**Notes:** 11-04 전례(과거 seed synthetic run이 헬스에 표시) 방지. 런타임 가드는 Core Value 0줄 가드와 충돌·1인 데모엔 과함 → CONTEXT D-06/D-07/D-08.

---

## README/데모 서사 재편

### 기본 재현 경로 & seed↔실데이터 서사

| Option | Description | Selected |
|--------|-------------|----------|
| seed=로컬 재현 유지 + 실데이터 헤드라인 재프레임 | '한눈에 3단계'는 seed 유지(진입장벽 0)하되 '로컬/테스트 전용·합성'으로 라벨, 실데이터(dev)를 라이브 실체로 재프레임. 라이브 URL은 18 | ✓ |
| 기본을 실데이터(dev)로 전환, seed는 대체로 강등 | '한눈에' 기본을 dev(실키 필요)로, seed는 'API 키 없이 보려면' 대체 | |
| You decide | Claude 재량 | |

**User's choice:** seed=로컬 재현 유지 + 실데이터 헤드라인 재프레임
**Notes:** 리뷰어 접근성(키 없는 재현) 보존 + REALDATA-02 정합. README·frontend/README 함께 재편 → CONTEXT D-09/D-10.

---

## Claude's Discretion

- 화면별 빈 상태 정확한 카피 문구·아이콘·톤(UI-SPEC 카피 계약·상태 컴포넌트 패턴과 일관).
- '수집 중' vs '데이터 없음' 판정 임계값과 프론트 판정 로직 배치.
- EmptyState 공유 기본 문구 문안 및 override prop 전달 지점.
- README 재편의 정확한 문장·섹션 배치·seed 라벨 문안.
- 깨끗한 볼륨 시작(D-07)의 검증 절차/실행 노트 문서화 여부·위치.

## Deferred Ideas

- 공개 배포·Dockerfile·라이브 데모 URL·prod/live 프로파일·서빙 방식·보안 게이트 — Phase 18 (DEPLOY-01..04).
- 실데이터 상시 축적(시간에 걸친 시계열·event-impact 성숙) — 배포 후 자연 축적.
- 수집 부트스트랩 정교화(initial-delay·watchlist 타이밍) — 이미 dev에 존재(quick 260630-em5), 필요 시 후속.
