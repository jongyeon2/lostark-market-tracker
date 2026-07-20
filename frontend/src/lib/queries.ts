import { useMutation, useQuery } from '@tanstack/react-query'

import { ApiError } from '@/lib/api'
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
  getGems,
  getMarketClasses,
  getAdventure,
  getAvatar,
  type MarketSort,
  type MarketDir,
  type EventImpactSort,
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
  EventType,
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

/*
  이벤트 영향. 필터·정렬·개수는 **서버가** 적용하므로 전부 쿼리 키에 들어간다(2026-07-20) —
  키가 같으면 캐시가 응답하고, 바뀌면 새로 받는다.

  placeholderData로 이전 결과를 유지한다: '더 보기'는 limit을 키워 다시 받는 방식이라 그때마다
  목록이 비었다가 다시 그려지면 방금까지 읽던 위치를 잃는다. 필터를 바꿀 때도 같은 이유.
*/
export function useEventImpact(
  id: number,
  window: number,
  params: { types: readonly EventType[]; sort: EventImpactSort; limit: number },
) {
  return useQuery({
    queryKey: ['event-impact', id, window, [...params.types].sort().join(','), params.sort, params.limit] as const,
    queryFn: () => getEventImpact(id, window, params),
    placeholderData: (prev) => prev,
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

/*
  보석 현재가 (Phase 26, GEM-02). staleTime은 백엔드 캐시 TTL(5분)과 맞춘다 — 그보다 자주 물어봐야
  같은 스냅샷이 돌아올 뿐이고, 경매장은 10분 수집과 레이트리밋 버킷을 공유하므로(Phase 24 §H2)
  불필요한 왕복을 만들 이유가 없다. 폴링 없음(useNews와 동일 — read-only 화면).
*/
const GEMS_KEY = ['gems'] as const

export function useGems() {
  return useQuery({ queryKey: GEMS_KEY, queryFn: getGems, staleTime: 5 * 60 * 1000 })
}

// ---- 거래소 검색 (아바타·모험의 서 실시간 조회) ----
// 백엔드가 5분 캐시하므로 프론트 staleTime도 5분: 같은 검색·페이지는 재요청하지 않아 레이트리밋 예산을
// 아낀다(디바운스가 타이핑 폭주를, 캐시가 반복 요청을 막는 이중 방어). 직업 목록은 거의 안 변해 길게 둔다.

/*
  🔑 429 자동 재시도. 거래소 검색은 백그라운드 수집기와 레이트리밋 버킷(분당 90)을 공유하므로, 새 검색·
  페이지를 연달아 넘기면 일시적으로 429가 난다(실측: 새 검색 10회 중 6회). 전역 기본값 retry:1은 "즉시
  1회"라 버킷이 회복되기 전에 또 429를 맞고 에러 화면을 띄운다 — 사용자가 손으로 '다시 불러오기'를 눌러야
  했던 원인이다. 여기서는 시간 간격을 두고 몇 번 자동 재시도해 버킷이 회복되면 조용히 성공시킨다.

  429(레이트리밋)·502(업스트림 일시 장애)만 재시도한다. 400(잘못된 정렬·직업)·404는 재시도해도 안
  고쳐지므로 즉시 에러로 둔다(무의미한 반복 호출로 예산을 더 태우지 않는다).
  버킷은 분당 90 = 초당 1.5로 회복되므로 1초만 기다려도 대개 토큰이 돌아온다(백오프 1s→2s→4s, 상한 6s).
*/
function marketRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError) {
    if (error.status === 429 || error.status === 502) return failureCount < 4
    return false // 400·404 등은 재시도 무의미
  }
  return failureCount < 2 // 네트워크 오류 등은 몇 번 더
}
const marketRetryDelay = (attempt: number) => Math.min(1000 * 2 ** attempt, 6000)

export type MarketSearchParams = {
  q: string
  sort: MarketSort
  dir: MarketDir
  page: number
}

const MARKET_CLASSES_KEY = ['market-classes'] as const

export function useMarketClasses() {
  return useQuery({
    queryKey: MARKET_CLASSES_KEY,
    queryFn: getMarketClasses,
    staleTime: 60 * 60 * 1000, // 1h — 직업 목록은 신규 직업이 나올 때만 바뀐다
    retry: marketRetry,
    retryDelay: marketRetryDelay,
  })
}

/*
  모험의 서 전량. 인자가 없다 — 대륙 전환·검색·정렬이 전부 로컬이라 쿼리 키가 하나뿐이고, 그래서
  대륙을 아무리 눌러도 네트워크 요청이 0이다(placeholderData로 깜빡임을 막을 이유도 사라졌다).
  staleTime은 서버 캐시 TTL(10분)에 맞춘다 — 더 짧게 잡아도 서버가 같은 캐시를 돌려줄 뿐이다.
*/
export function useAdventure() {
  return useQuery({
    queryKey: ['market-adventure'] as const,
    queryFn: getAdventure,
    staleTime: 10 * 60 * 1000,
    retry: marketRetry, // 429 자동 재시도(위 marketRetry 참조) — 수동 '다시 불러오기' 불필요
    retryDelay: marketRetryDelay,
  })
}

export function useAvatar(params: MarketSearchParams & { characterClass: string; part: string }) {
  return useQuery({
    // characterClass가 비면 쿼리를 실행하지 않는다 — 직업 선택이 필수이기 때문(백엔드도 400).
    queryKey: ['market-avatar', params.characterClass, params.part, params.q, params.sort, params.dir, params.page] as const,
    queryFn: () =>
      getAvatar({
        characterClass: params.characterClass,
        part: params.part || undefined,
        q: params.q || undefined,
        sort: params.sort,
        dir: params.dir,
        page: params.page,
      }),
    enabled: params.characterClass.length > 0,
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
    retry: marketRetry,
    retryDelay: marketRetryDelay,
  })
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
