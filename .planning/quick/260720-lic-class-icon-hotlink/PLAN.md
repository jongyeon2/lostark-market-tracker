---
quick_id: 260720-lic
slug: class-icon-hotlink
description: 저장소 public 전환에 앞서 번들된 공식 직업 아이콘 30개를 CDN 핫링크로 전환
date: 2026-07-20
status: complete
---

# Quick Task 260720-lic — 직업 아이콘 번들 → CDN 핫링크

## 왜 지금인가 — 저장소가 public이 되기 때문

`260720-fn3`에서 공식 직업 SVG 30개를 `frontend/public/class-icons/`에 **번들**했다. 그때의 근거는
지금도 유효하다: 이 URL들은 API가 준 값이 아니라 사이트 내부 자산(경로에 `2018/obt`)이라 예고 없이
옮겨질 수 있다.

바뀐 건 **전제**다. 오픈 API 이용약관 「지식재산권」:

> The Company owns and retains all rights, including all intellectual property rights, to all content
>
> **Storing any Content ... or providing such stored materials to others, constitutes a violation**
>
> The Company may **restrict the violating User's authorization to access the Website or the API Service**

private 저장소에서는 '보관'이었지만 public은 **'보관된 자료를 타인에게 제공'**이 된다. 게다가 5개는
`fill`을 고쳤으니 '수정'까지 겹친다.

🔑 **제재가 소송이 아니라 API 접근 권한 제한이라는 점이 결정적이다.** 링크가 깨지면 아이콘 자리만
비지만(폴백 있음), 키가 정지되면 loaket.kr이 통째로 멈춘다. 트레이드오프가 뒤집혔다.

## 사전 검증 (실측 2026-07-20)

| 확인 | 결과 |
|---|---|
| `cdn-lostark.game.onstove.com/2018/obt/assets/images/common/class/{slug}.svg` | 30/30 **200** |
| CSP `img-src` | **이미 허용된 호스트** (아이템 아이콘과 동일) — 정책 변경 0 |
| `ClassIcon` 로드 실패 폴백 | **이미 존재** (`onError` → 24px 자리 유지) |
| 원본 `fill` | 25개 `#222222` / 5개 `white` (로컬에서 고쳤던 그 5개와 정확히 일치) |

## 작업

1. `classIconUrl()` → CDN 절대 URL. 번들 시절 근거와 뒤집은 이유를 주석으로 남긴다.
2. `frontend/public/class-icons/` 30개 삭제.
3. 흰색 5개 문제를 **파일 수정 대신 CSS로** 해결 — `brightness-0 dark:invert`.
   단색 아이콘이라 원본 색과 무관하게 라이트=검정 / 다크=흰색으로 통일된다.

## 하지 않은 것

- **CSP는 넓히지 않는다.** 필요가 없었다(호스트가 이미 허용 목록에 있다). 필요했다면 이 작업의
  전제부터 다시 봤을 것이다.
- 아이템 아이콘 경로는 손대지 않는다 — 원래부터 API가 준 `iconUrl` 핫링크라 문제가 없다.
