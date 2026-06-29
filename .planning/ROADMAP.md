# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** — Phases 7–11 (shipped 2026-06-29) — [archive](milestones/v1.1-ROADMAP.md)
- 📋 **v1.2 (다음 마일스톤)** — 미정 — `/gsd-new-milestone`로 범위 정의

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–6) — SHIPPED 2026-06-25</summary>

- [x] Phase 1: Foundation + Task 0 — completed 2026-06-20
- [x] Phase 2: Collection Pipeline — completed 2026-06-22
- [x] Phase 3: Read API + Cache — completed 2026-06-23
- [x] Phase 4: Admin + Events — completed 2026-06-24
- [x] Phase 5: Event Impact (게이트 조건부) — completed 2026-06-24
- [x] Phase 6: Distribution + Docs — completed 2026-06-25

전체 상세: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v1.1 Frontend Demo Dashboard (Phases 7–11) — SHIPPED 2026-06-29</summary>

- [x] Phase 7: Frontend Foundation (3 plans) — completed 2026-06-25
- [x] Phase 8: Dashboard (3 plans) — completed 2026-06-25
- [x] Phase 9: Item Timeline (5 plans) — completed 2026-06-26
- [x] Phase 10: Event Impact (4 plans) — completed 2026-06-26
- [x] Phase 11: Demo Surface + Docs (4 plans, incl. gap closure 11-04) — completed 2026-06-27

전체 상세: [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md)

</details>

### 📋 v1.2 (다음 마일스톤 — 미정)

다음 마일스톤 범위는 아직 정의되지 않았다. `/gsd-new-milestone`로 착수한다(questioning → research → requirements → roadmap).

후보(PROJECT.md Active / v2 candidates 참조):
- **관측성(OPS-V2)** — Micrometer 카운터(429 / skipped tick / failed item / cache hit·miss) + Actuator
- **라이브 배포(DEPLOY-V2)** — Railway/Fly/Render + 프론트 CI
- **event-impact 고도화(IMPACT-V2)** — 카테고리 베이스라인 초과상승률, median/스무딩
- **소스 확장(SRC-V2)** — 경매장(AUCTIONS)/보석
- **매직넘버 외부화(CFG-V2)** — `@ConfigurationProperties`

## Progress

| Phase | Milestone | Plans | Status | Completed |
|-------|-----------|-------|--------|-----------|
| 1–6 (MVP) | v1.0 | 15 | ✅ Complete | 2026-06-25 |
| 7. Frontend Foundation | v1.1 | 3/3 | ✅ Complete | 2026-06-25 |
| 8. Dashboard | v1.1 | 3/3 | ✅ Complete | 2026-06-25 |
| 9. Item Timeline | v1.1 | 5/5 | ✅ Complete | 2026-06-26 |
| 10. Event Impact | v1.1 | 4/4 | ✅ Complete | 2026-06-26 |
| 11. Demo Surface + Docs | v1.1 | 4/4 | ✅ Complete | 2026-06-27 |
