import { Info } from 'lucide-react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

// "상관 ≠ 인과" always-on notice (D-10, IMPCT-04) — the face of this project's credibility. It is
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
      <AlertTitle>상관 ≠ 인과</AlertTitle>
      <AlertDescription>
        이 수치는 이벤트와 가격의 시점 상관일 뿐, 인과(이벤트가 가격을 올렸다)를 의미하지 않습니다.
      </AlertDescription>
    </Alert>
  )
}