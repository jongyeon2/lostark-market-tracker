import { Gauge, ShieldAlert, type LucideIcon } from 'lucide-react'

import { cn } from '@/lib/utils'

/*
  SummaryMarker — the optional summaryMessage diagnostic marker (D-06). Renders a FIXED categorical
  Korean label for each known category and NOTHING otherwise. It never echoes the raw summaryMessage
  value to the DOM: the backend response carries no secret field by design (DASH-02), and this maps
  only the known categories, so no token/stack/secret has a path to the screen. AUTH_ERROR shows in
  down/red, RATE_LIMITED in warning/amber; null (or any unmapped value) returns null.
  Purely presentational: the value is supplied by the caller (08-02 HealthCard).
*/

type MarkerMeta = { label: string; icon: LucideIcon; className: string }

const MARKER_MAP: Record<string, MarkerMeta> = {
  AUTH_ERROR: { label: '인증 오류', icon: ShieldAlert, className: 'text-down' },
  RATE_LIMITED: { label: '레이트리밋', icon: Gauge, className: 'text-warning' },
}

export function SummaryMarker({ summaryMessage }: { summaryMessage: string | null }) {
  // null OR any unmapped value → render nothing. The raw value is never echoed (no secret-leak path).
  const meta = summaryMessage === null ? undefined : MARKER_MAP[summaryMessage]
  if (!meta) return null
  const Icon = meta.icon

  return (
    // Label 14/600 line, icon-left with a text label always present (never color-only / icon-only).
    <p className={cn('flex items-center gap-1 text-sm font-semibold', meta.className)}>
      <Icon aria-hidden="true" className="size-4" />
      {meta.label}
    </p>
  )
}
