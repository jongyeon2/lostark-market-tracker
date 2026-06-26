import { Card, CardContent, CardHeader } from '@/components/ui/card'
import {
  changeRateColorClass,
  formatChangeRate,
  formatPrice,
  insufficientReason,
} from '@/features/impact/impactFormat'
import { EventTypeBadge, ImpactStatusBadge } from '@/features/impact/ImpactStatusBadge'
import { formatKst } from '@/lib/formatKst'
import { cn } from '@/lib/utils'
import type { EventImpactItem } from '@/lib/schemas'

// EventImpactCards — the <md card list (D-04, `md:hidden`, one row = one card). Same data and the
// same impactFormat / badge components as EventImpactTable so the table and cards never diverge.
// Renders the backend occurred_at-desc order as-is (no re-sort) and mixes ok + insufficient_data.
// High-priority fields (eventType badge, title, status, occurredAt, changeRate) lead; price plus the
// insufficient body/anchor evidence (D-08) sit in the secondary area. 30-min threshold never
// referenced — 희소/stale is insufficientReason's call.
export function EventImpactCards({ events }: { events: EventImpactItem[] }) {
  return (
    <div className="space-y-4 md:hidden">
      {events.map((event) => {
        const reason = insufficientReason(event)
        const isOk = event.status === 'ok' && event.changeRate != null
        return (
          <Card key={event.id}>
            <CardHeader>
              <div className="flex flex-wrap items-center gap-2">
                <EventTypeBadge eventType={event.eventType} />
                <span className="font-semibold">{event.title}</span>
                <ImpactStatusBadge status={event.status} />
              </div>
              <div className="flex items-center justify-between gap-4">
                <span className="text-muted-foreground text-sm tabular-nums">
                  {formatKst(event.occurredAt)} KST
                </span>
                {isOk ? (
                  <span
                    className={cn(
                      'text-base font-semibold tabular-nums',
                      changeRateColorClass(event.changeRate!),
                    )}
                  >
                    {formatChangeRate(event.changeRate!)}
                  </span>
                ) : (
                  <span className="text-warning text-sm font-semibold">{reason?.inlineLabel}</span>
                )}
              </div>
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              {isOk ? (
                <p className="tabular-nums">
                  이전가 {formatPrice(event.prePrice!)} → 이후가 {formatPrice(event.postPrice!)}
                </p>
              ) : (
                reason && (
                  <div className="flex flex-col gap-1">
                    <p className="text-muted-foreground text-sm">{reason.body}</p>
                    <p className="text-muted-foreground text-sm tabular-nums">
                      {`이전 기준: ${event.preAnchorAt ? formatKst(event.preAnchorAt) : '없음'} · 이후 기준: ${event.postAnchorAt ? formatKst(event.postAnchorAt) : '없음'}`}
                    </p>
                  </div>
                )
              )}
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
