import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { formatKst, toEpochMs } from '@/lib/formatKst'
import type { EventPoint, Timeline } from '@/lib/schemas'
import { EVENT_MARKERS, MARKER_DASH, MARKER_STROKE_WIDTH } from '@/features/timeline/eventMarkers'

// The headline visualization (TIME-02/03/04): the selected item's min_price line with event markers
// overlaid. Off-by-9h guard (Phase-7 D-09): the x-axis positions on raw UTC epoch ms and only the
// LABEL is KST — the instant is never hand-shifted. Honest downsampling (D-08): raw points carry
// dots, bucket averages don't, and a badge/tooltip say so. Correlation, not causation — the copy
// never claims an event caused a move (Phase 10 carries that explicit notice).

interface ChartRow {
  t: number
  price: number
  sampleCount: number | null
}

// Compact ko-KR gold for the y-axis ticks (e.g. 1250000 -> '125만').
const compactGold = new Intl.NumberFormat('ko-KR', { notation: 'compact' })

// KST month/day for the x-axis tick. Positions stay on the UTC instant; only the label is KST.
const kstMonthDay = new Intl.DateTimeFormat('en-US', {
  timeZone: 'Asia/Seoul',
  month: 'numeric',
  day: 'numeric',
})

interface PriceTooltipProps {
  downsampled: boolean
  // active/payload are injected by Recharts via cloneElement at render time.
  active?: boolean
  payload?: Array<{ payload?: ChartRow }>
}

// One Tooltip per chart in Recharts, so the price line uses a custom content component (the marker
// hover affordance in Task 2 is a separate SVG <title>, avoiding the single-Tooltip conflict).
function PriceTooltip({ active, payload, downsampled }: PriceTooltipProps) {
  if (!active || !payload || payload.length === 0) return null
  const row = payload[0]?.payload
  if (!row) return null
  const showAvg = downsampled && row.sampleCount != null
  return (
    <div className="bg-popover text-popover-foreground rounded-md border px-3 py-2 text-sm shadow-md">
      <p className="text-muted-foreground">{formatKst(row.t)} KST</p>
      <p className="font-semibold tabular-nums">
        {row.price.toLocaleString('ko-KR')} G
        {showAvg ? ` (${row.sampleCount}개 평균)` : ''}
      </p>
    </div>
  )
}

// Marker hover affordance (D-02): a transparent SVG hover target spanning the marker height with a
// native <title> child, so the browser shows "{event title} · {KST}" on hover. This sidesteps the
// Recharts single-Tooltip conflict (the price line owns the Tooltip) and keeps the chart label-free.
function MarkerLabel({
  ev,
  viewBox,
}: {
  ev: EventPoint
  // viewBox is injected by Recharts via cloneElement when this element is used as a label.
  viewBox?: { x?: number; y?: number; width?: number; height?: number }
}) {
  if (!viewBox || viewBox.x == null) return null
  const x = viewBox.x
  const y = viewBox.y ?? 0
  const height = viewBox.height ?? 0
  return (
    <g>
      <rect
        x={x - 5}
        y={y}
        width={10}
        height={height}
        fill="transparent"
        style={{ pointerEvents: 'all', cursor: 'help' }}
      >
        <title>{`${ev.title} · ${formatKst(ev.occurredAt)} KST`}</title>
      </rect>
    </g>
  )
}

export function PriceTimelineChart({
  snapshots,
  events,
  downsampled,
}: Pick<Timeline, 'snapshots' | 'events' | 'downsampled' | 'bucketWidth'>) {
  const rows: ChartRow[] = snapshots
    .map((s) => ({ t: toEpochMs(s.collectedAt), price: s.minPrice, sampleCount: s.sampleCount }))
    .sort((a, b) => a.t - b.t)

  return (
    <ResponsiveContainer width="100%" height={360} minHeight={280}>
      <LineChart data={rows} margin={{ top: 16, right: 16, bottom: 8, left: 8 }}>
        <CartesianGrid vertical={false} stroke="#E2E8F0" strokeDasharray="3 3" />
        <XAxis
          type="number"
          scale="time"
          dataKey="t"
          domain={['dataMin', 'dataMax']}
          tickFormatter={(ms: number) => kstMonthDay.format(new Date(ms))}
          tick={{ fontSize: 14, fill: '#64748B' }}
        />
        <YAxis
          width={56}
          tickFormatter={(v: number) => compactGold.format(v)}
          tick={{ fontSize: 14, fill: '#64748B' }}
        />
        <Tooltip content={<PriceTooltip downsampled={downsampled} />} />
        {/* Dots are density-based (D-08 refined for the 일별 view, quick 260630-h16): show them at
            <=60 points even when downsampled, so a sparse daily series — down to a single day — stays
            visible (a dot-less single point renders nothing). The badge still carries the
            'bucket average' honesty signal, so the dual signal is preserved. */}
        <Line
          type="monotone"
          dataKey="price"
          stroke="#2563EB"
          strokeWidth={2}
          dot={rows.length > 60 ? false : { r: 2.5 }}
          activeDot={{ r: 4 }}
          isAnimationActive={false}
        />
        {/* TIME-04: one dashed, eventType-colored vertical marker per window event. Dashed so it is
            distinguishable from the solid price line by FORM, not color alone (accessibility). */}
        {events.map((ev, i) => (
          <ReferenceLine
            key={i}
            x={toEpochMs(ev.occurredAt)}
            stroke={EVENT_MARKERS[ev.eventType].color}
            strokeDasharray={MARKER_DASH}
            strokeWidth={MARKER_STROKE_WIDTH}
            ifOverflow="extendDomain"
            label={<MarkerLabel ev={ev} />}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  )
}
