import { Button } from '@/components/ui/button'

// Controlled range controls (D-04, TIME-05): preset buttons + KST-aware native date inputs.
// Holds NO router/query state — 09-05 wires it to useTimelineParams and Phase 10 reuses it.
// Off-by-9h discipline (Phase-7 D-09) is honored at the INPUT boundary: the user sees/edits a
// KST calendar date while the emitted from/to are UTC instants.

const DAY_MS = 24 * 60 * 60 * 1000
const PRESETS = [7, 30, 90] as const

// KST calendar date (YYYY-MM-DD) of a UTC instant. en-CA yields the YYYY-MM-DD ordering directly.
const kstDateFormatter = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' })
function toKstDate(iso: string): string {
  return kstDateFormatter.format(new Date(iso))
}

function spanDays(from: string, to: string): number {
  return (new Date(to).getTime() - new Date(from).getTime()) / DAY_MS
}

const dateInputClass =
  'h-9 rounded-md border border-input bg-transparent px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]'

export function RangeControls({
  from,
  to,
  onRangeChange,
}: {
  from: string
  to: string
  onRangeChange: (fromIso: string, toIso: string) => void
}) {
  const span = spanDays(from, to)
  const activePreset = PRESETS.find((n) => Math.abs(span - n) <= 1) ?? null

  const applyPreset = (n: number) => {
    const now = Date.now()
    onRangeChange(new Date(now - n * DAY_MS).toISOString(), new Date(now).toISOString())
  }

  // Convert the picked KST calendar date back to a UTC instant at the KST day boundary so the
  // off-by-9h guard holds. A to <= from pick is intentionally NOT blocked — it must reach the
  // request so 09-05 can render the distinct 400 state (ROADMAP 완료조건 #5, D-09).
  const onFromDate = (v: string) => {
    if (!v) return
    onRangeChange(new Date(`${v}T00:00:00+09:00`).toISOString(), to)
  }
  const onToDate = (v: string) => {
    if (!v) return
    onRangeChange(from, new Date(`${v}T23:59:59+09:00`).toISOString())
  }

  return (
    <div className="flex flex-wrap items-end gap-4">
      <div className="flex items-center gap-2" role="group" aria-label="기간 프리셋">
        {PRESETS.map((n) => {
          const active = activePreset === n
          return (
            <Button
              key={n}
              type="button"
              size="sm"
              variant={active ? 'outline' : 'secondary'}
              data-active={active || undefined}
              aria-pressed={active}
              className={active ? 'border-primary text-primary' : undefined}
              onClick={() => applyPreset(n)}
            >
              {n}일
            </Button>
          )
        })}
      </div>
      <div className="flex items-end gap-2">
        <label className="flex flex-col gap-1 text-sm font-semibold">
          시작일
          <input
            type="date"
            value={toKstDate(from)}
            onChange={(e) => onFromDate(e.target.value)}
            className={dateInputClass}
          />
        </label>
        <label className="flex flex-col gap-1 text-sm font-semibold">
          종료일
          <input
            type="date"
            value={toKstDate(to)}
            onChange={(e) => onToDate(e.target.value)}
            className={dateInputClass}
          />
        </label>
      </div>
    </div>
  )
}
