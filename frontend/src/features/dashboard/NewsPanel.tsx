import type { ReactNode } from 'react'

import { useNews } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { NewsEvent, NewsNotice } from '@/lib/schemas'

/*
  NewsPanel — the dashboard's right-column widget (D-03/D-04/D-07). Consumes useNews() and wraps its
  OWN <AsyncBoundary> (isEmpty = events AND notices both empty) so a news failure renders an in-panel
  Loading/Empty/Error and NEVER blanks the item grid in the left column — per-widget isolation, the
  same discipline as HealthCard. Two sections: 진행중 이벤트 (제목 · 기간) and 공지사항 (타입 뱃지 ·
  제목 · 날짜), each ≤6 rows (the backend already caps; sliced defensively). Every row opens the
  official Lostark link in a NEW TAB with rel="noopener noreferrer" (tabnabbing guard, T-1723-01).
  The frontend calls only /api/news — never Lostark directly (D-06).
*/

const MAX_ROWS = 6

export function NewsPanel() {
  const { status, data, refetch } = useNews()
  const events = data?.events.slice(0, MAX_ROWS) ?? []
  const notices = data?.notices.slice(0, MAX_ROWS) ?? []
  const isEmpty = events.length === 0 && notices.length === 0

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">로스트아크 소식</CardTitle>
      </CardHeader>
      <CardContent>
        <AsyncBoundary
          status={status}
          isEmpty={isEmpty}
          onRetry={() => refetch()}
          emptyHeading="소식이 없어요"
          emptyBody="진행중인 이벤트나 새 공지가 아직 없어요."
        >
          <div className="space-y-6">
            <EventSection events={events} />
            <NoticeSection notices={notices} />
          </div>
        </AsyncBoundary>
      </CardContent>
    </Card>
  )
}

/* 진행중 이벤트 — 제목 + 기간(startDate~endDate). Its own list can be empty while notices are not,
   so it shows a per-section calm line rather than relying on the panel-level EmptyState. */
function EventSection({ events }: { events: NewsEvent[] }) {
  return (
    <section className="space-y-2">
      <SectionHeading>진행중 이벤트</SectionHeading>
      {events.length === 0 ? (
        <EmptyLine>진행중 이벤트가 없어요</EmptyLine>
      ) : (
        <ul className="space-y-0.5">
          {events.map((event) => (
            <li key={event.link}>
              <NewsRow href={event.link}>
                <span className="line-clamp-2 text-sm font-medium">{event.title}</span>
                <span className="text-muted-foreground text-xs tabular-nums">
                  {newsDate(event.startDate)} ~ {newsDate(event.endDate)}
                </span>
              </NewsRow>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/* 공지사항 — 타입 뱃지 + 제목 + 날짜(최신순). type is an opaque Korean category, rendered as a
   neutral badge (no color/enum assumption). */
function NoticeSection({ notices }: { notices: NewsNotice[] }) {
  return (
    <section className="space-y-2">
      <SectionHeading>공지사항</SectionHeading>
      {notices.length === 0 ? (
        <EmptyLine>새 공지가 없어요</EmptyLine>
      ) : (
        <ul className="space-y-0.5">
          {notices.map((notice) => (
            <li key={notice.link}>
              <NewsRow href={notice.link}>
                <span className="flex items-center gap-2">
                  <TypeBadge type={notice.type} />
                  <span className="line-clamp-2 text-sm font-medium">{notice.title}</span>
                </span>
                <span className="text-muted-foreground text-xs tabular-nums">{newsDate(notice.date)}</span>
              </NewsRow>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function SectionHeading({ children }: { children: ReactNode }) {
  return <h3 className="text-muted-foreground text-sm font-semibold tracking-wide">{children}</h3>
}

function EmptyLine({ children }: { children: ReactNode }) {
  return <p className="text-muted-foreground text-sm">{children}</p>
}

/* Shared clickable row — opens the official Lostark page in a new tab. rel="noopener noreferrer"
   severs window.opener so the external page can never reach back into this tab (T-1723-01). */
function NewsRow({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="hover:bg-muted/60 flex flex-col gap-0.5 rounded-md px-2 py-1.5 transition-colors"
    >
      {children}
    </a>
  )
}

function TypeBadge({ type }: { type: string }) {
  return (
    <span className="border-border text-muted-foreground shrink-0 rounded border px-1.5 py-0.5 text-[11px] font-semibold">
      {type}
    </span>
  )
}

/*
  News dates are the source's ISO-8601 LOCAL (KST wall-clock) strings WITHOUT an offset
  (e.g. "2026-07-20T06:00:00"). Slice the date part directly so NO timezone shift is applied —
  formatKst would treat them as UTC and wrongly add 9h. Displays as YYYY.MM.DD.
*/
function newsDate(iso: string): string {
  return iso.slice(0, 10).replace(/-/g, '.')
}
