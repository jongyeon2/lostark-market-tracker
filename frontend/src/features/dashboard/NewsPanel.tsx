import type { ReactNode } from 'react'

import { useNews } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { NewsEvent, NewsNotice } from '@/lib/schemas'

/*
  NewsPanel — the dashboard's right-column widget (D-03/D-04/D-07). Consumes useNews() and wraps its
  OWN <AsyncBoundary> (isEmpty = events AND notices both empty) so a news failure renders an in-panel
  Loading/Empty/Error and NEVER blanks the item grid in the left column — per-widget isolation, the
  same discipline as HealthCard.

  진행중 이벤트 renders loawa.com-style: the event's banner THUMBNAIL with the title (+기간) in small
  text beneath it (vertical cards, 1-column to fit the ~320px sidebar). 공지사항 is a compact list —
  타입 뱃지 · 제목(폭에 맞춰 2줄, 넘치면 … 말줄임) · 날짜. Each card/row opens the official Lostark
  link in a NEW TAB with rel="noopener noreferrer" (tabnabbing guard). The frontend calls only
  /api/news — never Lostark directly (D-06).
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

/*
  진행중 이벤트 — loawa.com 스타일: 배너 썸네일 이미지 + 그 아래 작은 제목·기간(세로 카드 스택,
  좁은 사이드바에 맞춰 1열). 썸네일이 없으면 텍스트만. 카드 전체가 로아 공식 link 새 탭.
*/
function EventSection({ events }: { events: NewsEvent[] }) {
  return (
    <section className="space-y-2">
      <SectionHeading>진행중 이벤트</SectionHeading>
      {events.length === 0 ? (
        <EmptyLine>진행중 이벤트가 없어요</EmptyLine>
      ) : (
        <ul className="space-y-3">
          {events.map((event) => (
            <li key={event.link}>
              <a
                href={event.link}
                target="_blank"
                rel="noopener noreferrer"
                className="hover:bg-muted/40 block overflow-hidden rounded-lg border transition-colors"
              >
                {event.thumbnail ? (
                  // 배너 이미지. alt="" — the title text directly below carries the accessible label.
                  <img
                    src={event.thumbnail}
                    alt=""
                    loading="lazy"
                    className="bg-muted aspect-[16/9] w-full object-cover"
                  />
                ) : null}
                <div className="space-y-0.5 px-2.5 py-2">
                  <p className="line-clamp-2 text-xs leading-snug font-medium">{event.title}</p>
                  <p className="text-muted-foreground text-[11px] tabular-nums">
                    {newsDate(event.startDate)} ~ {newsDate(event.endDate)}
                  </p>
                </div>
              </a>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/*
  공지사항 — 타입 뱃지 + 제목 + 날짜(최신순). 제목은 컬럼 폭에 맞춰 최대 2줄, 넘치면 … 말줄임
  (line-clamp-2). flex 안의 제목에 min-w-0/flex-1을 줘야 flex item이 컨테이너보다 좁아질 수 있어
  clamp가 실제 폭 기준으로 동작한다(min-w-0 없으면 내용 폭만큼 넘쳐 잘리지 않음).
*/
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
              <a
                href={notice.link}
                target="_blank"
                rel="noopener noreferrer"
                className="hover:bg-muted/60 flex flex-col gap-0.5 rounded-md px-2 py-1.5 transition-colors"
              >
                <span className="flex items-center gap-2">
                  <TypeBadge type={notice.type} />
                  <span className="line-clamp-1 min-w-0 flex-1 text-sm font-medium">
                    {notice.title}
                  </span>
                </span>
                <span className="text-muted-foreground pl-0.5 text-xs tabular-nums">
                  {newsDate(notice.date)}
                </span>
              </a>
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
