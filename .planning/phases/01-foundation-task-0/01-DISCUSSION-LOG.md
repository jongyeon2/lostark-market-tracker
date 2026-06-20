# Phase 1: Foundation + Task 0 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-20
**Phase:** 1-Foundation + Task 0
**Areas discussed:** Build & Runtime, Schema Migration, HTTP Client, Task 0 Spike

---

## Build & Runtime

| Option | Description | Selected |
|--------|-------------|----------|
| Gradle (Groovy) + Java 21 + Spring Boot 3.4.x | 모던 Spring 포트폴리오 표준, LTS, 가상 스레드 | ✓ |
| Maven + Java 17 | 더 보수적, 선언적 POM | |

**User's choice:** 추천대로 — Gradle + Java 21 + Spring Boot 3.4.x
**Notes:** "추천대로 전부"로 일괄 수락.

---

## Schema Migration

| Option | Description | Selected |
|--------|-------------|----------|
| Flyway 버전드 SQL + ddl-auto=validate | 스키마를 Flyway가 소유, JPA는 검증만 — 규율 showcase | ✓ |
| Liquibase | XML/YAML 체인지셋 | |
| JPA ddl-auto=update | 자동 생성 — 포트폴리오엔 비추 | |

**User's choice:** 추천대로 — Flyway + ddl-auto=validate
**Notes:** 포트폴리오에서 스키마 규율을 보이는 의도와 일치.

---

## HTTP Client

| Option | Description | Selected |
|--------|-------------|----------|
| RestClient (Spring 6.1+, 동기) | @Async 수집 모델과 단순 정합 | ✓ |
| WebClient (리액티브) | 비동기/논블로킹 — 이 규모엔 오버킬 | |
| RestTemplate | 레거시 | |

**User's choice:** 추천대로 — RestClient
**Notes:** 동기 클라이언트가 @Async 스레드풀 구조와 더 단순하게 맞음.

---

## Task 0 Spike

| Option | Description | Selected |
|--------|-------------|----------|
| `spike` 프로파일 @Disabled 통합 테스트(수동) | 실제 API 1회 호출, CI 미실행, 결과 아티팩트+설계문서 기록 | ✓ |
| CommandLineRunner | 앱 부팅 시 1회 실행 | |
| 일회용 스크립트 | 레포 밖 스크래치 | |

**User's choice:** 추천대로 — @Disabled 통합 테스트(spike 프로파일)
**Notes:** 매칭 규칙 결정과 avg_price/trade_count 제공 여부는 Task 0의 산출물 — 실측 전 핵심 모델 미확정(설계의 Task 0 종료 게이트).

---

## Claude's Discretion

- 패키지/레이어 구조, application.yml/프로파일 세부, 엔티티 매핑 디테일, docker-compose 버전 핀, Flyway 파일 분할.

## Deferred Ideas

None — 논의가 페이즈 스코프 안에 머물렀다.
