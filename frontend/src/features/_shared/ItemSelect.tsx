import { useItems } from '@/lib/queries'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import { ROLE_LABEL, sortByRole, type RoleGroup } from '@/features/_shared/roleGroup'

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
    return <Skeleton className="h-9 w-40" />
  }

  if (status === 'error') {
    return (
      <div className="flex flex-col gap-1">
        <Select disabled>
          <SelectTrigger className="w-40" aria-label="품목 선택">
            <SelectValue placeholder="품목 선택" />
          </SelectTrigger>
        </Select>
        <p className="text-muted-foreground text-sm">품목을 불러오지 못했어요</p>
      </div>
    )
  }

  // D-07: role-group then name (sortByRole, non-mutating). Sections render in this fixed order;
  // null roleGroup is included as a '기타' section so NO curated item is ever dropped (ICON-07).
  const sorted = sortByRole(data)
  // The trigger shows ONLY the selected item's role-group label (딜러/서포터/융화재료/기타) — a long
  // engraving name otherwise overflows the compact trigger. The full item name is never lost: it is
  // shown right below in the page's LatestPriceCard (icon + name + badge).
  const selected = data.find((item) => item.id === value)
  const triggerLabel = selected ? (selected.roleGroup ? ROLE_LABEL[selected.roleGroup] : '기타') : null
  const SECTIONS: { role: RoleGroup | null; label: string }[] = [
    { role: 'DEALER', label: ROLE_LABEL.DEALER },
    { role: 'SUPPORT', label: ROLE_LABEL.SUPPORT },
    { role: 'MATERIAL', label: ROLE_LABEL.MATERIAL },
    { role: null, label: '기타' },
  ]

  return (
    <Select
      value={value != null ? String(value) : undefined}
      onValueChange={(v) => onChange(Number(v))}
    >
      {/* Trigger shows the role-group label only (not the long name) — SelectValue's default echoes
          the selected option's full content, so we render the short label directly instead. */}
      <SelectTrigger className="w-40" aria-label="품목 선택">
        {triggerLabel ?? <span className="text-muted-foreground">품목 선택</span>}
      </SelectTrigger>
      <SelectContent>
        {/* Static role-group headers (D-08, NOT a filter control). Empty sections are skipped; each
            option carries [icon sm][name][role badge] inline (ICON-03·D-04). */}
        {SECTIONS.map(({ role, label }) => {
          const groupItems = sorted.filter((item) => item.roleGroup === role)
          if (groupItems.length === 0) return null
          return (
            <SelectGroup key={label}>
              <SelectLabel>{label}</SelectLabel>
              {groupItems.map((item) => (
                <SelectItem key={item.id} value={String(item.id)}>
                  <span className="flex items-center gap-2">
                    <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
                    {item.displayName}
                    <RoleBadge roleGroup={item.roleGroup} />
                  </span>
                </SelectItem>
              ))}
            </SelectGroup>
          )
        })}
      </SelectContent>
    </Select>
  )
}
