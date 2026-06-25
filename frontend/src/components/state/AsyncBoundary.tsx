import type { ReactNode } from 'react'

import { LoadingState } from '@/components/state/LoadingState'
import { EmptyState } from '@/components/state/EmptyState'
import { ErrorState } from '@/components/state/ErrorState'

type AsyncStatus = 'pending' | 'error' | 'success'

interface AsyncBoundaryProps {
  /** React Query result status. */
  status: AsyncStatus
  /** True when the successful response is logically empty (e.g. data.length === 0). */
  isEmpty?: boolean
  /** Re-run the query — forwarded to ErrorState's '다시 불러오기' CTA (D-02 retry loop). */
  onRetry?: () => void
  /** Optional per-screen override of the EmptyState body line. */
  emptyBody?: string
  /** The success view — screens write ONLY this path. */
  children: ReactNode
}

/*
  D-08: thin, generic wrapper mapping a React Query result to the right shared state
  component, so each screen writes only the success path (branching centralized, testable):
    pending           -> LoadingState
    error             -> ErrorState(onRetry)   ('다시 불러오기' re-runs the query — D-02)
    success && isEmpty -> EmptyState
    success           -> children
  It does no fetching and makes no assumption about the data type beyond these flags.
*/
export function AsyncBoundary({
  status,
  isEmpty = false,
  onRetry,
  emptyBody,
  children,
}: AsyncBoundaryProps) {
  if (status === 'pending') return <LoadingState />
  if (status === 'error') return <ErrorState onRetry={onRetry ?? (() => {})} />
  if (isEmpty) return <EmptyState body={emptyBody} />
  return <>{children}</>
}