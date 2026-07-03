# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** — Phases 7–11 (shipped 2026-06-29) — [archive](milestones/v1.1-ROADMAP.md)
- ✅ **v1.2 Item Visual/Data Enrichment** — Phases 12–14 (shipped 2026-06-30) — [archive](milestones/v1.2-ROADMAP.md)
- 🚧 **v1.3 관리자 콘솔 + 실데이터 라이브 배포** — Phases 15–18 (진행 중, 착수 2026-07-01)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–6) — SHIPPED 2026-06-25</summary>

- [x] **Phase 1: Foundation + Task 0** — 2026-06-20
- [x] **Phase 2: Collection Pipeline** — 2026-06-22
- [x] **Phase 3: Read API + Cache** — 2026-06-23
- [x] **Phase 4: Admin + Events** — 2026-06-24
- [x] **Phase 5: Event Impact (게이트 조건부)** — 2026-06-24
- [x] **Phase 6: Distribution + Docs** — 2026-06-25

전체 상세: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v1.1 Frontend Demo Dashboard (Phases 7–11) — SHIPPED 2026-06-29</summary>

- [x] **Phase 7: Frontend Foundation** — 2026-06-25
- [x] **Phase 8: Dashboard** — 2026-06-25
- [x] **Phase 9: Item Timeline** — 2026-06-26
- [x] **Phase 10: Event Impact** — 2026-06-26
- [x] **Phase 11: Demo Surface + Docs** — 2026-06-27

전체 상세: [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md)

</details>

<details>
<summary>✅ v1.2 Item Visual/Data Enrichment (Phases 12–14) — SHIPPED 2026-06-30</summary>

- [x] **Phase 12: API Spike + Data Lock (게이트)** — 큐레이션 15개·iconUrl CDN·fallback 잠금 (2026-06-29)
- [x] **Phase 13: Backend Enrichment + Seed Expansion** — V4 nullable 컬럼 + 4 DTO 패스스루 + seed 확장 (2026-06-29)
- [x] **Phase 14: Frontend Icons + Fallback + Docs** — 공용 `<ItemIcon>` + 3화면·셀렉터 아이콘·역할 배지 + docs (2026-06-30, UAT 8/8 + 보안 통과)

전체 상세: [milestones/v1.2-ROADMAP.md](milestones/v1.2-ROADMAP.md)

</details>

### 🚧 v1.3 관리자 콘솔 + 실데이터 라이브 배포 (Phases 15–18) — IN PROGRESS

**Goal:** 읽기전용 seed 데모를 → 실데이터로 수집·서빙되고, 관리자가 이벤트·워치리스트를 직접 관리하며, 무료로 라이브 배포된 데모로 승격(배포 직전 보안 검증 게이트 통과). 순서: 관리자 콘솔(15) → 대시보드 카드 개선(16) → 실데이터 전환(17) → 무료 배포+보안(18, 마지막). 15개 요구사항 100% 매핑.

**불변 제약(상시):** 프론트에서 Lostark API 직접 호출 금지 · 실키 코드/문서/로그/커밋 미기재 · 수집/캐시/event-impact는 Core Value(수집 신뢰성) 가드 하에서만.

#### Phase 15: 관리자 콘솔 UI

**Goal**: 기존 백엔드 `/api/admin/*`(X-Admin-Secret 게이트, 백엔드 인증 무변경)을 소비하는 프론트 관리자 콘솔 — 시크릿 입력 로그인 뒤에서 게임 이벤트·워치리스트를 관리하고 수집 상태를 모니터링한다.
**Depends on**: Nothing new (백엔드 admin 엔드포인트는 v1.0에 존재)
**Requirements**: ADMINUI-01, ADMINUI-02, ADMINUI-03, ADMINUI-04, ADMINUI-05, ADMINUI-06
**Success criteria**:
1. 관리자가 시크릿 로그인 후 관리자 작업이 인증되고 새로고침에도 세션이 유지된다
2. 잘못된 시크릿은 거부되고(401) 명확한 오류를 보여준다
3. 관리자가 게임 이벤트를 등록·수정·삭제하고 목록에서 반영을 확인한다
4. 관리자가 워치리스트 품목을 추가·비활성·재활성한다
5. 관리자가 수집 실행 이력을 확인하고, 미로그인 사용자에겐 쓰기 UI가 노출되지 않으며 로그아웃 시 세션이 종료된다

#### Phase 16: 대시보드 카드 개선

**Goal**: 대시보드 품목 카드에서 물품 고유 번호를 제거하고(아이콘·이름·골드·수집시각만), 카드 클릭 시 해당 품목이 선택된 상태로 품목 타임라인으로 딥링크 이동한다.
**Depends on**: Nothing (독립 프론트 변경) — 순서상 15 뒤
**Requirements**: CARD-01, CARD-02
**Success criteria**:
1. 대시보드 카드에 물품 고유 번호가 없고 아이콘·품목명·골드 가격·수집 시각만 표시된다
2. 카드를 클릭하면 해당 품목이 선택된 상태로 타임라인 화면으로 이동한다
3. v1.2 enrichment(아이콘·역할 배지)와 기존 카드 정보는 회귀 없이 유지된다

#### Phase 17: 실데이터 전환

**Goal**: 데모를 seed 합성 데이터에서 실수집(collection_run 기반) 데이터로 전환한다. 실 API 키로 수집기를 구동하고, seed 프로파일은 로컬/테스트 전용으로 격리하며, 초기 미축적 상태를 빈 화면 없이 정직히 처리한다.
**Depends on**: Nothing structural (수집기는 v1.0에 존재)
**Requirements**: REALDATA-01, REALDATA-02, REALDATA-03
**Success criteria**:
1. 실 API 키로 수집기가 구동되어 실데이터가 축적된다(collection_run SUCCESS)
2. 3화면이 실수집 데이터를 표시하고 seed 프로파일은 로컬/테스트 전용으로만 남는다
3. 데이터 미축적 초기 상태에서도 빈 화면 없이 '수집 중/데이터 없음'을 정직히 표시한다
4. 수집/캐시/event-impact 핵심 경로가 회귀 없이 그린(Core Value 가드)

#### Phase 18: 무료 라이브 배포 + 보안 검증 (마지막)

**Goal**: 무료 호스팅에 배포해 공개 URL로 데모 3화면 + 관리자 콘솔을 서빙한다. 프론트는 정적/단일 출처로 서빙하고, 실 키·시크릿은 서버 env only, 배포 직전 보안 검증 게이트를 통과한다. 무료 타깃(항상무료 VM vs 무료 PaaS)은 착수 시 리서치로 결정.
**Depends on**: Phase 15, 16, 17 (완성된 앱을 배포) — 마일스톤 마지막
**Requirements**: DEPLOY-01, DEPLOY-02, DEPLOY-03, DEPLOY-04
**Success criteria**:
1. 공개 URL로 데모 3화면과 관리자 콘솔에 접근된다
2. 프론트가 배포 환경에서 서빙되고 백엔드와 동일 출처로 API·관리자 시크릿 경로가 동작한다
3. 실 API 키·관리자 시크릿이 서버 env로만 주입되고 코드/문서/로그/커밋에 미기재된다
4. 배포 직전 보안 게이트 통과 — 관리자 쓰기 보호·API 키 미노출·DB/Redis 포트 비공개·HTTPS·CORS 정책

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–6 (Foundation → Distribution) | v1.0 | 15/15 | Complete | 2026-06-25 |
| 7–11 (Frontend Foundation → Demo Surface) | v1.1 | 19/19 | Complete | 2026-06-29 |
| 12–14 (API Spike → Frontend Icons) | v1.2 | 6/6 | Complete | 2026-06-30 |
| 15. 관리자 콘솔 UI | v1.3 | 4/4 | Complete    | 2026-07-03 |
| 16. 대시보드 카드 개선 | v1.3 | 1/1 | Complete    | 2026-07-03 |
| 17. 실데이터 전환 | v1.3 | 0/? | Planned | — |
| 18. 무료 라이브 배포 + 보안 검증 | v1.3 | 0/? | Planned | — |

**v1.3 Coverage:** v1.3 requirements 15 total · 매핑 **15/15 ✓** (ADMINUI 6 + CARD 2 + REALDATA 3 + DEPLOY 4)

---

_v1.0/v1.1/v1.2 상세는 milestones/ 아카이브. 현재 활성: v1.3 (Phases 15–18). 다음: `/gsd:discuss-phase 15` 또는 `/gsd:plan-phase 15`._
