import { EVENT_MARKERS } from '@/features/timeline/eventMarkers'
import type { EventType } from '@/lib/schemas'

// Horizontal legend of ALL FOUR eventType swatches + Korean labels (D-01, TIME-04). Always renders
// all four even when the current window contains none of a type — color↔meaning learning stability
// (09-UI-SPEC). Color never carries meaning alone: every swatch is paired with its text label.
export function EventMarkerLegend() {
  const entries = Object.entries(EVENT_MARKERS) as [EventType, { color: string; koLabel: string }][]
  return (
    <ul className="flex flex-wrap items-center gap-4" aria-label="이벤트 마커 범례">
      {entries.map(([type, { color, koLabel }]) => (
        <li key={type} className="flex items-center gap-1 text-sm font-semibold">
          <span
            className="inline-block size-3 rounded-sm"
            style={{ backgroundColor: color }}
            aria-hidden="true"
          />
          {koLabel}
        </li>
      ))}
    </ul>
  )
}
