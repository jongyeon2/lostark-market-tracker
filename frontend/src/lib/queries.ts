import { useMutation, useQuery } from '@tanstack/react-query'

import {
  getCollectionHealth,
  getItems,
  getLatestPrice,
  getTimeline,
  getEventImpact,
  getNews,
  getAdminEvents,
  createAdminEvent,
  replaceAdminEvent,
  deleteAdminEvent,
  getAdminItems,
  addAdminItem,
  deactivateAdminItem,
  getCoupons,
  getAdminCoupons,
  createAdminCoupon,
  replaceAdminCoupon,
  deleteAdminCoupon,
} from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type {
  AdminCouponRequest,
  AdminEventRequest,
  AdminItemRequest,
  TrackedItem,
} from '@/lib/schemas'

/*
  One typed React Query hook per endpoint (D-01). Each wraps the api.ts fn (which validates
  the response with zod at the boundary) under a stable queryKey; the QueryClient defaults
  supply the D-02 fetch-on-mount policy. No screen consumes these yet — 07-03 wires them
  through <AsyncBoundary>.
*/

export function useCollectionHealth() {
  return useQuery({ queryKey: ['collection-health'], queryFn: getCollectionHealth })
}

export function useItems() {
  return useQuery({ queryKey: ['items'], queryFn: getItems })
}

export function useLatestPrice(id: number) {
  return useQuery({ queryKey: ['latest', id], queryFn: () => getLatestPrice(id) })
}

export function useTimeline(id: number, from: string, to: string) {
  return useQuery({
    queryKey: ['timeline', id, from, to],
    queryFn: () => getTimeline(id, from, to),
  })
}

export function useEventImpact(id: number, window: number) {
  return useQuery({
    queryKey: ['event-impact', id, window],
    queryFn: () => getEventImpact(id, window),
  })
}

/*
  News panel (17.2) — one low-churn read for the dashboard's right column. staleTime 5min so the
  panel doesn't refetch aggressively (the backend already serves a 6h-polled Redis cache); no
  polling. getNews validates GET /api/news at the zod boundary — the frontend never calls Lostark
  directly (D-06).
*/
export function useNews() {
  return useQuery({ queryKey: ['news'], queryFn: getNews, staleTime: 5 * 60 * 1000 })
}

/*
  Admin game-event query + the project's FIRST TanStack MUTATIONS (D-10). Each mutation's onSuccess
  invalidates the ['admin-events'] key → refetch, keeping server state the single source of truth
  (no optimistic update). EventSection reads isPending/isError/isSuccess for its inline feedback.
*/
const ADMIN_EVENTS_KEY = ['admin-events'] as const

export function useAdminEvents() {
  return useQuery({ queryKey: ADMIN_EVENTS_KEY, queryFn: getAdminEvents })
}

function invalidateAdminEvents() {
  return queryClient.invalidateQueries({ queryKey: ADMIN_EVENTS_KEY })
}

export function useCreateEvent() {
  return useMutation({
    mutationFn: (body: AdminEventRequest) => createAdminEvent(body),
    onSuccess: invalidateAdminEvents,
  })
}

export function useReplaceEvent() {
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: AdminEventRequest }) => replaceAdminEvent(id, body),
    onSuccess: invalidateAdminEvents,
  })
}

export function useDeleteEvent() {
  return useMutation({
    mutationFn: (id: number) => deleteAdminEvent(id),
    onSuccess: invalidateAdminEvents,
  })
}

/*
  Admin watchlist-item query + mutations (ADMINUI-04). Consumes 15-01's GET /api/admin/items
  (active+inactive, D-13). Reactivate has no dedicated endpoint — it re-POSTs the item's
  externalItemId so the backend reactivates the soft-deleted row (200, D-13). Each mutation
  invalidates ['admin-items'] on success (D-10).
*/
const ADMIN_ITEMS_KEY = ['admin-items'] as const

export function useAdminItems() {
  return useQuery({ queryKey: ADMIN_ITEMS_KEY, queryFn: getAdminItems })
}

function invalidateAdminItems() {
  return queryClient.invalidateQueries({ queryKey: ADMIN_ITEMS_KEY })
}

export function useAddItem() {
  return useMutation({
    mutationFn: (body: AdminItemRequest) => addAdminItem(body),
    onSuccess: invalidateAdminItems,
  })
}

export function useDeactivateItem() {
  return useMutation({
    mutationFn: (id: number) => deactivateAdminItem(id),
    onSuccess: invalidateAdminItems,
  })
}

export function useReactivateItem() {
  return useMutation({
    // Reactivate = re-POST the same externalItemId → backend reactivates the soft-deleted row (D-13).
    mutationFn: (item: TrackedItem) =>
      addAdminItem({
        externalItemId: item.externalItemId,
        displayName: item.displayName,
        category: item.category,
      }),
    onSuccess: invalidateAdminItems,
  })
}

/*
  Coupon query hooks (Phase 17.3, COUPON-01). useCoupons is the PUBLIC read the dashboard (17.3-03)
  consumes — keyed ['coupons'], staleTime 5min (low-churn admin data, no polling). The admin query +
  mutations mirror the game-event pattern, but each mutation invalidates BOTH ['admin-coupons'] AND
  ['coupons'] so an admin change is reflected on the dashboard's public panel too. COUPONS_KEY is
  defined here (not in 17.3-03) because CouponSection and NewsPanel share it.
*/
const COUPONS_KEY = ['coupons'] as const

export function useCoupons() {
  return useQuery({ queryKey: COUPONS_KEY, queryFn: getCoupons, staleTime: 5 * 60 * 1000 })
}

const ADMIN_COUPONS_KEY = ['admin-coupons'] as const

export function useAdminCoupons() {
  return useQuery({ queryKey: ADMIN_COUPONS_KEY, queryFn: getAdminCoupons })
}

function invalidateCoupons() {
  // Admin changes must refresh both the admin listing AND the public dashboard panel.
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ADMIN_COUPONS_KEY }),
    queryClient.invalidateQueries({ queryKey: COUPONS_KEY }),
  ])
}

export function useCreateCoupon() {
  return useMutation({
    mutationFn: (body: AdminCouponRequest) => createAdminCoupon(body),
    onSuccess: invalidateCoupons,
  })
}

export function useReplaceCoupon() {
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: AdminCouponRequest }) =>
      replaceAdminCoupon(id, body),
    onSuccess: invalidateCoupons,
  })
}

export function useDeleteCoupon() {
  return useMutation({
    mutationFn: (id: number) => deleteAdminCoupon(id),
    onSuccess: invalidateCoupons,
  })
}
