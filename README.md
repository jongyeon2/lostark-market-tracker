# Lostark Market Tracker

Lostark Open API 기반 시장 가격 추적 백엔드 프로젝트입니다.

## Goal

- Lostark 시장/거래소 가격 데이터 수집
- 10분 단위 price snapshot 저장
- PostgreSQL 기반 시계열 조회
- Redis cache-aside 및 rate limit 전략 적용
- 수동 game event와 가격 타임라인 상관 분석

## Current Status

- gstack design doc approved
- engineering review cleared
- outside review reflectedg
- implementation starts from Task 0: Lostark API validation spike

## Documents

- `docs/design/gstack-approved-design.md`
- `docs/reviews/gstack-eng-review-test-plan.md`