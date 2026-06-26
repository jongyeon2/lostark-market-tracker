import { useSearchParams } from 'react-router-dom'

// URL-as-state for the event-impact screen — mirrors useTimelineParams (NOT a copy): ?item=&window=
// lives in the URL as the single source of truth so deep-links and screenshots reproduce exactly
// (D-01, the continuation of Phase-7 D-04 / Phase-9 D-04). All router knowledge stays here so
// WindowControls/ItemSelect remain dumb controlled components. `item` is shared with the timeline
// screen; this hook adds one integer `window`.

const DEFAULT_WINDOW = 24

export interface ImpactParams {
  itemId: number | null
  window: number
  setItem: (id: number) => void
  setWindow: (hours: number) => void
}

export function useImpactParams(): ImpactParams {
  const [searchParams, setSearchParams] = useSearchParams()

  const itemRaw = searchParams.get('item')
  const itemNum = itemRaw ? Number(itemRaw) : NaN
  const itemId = Number.isNaN(itemNum) ? null : itemNum

  // D-02: when the URL lacks a valid ?window=, default to 24h — but RETURN it only, never eagerly
  // write it. A bare /impact stays clean; only an explicit change writes to the URL (same policy as
  // Phase-9 D-03). A non-numeric ?window= also falls back to 24. NOTE: a positive integer is parsed
  // as-is — out-of-range values (≤0 / >168) are intentionally NOT clamped (D-03) so 10-04 can
  // demonstrate the backend 400 UI; setWindow likewise records them verbatim.
  const windowRaw = searchParams.get('window')
  const windowNum = windowRaw !== null ? Number(windowRaw) : NaN
  const window = Number.isInteger(windowNum) ? windowNum : DEFAULT_WINDOW

  // Functional updater merges into existing params so item/window stay independently shareable.
  const setItem = (id: number) => {
    setSearchParams((prev) => {
      prev.set('item', String(id))
      return prev
    })
  }

  // No client-side clamp (D-03): 0 / 169 are written verbatim so the backend 400 can be exercised.
  const setWindow = (hours: number) => {
    setSearchParams((prev) => {
      prev.set('window', String(hours))
      return prev
    })
  }

  return { itemId, window, setItem, setWindow }
}
