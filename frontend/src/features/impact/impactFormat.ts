import type { EventImpactItem, EventImpactStatus } from '@/lib/schemas'

// Pure domain formatters for the event-impact screen — the SINGLE source of truth for changeRate
// formatting, direction color, price formatting, status-badge meta, and the 희소/stale derivation.
// 10-03 (table/cards) consumes these so the table and cards can never drift.

// The backend `changeRate` is a RATIO (post/pre - 1, 4-dp e.g. 0.1230 — measured in
// EventImpactService.java), NOT the percent the screen shows. So this is the ONE place that ×100s
// it: forgetting this turns +12.3% into a silent +0.1% data distortion. Output: sign + percent +
// 1 decimal (0.123 → +12.3%, -0.04 → -4.0%, 0 → 0.0%). -0.0% can never occur (the === 0 branch
// catches -0 before toFixed runs).
export function formatChangeRate(rate: number): string {
  const pct = Math.round(rate * 1000) / 10
  if (pct === 0) return '0.0%'
  if (pct > 0) return `+${pct.toFixed(1)}%`
  return `${pct.toFixed(1)}%` // toFixed keeps the leading '-' for negatives
}

// changeRate direction color — KOREAN market convention (D-05): up = red, down = blue.
// We deliberately do NOT use index.css's --up (green-600) / --down (red-600) tokens: those encode
// a DIFFERENT meaning axis (generic price up/down) and are the exact OPPOSITE of this screen, where
// the domain audience (로아 유저 · 국내 면접관) reads the 국내 거래소/MTS convention (상승=빨강 · 하락=파랑).
// Down uses blue-700 (#1D4ED8), intentionally distinct from the blue-600 accent (#2563EB): the
// accent is reserved for structure (buttons/focus/selection), this blue is inline number text only.
export function changeRateColorClass(rate: number): string {
  if (rate > 0) return 'text-[#DC2626]' // 상승 = 빨강 (red-600)
  if (rate < 0) return 'text-[#1D4ED8]' // 하락 = 파랑 (blue-700)
  return 'text-[#64748B]' // 보합 = 중립 (slate-500)
}

// Thousands-separated price (D-06) — reuses the LatestPriceCard convention.
export function formatPrice(n: number): string {
  return `${n.toLocaleString('ko-KR')} G`
}

// status → label + semantic color (D-07). ok = green (비교 가능), insufficient_data = amber
// (데이터 부족) — the direct extension of Phase-7's insufficient_data → warning semantic color.
// Always paired with a text label by the badge component (color-alone meaning is forbidden).
export const STATUS_BADGE_META: Record<EventImpactStatus, { label: string; className: string }> = {
  ok: { label: '비교 가능', className: 'bg-up/10 text-up' },
  insufficient_data: { label: '데이터 부족', className: 'bg-warning/10 text-warning' },
}

/*
  The lens an ok row was measured through (Phase 25), or null when nothing needs saying.
  SNAPSHOT_MIN is the default meaning of every number on this page (10분 최저 호가), so labeling it
  would be noise; DAILY_AVG is a different measurement (하루 전체의 체결 평균) reported at day
  resolution, and showing it unlabeled beside snapshot rows would imply the two are the same thing.
*/
export function anchorSourceLabel(item: EventImpactItem): string | null {
  return item.anchorSource === 'DAILY_AVG' ? '일별 평균 기준' : null
}

// Derive 희소(sparse) vs stale from ANCHOR NULLNESS ALONE (D-08) — the 30-minute staleness
// threshold is intentionally NOT referenced here, to avoid coupling the frontend to a backend
// constant (the most honest stance). This matches EventImpactService.toImpactItem: a side with 0
// snapshots in the window yields a null anchor (= 희소); both anchors present yet still
// insufficient means the backend judged them too far from the event (= stale).
export function insufficientReason(
  item: EventImpactItem,
): { kind: 'sparse' | 'stale'; inlineLabel: string; body: string } | null {
  if (item.status !== 'insufficient_data') return null
  if (item.preAnchorAt == null || item.postAnchorAt == null) {
    return {
      kind: 'sparse',
      inlineLabel: '데이터 부족 · 희소',
      body: '이벤트 전후 윈도우에 수집된 시세가 없어 비교 기준을 잡지 못했어요.',
    }
  }
  return {
    kind: 'stale',
    inlineLabel: '데이터 부족 · 오래됨',
    body: '기준 시각이 이벤트에서 너무 떨어져 비교를 신뢰할 수 없어요.',
  }
}
