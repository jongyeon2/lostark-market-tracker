# Roadmap: 로스트아크 거래소 시세 수집·분석 파이프라인

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-06-25) — [archive](milestones/v1.0-ROADMAP.md)
- ✅ **v1.1 Frontend Demo Dashboard** — Phases 7–11 (shipped 2026-06-29) — [archive](milestones/v1.1-ROADMAP.md)
- ✅ **v1.2 Item Visual/Data Enrichment** — Phases 12–14 (shipped 2026-06-30) — [archive](milestones/v1.2-ROADMAP.md)
- 📋 **다음 마일스톤** — `/gsd-new-milestone`으로 정의 (Phase 15부터 연속 번호)

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

### 📋 다음 마일스톤 (미정)

`/gsd-new-milestone`으로 다음 마일스톤(요구사항·로드맵)을 정의한다. Phase 번호는 14에 이어 **15부터 연속**. v2 후보: 그룹 필터(FILTER-V2) · 등급 색상/정렬(GRADE-V2) · 관측성(OPS-V2) · 라이브 배포(DEPLOY-V2) · event-impact 고도화(IMPACT-V2) · 경매장/보석(SRC-V2) · 매직넘버 외부화(CFG-V2).

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–6 (Foundation → Distribution) | v1.0 | 15/15 | Complete | 2026-06-25 |
| 7–11 (Frontend Foundation → Demo Surface) | v1.1 | 19/19 | Complete | 2026-06-29 |
| 12. API Spike + Data Lock | v1.2 | 1/1 | Complete | 2026-06-29 |
| 13. Backend Enrichment + Seed | v1.2 | 2/2 | Complete | 2026-06-29 |
| 14. Frontend Icons + Fallback + Docs | v1.2 | 3/3 | Complete | 2026-06-30 |

**v1.2 Coverage:** v1 requirements 21 total · 완료 **21/21 ✓**

---

_v1.0/v1.1/v1.2 상세는 milestones/ 아카이브. 다음 마일스톤은 `/gsd-new-milestone`._
