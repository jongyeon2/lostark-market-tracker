import { useMutation, useQuery } from '@tanstack/react-query'

import {
  getCollectionHealth,
  getItems,
  getLatestPrice,
  getTimeline,
  getEventImpact,
  getAdminEvents,
  createAdminEvent,
  replaceAdminEvent,
  deleteAdminEvent,
} from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { AdminEventRequest } from '@/lib/schemas'

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
