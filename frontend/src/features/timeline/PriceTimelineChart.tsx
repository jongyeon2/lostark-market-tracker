import {
  CartesianGrid,
  Legend,
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

// The headline visualization (TIME-02/03/04): the selected item's price over time with event markers
// overlaid. Off-by-9h guard (Phase-7 D-09): the x-axis positions on raw UTC epoch ms and only the
// LABEL is KST — the instant is never hand-shifted. Correlation, not causation — the copy never claims
// an event caused a move (Phase 10 carries that explicit notice).
//
// Two DISTINCT metrics are drawn as SEPARATE series, never joined into one line (Phase 17.4 D-02):
//   • 백필 일평균(거래가): the continuous base line (connectNulls) — traded daily average, fills
//     server-off gaps so any window reads continuously (D-04 always-on).
//   • 실측 최저호가: a different metric (real-time min ask, here daily-averaged) overlaid as distinctly
//     colored markers ON TOP. Joining a min-ask to a traded-avg would fake a step at the boundary.

interface ChartRow {
  t: number
  price?: number | null // 실측 최저호가(일별) — undefined on a backfill-only day
  sampleCount?: number | null
  avg?: number | null // 백필 일평균(거래가) — undefined on a snapshot-only instant
}

// Distinct colors so the two metrics are told apart by COLOR (and the backfill legend label), never
// merged. 실측=파랑(호가), 백필=주황(거래 평균).
const MIN_COLOR = '#2563EB'
const BACKFILL_COLOR = '#D97706'

// Compact ko-KR gold for the y-axis ticks (e.g. 1250000 -> '125만').
const compactGold = new Intl.NumberFormat('ko-KR', { notation: 'compact' })

// KST month/day for the x-axis tick. Positions stay on the UTC instant; only the label is KST.
const kstMonthDay = new Intl.DateTimeFormat('en-US', {
  timeZone: 'Asia/Seoul',
  month: 'numeric',
  day: 'numeric',
})

// A backfill statDate ("YYYY-MM-DD", KST) positioned at KST-midnight epoch ms. Consistent with the
// rest of the chart: the position is that KST midnight's true UTC instant; only the axis label is KST.
function kstMidnightEpochMs(statDate: string): number {
  return Date.parse(`${statDate}T00:00:00+09:00`)
}

interface PriceTooltipProps {
  // active/payload are injected by Recharts via cloneElement at render time.
  active?: boolean
  payload?: Array<{ payload?: ChartRow }>
}

// One Tooltip per chart in Recharts, so the price line uses a custom content component (the marker
// hover affordance is a separate SVG <title>, avoiding the single-Tooltip conflict). Shows whichever
// of the two metrics is present at the hovered point, each honestly labeled (D-02).
function PriceTooltip({ active, payload }: PriceTooltipProps) {
  if (!active || !payload || payload.length === 0) return null
  const row = payload[0]?.payload
  if (!row) return null
  return (
    <div className="bg-popover text-popover-foreground rounded-md border px-3 py-2 text-sm shadow-md">
      <p className="text-muted-foreground">{formatKst(row.t)} KST</p>
      {row.price != null && (
        <p className="font-semibold tabular-nums">
          <span className="text-muted-foreground font-normal">최저호가 </span>
          {row.price.toLocaleString('ko-KR')} G{row.sampleCount != null ? ` (${row.sampleCount}개 평균)` : ''}
        </p>
      )}
      {row.avg != null && (
        <p className="font-semibold tabular-nums">
          <span className="text-muted-foreground font-normal">일평균 거래가 </span>
          {Math.round(row.avg).toLocaleString('ko-KR')} G
        </p>
      )}
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
  backfill,
}: Pick<Timeline, 'snapshots' | 'events' | 'downsampled' | 'bucketWidth' | 'backfill'>) {
  // Merge the two series by x into ONE dataset but keep them as SEPARATE dataKeys (price vs avg) so
  // Recharts never joins them into a single line (D-02). Snapshots position on their UTC instant;
  // backfill days position at KST midnight.
  const byT = new Map<number, ChartRow>()
  for (const s of snapshots) {
    const t = toEpochMs(s.collectedAt)
    const row = byT.get(t) ?? { t }
    row.price = s.minPrice
    row.sampleCount = s.sampleCount
    byT.set(t, row)
  }
  for (const b of backfill) {
    const t = kstMidnightEpochMs(b.statDate)
    const row = byT.get(t) ?? { t }
    row.avg = b.avgPrice
    byT.set(t, row)
  }
  const rows: ChartRow[] = [...byT.values()].sort((a, b) => a.t - b.t)

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
        <Tooltip content={<PriceTooltip />} />
        <Legend
          verticalAlign="top"
          height={28}
          formatter={(value: string) => <span className="text-muted-foreground text-sm">{value}</span>}
        />
        {/* 백필 일평균(거래가): the continuous base line. connectNulls bridges server-off days so the
            window reads continuously (D-04 always-on). A distinct color/metric from 실측 (D-02).
            Density-based dots (like the min line, quick 260630-h16): materials backfill 14 detail-Stats
            days → a line, but engravings only get YDayAvgPrice going-forward (detail Stats=0, API limit),
            so early on there is a SINGLE point — a dot-less line would render nothing. Dots keep that
            lone point (and any sparse series) visible; they turn off only for very dense windows. */}
        <Line
          type="monotone"
          dataKey="avg"
          name="백필·일평균(거래가)"
          stroke={BACKFILL_COLOR}
          strokeWidth={2}
          dot={backfill.length > 60 ? false : { r: 2.5, fill: BACKFILL_COLOR }}
          activeDot={{ r: 4 }}
          connectNulls
          isAnimationActive={false}
        />
        {/* 실측 최저호가: a DIFFERENT metric overlaid as distinctly-colored dot markers. Separate dataKey,
            connectNulls=false so it never bridges into the backfill line (no mixing, D-02). Dots are
            density-based (quick 260630-h16) so a sparse/single day stays visible. */}
        <Line
          type="monotone"
          dataKey="price"
          name="실측 최저호가(일별)"
          stroke={MIN_COLOR}
          strokeWidth={1.5}
          dot={rows.length > 60 ? false : { r: 2.5 }}
          activeDot={{ r: 4 }}
          connectNulls={false}
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
