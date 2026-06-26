import type { EventType } from '@/lib/schemas'

// Single source of truth for event-marker color/label (D-01, 09-UI-SPEC Event Marker Palette)
// and the downsample bucket-width copy (D-08). Both the chart (09-04) and the legend consume
// this so the meaning→color mapping is locked once, here.

// Keyed by the EventType zod-derived enum so dropping/renaming a member is a compile error —
// the color map and the schema stay in lockstep.
export const EVENT_MARKERS: Record<EventType, { color: string; koLabel: string }> = {
  LOA_ON: { color: '#7C3AED', koLabel: '로아ON' },
  MAJOR_UPDATE: { color: '#EA580C', koLabel: '대규모 업데이트' },
  SEASON_END: { color: '#DB2777', koLabel: '시즌 종료' },
  BALANCE_PATCH: { color: '#0D9488', koLabel: '밸런스 패치' },
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
