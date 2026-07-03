import {
  collectionHealthSchema,
  trackedItemsSchema,
  latestPriceSchema,
  timelineSchema,
  eventImpactSchema,
  gameEventResponseSchema,
  gameEventsSchema,
  type CollectionHealth,
  type TrackedItems,
  type LatestPrice,
  type Timeline,
  type EventImpact,
  type AdminEventRequest,
  type GameEventResponse,
  type GameEvents,
} from '@/lib/schemas'
import { getAdminSecret } from '@/features/admin/auth/adminSecret'

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

/*
  Admin WRITE surface (Phase 15). Unlike the public read `request` above, every admin call carries
  the X-Admin-Secret header — attached HERE from sessionStorage (adminSecret.ts, D-01) so no call
  site handles the header. A 401 anywhere AFTER login means the secret is stale/rotated → the
  registered onAdminUnauthorized handler fires the global auto-logout (D-03) before the error
  propagates. The secret VALUE is never logged or interpolated into a thrown message — ApiError
  carries only status + path.
*/
let onAdminUnauthorized: () => void = () => {}

/** Registered once by AdminAuthProvider so a 401 in adminRequest triggers the global auto-logout (D-03). */
export function setAdminUnauthorizedHandler(handler: () => void): void {
  onAdminUnauthorized = handler
}

type AdminMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export async function adminRequest(
  path: string,
  { method, body }: { method: AdminMethod; body?: unknown },
): Promise<unknown> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'X-Admin-Secret': getAdminSecret() ?? '',
  }
  const init: RequestInit = { method, headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }
  const res = await fetch(path, init)
  if (!res.ok) {
    // Stale/rotated secret: auto-logout BEFORE the error reaches the caller (D-03).
    if (res.status === 401) {
      onAdminUnauthorized()
    }
    throw new ApiError(res.status, path)
  }
  // 204 No Content (DELETE) has no JSON body.
  return res.status === 204 ? undefined : res.json()
}

/*
  Side-effect-free login probe (D-02): a GET that mutates nothing, using the PASSED secret (NOT
  sessionStorage — the caller stores it only on success). 200 ⇒ valid, 401 ⇒ rejected, any other
  non-2xx / network throw ⇒ a connection failure the login form distinguishes from a rejection.
*/
export async function probeAdminSecret(secret: string): Promise<boolean> {
  const res = await fetch('/api/admin/events', {
    headers: { Accept: 'application/json', 'X-Admin-Secret': secret },
  })
  if (res.status === 200) {
    return true
  }
  if (res.status === 401) {
    return false
  }
  throw new ApiError(res.status, '/api/admin/events')
}

// ---- Admin game-event CRUD (Phase 15, ADMINUI-03) ----
// Each fn wraps adminRequest and validates the response with the zod boundary schema (D-05/06 on
// the write path). occurredAt crosses as a UTC ISO '...Z' string (converted in the form, D-08).

export async function getAdminEvents(): Promise<GameEvents> {
  return gameEventsSchema.parse(await adminRequest('/api/admin/events', { method: 'GET' }))
}

export async function createAdminEvent(body: AdminEventRequest): Promise<GameEventResponse> {
  return gameEventResponseSchema.parse(
    await adminRequest('/api/admin/events', { method: 'POST', body }),
  )
}

export async function replaceAdminEvent(
  id: number,
  body: AdminEventRequest,
): Promise<GameEventResponse> {
  return gameEventResponseSchema.parse(
    await adminRequest(`/api/admin/events/${id}`, { method: 'PUT', body }),
  )
}

export async function deleteAdminEvent(id: number): Promise<void> {
  // 204 No Content — adminRequest resolves undefined; nothing to parse.
  await adminRequest(`/api/admin/events/${id}`, { method: 'DELETE' })
}
