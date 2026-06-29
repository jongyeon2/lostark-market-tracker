# Phase 12: API Spike + Data Lock (게이트) - Context

**Gathered:** 2026-06-29
**Status:** Ready for planning

<domain>
## Phase Boundary

본인 JWT 키로 `/markets/options`·`/markets/items`를 **1회 실측**해 융화재료·유물 각인서의 `external_item_id`·`display_name`·`category`·**iconUrl 제공 여부**를 확정하고, 거래 가능·아이콘 구별 여부가 확인된 **큐레이션 ~17개**(범위 12~20)와 **fallback 전략**을 `12-SPIKE-FINDINGS.md`로 잠근다. 이후 Phase 13(백엔드 enrichment/seed)·14(프론트 아이콘)는 이 findings 상수만 소비한다(런타임은 키 불필요). v1.0 Task 0의 spike-then-lock 패턴 재사용.

이 페이즈는 **데이터를 잠그는 게이트**다 — 코드 산출물은 기존 spike 인프라 확장 + findings 문서이며, enrichment 컬럼·DTO·seed·프론트는 이 페이즈가 만들지 않는다(후속 phase).

**불변 제약(상시 가드):**
- 실 API 키·계정 식별자·가격 원문을 findings·seed·로그·커밋에 미기재 (공개 메타데이터만)
- 수집 스케줄러·Redis 캐시·EventImpactService **0줄** 무변경 (이 페이즈는 spike 패키지만 건드림)
- 큐레이션은 **API 실측으로 거래 가능·item id 확인된 것만** 확정 (추정/플레이스홀더 0건)

</domain>

<decisions>
## Implementation Decisions

### 큐레이션 구성·분류
- **D-01:** 융화재료는 **상급 오레하 + 최상급 오레하 + 아비도스 융화재료 + 상급 아비도스 융화재료(T4)** 4종을 목표로 잠근다. (상급 아비도스 융화재료는 4티어 강화 융화재료 — 사용자 도메인 확인으로 추가, 2026-06-29.) 운명 계열은 스파이크에서 거래 가능·식별이 확인되면 추가 검토, 미확인 시 보류(→ Deferred). **거래 가능·정확 `external_item_id`·`iconUrl`은 스파이크가 실측 확정** — 미거래/식별 불가 품목은 findings에서 제외.
- **D-02:** 각인서는 **딜러·서포터 균형** 구성 — 딜러 각인 다수(~9) + 서포터 각인(각성/만개/전문의 등, ~4)을 포함해 role_group 배지 데모가 3군을 모두 보이게 한다. 전체 덤프가 아닌 도메인 안목 큐레이션.
- **D-03:** `role_group` enum = **`DEALER` / `SUPPORT` / `MATERIAL`** 3값으로 잠근다. (딜러각인=DEALER, 서포터각인=SUPPORT, 융화재료=MATERIAL.) `item_group`은 이와 별개로 각인서/강화재료(융화재료) 등 품목군 구분에 사용 — Phase 14 배지·zod 스키마가 이 값을 소비.
- **D-04:** 총 큐레이션 목표 **~17개** (융화재료 4 + 각인서 ~13). 최종 개수는 스파이크 거래확인 통과분으로 확정하되 12~20 범위 유지.

### Fallback·아이콘 구별
- **D-05:** 아이콘 부재/로딩 실패 fallback = **역할색 배경 + lucide 글리프**(각인서=책 계열, 융화재료=플라스크/망치 계열). 외부 라이브러리 없이 기존 lucide만 사용. 고정 슬롯이라 레이아웃 시프트 없음(ICON-01에서 이미 확정). 정확한 글리프·역할색 팔레트는 Phase 14 재량.
- **D-06:** 각인서 아이콘이 서로 **동일한 책 글리프**로 확인돼도 **라벨 병기로 충분**으로 처리한다(카드·셀렉터에 품목명 라벨이 항상 붙음). 스파이크는 각 iconUrl이 **서로 구별되는지** 눈으로 확인해 findings에 "구별됨/동일함"을 명시한다(Pitfall 7).

### findings 산출물 계약
- **D-07:** findings 파일 = **`.planning/phases/12-api-spike-data-lock/12-SPIKE-FINDINGS.md`** (Task 0의 `TASK0-FINDINGS.md` 포맷 계승).
- **D-08:** 품목당 **잠금 스키마 = 6필드**: `external_item_id` / `display_name` / `category_code` / `icon_url` / `item_group` / `role_group`. findings는 이 6필드를 **품목별 표**로 + 마스킹된 실응답 발췌(증거)로 구성한다. Phase 13 seed/TrackedItem/DTO가 이 표를 그대로 옮겨 적는다(전사 계약). `grade`·정확 `CategoryCode` 등은 findings 산문에 증거로 남기되 seed 계약 6필드엔 미포함. 가격·키·식별자 제외.

### 스파이크 실행·안전캡처
- **D-09:** **기존 spike 인프라 확장** — `MarketsApiSpikeTest`에 `/markets/options` 호출 + 각인서(40000)/융화재료 카테고리 검색 케이스를 추가하고, `LostarkSpikeClient`에 options 조회 메서드를 추가한다. `@Disabled` + `spike` 프로파일 유지(CI 미실행, 키 필요). 신규 별도 테스트 만들지 않음(클라이언트 중복 회피).
- **D-10:** 안전 캡처 = **콘솔 출력 → 마스킹 수기정리**. 사용자가 로컬 env JWT로 spike 테스트를 1회 실행 → 콘솔 출력만(디스크 미기록) → 그 출력을 6필드 표·마스킹 발췌로 findings에 정리 → **커밋 전 `bearer`/JWT/가격 패턴 grep**으로 0건 확인. raw 응답을 디스크에 덤프하지 않음.

### Claude's Discretion
- 정확한 lucide 글리프 선택·역할색 팔레트(Phase 14 UI), spike 테스트 케이스 분할·`LostarkSpikeClient` options 메서드 시그니처, findings 표의 열 정렬·발췌 형식, `/markets/options` 응답 파싱 디테일 — planner/executor 재량.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 이 페이즈 계획·범위
- `.planning/ROADMAP.md` (§Phase 12: API Spike + Data Lock) — 목표 + 5개 성공 기준 + 검증 방법 + 사용자 확인 포인트
- `.planning/REQUIREMENTS.md` (SPIKE-01..05) — 이 페이즈가 충족하는 요구사항 5건
- `.planning/PROJECT.md` — Core Value, v1.2 불변 제약, Key Decisions 표(spike-then-lock·무변경 원칙·아이콘 출처=API Icon URL→DB)

### v1.2 리서치 (스파이크 직접 관련)
- `.planning/research/SUMMARY.md` — spike-then-lock 권장 접근 + Gaps(Icon 필드·CategoryCode·아비도스 포함 여부는 Phase 12 실측 확정)
- `.planning/research/PITFALLS.md` — Pitfall 1(키 유출)·5(비거래/잘못된 id)·7(각인서 동일 아이콘)이 이 페이즈 가드
- `.planning/research/FEATURES.md`, `.planning/research/ARCHITECTURE.md` — enrichment read-path 한 겹 구조(다운스트림 맥락)

### 선례 (동일 API 모델 잠금)
- `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — 동일 markets API 스파이크 findings 포맷·증거 마스킹·D-05/D-06 결정 선례. **이번 findings의 포맷 기준.** 이미 `/markets/items` 리스트 응답에 `Icon`(CDN URL) 필드 존재가 확인됨 — Phase 12는 필드명·도메인·각인서 구별 여부를 재확인·기록.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/com/lostark/tracker/spike/LostarkSpikeClient.java` — RestClient 기반 spike 전용 클라이언트(키 env, 선행 `bearer `·공백 strip). **`/markets/options` + 카테고리 검색 메서드를 여기에 추가**(D-09).
- `src/test/java/com/lostark/tracker/spike/MarketsApiSpikeTest.java` — `@Disabled`·`spike` 프로파일 통합 테스트. **이 테스트에 옵션 조회 + 각인서/융화재료 검색 케이스 추가**(D-09). 사용자가 로컬 키로 1회 실행.
- `src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java` — 큐레이션 품목 등록처(Phase 13 다운스트림 소비처). 이 페이즈의 6필드 findings 표가 여기로 베이크된다.

### Established Patterns
- **spike-then-lock**: @Disabled spike 테스트로 실응답 캡처 → findings 문서로 모델·상수 잠금 → 후속 phase는 상수만 소비(런타임 키 불필요). Task 0에서 검증됨.
- **키 안전**: 키는 셸 env에만, findings엔 공개 메타데이터만, 커밋 전 grep. (TASK0-FINDINGS가 마스킹 발췌 선례.)
- **CategoryCode는 leaf여야 함**: 부모 코드(예: 50000)는 `TotalCount:0`, leaf(예: 50010)만 품목 반환. leaf 코드는 `/markets/options`의 `Categories[].Subs[].Code`에서 얻음(Task 0 발견). 각인서=40000·융화재료 코드는 이 경로로 실측 확정.

### Integration Points
- 이 페이즈 산출물(`12-SPIKE-FINDINGS.md`의 6필드 표 + fallback 전략)이 **Phase 13의 V4 컬럼·TrackedItem 필드·4개 read DTO·WatchlistSeeder/SyntheticDemoData**와 **Phase 14의 `<ItemIcon>` fallback·role 배지**의 직접 입력이다. 이 페이즈는 그 코드를 만들지 않고 **계약(상수+전략)만 잠근다**.

</code_context>

<specifics>
## Specific Ideas

- 도메인 안목 강조: "골드 많이 드는 고변동 융화재료가 로아온·시즌말·대형 업데이트에 변동이 가장 심하다"(PROJECT.md) — 큐레이션이 단순 덤프가 아니라 이 안목을 보이는 선택임을 findings에 짧게 서술하면 포트폴리오 서사로 연결.
- role_group(DEALER/SUPPORT/MATERIAL)은 금융 마켓의 "자산 섹터" 비유와 동형 — findings/README에서 이 비유를 살리면 면접 서사 강화.
- 각인서 iconUrl 구별 여부는 findings에 **명시적 한 줄**로 기록(구별됨/동일함) — Phase 14가 라벨 병기 결정을 그대로 받게.

</specifics>

<deferred>
## Deferred Ideas

- **운명 계열 융화재료** — 스파이크에서 거래 가능·식별이 확인되면 큐레이션 추가 검토, 미확인 시 v1.2 범위 밖.
- **그룹 필터 컨트롤** — v2 (FILTER-V2-01); v1.2는 역할 배지로 대체.
- **등급별 색상·정렬 정교화** — v2 (GRADE-V2-01).

None 외 모두 위에 보존됨 — 논의는 페이즈 스코프(실측·잠금) 안에 머물렀다.

</deferred>

---

*Phase: 12-API Spike + Data Lock (게이트)*
*Context gathered: 2026-06-29*
