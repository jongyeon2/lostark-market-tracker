import { CircleCheck, TriangleAlert } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { STATUS_BADGE_META } from '@/features/impact/impactFormat'
import { EVENT_MARKERS } from '@/features/timeline/eventMarkers'
import type { EventImpactStatus, EventType } from '@/lib/schemas'

// ImpactStatusBadge — the D-07 status contract as ONE badge (semantic color + Korean label + lucide
// icon), the same shape as Phase-8 StatusBadge. ok → "비교 가능" (green bg-up/10 text-up),
// insufficient_data → "데이터 부족" (amber bg-warning/10 text-warning). The icon is always paired with
// an explicit text label (never icon-only; color-alone meaning is forbidden).
export function ImpactStatusBadge({ status }: { status: EventImpactStatus }) {
  const meta = STATUS_BADGE_META[status]
  const Icon = status === 'ok' ? CircleCheck : TriangleAlert
  return (
    <Badge variant="secondary" className={cn(meta.className, 'text-sm font-semibold')}>
      <Icon aria-hidden="true" />
      {meta.label}
    </Badge>
  )
}

// EventTypeBadge — reuses the Phase-9 EVENT_MARKERS 4-color tokens (D-07): an outline badge whose
// border + text color is the category hex, paired with the Korean koLabel (color + label, never
// color alone).
export function EventTypeBadge({ eventType }: { eventType: EventType }) {
  const m = EVENT_MARKERS[eventType]
  return (
    <Badge
      variant="outline"
      style={{ borderColor: m.color, color: m.color }}
      className="text-sm font-semibold"
    >
      {m.koLabel}
    </Badge>
  )
}
