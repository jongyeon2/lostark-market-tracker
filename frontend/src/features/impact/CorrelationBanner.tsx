import { Info } from 'lucide-react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

// "해석에 주의하세요" always-on notice (D-10, IMPCT-04) — the face of this project's credibility. It is
// read before the table and is always visible: there is NO close button, because hiding it would
// defeat the over-reading guard. variant is `default` (neutral slate — the default Alert variant,
// not the red error one — because this is interpretation guidance, not an error, so no red
// mis-signal) with a lucide Info icon (not a warning triangle). Placement (content-top, full width)
// is 10-04's responsibility. Pure
// presentational: no state, no dismiss callback.
export function CorrelationBanner({ className }: { className?: string }) {
  return (
    <Alert className={className}>
      <Info />
      <AlertTitle>해석에 주의하세요</AlertTitle>
      <AlertDescription>
        이벤트와 가격 변동이 비슷한 시점에 나타났다는 의미일 뿐, 이벤트가 가격을 움직였다는 근거는 아닙니다.
      </AlertDescription>
    </Alert>
  )
}