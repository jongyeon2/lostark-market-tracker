import {
  collectionHealthSchema,
  trackedItemSchema,
  trackedItemsSchema,
  latestPriceSchema,
  timelineSchema,
  eventImpactSchema,
  gameEventResponseSchema,
  gameEventsSchema,
  newsResponseSchema,
  couponSchema,
  couponsSchema,
  gemsResponseSchema,
  marketSearchResponseSchema,
  marketClassesSchema,
  type MarketSearchResponse,
  type MarketClasses,
  type CollectionHealth,
  type TrackedItem,
  type TrackedItems,
  type LatestPrice,
  type Timeline,
  type EventImpact,
  type AdminEventRequest,
  type AdminItemRequest,
  type GameEventResponse,
  type GameEvents,
  type NewsResponse,
  type Coupon,
  type Coupons,
  type AdminCouponRequest,
  type GemsResponse,
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

// GET /api/news (17.2) — public read; the frontend never calls Lostark directly (D-06). Same zod
// boundary as the other reads: a non-2xx or schema mismatch throws → the panel's ErrorState.
export async function getNews(): Promise<NewsResponse> {
  return newsResponseSchema.parse(await request('/api/news'))
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

// ---- Admin watchlist-item CRUD (Phase 15, ADMINUI-04) ----
// Consumes the 15-01 read-only GET /api/admin/items (active+inactive, D-13). The response reuses the
// existing trackedItemSchema boundary (no new schema). A 409 (active duplicate) surfaces as ApiError
// so the form can special-case it.

export async function getAdminItems(): Promise<TrackedItems> {
  return trackedItemsSchema.parse(await adminRequest('/api/admin/items', { method: 'GET' }))
}

export async function addAdminItem(body: AdminItemRequest): Promise<TrackedItem> {
  // POST doubles as create (201) and reactivate (200): the backend branches on externalItemId (D-13).
  return trackedItemSchema.parse(await adminRequest('/api/admin/items', { method: 'POST', body }))
}

export async function deactivateAdminItem(id: number): Promise<void> {
  // 204 soft-delete (active=false; row + price history preserved, backend D-03).
  await adminRequest(`/api/admin/items/${id}`, { method: 'DELETE' })
}

// ---- Coupon read + admin CRUD (Phase 17.3, COUPON-01) ----
// Public getCoupons consumes GET /api/coupons (unexpired, soonest-first) — the dashboard (17.3-03)
// reads it too. The admin fns mirror the game-event CRUD: adminRequest attaches X-Admin-Secret and
// each response is validated at the zod boundary. expiresAt crosses as the raw "YYYY-MM-DD" date
// string (no KST conversion, D-01). The frontend never calls Lostark directly.

export async function getCoupons(): Promise<Coupons> {
  return couponsSchema.parse(await request('/api/coupons'))
}

// ---- 보석 현재가 read (Phase 26, GEM-02) ----
// GET /api/gems — 백엔드가 경매장을 조회·캐시해 서빙한다. 프론트는 경매장을 직접 호출하지 않는다(D-06):
// 실 키는 서버 env에만 있고, 경매장은 10분 수집과 레이트리밋 버킷을 공유하므로(Phase 24 §H2) 호출
// 예산 관리는 백엔드 캐시의 책임이다.
export async function getGems(): Promise<GemsResponse> {
  return gemsResponseSchema.parse(await request('/api/gems'))
}

// ---- 거래소 검색 read (아바타·모험의 서 실시간 조회) ----
// GET /api/market/{adventure,avatar,classes} — 백엔드가 거래소를 조회·캐시해 서빙한다. 보석과 동일하게
// 프론트는 로스트아크를 직접 호출하지 않는다(D-06): 실 키는 서버 env에만 있고 호출 예산은 백엔드가 관리.
// 정렬·직업·부위는 백엔드가 화이트리스트 검증하므로 잘못된 값은 400 → ApiError로 화면 ErrorState.

export type MarketSort = 'min_price' | 'recent_price'
export type MarketDir = 'asc' | 'desc'

export async function getMarketClasses(): Promise<MarketClasses> {
  return marketClassesSchema.parse(await request('/api/market/classes'))
}

/*
  모험의 서 전량(~140). 파라미터가 없다 — 대륙 분류·검색·정렬을 전부 클라이언트가 이 140행 위에서
  하기 때문이다(대륙 매핑은 features/market/tomes.ts). 서버는 10분 캐시라 대륙을 아무리 눌러도
  API 호출이 늘지 않는다.
*/
export async function getAdventure(): Promise<MarketSearchResponse> {
  return marketSearchResponseSchema.parse(await request('/api/market/adventure'))
}

export async function getAvatar(params: {
  characterClass: string
  part?: string
  q?: string
  sort: MarketSort
  dir: MarketDir
  page: number
}): Promise<MarketSearchResponse> {
  const qs = new URLSearchParams({
    class: params.characterClass,
    sort: params.sort,
    dir: params.dir,
    page: String(params.page),
  })
  if (params.part) qs.set('part', params.part)
  if (params.q) qs.set('q', params.q)
  return marketSearchResponseSchema.parse(await request(`/api/market/avatar?${qs.toString()}`))
}

export async function getAdminCoupons(): Promise<Coupons> {
  return couponsSchema.parse(await adminRequest('/api/admin/coupons', { method: 'GET' }))
}

export async function createAdminCoupon(body: AdminCouponRequest): Promise<Coupon> {
  return couponSchema.parse(await adminRequest('/api/admin/coupons', { method: 'POST', body }))
}

export async function replaceAdminCoupon(id: number, body: AdminCouponRequest): Promise<Coupon> {
  return couponSchema.parse(
    await adminRequest(`/api/admin/coupons/${id}`, { method: 'PUT', body }),
  )
}

export async function deleteAdminCoupon(id: number): Promise<void> {
  // 204 No Content — adminRequest resolves undefined; nothing to parse.
  await adminRequest(`/api/admin/coupons/${id}`, { method: 'DELETE' })
}
