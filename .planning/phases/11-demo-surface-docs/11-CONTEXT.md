# Phase 11: Demo Surface + Docs - Context

**Gathered:** 2026-06-27
**Status:** Ready for planning

<domain>
## Phase Boundary

리뷰어가 **seed 프로파일 백엔드 + `npm run dev`만으로 3화면(Dashboard / Item Timeline / Event Impact)을 비어있지 않게 재현**하도록 문서화하고, 시각적 마감과 스크린샷으로 **포트폴리오 데모 표면을 완성**하는 phase. (DEMO-01, DEMO-02)

구체적으로:
- **`frontend/README` 신규 작성** — 사전조건(seed 백엔드 기동) → 실행 명령 → 3화면 + 스크린샷 + Vite 프록시 동작 설명.
- **루트 README에 짧은 "프론트 데모 섹션" 추가** — 실행 순서 요약 + 대표 스크린샷 + `frontend/README` 링크. **기존 curl 데모 표면은 그대로 유지(회귀 금지).**
- **스크린샷 3장**(3화면 각 1장)을 `frontend/docs/screenshots/`에 커밋.
- **시각적 마감 점검·정렬 패스** — 로딩/빈/에러 상태와 데스크톱 기준 반응형을 3화면에 일관 마감(audit-and-fix, 리디자인 아님).

**Read-only · 백엔드 무변경.** 기존 read API만 소비하고 백엔드 `src/`는 0줄 변경. Phase 8·9·10이 이미 human-verified 완료 → 3화면은 **이미 동작**하므로 DEMO-01은 주로 "재현 검증 + 문서화"이지 신규 기능 빌드가 아니다.

**범위 밖(이 phase 아님):**
- **DEMO-03 정적 서빙(Spring `resources/static` 단일 출처 패키징) → v2로 미룸**(D-01). 백엔드 변경·SPA fallback·Docker 빌드 스테이지를 동반해 v1.1 프론트 데모 범위를 넘음.
- 모바일 완성형 반응형(D-08), 6-pillar 리디자인 감사(D-07), 스크린샷 자동 캡처 파이프라인(D-02) — 전부 과잉/v2.
- 실배포(Railway/Fly/Render, FE-V2-04), 관리자 쓰기 UI(FE-V2-01), 실시간 갱신(FE-V2-02).
</domain>

<decisions>
## Implementation Decisions

### 정적 서빙 채택 여부 (DEMO-03)
- **D-01:** **DEMO-03 정적 서빙은 v2로 미룬다.** Phase 11은 문서·스크린샷·시각적 마감에만 집중. 데모 재현은 "seed 백엔드 + `npm run dev`"(이미 동작)로 충분하며, 정적 서빙은 (a) Vite `base`/`build.outDir`→`resources/static` 배선, (b) React Router용 SPA fallback(딥링크 새로고침 시 Spring이 index.html 반환), (c) Dockerfile frontend 빌드 스테이지, (d) read 엔드포인트와 정적 경로 충돌 방지를 동반 → **백엔드 무변경 원칙과 긴장 + v1.1 범위 초과**. REQUIREMENTS가 이미 DEMO-03을 "선택/stretch, 슬립 가능"으로 표시. **결과: Phase 11 완료조건 5(정적 서빙)는 이번에 미충족 처리 — 의도된 스코프 결정.**

### 스크린샷 (DEMO-02)
- **D-02:** **수동 캡처.** 개발자가 seed 백엔드 + `npm run dev` 띄우고 브라우저에서 직접 캡처. Playwright 자동 캡처는 seed 기동·뷰포트 고정·스크립트 배선이 한 회성 산출물에 과한 인프라 → 기각. **plan/실행에서는 "이 자리에 스크린샷" 슬롯·파일명·alt 텍스트만 마련하고 실제 PNG 삽입은 사용자가 수행**(다운스트림 에이전트는 바이너리 캡처 불가).
- **D-03:** **`frontend/docs/screenshots/`에 PNG 커밋, README는 상대경로 참조.** GitHub·클론에서 바로 렌더·오프라인 재현. 외부 링크/CDN은 링크 깨짐 시 데모 표면 손상이라 기각. `frontend/.gitignore`의 `dist`/`dist-ssr`와 **별개 경로**(스크린샷은 추적 대상). 몇 장(D-04) 수준이라 바이너리 용량 부담 미미.
- **D-04:** **핵심 3화면 각 1장(총 3장).** Dashboard·Item Timeline·Event Impact의 "가장 잘 나온" seed 상태 1장씩. README의 "5분 안에 띄워볼 수 있다" 증명에 충분, 최신화 부담 최소. 상태 변형(insufficient_data·다운샘플 등) 추가 캡처는 ROI 대비 최신화 부담 커서 기각.

### README 분업 & 깊이 (DEMO-02)
- **D-05:** **루트 README = 짧은 포인터, `frontend/README` = 상세 일원화.** 루트에는 "프론트 데모" 섹션(실행 순서 3단계 요약 + 대표 스크린샷 1장 + `frontend/README` 링크)만 추가하고 상세는 `frontend/README`로 모음 → 중복 최소·단일 진실 원천. **기존 curl/샘플 JSON 데모 섹션은 손대지 않고 유지(회귀 금지 — ROADMAP 완료조건 3·검증 항목).** 두 README 각자 완결(실행 순서·스크린샷 양쪽 중복)은 최신화 지점이 둘이 되어 기각.
- **D-06:** **`frontend/README`는 재현 핵심 + 프록시 설명까지, 간결하게.** 포함: 사전조건(`SPRING_PROFILES_ACTIVE=seed`로 백엔드 기동) → `npm install && npm run dev` → 3화면 설명 + 스크린샷 + Vite 프록시 동작(왜 백엔드 무변경으로 `/api`가 동일 출처로 보이는지) 1문단. 트러블슈팅·`npm run build` 상세 안내는 최소/생략(정적 서빙 v2이므로 build 강조 불요).

### 시각적 마감 (완료조건 4)
- **D-07:** **audit-and-fix 점검·정렬 패스(리디자인 아님).** 3화면을 순회하며 로딩/빈/에러 상태와 간격·정렬 불일치를 찾아 고치는 일관성 회귀 수정. 공용 컴포넌트(`AsyncBoundary`·`LoadingState`·`EmptyState`·`ErrorState`)가 Phase 7~10에서 이미 구축됐으므로 신규 구축 아님. **명백한 회귀·불일치만 수정**, 6-pillar 체계 감사·대규모 리디자인 없음(그건 `/gsd-ui-review` v2 영역).
- **D-08:** **반응형 = 데스크톱 우선 + 좁은 화면 깨짐 없음.** 면접 데모는 노트북/모니터 기준. 데스크톱을 제대로 마감하고, 좁은 폭(태블릿~좌우 분할)에서 레이아웃이 깨지지 않으면 충분. Phase 10 D-04의 표↔카드 전환이 이미 그 선을 다룸. 모바일 완성형 브레이크포인트(Recharts 차트·네비게이션 모바일 대응)는 ROI 낮아 v2.

### Claude's Discretion
- 스크린샷 정확한 파일명·alt 텍스트·README 내 배치 위치(슬롯만 마련; 실제 삽입은 사용자).
- `frontend/README` 정확한 섹션 순서·헤딩·프록시 설명 문구 톤.
- 루트 README "프론트 데모" 섹션의 정확한 삽입 위치(기존 "데모 — curl + 샘플 JSON" 섹션 앞/뒤 등 — curl 섹션 회귀 없이).
- 시각적 마감 패스에서 실제로 수정할 불일치 항목 선정(점검 결과 기반).
- DEMO-01 재현 검증을 클린 클론에서 할지 별도 디렉터리에서 할지(검증 절차 세부 — verification 단계 재량).
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·범위·완료조건
- `.planning/REQUIREMENTS.md` — **DEMO-01~03 정의** + "확정 결정(이 마일스톤)" 표(seed 프로파일 우선·Vite 프록시·백엔드 무변경) + "실제 API 계약" 표 + Out of Scope(정적 서빙은 DEMO-03, CD는 v2). **MUST read** — 무엇을 문서화·재현해야 하는지의 기준.
- `.planning/ROADMAP.md` §"Phase 11: Demo Surface + Docs" — Goal · 완료조건(5; **5번=정적 서빙은 D-01로 v2 미룸 → 이번 미충족**) · 검증 방법(클린 클론 재현·스크린샷 최신화·기존 curl 섹션 회귀 없음) · 사용자 확인 포인트(README "5분 재현" 충족 여부 · DEMO-03 포함/연기 — **D-01에서 연기로 확정**).

### 상속 결정 (재정의 금지 — 데모/문서가 이 결정들을 정직히 서술해야 함)
- `.planning/phases/07-frontend-foundation/07-CONTEXT.md` — **상속:** Vite 프록시(백엔드 무변경)·`formatKst` off-by-9h 가드·`AsyncBoundary`+상태 컴포넌트(D-07 마감 대상)·feature 폴더 구조. `frontend/README`의 "프록시 동작"·"KST 표시" 서술 근거.
- `.planning/phases/08-dashboard/08-CONTEXT.md` · `.planning/phases/09-item-timeline/09-CONTEXT.md` · `.planning/phases/10-event-impact/10-CONTEXT.md` — 3화면 각각의 도메인·상태 분기(빈/400/404/insufficient)·공용 컴포넌트(`_shared/ItemSelect`·`LatestPriceCard`)·반응형(Phase 10 D-04 표↔카드). 스크린샷이 담을 "잘 나온 seed 상태"와 마감 점검 대상의 출처.
- `.planning/phases/07-frontend-foundation/07-UI-SPEC.md` · `.planning/phases/09-item-timeline/09-UI-SPEC.md` · (있으면) `.planning/phases/10-event-impact/10-UI-SPEC.md` — 디자인 시스템(shadcn slate·new-york, lucide-react, Inter, 색·간격 토큰). D-07 시각적 마감의 "일관" 기준선.

### 기존 데모 표면 (회귀 금지)
- `README.md` (루트, 현재 235줄) — §"데모 — curl + 샘플 JSON"(107~216줄)·§"한눈에 — 3단계 재현"·§"로컬 실행"(seed 프로파일 기동 명령 포함). **MUST read** — D-05의 "프론트 데모 섹션 추가하되 curl 섹션 유지", seed 기동 명령 재사용.

### 프로젝트 원칙
- `.planning/PROJECT.md` §"Core Value"·§"Context"·§"Key Decisions" — 백엔드 무변경(사용자 제약), 상관≠인과·honest-data 에토스(README 카피가 과대주장 금지), seed 데모 이벤트 5분 오프셋 배치(non-zero changeRate 의도 — 스크린샷이 담는 데이터의 출처).
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `README.md` (루트) — 기존 curl 데모·seed 기동 명령(`SPRING_PROFILES_ACTIVE=seed`). 루트 "프론트 데모" 섹션은 이 위에 얹고 기존 섹션은 보존(D-05).
- `frontend/vite.config.ts` — `server.proxy['/api']` → `:8080`, `VITE_API_TARGET` override 가능. `frontend/README` 프록시 설명(D-06)의 코드 근거.
- `frontend/.env.example` — 프록시 타깃 override 변수 예시(현재 워킹트리에 1줄 수정 미커밋). frontend/README의 환경 변수 안내와 연결.
- `frontend/src/components/state/{AsyncBoundary,LoadingState,EmptyState,ErrorState}.tsx` — Phase 7~10 공용 상태 컴포넌트. D-07 마감은 이들을 **점검·정렬**(신규 구축 아님).
- `frontend/src/features/{dashboard,timeline,impact}/` + `frontend/src/features/_shared/` — 3화면 + 공용 셀렉터/최신가 카드. 스크린샷·마감 점검 대상.
- `frontend/package.json` — `dev`/`build`/`preview` 스크립트. README 실행 명령의 출처(`npm install && npm run dev`).

### Established Patterns
- **컴포넌트별 `<AsyncBoundary>` 상태 격리**(Phase 7 D-08) — 마감 점검(D-07)이 "각 화면의 로딩/빈/에러가 일관 렌더되는가"를 보는 단위.
- **URL searchParams 단일 소스**(Phase 9/10) — `?item=&from=&to=`·`?window=` 딥링크. 스크린샷이 특정 상태를 재현·고정하는 수단(딥링크 URL로 캡처 장면 재현).
- **UTC 데이터 → KST 표시**(Phase 7 D-09) — README가 서술할 "시각은 KST, 정렬·계산은 UTC" 정직성 포인트.

### Integration Points
- **신규 파일:** `frontend/README.md`(D-05/D-06), `frontend/docs/screenshots/*.png`(D-03/D-04, 사용자 삽입).
- **수정 파일:** 루트 `README.md`(프론트 데모 섹션 추가 — curl 섹션 회귀 없이, D-05).
- **마감 수정 가능 파일:** `frontend/src/**`(D-07 점검 결과 불일치만 — 백엔드 `src/`는 절대 무변경).
- **신규 라이브러리 불요** — 문서·스크린샷·기존 컴포넌트 점검 phase.
- Vite 프록시 `/api`→`:8080`, **seed 프로파일 백엔드** 기동 전제(재현 검증·스크린샷의 데이터 출처).
</code_context>

<specifics>
## Specific Ideas

- **"5분 안에 띄워볼 수 있다"가 합격선:** ROADMAP 사용자 확인 포인트 — 면접관이 README만 따라 클린 클론에서 3화면을 재현. frontend/README(D-06)는 그 한 줄을 충족하는 최소 경로로 작성.
- **기존 curl 데모는 자산이지 부채가 아님:** 백엔드 포트폴리오의 헤드라인을 파는 표면이므로 프론트 데모가 대체가 아니라 **추가**(D-05). 루트 README는 "curl로도, 브라우저로도" 두 길을 제시.
- **정직성 일관:** README 카피는 상관≠인과·seed 합성 데이터임을 명시(PROJECT honest-data 에토스). 스크린샷이 담는 seed 화면도 "실데이터 아님"을 흐리지 않음.
- **정적 서빙 v2 결정도 정직히 기록:** D-01은 "못 해서"가 아니라 "범위 결정". README/문서에 단일 출처 패키징은 v2 후보임을 한 줄로 정직히 남길 수 있음(과대 약속 회피).
</specifics>

<deferred>
## Deferred Ideas

- **DEMO-03 정적 서빙(Spring `resources/static` 단일 출처)** — v2(FE-V2 계열). D-01에서 이번 미루기 확정. 채택 시 Vite `base`·SPA fallback·Dockerfile 빌드 스테이지·정적/`/api` 경로 충돌 처리 동반.
- **스크린샷 자동 캡처(Playwright) + 최신화 게이트** — D-02에서 수동 채택. 화면이 자주 바뀌는 v2 시점에 자동화 ROI 발생.
- **모바일 완성형 반응형(차트·네비 모바일 대응)** — D-08에서 데스크톱 우선. v2 프론트 고도화.
- **6-pillar UI 리디자인 감사(`/gsd-ui-review`)** — D-07에서 점검·정렬 패스로 한정. 본격 비주얼 고도화는 v2.
- **실배포(Railway/Fly/Render) + 프론트 CI(타입체크/빌드) 게이트** — FE-V2-04(backend DEPLOY-V2-01 동반).

None other — 논의는 Phase 11 범위(DEMO-01~03) 내에 머물렀다.
</deferred>

---

*Phase: 11-demo-surface-docs*
*Context gathered: 2026-06-27*
