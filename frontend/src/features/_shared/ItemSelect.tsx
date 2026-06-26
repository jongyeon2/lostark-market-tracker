import { useItems } from '@/lib/queries'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'

// Controlled dropdown item selector (D-05) over the 09-01 shadcn select block — the compact
// selector that leaves room for the chart + latest-price card on a single-item screen. Shared by
// Phase 9 and Phase 10 (D-07: extracted to _shared so the layer dependency never flows
// impact→timeline). Holds NO URL/router state — selection is reported only via onChange. Radix
// select works on string values, so we convert at the boundary.
export function ItemSelect({
  value,
  onChange,
}: {
  value: number | null
  onChange: (id: number) => void
}) {
  const { status, data } = useItems()

  // A selector failure must NOT blank the page — pending/error are handled inline here, never the
  // screen-level ErrorState.
  if (status === 'pending') {
    return <Skeleton className="h-9 w-56" />
  }

  if (status === 'error') {
    return (
      <div className="flex flex-col gap-1">
        <Select disabled>
          <SelectTrigger className="w-56" aria-label="품목 선택">
            <SelectValue placeholder="품목 선택" />
          </SelectTrigger>
        </Select>
        <p className="text-muted-foreground text-sm">품목을 불러오지 못했어요</p>
      </div>
    )
  }

  return (
    <Select
      value={value != null ? String(value) : undefined}
      onValueChange={(v) => onChange(Number(v))}
    >
      <SelectTrigger className="w-56" aria-label="품목 선택">
        <SelectValue placeholder="품목 선택" />
      </SelectTrigger>
      <SelectContent>
        {data.map((item) => (
          <SelectItem key={item.id} value={String(item.id)}>
            {item.displayName}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
