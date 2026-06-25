# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–6) — SHIPPED 2026-06-25</summary>

- [x] **Phase 1: Foundation + Task 0** — 실행 가능한 골격 + 실측으로 잠근 데이터 모델 (3/3 plans) — 2026-06-20
- [x] **Phase 2: Collection Pipeline** — 레이트리밋·재시도·부분 실패를 처리하는 신뢰 가능한 10분 수집기 (3/3 plans) — 2026-06-22
- [x] **Phase 3: Read API + Cache** — Redis 캐시 latest/타임라인/다운샘플 + 헬스 (3/3 plans) — 2026-06-23
- [x] **Phase 4: Admin + Events** — 시크릿 인증 뒤 품목/이벤트 CRUD (2/2 plans) — 2026-06-24
- [x] **Phase 5: Event Impact (게이트 조건부)** — 이벤트 전후 변화율 + 데이터 충분성 가드 (2/2 plans) — 2026-06-24
- [x] **Phase 6: Distribution + Docs** — CI · 격리 시드 · README 데모 표면 (2/2 plans) — 2026-06-25

전체 페이즈 상세는 [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md) 참조.

</details>

### 다음 마일스톤 (계획 예정)

다음 마일스톤은 `/gsd-new-milestone`으로 시작합니다 (questioning → research → requirements → roadmap). v2 후보는 아카이브된 REQUIREMENTS의 v2 섹션 참조.

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation + Task 0 | v1.0 | 3/3 | Complete | 2026-06-20 |
| 2. Collection Pipeline | v1.0 | 3/3 | Complete | 2026-06-22 |
| 3. Read API + Cache | v1.0 | 3/3 | Complete | 2026-06-23 |
| 4. Admin + Events | v1.0 | 2/2 | Complete | 2026-06-24 |
| 5. Event Impact (게이트 조건부) | v1.0 | 2/2 | Complete | 2026-06-24 |
| 6. Distribution + Docs | v1.0 | 2/2 | Complete | 2026-06-25 |
