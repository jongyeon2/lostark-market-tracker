<!-- GSD:project-start source:PROJECT.md -->
## Project

**로스트아크 거래소 시세 수집·분석 파이프라인 (Lostark Price Tracker)**

레이트리밋이 걸린 로스트아크 거래소(MARKETS) Open API에서 시세를 주기적으로 수집해 시계열로 적재하고, Redis 캐시 계층으로 서빙하며, 관리자가 등록한 주요 게임 이벤트와 가격 변동을 시점 상관시키는 백엔드 데이터 파이프라인이다. 신입 백엔드 개발자의 **설명 가능한 엔지니어링 실력**을 증명하기 위한 포트폴리오 프로젝트로, 도메인은 게임이지만 구조는 금융 마켓 데이터 파이프라인과 동일하다. 대상 청중은 신입 백엔드 채용 면접관과 본인(로아 유저)이다.

**Core Value:** **레이트리밋이 걸린 외부 마켓 API에서 시세를 빠짐없이 수집해 시계열로 쌓고, 캐시로 안정적으로 서빙한다.** 다른 모든 게 실패해도 이 수집·저장·서빙 파이프라인은 동작해야 한다. event-impact(이벤트 상관)는 그 위에 얹는 헤드라인 기능이며, 수집 신뢰성이 흔들리면 상관 분석은 나쁜 데이터 위 장식 수학이 된다.

### Constraints

- **Timeline**: 약 1개월 (단일 개발자)
- **Tech stack**: Java / Spring Boot / JPA / Redis / **PostgreSQL** — MySQL 전용 SQL·타입 미사용
- **DB 환경**: 개발 = docker-compose `postgres` 컨테이너, 테스트 = Testcontainers PostgreSQL (로컬·CI 동일 메커니즘)
- **제외 기술**: MSA · Kafka · Spring Batch (명시적 제외)
- **API**: 로스트아크 개발자 포털 JWT 키(재발급 가능), `Authorization: bearer {token}`, 레이트리밋 키당 분당 100회(정확 수치 Task 0 확인)
- **데이터 소스**: 거래소(MARKETS)만. 경매장/보석은 v2
- **착수 게이트**: Task 0(API 검증 스파이크) 전엔 데이터 모델 미확정 — 특히 avg_price/trade_count 실제 제공 여부
<!-- GSD:project-end -->

<!-- GSD:stack-start source:STACK.md -->
## Technology Stack

Technology stack not yet documented. Will populate after codebase mapping or first phase.
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
