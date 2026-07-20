/*
  거래소 아이템 등급 → 색 유틸 (단일 출처).

  등급 문자열은 API가 `Grade`로 그대로 준다(모험의 서 실측 분포: 일반 40·고급 33·희귀 33·영웅 27·
  전설 7 = 140. 아바타는 영웅·전설만). 색상 값 자체와 "왜 게임 색을 그대로 안 쓰는가"는 index.css의
  --grade-* 토큰 주석에 있다.

  🔑 모르는 등급은 색을 지어내지 않는다. 로스트아크가 새 등급(예: 유물·고대)을 이 카테고리에
  들이면 여기 없는 문자열이 오는데, 그때 아무 색이나 골라 칠하면 **틀린 등급을 자신 있게 표시**하게
  된다. null을 돌려 기본 회색 배지로 떨어뜨리면 라벨은 여전히 정확하고(문자열은 API가 준 그대로)
  색만 없다 — 색은 라벨의 보조지 대체물이 아니다.
*/

/*
  색은 **배지에만** 칠한다(사용자 결정 2026-07-20). 게임은 아이템 이름 자체를 등급색으로 칠하지만,
  그 방식은 검은 배경에 아이템만 떠 있는 화면에서 성립한다. 흰 카드에 이름·등급·최저가·최근가가
  한 줄로 늘어선 우리 행에서는 이름까지 색이 들어가면 행마다 본문 색이 달라져 목록이 산만해진다.
  등급을 말하는 자리는 어차피 배지 하나이므로 색도 거기 모은다 — 이름은 전부 기본 본문색.
*/
const GRADE_BADGE: Record<string, string> = {
  일반: 'bg-grade-normal/10 text-grade-normal border-grade-normal/20',
  고급: 'bg-grade-uncommon/10 text-grade-uncommon border-grade-uncommon/20',
  희귀: 'bg-grade-rare/10 text-grade-rare border-grade-rare/20',
  영웅: 'bg-grade-epic/10 text-grade-epic border-grade-epic/20',
  전설: 'bg-grade-legendary/10 text-grade-legendary border-grade-legendary/20',
}

/** 등급 배지의 색 클래스, 또는 모르는 등급이면 null(호출부가 기본 outline으로 떨어뜨린다). */
export function gradeBadgeClass(grade: string): string | null {
  return GRADE_BADGE[grade] ?? null
}