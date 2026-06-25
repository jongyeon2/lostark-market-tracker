import { useItems } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Card, CardContent } from '@/components/ui/card'

/*
  FND-02 end-to-end proof + FND-04 state demo on a REAL screen: useItems() flows through
  <AsyncBoundary> — with the seed backend up, /api/items is fetched via the Vite proxy and
  rendered (no CORS); with the backend down, the shared ErrorState shows '다시 불러오기'
  instead of a crash. The Phase-8 health/latest widgets are intentionally NOT built here.
*/
export function DashboardPage() {
  const { status, data, refetch } = useItems()
  const isEmpty = (data?.length ?? 0) === 0

  return (
    <div className="space-y-6">
      <h1 className="text-[28px] leading-tight font-semibold">대시보드</h1>

      <AsyncBoundary status={status} isEmpty={isEmpty} onRetry={() => refetch()}>
        <Card>
          <CardContent className="space-y-3">
            <p className="text-muted-foreground text-sm">추적 중 {data?.length ?? 0}개 품목</p>
            <ul className="space-y-1">
              {data?.map((item) => (
                <li key={item.id} className="text-base">
                  {item.displayName}{' '}
                  <span className="text-muted-foreground text-sm">({item.category})</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </AsyncBoundary>
    </div>
  )
}