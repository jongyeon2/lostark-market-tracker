# 데모 스크린샷 — 캡처 프로토콜

이 폴더는 루트 `README.md`와 `frontend/README.md`가 **상대경로로 참조**하는 데모 스크린샷 3장을 담는다(D-03/D-04). 외부 링크/CDN이 아니라 레포에 커밋된 PNG이므로 GitHub에서 바로 렌더되고 클론·오프라인에서도 재현된다.

> 다운스트림 에이전트는 브라우저 바이너리 캡처를 할 수 없으므로(D-02), 이 문서는 **슬롯·확정 파일명·alt 텍스트·재현 딥링크·캡처 전제**까지만 마련한다. 실제 PNG 3장은 아래 프로토콜대로 **사용자가 직접 캡처**해 채운다.

## 전제 (캡처 전 기동)

1. **seed 프로파일로 백엔드 기동** — `SPRING_PROFILES_ACTIVE=seed`로 실행한다(루트 `README.md`의 seed 기동 명령 재사용). seed = 합성 8일치 시세 + 데모 이벤트라 **API 키 불필요**.
2. **프론트 dev 서버 기동** — `cd frontend && npm install && npm run dev` → Vite dev 서버(기본 `http://localhost:5173`). `/api/*`는 Vite 프록시가 `:8080` 백엔드로 넘긴다.

> ⚠️ **honest-data:** 이 스크린샷이 담는 데이터는 **seed 합성 데이터**이며 실데이터가 아니다. 변화율은 상관이지 인과가 아니다.

## 캡처 대상 (3장 — 화면당 1장, D-04)

`<품목ID>`는 `GET /api/items`(또는 화면 셀렉터)에서 "가장 잘 나온" seed 품목 하나를 골라 채운다. 라우트 prefix·쿼리키는 `useTimelineParams`/`useImpactParams` 규약과 일치시킨다(`?item=`, `?window=`). Timeline은 `from`/`to`를 생략하면 자동으로 최근 30일(seed 8일 윈도우 포함)이 적용돼 비어있지 않게 뜬다.

| 파일명 | 화면 | 재현 딥링크 | alt 텍스트 |
|--------|------|-------------|------------|
| `dashboard.png` | Dashboard | `http://localhost:5173/` | `Dashboard 화면 — 수집 파이프라인 헬스 카드와 활성 품목 최신가 워치리스트` |
| `item-timeline.png` | Item Timeline | `http://localhost:5173/timeline?item=<품목ID>` | `Item Timeline 화면 — 시세 라인 차트와 게임 이벤트 세로 마커` |
| `event-impact.png` | Event Impact | `http://localhost:5173/impact?item=<품목ID>&window=24` | `Event Impact 화면 — 이벤트 전후 변화율 결과 표와 상관≠인과 안내` |

## 뷰포트 가이드

- **데스크톱 폭 권장(~1440px)** — 면접 데모 기준(D-08 데스크톱 우선). 노트북/모니터 가로폭.
- 브라우저 **100% 배율**(확대/축소 없음).
- **화면당 1장**(D-04). 상태 변형(insufficient_data·다운샘플 등) 추가 캡처는 최신화 부담 대비 ROI가 낮아 3장으로 한정.

## 커밋 안내

1. 위 3개 딥링크에서 각 1장씩 캡처해 **확정 파일명**(`dashboard.png` / `item-timeline.png` / `event-impact.png`)으로 이 폴더(`frontend/docs/screenshots/`)에 저장한다.
2. PNG 3장을 커밋한다 — `docs/`는 `.gitignore` 대상이 아니므로(추적 경로) 그대로 추적된다.
3. **외부 링크/CDN 사용 금지** — 링크가 깨지면 데모 표면이 손상된다. 반드시 레포 상대경로 PNG만 사용.
4. 캡처 시 화면에 실제 시크릿(API 키·Admin secret)이 보이지 않는지 확인한다(seed 화면은 공개 read 데이터만 표시).
