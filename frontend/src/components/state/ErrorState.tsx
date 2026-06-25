import { RefreshCw, TriangleAlert } from 'lucide-react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'

// FND-04 / D-07: shared error state — UI-SPEC error copy (problem + resolution path) using
// the destructive token, plus the "다시 불러오기" primary CTA wired to onRetry (closes the
// D-02 user-retry loop). User-facing copy only — no raw exception/stack is rendered (T-0703-03).
const DEFAULT_MESSAGE =
  '백엔드에 연결하지 못했어요. seed 백엔드(:8080)가 켜져 있는지 확인하고 다시 불러오세요.'

export function ErrorState({
  onRetry,
  message = DEFAULT_MESSAGE,
}: {
  onRetry: () => void
  message?: string
}) {
  return (
    <div className="flex flex-col items-center gap-4 py-16">
      <Alert variant="destructive" className="max-w-md">
        <TriangleAlert />
        <AlertTitle>연결 오류</AlertTitle>
        <AlertDescription>{message}</AlertDescription>
      </Alert>
      <Button onClick={onRetry}>
        <RefreshCw />
        다시 불러오기
      </Button>
    </div>
  )
}