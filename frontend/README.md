# Lostark Tracker — 프론트 데모 (브라우저)

백엔드 read API를 소비하는 **데스크톱 우선 React 데모**입니다. 3화면(Dashboard / Item Timeline / Event Impact)을 **`seed` 백엔드 + `npm run dev`만으로** 비어있지 않게 재현할 수 있습니다. 백엔드는 **0줄도 바뀌지 않습니다** — 브라우저가 보는 `/api/*`는 Vite dev 프록시가 같은 출처처럼 백엔드로 넘깁니다.

> **데이터 출처는 백엔드 프로파일이 정합니다** — `seed`는 **로컬/테스트 전용 합성 데이터**(API 키 없이 즉시 재현), `dev`는 **실수집 실데이터**(라이브 데모의 실체)입니다. 프론트는 어느 쪽이든 **같은 read API/DTO만** 소비하므로 화면 코드는 동일합니다. 변화율은 **시점 상관**이며 인과가 아닙니다.

## 사전조건 — 백엔드 기동 (`seed` 즉시 재현 / `dev` 실수집)

루트 [`README.md`](../README.md)의 "실행 방법"대로 인프라(Postgres 16 + Redis 7)를 띄우고 백엔드를 실행합니다. **즉시 재현**은 `seed` 프로파일(합성 8일치 시세 + 데모 이벤트, **API 키 불필요** — 로컬/테스트 전용), **라이브 실체**는 `dev` 프로파일(`.env`의 `LOSTARK_API_KEY`로 실수집)입니다. 프론트는 어느 쪽이든 같은 read API를 봅니다.

```bash
# 레포 루트에서
docker compose up -d

# 즉시 재현 — seed(로컬/테스트 전용 합성)
./gradlew bootRun --args='--spring.profiles.active=seed'

# 또는 라이브 실체 — dev(실키 실수집; .env 의 LOSTARK_API_KEY 로드)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 실행

```bash
cd frontend
npm install
npm run dev          # Vite dev 서버 → http://localhost:5173
```

`package.json` 스크립트: `dev`(개발 서버) · `build`(`tsc -b && vite build`) · `preview`(빌드 결과 미리보기).

## 3화면

| 화면 | 라우트 | 설명 |
|------|--------|------|
| **Dashboard** | `/` | 수집 파이프라인 헬스 카드 + 활성 품목 최신가 워치리스트 |
| **Item Timeline** | `/timeline?item=<품목ID>` | 시세 라인 차트 + 게임 이벤트 세로 마커(딥링크로 기간 재현, 생략 시 최근 30일) |
| **Event Impact** | `/impact?item=<품목ID>&window=24` | 이벤트 전후 변화율 결과 표 + 상관≠인과 안내 배너 |

![Dashboard 화면 — 수집 파이프라인 헬스 카드와 활성 품목 최신가 워치리스트](docs/screenshots/dashboard.png)

![Item Timeline 화면 — 시세 라인 차트와 게임 이벤트 세로 마커](docs/screenshots/item-timeline.png)

![Event Impact 화면 — 이벤트 전후 변화율 결과 표와 상관≠인과 안내](docs/screenshots/event-impact.png)

> 스크린샷은 레포에 커밋된 상대경로 PNG입니다([캡처 프로토콜](docs/screenshots/README.md)). 캡처는 `seed`(합성) 화면 기준이며, `dev` 실수집에서도 같은 화면 구조로 실데이터가 표시됩니다.

## 아이콘 + 역할 배지

3화면은 품목을 텍스트만이 아니라 **아이콘 + 역할 배지**로 보여줍니다. seed 백엔드만 띄우면(키 불필요) `npm run dev`로 아이콘까지 그대로 뜹니다.

### 소비 방식 — 백엔드 DTO만 본다

백엔드 4개 read 응답의 `iconUrl` / `itemGroup` / `roleGroup`을 **zod 스키마**(`src/lib/schemas.ts`, 단일 출처)가 `.nullable()`로 검증해 받습니다(미상 `roleGroup`은 `.parse` 경계에서 loud-fail). 프론트는 **Lostark Open API를 직접 부르지 않고** 백엔드 DTO만 소비합니다. 공용 `<ItemIcon>` / `<RoleBadge>`가 4곳에서 이 값을 소비합니다 — Dashboard 품목 카드 · 품목 셀렉터 · Timeline 최신가 카드 · Event Impact 정체성 영역.

### `<ItemIcon>` 구현 (`src/features/_shared/ItemIcon.tsx`)

- **고정 px 슬롯**(md 32 / sm 20) → 로딩·실패·부재 어느 상태에서도 동일 footprint, **레이아웃 시프트 0**.
- `iconUrl == null` 또는 `<img>`의 `onError`(CDN 차단·깨진 URL 포함) → **역할색 글리프 타일** fallback: 딜러·서포터 = `ScrollText`, 융화재료 = `FlaskConical`, 미상 = `Package`.
- 외부 아이콘 라이브러리 없이 기존 **`lucide-react`만** 사용(`<img>`는 `loading="lazy"` · `decoding="async"` · `alt=""`).

### 역할 배지 · 셀렉터

- **`<RoleBadge>`** — 역할군(딜러 / 서포터 / 융화재료)을 solid 색배경 + 흰 텍스트 + 한글 라벨 pill로 표시(색 단독 금지, 라벨 항상 병기). `roleGroup`이 null이면 아무것도 렌더하지 않습니다.
- **품목 셀렉터** — 옵션을 역할군 그룹 헤더(`딜러` / `서포터` / `융화재료`, null은 `기타`)로 묶고 역할군 → 이름 순으로 정렬합니다. 큐레이션 15개가 누락 없이 선택 가능합니다. (역할군 **필터** 컨트롤은 v2 후보 — v1은 그룹 헤더까지.)

### fallback 직접 확인

브라우저 devtools를 **offline**으로 두거나(또는 `iconUrl`을 손상시키면) 아이콘이 **역할색 글리프로 대체**되고 레이아웃이 그대로 유지됩니다 — "미완성"이 아니라 의도된 디자인임을 눈으로 확인할 수 있습니다.

## Vite 프록시 — 왜 백엔드 무변경인가

`vite.config.ts`의 `server.proxy['/api']`가 브라우저의 `/api/*` 요청을 `http://localhost:8080`(Spring 백엔드)로 넘깁니다. 그래서 브라우저는 API를 **같은 출처(same-origin)** 로 인식하고 **CORS 설정이 필요 없으며 백엔드는 0줄도 바뀌지 않습니다**. 타깃을 바꾸려면 추적 파일을 수정하지 말고 `VITE_API_TARGET` 환경 변수로 override 하세요(예시: [`.env.example`](.env.example)).

```bash
VITE_API_TARGET=http://localhost:9090 npm run dev   # 백엔드를 다른 포트로 띄웠을 때
```

## 정직성 노트

- **시각:** 화면 표시는 **KST**, 정렬·계산은 **UTC** 인스턴트 기준입니다(off-by-9h 가드, Phase 7).
- **데이터:** `seed`는 **합성 데이터**(로컬/테스트 전용, 실데이터 아님)이고, `dev`는 **실수집 실데이터**(라이브 데모의 실체)입니다 — 프론트는 출처와 무관하게 같은 백엔드 DTO만 소비합니다. seed가 '합성'임을 숨기지 않습니다.
- **변화율:** event-impact의 변화율은 **상관**이지 인과가 아닙니다(이벤트가 가격을 올렸다고 단정하지 않음). 앵커가 희소/stale하면 `insufficient_data`로 정직하게 표시합니다.
- **정적 서빙(단일 출처 패키징):** Spring `resources/static`으로 묶는 단일 출처 정적 서빙은 **v2 후보**입니다(범위 결정 — 못 해서가 아님). v1 데모 재현은 "seed 백엔드 + `npm run dev`"로 충분합니다.
