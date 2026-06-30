import type { PricePoint } from '@/lib/schemas'

// KST calendar date (YYYY-MM-DD) of a UTC instant — en-CA yields the YYYY-MM-DD ordering directly.
// The day a snapshot belongs to is its KST date (NOT its UTC date), so buckets line up with the
// Korean game day — the off-by-9h discipline (Phase-7 D-09), same convention as RangeControls.
const kstDate = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' })

interface DayAccumulator {
  weightedSum: number
  weight: number
}

/**
 * Collapse an item's window snapshots into ONE point per KST calendar day — the timeline's
 * "일별 평균 최저가" view (quick 260630-h16). Each day's value is the weighted average of that day's
 * min_price points (weight = sampleCount ?? 1) so it behaves uniformly whether the backend returned
 * raw 10-min points (sampleCount null → weight 1) or already-bucketed averages (sampleCount = N).
 *
 * The bucket's collectedAt is KST-midnight of the day re-expressed as a UTC instant, honoring the
 * off-by-9h guard (the chart positions on the UTC instant; only the label is KST). The carried
 * sampleCount is the total points behind the day, feeding the chart's "N개 평균" tooltip. Events are
 * NOT aggregated here — markers stay on their own instants. Empty input → empty output.
 */
export function aggregateDailyAverage(snapshots: PricePoint[]): PricePoint[] {
  const byDay = new Map<string, DayAccumulator>()
  for (const s of snapshots) {
    const day = kstDate.format(new Date(s.collectedAt))
    const weight = s.sampleCount ?? 1
    const acc = byDay.get(day) ?? { weightedSum: 0, weight: 0 }
    acc.weightedSum += s.minPrice * weight
    acc.weight += weight
    byDay.set(day, acc)
  }

  return [...byDay.entries()]
    .map(([day, acc]) => ({
      // KST midnight of the day as a UTC instant — same boundary convention as RangeControls.
      collectedAt: new Date(`${day}T00:00:00+09:00`).toISOString(),
      minPrice: Math.round(acc.weightedSum / acc.weight),
      sampleCount: acc.weight,
    }))
    // ISO-8601 'Z' strings sort lexicographically == chronologically (ascending for the chart).
    .sort((a, b) => a.collectedAt.localeCompare(b.collectedAt))
}
