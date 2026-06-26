import { useSearchParams } from 'react-router-dom'

// URL-as-state for the timeline (D-04): ?item=&from=&to= lives in the URL so deep-links and
// screenshots reproduce exactly — the realization of Phase-7 D-04's forward note. All router
// knowledge stays here so RangeControls/ItemSelect stay dumb controlled components reusable by
// Phase 10. from/to are ISO-8601 UTC instants directly consumable by getTimeline (the backend
// PricesController parses them as ISO.DATE_TIME OffsetDateTime).

const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000

export interface TimelineParams {
  itemId: number | null
  from: string
  to: string
  setItem: (id: number) => void
  setRange: (fromIso: string, toIso: string) => void
}

export function useTimelineParams(): TimelineParams {
  const [searchParams, setSearchParams] = useSearchParams()

  const itemRaw = searchParams.get('item')
  const itemNum = itemRaw ? Number(itemRaw) : NaN
  const itemId = Number.isNaN(itemNum) ? null : itemNum

  const fromParam = searchParams.get('from')
  const toParam = searchParams.get('to')

  // D-03: when the URL lacks from OR to, default to the last 30 days. This window contains the
  // seed 8-day window so the chart and its 2 demo markers are never empty on entry. The default
  // is returned but NOT eagerly written — a bare /timeline stays clean; only explicit changes write.
  let from: string
  let to: string
  if (fromParam && toParam) {
    from = fromParam
    to = toParam
  } else {
    const now = Date.now()
    to = new Date(now).toISOString()
    from = new Date(now - THIRTY_DAYS_MS).toISOString()
  }

  // Functional updater merges into existing params so item/from/to stay independently shareable.
  const setItem = (id: number) => {
    setSearchParams((prev) => {
      prev.set('item', String(id))
      return prev
    })
  }

  const setRange = (fromIso: string, toIso: string) => {
    setSearchParams((prev) => {
      prev.set('from', fromIso)
      prev.set('to', toIso)
      return prev
    })
  }

  return { itemId, from, to, setItem, setRange }
}
