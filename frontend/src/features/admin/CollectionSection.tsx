import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { HealthCard } from '@/features/dashboard/HealthCard'

/*
  수집 상태 section (D-11, ADMINUI-05). Reuses the existing self-contained <HealthCard/>
  (GET /api/health/collection — no admin secret, no props, no backend change) inside a titled
  section Card, matching the other console sections. Zero backend touch.
*/
export function CollectionSection() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">수집 상태</CardTitle>
      </CardHeader>
      <CardContent>
        <HealthCard />
      </CardContent>
    </Card>
  )
}
