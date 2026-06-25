import { QueryClient } from '@tanstack/react-query'

/*
  Single QueryClient encoding the D-02 fetch-on-mount / route-entry policy: the seed data is
  effectively static, so NO window-focus refetch, a long staleTime, and NO polling/interval
  (real-time auto-refresh is deferred to FE-V2-02). User-initiated retry is the UI-SPEC
  "다시 불러오기" CTA calling the query's refetch — wired in 07-03.
*/
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 5 * 60 * 1000, // 5 min — seed data rarely changes within a demo session
      retry: 1,
    },
  },
})
