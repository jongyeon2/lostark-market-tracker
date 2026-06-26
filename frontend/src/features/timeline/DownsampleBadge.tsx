import { Badge } from '@/components/ui/badge'
import { bucketWidthLabel } from '@/features/timeline/eventMarkers'

// Honest downsample signal (D-08, TIME-03): when the response is a bucket average, a neutral slate
// badge says so — '버킷 평균 · 1시간'/'1일' (NOT the raw 'hour'/'day' literal, NOT the 09-UI-SPEC
// '1h' draft). Renders nothing when the data is raw. Positioning (e.g. chart top-right) is the
// caller's (09-05) concern; this component just renders the badge.
export function DownsampleBadge({
  downsampled,
  bucketWidth,
}: {
  downsampled: boolean
  bucketWidth: string | null
}) {
  if (!downsampled) return null
  const label = bucketWidthLabel(bucketWidth)
  return (
    <Badge variant="secondary" title={`이 구간은 원본이 아니라 ${label} 단위 버킷 평균입니다.`}>
      버킷 평균 · {label}
    </Badge>
  )
}
