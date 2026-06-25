import { CircleCheck, CircleX, Clock, TriangleAlert, type LucideIcon } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

/*
  StatusBadge — the DASH-02 status contract as ONE badge (semantic color + Korean label + lucide
  icon). Maps the backend's four measured `status` values (실측: PriceCollector.java /
  CollectionHealthResponse.java) to the 08-UI-SPEC Status Badge Mapping (D-05): FAILED is its own
  '위험' grade, never folded into PARTIAL_SUCCESS. Any unknown value degrades to a neutral badge
  whose label is the raw status string — the value is shown, never hidden, never a crash
  (honest-data ethos). Colors are the semantic '연한 배경 + 진한 텍스트' utilities
  (--up/--down/--warning/--neutral in index.css), NOT the blue-600 accent (accent ≠ semantic).
  Purely presentational: the status string is supplied by the caller (08-02 HealthCard).
*/

type StatusMeta = { label: string; icon: LucideIcon; className: string }

const STATUS_MAP: Record<string, StatusMeta> = {
  SUCCESS: { label: '정상', icon: CircleCheck, className: 'bg-up/10 text-up' },
  PARTIAL_SUCCESS: { label: '일부 실패', icon: TriangleAlert, className: 'bg-warning/10 text-warning' },
  FAILED: { label: '전체 실패', icon: CircleX, className: 'bg-down/10 text-down' },
  NO_RUNS: { label: '수집 대기', icon: Clock, className: 'bg-neutral/10 text-neutral' },
}

// Unknown/未知 status → neutral badge that surfaces the raw string (08-UI-SPEC '그 외 미지값').
const UNKNOWN_CLASSNAME = 'bg-neutral/10 text-neutral'

export function StatusBadge({ status }: { status: string }) {
  const meta: StatusMeta =
    STATUS_MAP[status] ?? { label: status, icon: Clock, className: UNKNOWN_CLASSNAME }
  const Icon = meta.icon

  return (
    // `secondary` variant + semantic className: tailwind-merge lets the className win the
    // bg/text utilities, so the semantic color overrides the variant default. Label 14/600
    // typography; the lucide icon is left of an always-present text label (never icon-only).
    <Badge variant="secondary" className={cn(meta.className, 'text-sm font-semibold')}>
      <Icon aria-hidden="true" />
      {meta.label}
    </Badge>
  )
}
