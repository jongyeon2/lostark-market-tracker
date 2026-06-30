---
phase: 14
slug: frontend-icons-fallback-docs
status: verified
threats_open: 0
asvs_level: 1
created: 2026-06-30
---

# Phase 14 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Phase 14는 **순수 프론트 소비 한 겹 + docs**(백엔드 `src/` 0줄)다. 신규 네트워크·폼·CTA·
> destructive 액션·외부 입력 0 — 공격 표면이 거의 없다. register는 3개 PLAN의
> `<threat_model>` 블록에서 plan-time에 작성됐고(register_authored_at_plan_time: true),
> 본 검토는 각 완화책의 존재를 실측으로 확인했다.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| 백엔드 DTO(iconUrl 문자열) → 브라우저 `<img src>` | 백엔드가 시드 상수(CDN base 접두)로 채운 URL을 그대로 렌더 — 외부 사용자 입력 아님 | 공개 아이콘 CDN URL (비민감) |
| zod `.parse` boundary | enrichment 필드가 4개 read 스키마에 추가됨 — 미상 roleGroup은 `.parse`에서 throw(loud-fail, D-06) | 공개 분류(itemGroup)·역할군(roleGroup) (비민감) |
| 프론트 → 외부 API | Lostark Open API 직접 호출 금지(상시 가드) — 기존 useItems/useLatestPrice 재사용, 신규 fetch 0 | (없음 — 신규 네트워크 0) |
| 내부 findings/실측 → 공개 README | 스파이크 findings를 요약해 공개 문서에 노출 | 공개 메타데이터만 (키·계정·가격 원문 제외) |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-14-01 | Tampering (XSS) | iconUrl이 비-CDN URL이면 `<img src>` 오남용 | accept | iconUrl은 백엔드 시드 상수·사용자 입력 아님; `<img src>`는 스크립트 실행 불가(href 아님); 깨진/비정상 URL은 onError→역할색 글리프 fallback으로 흡수(UAT Test 5 실측) | closed |
| T-14-02 | Information Disclosure | enrichment 노출로 민감정보 유출 | accept | 노출 값은 공개 아이콘 URL·품목 분류·역할군뿐 — 가격 원문·키·계정 식별자 아님(상시 가드) | closed |
| T-14-03 | Denial of Service | 아이콘 추가로 추가 네트워크/DB 호출 유발 | accept | enrichment는 이미 로드된 useItems()/useLatestPrice 응답에 포함 — 신규 쿼리 0; `<img loading="lazy">` | closed |
| T-14-04 | Tampering | 셀렉터 그룹핑/정렬이 일부 품목 누락(ICON-07 위반) | mitigate | sortByRole 원본 불변 + 전 항목 포함, null role은 '기타' 섹션 흡수 — UAT Test 2에서 큐레이션 15개 누락 0 실측 | closed |
| T-14-05 | Spoofing (UI 혼동) | 역할 배지가 이벤트/상태 배지와 충돌 | mitigate | ICON-05 정체성 영역 1회 한정, EventImpactCards 미터치; solid(역할) vs tinted(상태)/outline(이벤트) 처리 분리(D-04) — UAT Test 4/6에서 한 화면 3처리 공존·구별 실측 | closed |
| T-14-06 | Information Disclosure | README에 실 API 키·계정 식별자·가격 원문 유출 | mitigate | 공개 메타데이터·키 없는 seed 재현만 기재; 시크릿 grep(`bearer`/`LOSTARK_API_KEY=`/JWT) 0건 게이트 — 본 검토에서 README 2개 grep 0건 확인 | closed |
| T-14-07 | Tampering | findings 전재로 문서 비대·드리프트 | accept | 요약 + 상대경로 링크만(D-10) — 단일 출처는 12-SPIKE-FINDINGS.md 유지 | closed |
| T-14-SC | Tampering (Supply Chain) | 의존성 공급망(신규 npm) | accept | 신규 패키지 0 — lucide-react/zod/cva 기존 의존만(D-02·Registry Safety); Phase 14 커밋이 package.json 미변경 확인 | closed |
| T-14-BE | Tampering | 프론트/docs 변경이 백엔드 핵심 경로 오염 | mitigate | Phase 14 커밋 8건이 건드린 파일 = frontend/src/* 11개 + README 2개뿐 — 백엔드 `src/` diff 0줄 확인(git file-scope 게이트) | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-14-01 | T-14-01 | iconUrl은 백엔드 시드 상수(CDN base 접두)이며 사용자 입력 아님. `<img src>`는 스크립트 실행 불가(href 아님)이고, 깨진 URL은 onError→fallback으로 흡수돼 잔여 위험 무시 가능 | jongyeon | 2026-06-30 |
| R-14-02 | T-14-02 | enrichment 노출 값은 공개 아이콘 URL·품목 분류·역할군뿐 — 가격 원문/키/계정 식별자 미포함(상시 가드 준수) | jongyeon | 2026-06-30 |
| R-14-03 | T-14-03 | enrichment는 기존 read 응답에 동봉돼 신규 쿼리 0, 아이콘은 lazy load — 부하 증가 무시 가능 | jongyeon | 2026-06-30 |
| R-14-07 | T-14-07 | 요약+상대경로 링크 정책으로 문서 드리프트 차단, 단일 출처(12-SPIKE-FINDINGS.md) 유지 | jongyeon | 2026-06-30 |
| R-14-SC | T-14-SC | 신규 npm 패키지 0(기존 의존만) — 공급망 신규 노출 없음 | jongyeon | 2026-06-30 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-06-30 | 9 | 9 | 0 | secure-phase 14 (inline orchestrator; short-circuit: threats_open 0 & register_authored_at_plan_time true; 14-UAT 실측 + git/grep 게이트 증거) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-06-30
