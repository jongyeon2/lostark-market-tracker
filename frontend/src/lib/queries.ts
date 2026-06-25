import { useQuery } from '@tanstack/react-query'

import {
  getCollectionHealth,
  getItems,
  getLatestPrice,
  getTimeline,
  getEventImpact,
} from '@/lib/api'

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
