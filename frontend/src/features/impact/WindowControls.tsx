import { Button } from '@/components/ui/button'

// Controlled window controls (D-01, IMPCT-01) — mirrors Phase-9 RangeControls (preset buttons +
// native input, holds NO router/query state; 10-04 wires it to useImpactParams). Preset 6/24/72h
// plus a native number input. Deliberately does NOT bound out-of-range values (D-03): 0 / 169 flow
// straight through onWindowChange so the backend 400 → 400 UI can be demonstrated downstream
// (10-04; ROADMAP window=0→400 검증). A preset is active only when `window` equals it exactly.

const PRESETS = [6, 24, 72] as const

// Same input styling as RangeControls' dateInputClass.
const inputClass =
  'h-9 rounded-md border border-input bg-transparent px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]'

export function WindowControls({
  window,
  onWindowChange,
}: {
  window: number
  onWindowChange: (hours: number) => void
}) {
  return (
    <div className="flex flex-wrap items-end gap-4">
      <div className="flex items-center gap-2" role="group" aria-label="윈도우 프리셋">
        {PRESETS.map((n) => {
          const active = window === n
          return (
            <Button
              key={n}
              type="button"
              size="sm"
              variant={active ? 'outline' : 'secondary'}
              data-active={active || undefined}
              aria-pressed={active}
              className={active ? 'border-primary text-primary' : undefined}
              onClick={() => onWindowChange(n)}
            >
              {n}시간
            </Button>
          )
        })}
      </div>
      <label className="flex flex-col gap-1 text-sm font-semibold">
        윈도우(시간)
        <input
          type="number"
          value={window}
          onChange={(e) => {
            const v = Number(e.target.value)
            // No range bounding (D-03): out-of-range integers pass through so the backend 400 stays
            // reachable. Math.trunc only blocks non-integer/garbage — it does not bound the range.
            if (e.target.value !== '' && !Number.isNaN(v)) onWindowChange(Math.trunc(v))
          }}
          className={inputClass}
        />
      </label>
    </div>
  )
}