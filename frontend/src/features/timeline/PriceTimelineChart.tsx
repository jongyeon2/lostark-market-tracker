import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { formatKst, toEpochMs } from '@/lib/formatKst'
import type { Timeline } from '@/lib/schemas'

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

export function PriceTimelineChart({
  snapshots,
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
        {/* downsample dual signal (D-08): no dots for bucket averages; dots for raw (<=60 pts). */}
        <Line
          type="monotone"
          dataKey="price"
          stroke="#2563EB"
          strokeWidth={2}
          dot={downsampled ? false : rows.length > 60 ? false : { r: 2.5 }}
          activeDot={{ r: 4 }}
          isAnimationActive={false}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
