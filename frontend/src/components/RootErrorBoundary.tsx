import { useRouteError } from 'react-router-dom'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

/*
  Top-level fallback (React Router errorElement): a thrown render/route error shows this
  card instead of a white screen (T-0703-02). Only user-facing copy is rendered — no raw
  exception/stack/response-body text leaks into the UI (T-0703-03); details go to the dev
  console only.
*/
export function RootErrorBoundary() {
  const error = useRouteError()
  if (import.meta.env.DEV) {
    // eslint-disable-next-line no-console
    console.error(error)
  }

  return (
    <div className="bg-background flex min-h-screen items-center justify-center p-8">
      <Card className="max-w-md">
        <CardHeader>
          <CardTitle>예기치 못한 오류가 발생했어요</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground text-base">
            화면을 새로고침해 주세요. 문제가 계속되면 seed 백엔드(:8080) 상태를 확인하세요.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}