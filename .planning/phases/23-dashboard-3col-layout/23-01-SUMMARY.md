---
phase: 23-dashboard-3col-layout
plan: 01
status: complete
requirements: [UX-01, UX-02]
commits: [933caa4]
---

# 23-01 SUMMARY — 대시보드 3열 카테고리 레이아웃 (UX-01/UX-02)

## 무엇을 했나

대시보드를 2열(좌 물품[각인/재료 섹션] / 우 소식)에서 **maplanet식 3열**(좌 카테고리 필터 / 중앙 물품 / 우 소식)로
재구성했다. **Core Value 0줄** — 순수 프론트 read-path, 백엔드/수집/캐시 무변경.

- **`categories.ts`(신규):** 카테고리 taxonomy 단일 출처(`roleGroup.ts` 동형). leaf 7종(각인 딜러/서포터 =
  roleGroup, 재료 5 = itemGroup)·표시순서·predicate를 한 곳에 잠금. `deriveCategories(items)`=count>0 leaf만 순서대로,
  `filterByCategory(items,id)`, `firstCategoryId(cats)`. 순수 함수(React·페칭 없음). `roleGroup=null`/미지정 itemGroup은
  어느 leaf에도 안 들어감(현행 미표시 유지).
- **`CategoryNav.tsx`(신규):** 무상태 반응형 컴포넌트. props `{categories, selectedId, onSelect}`.
  - 데스크톱(lg+): `<nav aria-label="카테고리">` 2단계 그룹(헤더 각인/재료 = 비선택 라벨) + leaf 버튼(라벨 + count badge).
    `lg:sticky lg:top-6`. 활성 leaf = `text-primary + bg-muted + 좌측 2px primary + font-semibold + aria-current`(색 단독 아님).
  - 모바일(<lg): 같은 컴포넌트가 상단 가로 스크롤 칩(그룹 헤더 생략). 활성 칩 = `bg-primary/text-primary-foreground`.
- **`DashboardPage.tsx`(수정):** 2열→3열 grid `lg:grid-cols-[11rem_minmax(0,1fr)_20rem] max-w-7xl`.
  `useState<picked>` + 파생 `selectedId`(picked가 현재 cats에 없으면 첫 leaf로 **자기치유** — useEffect 없이 렌더 파생).
  중앙 = `filterByCategory(sorted, selectedId)`를 ItemCard 세로 스택(선택=문맥이라 섹션 헤더 없음). 기본 = 첫 비어있지 않은 leaf(딜러 각인).
  기존 `sortByRole`·물품/소식 독립 `AsyncBoundary` 유지. 모바일 소스 순서 = 칩 → 물품 → 소식.
- **무변경:** `ItemCard`·`NewsPanel`(내용 계약 그대로), `roleGroup.ts`, 백엔드·쿼리·스키마. 프론트 `itemGroup`은
  `z.string().nullable()` freeform이라 재료 5그룹 자동 수용.

## 검증

- **`cd frontend && npm run build`(tsc -b + vite build) 종료코드 0** — 타입 에러 0, 2621 모듈 번들 성공(청크>500kB 경고는
  기존 recharts 등, 비차단). 프론트 테스트 러너 없음 → 빌드가 게이트.
- **must_haves 대조(코드):** 3열 grid·2단계 그룹 nav·leaf 필터·기본/폴백 선택·빈 카테고리 숨김(deriveCategories filter)·
  모바일 칩(`lg:hidden`/`hidden lg:block`)·활성 다중 인코딩(aria-current+bg+굵기)·독립 AsyncBoundary — 전부 충족.
- **Core Value 가드:** 백엔드/수집/캐시/event-impact 파일 **0개 변경**, 신규 디자인 토큰 0(기존 primary/muted/ring 재사용).
- **✅ 라이브 시각 QA 완료(Playwright, 로컬 풀스택):** 데스크톱(1280px) 3열 렌더·2단계 그룹 nav·count badge 확인;
  `재련재료` 클릭 → 중앙이 딜러 각인 → 재련재료 2종으로 전환(필터 동작); 빈 카테고리 숨김 실증(상급재련/재련보조/
  아크그리드젬은 현재 dev DB에 활성 품목 없어 자동 숨김); 모바일(390px) 상단 가로 칩 → 물품 → 소식 스택, 선택 상태
  뷰포트 전환에도 유지. 스크린샷 `phase23-desktop-3col.png`·`phase23-mobile-chips.png`.
  - *참고(비버그): 상급재련/재련보조/아크그리드젬 카테고리는 갱신된 워치리스트가 실행 중 백엔드에 재시드된 뒤 나타난다
    (현재 로컬 백엔드는 이전 seed 상태). 빈-숨김 로직이 정상 동작함을 오히려 실증.*

## 후속

- v1.5 마일스톤 **4/4 완료**(Phase 20·21·22·23). Phase 24(경매장 보석)는 v1.6 후보.
- 배포: 이 커밋들은 로컬 main에만. push 시 CI가 프론트 빌드 후 GHCR 이미지 재배포.

## 커밋

- (code) `933caa4` — categories.ts · CategoryNav.tsx · DashboardPage.tsx
- (docs) 23-UI-SPEC(9f33e02) · 23-01-PLAN/SUMMARY/VERIFICATION · STATE/ROADMAP
