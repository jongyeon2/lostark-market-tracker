# Requirements: 로스트아크 거래소 시세 수집·분석 파이프라인 — v1.3

**Defined:** 2026-07-01
**Milestone:** v1.3 관리자 콘솔 + 실데이터 라이브 배포
**Core Value:** 레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다

읽기전용 seed 데모를 → 실데이터로 수집·서빙되고, 관리자가 이벤트·워치리스트를 직접 관리하며, 무료로 라이브 배포된 데모로 승격한다(배포 직전 보안 검증 게이트 통과). Phase 15부터 연속 번호.

**불변 제약(상시 가드):** 프론트에서 Lostark API 직접 호출 금지 · API key는 백엔드 env에서만 · 실키를 코드/문서/로그/커밋에 미기재 · 수집/캐시/event-impact 변경은 Core Value(수집 신뢰성) 가드 하에만.

## v1.3 Requirements

이번 마일스톤 범위. 각 항목은 로드맵 페이즈(Phase 15부터)에 매핑된다.

### 관리자 콘솔 (ADMINUI)

기존 백엔드 `/api/admin/*`(X-Admin-Secret 게이트, 백엔드 인증 무변경)을 소비하는 프론트 관리자 콘솔.

- [x] **ADMINUI-01**: 관리자가 시크릿 입력 화면에서 관리자 시크릿을 입력해 콘솔에 로그인하면, 이후 관리자 요청에 `X-Admin-Secret`가 자동 첨부되고 세션이 새로고침에도 유지된다
- [x] **ADMINUI-02**: 잘못된 시크릿이면 로그인이 거부되고(401) 명확한 오류 메시지를 보여준다
- [x] **ADMINUI-03**: 관리자가 게임 이벤트를 목록으로 보고 등록·수정·삭제할 수 있다 (event-impact 입력)
- [x] **ADMINUI-04**: 관리자가 워치리스트 품목을 추가·비활성화(soft-delete)·재활성화할 수 있다
- [x] **ADMINUI-05**: 관리자가 최근 수집 실행 이력(collection_run 성공/부분/실패 카운트·시각)을 콘솔에서 확인할 수 있다
- [x] **ADMINUI-06**: 로그인하지 않은 사용자에게는 관리자 쓰기 UI가 노출되지 않으며, 관리자는 로그아웃해 세션을 종료할 수 있다

### 대시보드 카드 (CARD)

- [x] **CARD-01**: 대시보드 품목 카드가 물품 고유 번호 없이 아이콘·품목명·골드 가격·수집 시각만 표시한다
- [x] **CARD-02**: 대시보드 품목 카드를 클릭하면 해당 품목이 선택된 상태로 품목 타임라인 화면으로 이동한다

### 실데이터 전환 (REALDATA)

- [ ] **REALDATA-01**: 배포/실행 인스턴스가 실 API 키로 수집기를 구동해 실수집 데이터를 축적한다 (seed 합성 데이터 아님)
- [ ] **REALDATA-02**: 프론트 데모 3화면이 실수집(collection_run 기반) 데이터를 표시하며, seed 프로파일은 로컬/테스트 전용으로만 남는다
- [ ] **REALDATA-03**: 데이터가 아직 축적되지 않은 초기 상태에서도 3화면이 빈 화면 없이 '수집 중/데이터 없음'을 정직히 표시한다 (조기 배포 후 축적 전제)

### 라이브 배포 + 보안 (DEPLOY)

v1.0에서 v2로 연기했던 `DEPLOY-V2-01`(무료 호스팅 데모 배포)을 이번 마일스톤에서 실현. 무료 타깃(항상무료 VM vs 무료 PaaS)은 배포 phase 착수 시 리서치로 결정.

- [ ] **DEPLOY-01**: 프로젝트가 무료 호스팅에 배포되어 공개 URL로 데모 3화면과 관리자 콘솔에 접근할 수 있다
- [ ] **DEPLOY-02**: 프론트엔드가 배포 환경에서 서빙되며(정적/단일 출처, 보류됐던 DEMO-03 재활성 후보), 백엔드와 동일 출처로 API·관리자 시크릿 경로가 동작한다
- [ ] **DEPLOY-03**: 배포 직전 보안 검증 게이트를 통과한다 — 관리자 쓰기 보호·API 키 미노출·DB/Redis 포트 비공개·HTTPS·CORS 정책
- [ ] **DEPLOY-04**: 실 API 키와 관리자 시크릿이 서버 환경변수로만 주입되고 코드·문서·로그·커밋에 미기재된다

## v2 Requirements

향후 릴리스로 연기. 추적하되 이번 로드맵엔 없음.

### Frontend (FE-V2)
- **FE-V2-01**: 실시간 자동 갱신(WebSocket/폴링)
- **FE-V2-02**: 다크모드 / i18n
- **FILTER-V2-01**: 셀렉터 그룹 필터 토글
- **GRADE-V2-01**: 등급별 색상 · 정렬 정교화

### Analysis (IMPACT-V2)
- **IMPACT-V2-01**: 카테고리 베이스라인 대비 초과 상승률 비교
- **IMPACT-V2-02**: event-impact median/스무딩(노이즈 완화)

### Observability / Config / Sources / Auth (기타 V2)
- **OPS-V2-01**: Micrometer 관측성(429 / skipped tick / failed item / cache hit·miss)
- **CFG-V2-01**: 매직 넘버 `@ConfigurationProperties` 외부화
- **SRC-V2-01**: 경매장(AUCTIONS) / 보석 소스 확장
- **AUTH-V2-01**: 풀 유저 / 권한 모델
- **CD-V2-01**: 자동 배포(CD) 파이프라인

## Out of Scope

명시적 제외. 스코프 크리프 방지용.

| Feature | Reason |
|---------|--------|
| 풀 로그인(아이디/비번) 유저·권한 모델 | 관리자 시크릿 게이트 한 겹으로 데모 충분 — AUTH-V2 |
| 자동 배포(CD) 파이프라인 | 1회성 데모 배포엔 수동 배포로 충분 — 필요 시 CD-V2 |
| 실시간 자동 갱신(WebSocket/폴링) | TanStack Query fetch-on-mount로 충분 — FE-V2-01 |
| 다크모드 / i18n | 데모 범위 밖 — FE-V2-02 |
| 관측성(Micrometer) | 배포와 별개 관심사, 이번 게이트는 보안 검증까지 — OPS-V2 |
| 인과 주장(이벤트가 가격을 올렸다) | 상관 ≠ 인과 — 시점 상관까지만 (상시) |

## Traceability

페이즈별 요구사항 매핑. 로드맵 생성 시 채워진다.

| Requirement | Phase | Status |
|-------------|-------|--------|
| ADMINUI-01..06 | Phase 15 | Complete |
| CARD-01..02 | Phase 16 | Pending |
| REALDATA-01..03 | Phase 17 | Pending |
| DEPLOY-01..04 | Phase 18 | Pending |

**Coverage:**
- v1.3 requirements: 15 total
- Mapped to phases: 15
- Unmapped: 0 ✓

---
*Requirements defined: 2026-07-01*
*Last updated: 2026-07-01 — v1.3 마일스톤 요구사항 초기 정의*
