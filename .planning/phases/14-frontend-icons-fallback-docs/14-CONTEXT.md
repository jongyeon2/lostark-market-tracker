# Phase 14: Frontend Icons + Fallback + Docs - Context

**Gathered:** 2026-06-29
**Status:** Ready for planning

<domain>
## Phase Boundary

백엔드 `src/` **0줄 변경** 원칙대로, Phase 13이 4개 read 응답(item list / latest / timeline / event-impact)에 노출한 `iconUrl·itemGroup·roleGroup`을 소비하는 **공용 `<ItemIcon>`(onError·null fallback)**으로 Dashboard 품목 카드·ItemSelect 셀렉터·Timeline 최신가 카드·Event Impact 품목 카드에 아이콘과 **역할 그룹 배지**를 입히고, 데이터 출처·API 실측·fallback 전략을 docs(루트 README + frontend/README)에 기록한다.

이 페이즈는 **순수 프론트 소비 한 겹**이다 — 새 백엔드 컬럼·DTO·수집/캐시/event-impact 로직을 만들지 않고(이미 13에서 완료), zod 스키마 확장 + 컴포넌트 + docs만 추가한다.

**불변 제약(상시 가드):**
- 프론트에서 Lostark Open API **직접 호출 금지** — 백엔드 DTO만 소비 (키 노출·CORS·레이트리밋 위반)
- 백엔드 `src/` **0줄 변경** (`git status`에 백엔드 변경 0이 검증 기준)
- 실 API 키·계정 식별자·가격 원문을 코드·문서·로그·커밋에 미기재 (공개 메타데이터만)
- `npm run build`(tsc 포함) 통과 + seed만으로(키 없이) 3화면에 아이콘 표시 + CDN 차단/아이콘 부재에도 UI 무파손
- 그룹 **필터 컨트롤은 v2** — v1.2는 역할 배지 + 그룹 헤더로 대체

</domain>

<decisions>
## Implementation Decisions

### 역할 시각 시스템 (팔레트·글리프·배지)
- **D-01:** 역할 3군(`DEALER`/`SUPPORT`/`MATERIAL`) 팔레트 = **도메인 직관색** — 딜러=레드 계열(공격), 서포터=블루/그린 계열(회복·버프), 융화재료=앰버/골드 계열(재화). 역할이 색만으로 즉시 읽히고 금융 마켓 "자산 섹터" 비유와 동형. 정확한 hex/Tailwind 토큰·대비는 Phase 14 재량(가독성·라이트 테마 대비 우선).
- **D-02:** fallback lucide 글리프 = **각인서(item_group=각인서, role_group=DEALER/SUPPORT)→`ScrollText`**, **융화재료(MATERIAL)→`FlaskConical`**. 기존 `lucide-react` 의존만 사용, 외부 라이브러리 추가 금지.
- **D-03:** 역할 배지 형태 = **색 배경 텍스트 배지** — 한글 라벨(딜러/서포터/융화재료) 명시. 기존 `ui/badge.tsx`(shadcn cva) + `StatusBadge`/`ImpactStatusBadge` 색 배지 관습을 재사용(역할색은 신규 variant 추가 또는 className 오버라이드 — 현 variant엔 역할색 없음). 라벨 병기(Phase 12 D-06)와 시너지.
- **D-04:** 배지 배치 = **품목명 옆 inline** — 이름과 함께 읽혀 식별 명확. 4곳(`ItemCard`·`ItemSelect`·`LatestPriceCard`·`EventImpactCards`) 공통 적용.

### 동일 각인서 아이콘 + fallback 트리거
- **D-05:** 유물 각인서 11종이 전부 동일 아이콘(`use_9_25.png`, Phase 12 findings (d) = identical)인 상황 처리 = **실아이콘 그대로 렌더 + 한글 라벨 병기(Phase 12 D-06) + 역할 배지로 식별**. fallback 글리프로 일부러 대체하지 않는다(실측 데이터를 정직하게 노출 — 이 프로젝트의 honesty 기조; 실제 게임 아이콘이라 "완성도"도 높아 보임). 융화재료 4종은 아이콘이 서로 구별됨.
- **D-06:** 공용 `<ItemIcon>` fallback 트리거 범위 = **`onError`(로딩 실패) AND `iconUrl`이 null/undefined일 때 둘 다** → 역할색 배경 + 글리프(D-02)로 대체. zod `iconUrl`은 `.nullable()`로 모델링(미래 아이콘 없는 품목·깨진 URL 모두 방어). 고정 슬롯이라 레이아웃 시프트 없음(ICON-01).

### 셀렉터·대시보드 조직
- **D-07:** 품목 정렬 기준 = **역할군(DEALER → SUPPORT → MATERIAL) → 이름** 순. 셀렉터·대시보드 공통. 백엔드 0줄이므로 **프론트 클라이언트 측 정렬**(`useItems()` 결과 정렬).
- **D-08:** ItemSelect 셀렉터 = **역할군 그룹 헤더(`SelectGroup`/`SelectLabel`)** 로 딜러/서포터/융화재료 섹션 구분(정적 헤더 — v2 필터 컨트롤과 다름). shadcn Select 기본 제공. 18개+ 스캔 명확.

### Docs
- **D-09:** README 역할 분담 = **루트 README**(포트폴리오 진입점)=데이터 출처·API 실측·fallback 전략·도메인 안목/자산섹터 서사 / **frontend/README**=아이콘 소비·`<ItemIcon>`·fallback **구현·실행** 안내. 둘 다 ICON-08 충족(중복 최소).
- **D-10:** findings 노출 정도 = **요약 + 링크 + 서사**. 출처(API `Icon` URL→DB), 실측 요약(각인서 등급 단일 글리프=동일·CDN 도메인 `cdn-lostark.game.onstove.com/efui_iconatlas/use/`), fallback 전략을 README에 요약하고 `12-SPIKE-FINDINGS.md`로 링크. `role_group`=금융 "자산 섹터" 비유·고변동 융화재료 도메인 안목을 한 줄 서사로.
- **D-11:** 스크린샷 = **아이콘·역할 배지 반영 새 스크린샷으로 3화면 갱신**(기존 README 스크린샷은 아이콘 없는 구버전). 시각 enrichment의 실제 증거. ⚠ 캡처는 **수동**(앱 실행 + 브라우저) — 사용자 액션 필요(아래 deferred/note 참조).

### Claude's Discretion
- 정확한 역할색 hex/Tailwind 토큰·대비, 글리프 크기, `<ItemIcon>` 슬롯 px 크기·`img` sizing(`width`/`height`/`loading`)·`alt` 텍스트 문구, 배지 spacing.
- zod 스키마 확장 형태(enrichment 3필드 배치, event-impact wrapper의 `enrichment` nesting → zod 매핑), 클라 정렬 구현 위치.
- `SelectGroup` 헤더 라벨 문구, README 정확 문구·스크린샷 도구.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 이 페이즈 계획·범위
- `.planning/ROADMAP.md` (§Phase 14: Frontend Icons + Fallback + Docs) — 목표 + 5개 성공 기준 + 검증 방법 + 사용자 확인 포인트
- `.planning/REQUIREMENTS.md` (ICON-01..08) — 이 페이즈가 충족하는 요구사항 8건
- `.planning/PROJECT.md` — Core Value, v1.2 불변 제약(프론트 API 직접호출 금지·백엔드 0줄·아이콘 출처=API Icon URL→DB), Key Decisions 표(프론트=Vite 프록시·zod 단일출처·UTC→KST 가드)

### 데이터 계약 (이 페이즈가 소비하는 것 — 필수)
- `.planning/phases/12-api-spike-data-lock/12-SPIKE-FINDINGS.md` — 6필드 큐레이션 **15개**(융화재료 4 + 딜러 9 + 서포터 2), iconUrl CDN base, **각인서 11종 동일 글리프(identical)·융화재료 구별됨**((d)), fallback 전략((e)). `<ItemIcon>`·역할 배지가 그대로 소비.
- `.planning/phases/13-backend-enrichment-seed/13-02-SUMMARY.md` — 4개 read 응답 enrichment 노출 형태. **event-impact = `EnrichedEventImpactResponse` wrapper(`itemId`/`window`/`enrichment`/`events`)**, latest=캐시 베이크(HIT zero-DB), item list/latest/timeline=제자리 확장. **zod 스키마 확장 시 이 형태에 정렬**(특히 event-impact는 현 zod 형태와 다름).
- `.planning/phases/13-backend-enrichment-seed/13-01-SUMMARY.md`, `.planning/phases/13-backend-enrichment-seed/VERIFICATION.md` — V4 nullable 컬럼·seed/watchlist 확장 결과(참고)

### 리서치 (다운스트림 가드)
- `.planning/research/PITFALLS.md` — Pitfall 7(각인서 동일 아이콘) → 라벨 병기로 식별(D-05 가드)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/lib/schemas.ts` — zod **단일 출처**(`z.infer`로 타입 파생). `trackedItemSchema`/`latestPriceSchema`/`timelineSchema`/`eventImpactSchema`에 `iconUrl`·`itemGroup`·`roleGroup` 추가(`.nullable()`). `roleGroup`은 `z.enum(['DEALER','SUPPORT','MATERIAL'])` 형태 권장. **현재 이 4개 스키마엔 enrichment 필드 없음 — 추가 필요.**
- `frontend/src/components/ui/badge.tsx` — shadcn Badge(cva variants: default/secondary/destructive/outline/ghost/link). **역할색 variant 없음** → 신규 variant 추가 또는 className으로 D-01 팔레트 적용.
- `frontend/src/features/dashboard/StatusBadge.tsx`, `frontend/src/features/impact/ImpactStatusBadge.tsx` — 기존 **색 배지 선례** 패턴(역할 배지 구현 참고).
- `frontend/src/features/_shared/ItemSelect.tsx` — shadcn Select **평면 `data.map`**(현재 `item.displayName`만 렌더). `SelectGroup`/`SelectLabel`로 역할군 그룹핑(D-08) + 각 옵션에 `<ItemIcon>`+배지(D-04). `useItems()` 소비 → 정렬(D-07)도 여기서.
- `frontend/src/features/dashboard/ItemCard.tsx` — `CardTitle`=displayName, category 서브타이틀. `TrackedItem` 타입 소비. ItemIcon+배지를 타이틀 옆(D-04).
- `lucide-react` — 이미 의존. `ScrollText`/`FlaskConical` 글리프(D-02).

### Established Patterns
- **zod `.parse`-at-boundary 단일 출처** + 백엔드 0줄 변경 + Vite 프록시 동일 출처(v1.1 D-05).
- **인라인 상태 매핑**(카드 레벨 실패가 화면 ErrorState로 번지지 않음 — ItemCard/LatestPriceCard) → `<ItemIcon>` fallback도 동일 철학: 아이콘 실패가 카드를 깨지 않고 글리프로 흡수.
- **UTC→KST 표시 가드**(`formatKst`) 유지.

### Integration Points
- **신규 공용 컴포넌트** `frontend/src/features/_shared/ItemIcon.tsx`(또는 `components/`) — props=`iconUrl`(nullable)+`roleGroup`(+크기), `onError`/null→역할색 글리프 fallback(D-06), 고정 슬롯. 4곳에서 소비.
- ⚠ `frontend/src/features/_shared/LatestPriceCard.tsx`(Timeline·Impact 공유) — 현재 props=`itemId`+`displayName`**만**. ICON-04 아이콘 표시하려면 `iconUrl`/`roleGroup` props 추가 **또는** timeline 응답 최상위 enrichment 소비. (호출처 `TimelinePage`/`ImpactPage` 수정 동반.)
- `frontend/src/features/impact/EventImpactCards.tsx`(ICON-05) — event-impact **wrapper 응답의 enrichment**(itemId/window/enrichment/events) 소비. zod `eventImpactSchema`가 wrapper 형태로 바뀌어야 함(현재 `{itemId, window, events}`만 — enrichment 누락).
- `frontend/src/components/ui/select.tsx` — `SelectGroup`/`SelectLabel` export 여부 확인(D-08; shadcn select 표준 포함이나 미export면 추가).
- 정렬(D-07)은 **프론트 클라**(`useItems()` 결과) — 백엔드 0줄.

</code_context>

<specifics>
## Specific Ideas

- **도메인 안목 서사**(D-10): 고변동·고가 융화재료(오레하/아비도스)가 로아온·시즌말·대형 업데이트에 시세 변동이 가장 크다(PROJECT.md) — 큐레이션이 단순 덤프 아닌 안목임을 README에 짧게 서술해 포트폴리오 서사로 연결.
- **`role_group`=금융 "자산 섹터" 비유** — README/배지 맥락에 살리면 면접 서사 강화.
- **"fallback이 미완성처럼 안 보이게"**(ROADMAP 사용자 확인 포인트) — 실아이콘 + 라벨 + 역할 배지로 식별을 캐리하고(D-05), 역할색 글리프가 "의도된 디자인"으로 보이도록 역할색 배경 채움(D-01/D-02). 회색 플레이스홀더처럼 보이지 않게.

</specifics>

<deferred>
## Deferred Ideas

- **그룹 필터 컨트롤** — v2(FILTER-V2-01). v1.2는 역할 배지(D-03) + 셀렉터 그룹 헤더(D-08)로 대체.
- **등급별 색상·정렬 정교화** — v2(GRADE-V2-01).
- **운명 계열 융화재료 / 만개·구원 각인서** — Phase 12 스파이크에서 미확인(만개=검색 0건 보류, 구원=실재 아님 제외) → v1.2 범위 밖. 큐레이션 15개 고정.
- **다크모드 · i18n · 실시간 갱신** — FE-V2, v1.2 시각 enrichment 범위 밖.

### Note — 수동 사용자 액션 (자동 증명 불가)
- **D-11 스크린샷 캡처**: 앱 실행(`docker compose up -d postgres redis` → seed 백엔드 + `npm run dev`) + 브라우저 캡처. executor는 텍스트 docs·`<img>` 슬롯·fallback 코드까지 작성하나 실제 스크린샷 이미지는 사용자가 캡처해 교체.
- **ROADMAP 사용자 확인 포인트**: 아이콘·역할 배지가 3화면에서 일관·식별적인지, fallback이 미완성처럼 안 보이는지 — 브라우저에서 직접 확인.

None 외 모두 위에 보존됨 — 논의는 페이즈 스코프(이미 잠긴 데이터를 어떻게 소비/표시) 안에 머물렀다.

</deferred>

---

*Phase: 14-Frontend Icons + Fallback + Docs*
*Context gathered: 2026-06-29*
