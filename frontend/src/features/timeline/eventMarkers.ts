import type { EventType } from '@/lib/schemas'

// Single source of truth for event-marker color/label (D-01, 09-UI-SPEC Event Marker Palette)
// and the downsample bucket-width copy (D-08). Both the chart (09-04) and the legend consume
// this so the meaning→color mapping is locked once, here.

// Keyed by the EventType zod-derived enum so dropping/renaming a member is a compile error —
// the color map and the schema stay in lockstep.
// 색은 dataviz 검증기(scripts/validate_palette.js) 통과 팔레트 — 기존 4색과 합친 7색이 light 모드
// CVD/대비 전 항목 PASS(worst adjacent ΔE 15.1). 마커는 대시 수직선 + 범례 한글 라벨로 색-only
// 의존이 아니다(2차 인코딩).
//
// 다크모드(2026-07-20): hex를 --event-* 토큰으로 올렸다. light 값은 그대로고 다크 값만 추가됐다.
// 여기가 var()를 담아도 소비처는 하나도 안 바뀐다 — 차트는 SVG `stroke`, 배지는 inline
// `style.borderColor/color`, 범례는 inline `style.backgroundColor`인데 셋 다 CSS 변수를 그대로 받는다.
export const EVENT_MARKERS: Record<EventType, { color: string; koLabel: string }> = {
  LOA_ON: { color: 'var(--event-loa-on)', koLabel: '로아ON' },
  MAJOR_UPDATE: { color: 'var(--event-major-update)', koLabel: '대규모 업데이트' },
  SEASON_END: { color: 'var(--event-season-end)', koLabel: '시즌 종료' },
  BALANCE_PATCH: { color: 'var(--event-balance-patch)', koLabel: '밸런스 패치' },
  // v1.5(EVT-01) additive
  NEW_CLASS: { color: 'var(--event-new-class)', koLabel: '신규 캐릭터 출시' },
  NEW_RAID: { color: 'var(--event-new-raid)', koLabel: '신규 레이드 출시' },
  GENERAL_PATCH: { color: 'var(--event-general-patch)', koLabel: '일반 패치' },
}

// Dashed vertical-line style for the ReferenceLine markers (09-UI-SPEC): form-distinct from
// the solid price line so meaning does not depend on color alone (accessibility).
export const MARKER_DASH = '4 4'
export const MARKER_STROKE_WIDTH = 1.5

// The backend returns bucketWidth as "hour" or "day" (verified in DownsampleService.java
// chooseBucketUnit()), NOT the "1h" literal the 09-UI-SPEC copy draft assumed. This helper is
// the one place that reconciles the two so the downsample badge (09-04) reads truthfully.
export function bucketWidthLabel(bucketWidth: string | null): string {
  if (bucketWidth === null) return ''
  if (bucketWidth === 'hour') return '1시간'
  if (bucketWidth === 'day') return '1일'
  return bucketWidth // honest fallback: surface the raw value rather than a wrong literal
}
