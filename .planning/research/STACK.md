# Stack Research

**Domain:** 게임 마켓 데이터 파이프라인 — 기존 수집·서빙 위에 품목 메타데이터(아이콘/그룹) enrichment 추가
**Researched:** 2026-06-29
**Confidence:** HIGH (기존 스택 재사용) / MEDIUM (Lostark Icon 필드 — Phase 0 스파이크에서 확정)

## Recommended Stack

핵심 결론: **신규 런타임 의존성 추가 없음.** v1.2는 이미 배포된 v1.0(백엔드)·v1.1(프론트) 스택 위 enrichment이며, 필요한 모든 도구가 이미 프로젝트에 존재한다. "무엇을 추가하느냐"보다 "기존 도구를 enrichment 경로에 어떻게 잇느냐"가 본질이다.

### Core Technologies (전부 기존)

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Flyway | 기존 (V1–V3 운영 중) | `tracked_item` enrichment 컬럼(icon_url/item_group/role_group) 추가 | 이미 스키마 소유권을 Flyway가 가짐 → 새 `V4__add_item_enrichment.sql` additive migration 한 장이 정석 |
| Spring Data JPA | 3.4.1 | 신규 컬럼을 엔티티 필드로 매핑 | `ddl-auto=validate`가 이미 Flyway와 1:1 일치 검증 중 |
| zod | 기존 (v1.1 DTO 단일 출처) | 응답 DTO에 `iconUrl`/`itemGroup`/`roleGroup` 옵셔널 필드 추가 | `.parse`-at-boundary가 신규 필드의 타입 안전을 자동 보장 |
| React + `<img>` | React 19 | 아이콘 렌더 + `onError` fallback | 외부 라이브러리 불필요 — 네이티브 `onError`로 충분 |
| Lostark Open API `/markets/items` | v1 | 아이콘 URL·item id·display_name·grade 실측 출처 | 응답에 `Icon`(CDN URL) 필드 포함(스파이크 확정 대상) |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| lucide-react | 기존 (v1.1) | 아이콘 로딩 실패 시 fallback 글리프(예: `Package`/`ScrollText`) | `<img onError>`가 발동했을 때 텍스트 대신 역할 아이콘 표시 |
| Tailwind v4 | 기존 | 고정 크기 아이콘 슬롯·역할 색상 배지 | 레이아웃 시프트 방지(고정 width/height) |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| 로아 개발자 포털 "API 테스트" 콘솔 | Phase 0 스파이크 — `/markets/options`·`/markets/items` 실응답 캡처 | 브라우저에서 키로 직접 호출 → 응답 JSON을 findings 문서에 붙여 모델 잠금 |
| curl + 본인 JWT(로컬 env) | 스파이크 재현·검증 | 키는 셸 env에만, 출력은 마스킹 후 기록 |

## Installation

```bash
# 신규 설치 없음 — 기존 build.gradle / package.json 그대로.
# 유일한 스키마 변경: 신규 Flyway 마이그레이션 파일 한 장
#   src/main/resources/db/migration/V4__add_item_enrichment.sql
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| API `Icon` CDN URL을 DB에 적재(사용자 선택) | 프론트 정적 에셋 번들 | 외부 CDN 비의존(완전 오프라인 데모)이 필수일 때 — 단, 에셋 수급·라이선스·번들 크기 부담 |
| `<img onError>` 텍스트/글리프 fallback | `next/image` 류 최적화 이미지 컴포넌트 | Vite(비 Next) 환경이라 부적합 — 채택 안 함 |
| 신규 `V4` 마이그레이션 | 기존 `V1` 수정 | 절대 금지(아래 PITFALLS) — Flyway 체크섬 깨짐 |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| 프론트에서 Lostark API 직접 호출 | 마일스톤 불변 제약(키 노출·CORS) | 백엔드 DTO `iconUrl` 소비만 |
| 신규 이미지 CDN/스토리지(S3 등) | enrichment 한 겹에 인프라 과중 | Lostark 공개 CDN URL 문자열 저장 |
| 기존 `V1–V3` 마이그레이션 편집 | Flyway 체크섬 불일치 → 부팅 실패 | 신규 `V4` additive only |

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Flyway V4 | JPA `ddl-auto=validate` | 신규 컬럼은 **nullable**로 추가해야 기존 행·검증과 충돌 없음 |
| zod 옵셔널 필드 | TanStack Query 캐시 | `iconUrl?`/`roleGroup?` 옵셔널 → 백엔드 미배포 시에도 프론트 파싱 실패 안 함(점진 배포 안전) |

## Sources

- https://developer-lostark.game.onstove.com/ — 공식 포털(스파이크 1차 출처)
- https://www.inven.co.kr/board/lostark/4821/104361 — 각인서 CategoryCode=40000, ItemGrade="유물" 확인(MEDIUM)
- 기존 프로젝트 `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — 동일 API의 모델 잠금 선례(HIGH)

---
*Stack research for: item enrichment on existing Lostark market pipeline*
*Researched: 2026-06-29*
