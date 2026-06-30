---
quick_id: 260630-lu5
slug: readme-jscode
status: complete
date: 2026-06-30
files_modified:
  - README.md
---

# Quick 260630-lu5: README JSCODE 스타일 재구성 Summary

**루트 README.md를 JSCODE-EDU 포트폴리오 템플릿 방향으로 재구성. "이게 어떤
프로젝트이고 주제가 무엇인지"가 비개발자에게도 읽히도록 친근한 인트로를 상단에
두고, 기존 깊은 엔지니어링 내용(curl/JSON·설계 결정·아키텍처)은 구분선 아래
보존 — 면접관용 깊이와 일반 독자용 접근성 둘 다 확보. 코드 0줄(docs 전용).**

## 배경

기존 README는 한 줄 소개 직후 곧장 아키텍처·설계 트레이드오프·curl/JSON으로
들어가 "거의 논문"에 가까웠고, 비개발자 기준 프로젝트 주제·동기 서술이 약했다.
사용자가 JSCODE-EDU(github.com/JSCODE-EDU/README) 템플릿을 참고 방향으로 지정.

## 결정 (AskUserQuestion 확정)

1. **구성 방향 = 인트로 강화 + 깊이 유지** — 가벼운 전환이 아니라, 친근한 소개
   층을 추가하되 기존 엔지니어링 깊이는 하단에 보존(이 프로젝트의 목적이 신입
   백엔드 면접관에게 엔지니어링 실력 증명이라 깊은 내용도 자산).
2. **작성자 = jongyeon / [@jongyeon2](https://github.com/jongyeon2) / 저장소
   lostark-market-tracker** (기존 CI 뱃지 기준).
3. **기간 = 2026.06 ~ 2026.07 (~1개월, 단일 개발자).**
4. **배포 = 라이브 URL 없음** — seed 모드 로컬 실행 데모 + 3화면 스크린샷만
   (실제 코드 상태=단일 인스턴스·HA v2와 일치, 과대 표기 금지).

## 수정

**README.md 단일 파일 재구성:**

- **상단(JSCODE 순서·친근):** 제목+이모지 → 뱃지(CI·Java 21·Spring Boot 3.4·
  PostgreSQL 16·Redis 7·React 19) → 한 줄 소개(blockquote) → 대표 이미지
  (dashboard) → 📖 프로젝트 소개(주제·동기·왜 만들었나) → 🎯 핵심 가치(상관≠
  인과 포함) → ⭐ 주요 기능(표 6행) → 🖼 데모(3화면) → 🔧 기술 스택(backend+
  frontend) → 💻 실행 방법(3단계 재현 + dev/seed + 프론트 데모).
- **구분선 후 하단(엔지니어링 깊이·보존):** 🏗 아키텍처(mermaid + data flow) →
  🧠 설계 결정 & 트레이드오프 → 🎨 시각 enrichment(아이콘·역할 배지·fallback)
  → 📚 API 데모(curl + JSON 4개 엔드포인트 + 표) → 🗂 프로젝트 구조(신규 트리)
  → ⚙️ 한계/스코프 · 데이터 보존 · 기술 스택&문서 링크 → 👤 Developer & 정보.
- **신규 추가:** 프로젝트 구조 트리(`com.lostark.tracker` 패키지 + frontend +
  .planning), Developer/기간 섹션, 스택 뱃지 5종, 주요 기능 표.

## 검증

- 헤딩 구조: 친근 인트로 5섹션(소개/기능/데모/스택/실행) 상단 + 깊이 8섹션 하단.
- 보존 확인: curl 블록 5개(3단계 1 + API 데모 4), 스크린샷 경로 3개(dashboard/
  item-timeline/event-impact), mermaid 1개 — 전부 유지.
- 코드 가드: `git status src/ frontend/src/` 0줄(docs 전용).

## 결과

루트 README가 포트폴리오 진입점으로서 "무엇을·왜"를 먼저 보여주고, "어떻게"를
하단 깊이로 보존. 일반 독자·면접관 모두에게 읽히는 구조. 사실/엔드포인트/
스크린샷/설계 결정 무손실, 코드 무변경.
