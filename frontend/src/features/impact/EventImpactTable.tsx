import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  anchorSourceLabel,
  changeRateColorClass,
  formatChangeRate,
  formatPrice,
  insufficientReason,
} from '@/features/impact/impactFormat'
import { EventTypeBadge, ImpactStatusBadge } from '@/features/impact/ImpactStatusBadge'
import { formatKst } from '@/lib/formatKst'
import { cn } from '@/lib/utils'
import type { EventImpactItem } from '@/lib/schemas'

// EventImpactTable — the ≥md results table (D-04, `hidden md:block`). Renders the backend's
// occurred_at-desc order AS-IS (no front-end re-sort, D-09) and mixes ok + insufficient_data in one
// table, separated only by badges. Honest-data headline: a null changeRate is never shown as 0 or
// blank — an insufficient row replaces the number with its reason inlineLabel and surfaces the
// anchor times (D-08) as the evidence of "why". The 30-minute threshold is never referenced here —
// 희소/stale is entirely insufficientReason's call.
export function EventImpactTable({ events }: { events: EventImpactItem[] }) {
  return (
    <div className="hidden md:block">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>이벤트</TableHead>
            <TableHead>종류</TableHead>
            <TableHead>발생 시각</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>변화율</TableHead>
            <TableHead>이전가</TableHead>
            <TableHead>이후가</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((event) => {
            const reason = insufficientReason(event)
            const isOk = event.status === 'ok' && event.changeRate != null
            return (
              <TableRow key={event.id}>
                <TableCell>
                  <div className="flex flex-col gap-1">
                    <span>{event.title}</span>
                    {reason && (
                      <span className="text-muted-foreground text-sm tabular-nums">
                        {`이전 기준: ${event.preAnchorAt ? formatKst(event.preAnchorAt) : '없음'} · 이후 기준: ${event.postAnchorAt ? formatKst(event.postAnchorAt) : '없음'}`}
                      </span>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <EventTypeBadge eventType={event.eventType} />
                </TableCell>
                <TableCell>
                  <span className="tabular-nums">{formatKst(event.occurredAt)} KST</span>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col items-start gap-1">
                    <ImpactStatusBadge status={event.status} />
                    {/* 어떤 자로 잰 수치인지 — 스냅샷 행과 나란히 놓일 때 같은 의미로 읽히지 않도록. */}
                    {anchorSourceLabel(event) && (
                      <span className="text-muted-foreground text-xs">{anchorSourceLabel(event)}</span>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  {isOk ? (
                    <span className={cn('tabular-nums', changeRateColorClass(event.changeRate!))}>
                      {formatChangeRate(event.changeRate!)}
                    </span>
                  ) : (
                    <span className="text-warning text-sm">{reason?.inlineLabel}</span>
                  )}
                </TableCell>
                <TableCell>
                  {isOk ? (
                    <span className="tabular-nums">{formatPrice(event.prePrice!)}</span>
                  ) : (
                    '—'
                  )}
                </TableCell>
                <TableCell>
                  {isOk ? (
                    <span className="tabular-nums">{formatPrice(event.postPrice!)}</span>
                  ) : (
                    '—'
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )
}
