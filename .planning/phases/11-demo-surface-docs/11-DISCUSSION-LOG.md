# Phase 11: Demo Surface + Docs - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-27
**Phase:** 11-demo-surface-docs
**Areas discussed:** 정적 서빙 채택 여부(DEMO-03), 스크린샷 캡처·저장·커밋, README 분업 & 깊이, 시각적 마감 범위

---

## 정적 서빙 채택 여부 (DEMO-03)

| Option | Description | Selected |
|--------|-------------|----------|
| v2로 미룸 (권장) | Phase 11은 문서+스크린샷+마감에 집중. 재현은 seed+`npm run dev`로 충분. 정적 서빙은 백엔드 변경·SPA fallback·Docker 빌드 동반 → v1.1 범위 초과 | ✓ |
| 이번에 포함 | docker compose up 한 번 단일 포트 데모. Vite base·SPA fallback·Dockerfile 빌드 스테이지 배선, 백엔드 설정 변경 감수 | |
| 결정 보류 — 더 논의 | 장단점을 더 파고든 뒤 결정 | |

**User's choice:** v2로 미룸 (권장)
**Notes:** ROADMAP이 명시적으로 사용자 최종 결정을 요구한 헤드라인 항목. REQUIREMENTS도 DEMO-03을 "선택/stretch, 슬립 가능"으로 표시. → CONTEXT D-01. Phase 11 완료조건 5(정적 서빙)는 의도된 미충족으로 처리.

---

## 스크린샷 — 캡처 방식

| Option | Description | Selected |
|--------|-------------|----------|
| 수동 캡처 (권장) | 개발자가 seed 백엔드+dev 띄우고 브라우저에서 직접 캡처. 자동화 인프라는 한 회성 산출물에 과잉 | ✓ |
| Playwright 자동 캡처 | Playwright MCP로 뷰포트 고정·일관 상태 자동 캡처. 재현 용이하나 기동·뷰포트·스크립트 배선 필요 | |

**User's choice:** 수동 캡처 (권장)
**Notes:** → CONTEXT D-02. plan/실행은 README 슬롯·파일명만 마련, 실제 PNG 삽입은 사용자.

---

## 스크린샷 — 저장·커밋

| Option | Description | Selected |
|--------|-------------|----------|
| frontend/docs/screenshots/ 커밋 (권장) | PNG를 레포에 커밋하고 README가 상대경로 참조. GitHub·클론에서 바로 렌더·오프라인 재현 | ✓ |
| 외부 링크/CDN | imgur 등 외부 호스팅 URL. 레포 용량은 적지만 링크 깨짐 시 데모 표면 손상 | |

**User's choice:** frontend/docs/screenshots/ 커밋 (권장)
**Notes:** → CONTEXT D-03. dist gitignore와 별개 경로.

---

## 스크린샷 — 범위

| Option | Description | Selected |
|--------|-------------|----------|
| 핵심 3화면 각 1장 (권장) | Dashboard·Timeline·Impact 각 1장씩 총 3장. "5분 재현" 증명에 충분, 최신화 부담 최소 | ✓ |
| 3화면 + 핵심 상태 변형 | insufficient_data·다운샘플 등 honest-data 상태 1~2장 추가(총 4~5장). 정직성 강조 강하나 최신화 부담 증가 | |

**User's choice:** 핵심 3화면 각 1장 (권장)
**Notes:** → CONTEXT D-04.

---

## README 분업

| Option | Description | Selected |
|--------|-------------|----------|
| 루트=포인터, frontend=상세 (권장) | 루트에 짧은 프론트 데모 섹션(실행 요약+대표 스크린샷+링크), 상세는 frontend/README 일원화. 중복 최소·단일 진실 원천. 기존 curl 섹션 유지 | ✓ |
| 두 README 각자 완결 | 루트도 프론트 실행·스크린샷 자체 완결 + frontend/README도 독립 완결. 실행 순서·스크린샷이 양쪽 중복되어 최신화 지점 둘 | |

**User's choice:** 루트=포인터, frontend=상세 (권장)
**Notes:** → CONTEXT D-05. 기존 curl/샘플 JSON 섹션 회귀 금지.

---

## frontend/README 깊이

| Option | Description | Selected |
|--------|-------------|----------|
| 재현 핵심 + 프록시 설명 (권장) | 사전조건(SPRING_PROFILES_ACTIVE=seed) → npm install && npm run dev → 3화면+스크린샷+Vite 프록시 동작 1문단. 5분 재현에 필요한 것만 간결 | ✓ |
| 상세 + 트러블슈팅 | 위 + 트러블슈팅(백엔드 꺼짐·VITE_API_TARGET·포트 충돌) + npm run build 안내. 친절하나 길어지고 최신화 부담 | |

**User's choice:** 재현 핵심 + 프록시 설명 (권장)
**Notes:** → CONTEXT D-06. build 강조는 정적 서빙 v2이므로 불요.

---

## 시각적 마감 — 성격·깊이

| Option | Description | Selected |
|--------|-------------|----------|
| 점검·정렬 패스 (권장) | 3화면 순회하며 로딩/빈/에러·간격·정렬 불일치를 찾아 고치는 audit-and-fix. 공용 컴포넌트 기구축 → 일관성 회귀 수정. 명백한 회귀만, 리디자인 없음 | ✓ |
| 6-pillar UI 감사 동반 | /gsd-ui-review 스타일 6기둥 체계 감사+점수화 후 수정. 철저하나 범위 커지고 문서보다 리디자인 무게 | |

**User's choice:** 점검·정렬 패스 (권장)
**Notes:** → CONTEXT D-07.

---

## 시각적 마감 — 반응형 목표 폭

| Option | Description | Selected |
|--------|-------------|----------|
| 데스크톱 우선 + 좁은 화면 깨짐 없음 (권장) | 면접 데모는 노트북/모니터 기준. 데스크톱 마감 + 좁은 폭(태블릿~좌우분할) 깨지지 않음. Phase 10 표↔카드가 이미 그 선 | ✓ |
| 모바일까지 완성 | 모바일 브레이크포인트(네비·차트·표)까지 의도 마감. 완성도 높으나 Recharts·네비 모바일 대응 부담·ROI 낮음 | |

**User's choice:** 데스크톱 우선 + 좁은 화면 깨짐 없음 (권장)
**Notes:** → CONTEXT D-08.

---

## Claude's Discretion

- 스크린샷 정확한 파일명·alt 텍스트·README 내 배치 위치(슬롯만 마련; 실제 삽입은 사용자).
- frontend/README 정확한 섹션 순서·헤딩·프록시 설명 문구 톤.
- 루트 README "프론트 데모" 섹션의 정확한 삽입 위치(curl 섹션 회귀 없이).
- 시각적 마감 패스에서 실제로 수정할 불일치 항목 선정.
- DEMO-01 재현 검증을 클린 클론 vs 별도 디렉터리에서 할지(verification 세부).

## Deferred Ideas

- DEMO-03 정적 서빙(Spring resources/static 단일 출처) — v2(D-01).
- 스크린샷 자동 캡처(Playwright) + 최신화 게이트 — v2(D-02).
- 모바일 완성형 반응형 — v2(D-08).
- 6-pillar UI 리디자인 감사 — v2(D-07).
- 실배포(Railway/Fly/Render) + 프론트 CI 게이트 — FE-V2-04.
