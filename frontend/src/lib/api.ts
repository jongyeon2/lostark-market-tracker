import {
  collectionHealthSchema,
  trackedItemsSchema,
  latestPriceSchema,
  timelineSchema,
  eventImpactSchema,
  type CollectionHealth,
  type TrackedItems,
  type LatestPrice,
  type Timeline,
  type EventImpact,
} from '@/lib/schemas'

/*
  Single typed API client (D-12). Every response is validated at the boundary with the
  matching zod schema's .parse (D-06): a non-2xx status OR a schema mismatch THROWS, so
  400 (bad range / window) · 404 (unknown item) · drift surface as React Query errors →
  the shared ErrorState ("loudly fail"), never a silent empty render. Paths are the
  relative proxied /api/... (the 07-01 Vite proxy fronts :8080, same-origin). Endpoints
  are permitAll — no auth header; retry policy lives in React Query, not here.
*/

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly url: string,
  ) {
    super(`API ${status} for ${url}`)
    this.name = 'ApiError'
  }
}

async function request(path: string): Promise<unknown> {
  const res = await fetch(path, { headers: { Accept: 'application/json' } })
  if (!res.ok) {
    // Surface the HTTP status so 400/404 reach the screen's ErrorState, not a silent empty render.
    throw new ApiError(res.status, path)
  }
  return res.json()
}

export async function getCollectionHealth(): Promise<CollectionHealth> {
  return collectionHealthSchema.parse(await request('/api/health/collection'))
}

export async function getItems(): Promise<TrackedItems> {
  return trackedItemsSchema.parse(await request('/api/items'))
}

export async function getLatestPrice(id: number): Promise<LatestPrice> {
  return latestPriceSchema.parse(await request(`/api/items/${id}/latest`))
}

export async function getTimeline(id: number, from: string, to: string): Promise<Timeline> {
  const qs = new URLSearchParams({ from, to }).toString()
  return timelineSchema.parse(await request(`/api/items/${id}/prices?${qs}`))
}

export async function getEventImpact(id: number, window: number): Promise<EventImpact> {
  const qs = new URLSearchParams({ window: String(window) }).toString()
  return eventImpactSchema.parse(await request(`/api/items/${id}/event-impact?${qs}`))
}
