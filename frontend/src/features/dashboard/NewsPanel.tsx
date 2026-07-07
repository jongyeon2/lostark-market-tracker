import { useState, type ReactNode } from 'react'

import { useCoupons, useNews } from '@/lib/queries'
import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Coupon, NewsEvent, NewsNotice } from '@/lib/schemas'

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
      <CardContent className="space-y-6">
        {/* 쿠폰이 맨 위 — 가장 actionable (D-02). 자체 AsyncBoundary라 뉴스 실패와 서로 가리지 않는다. */}
        <CouponSection />
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
  쿠폰 섹션 — 관리자가 등록한 미만료 쿠폰(GET /api/coupons, 17.3-02 useCoupons). 백엔드가 이미
  만료임박순으로 정렬·만료분 제외(17.3-01)하므로 여기선 방어적 ≤6 slice만. useNews와 독립된 자체
  <AsyncBoundary>라 쿠폰 로딩 실패가 이벤트/공지를 가리지 않는다(COUPON-03). 각 행은 code(강조)·reward·
  만료일(newsDate, date-only YYYY.MM.DD — formatKst 금지) + 원클릭 복사 버튼(D-03).
*/
function CouponSection() {
  const couponsQuery = useCoupons()
  const [copiedId, setCopiedId] = useState<number | null>(null)
  const coupons = couponsQuery.data?.slice(0, MAX_ROWS) ?? []

  async function copy(coupon: Coupon) {
    try {
      await navigator.clipboard.writeText(coupon.code)
      setCopiedId(coupon.id)
      // Revert the clicked row's label after ~2s; guard against clearing a newer selection.
      setTimeout(() => setCopiedId((cur) => (cur === coupon.id ? null : cur)), 2000)
    } catch {
      // Clipboard unavailable (insecure context / denied permission) — ignore silently, never crash (D-03).
    }
  }

  return (
    <section className="space-y-2">
      <SectionHeading>쿠폰</SectionHeading>
      <AsyncBoundary
        status={couponsQuery.status}
        isEmpty={coupons.length === 0}
        onRetry={() => couponsQuery.refetch()}
        emptyHeading="사용 가능한 쿠폰이 없어요"
        emptyBody="관리자가 쿠폰을 등록하면 여기에 표시됩니다."
      >
        <ul className="-mx-2 space-y-0.5">
          {/* -mx-2 aligns each row's px-2 content with the "쿠폰" heading's first char. */}
          {coupons.map((coupon) => (
            <li key={coupon.id}>
              <div className="flex items-center gap-2 rounded-md px-2 py-1.5">
                <div className="min-w-0 flex-1 space-y-0.5">
                  <p className="truncate text-sm font-semibold tabular-nums">{coupon.code}</p>
                  <p className="text-muted-foreground text-xs">
                    {coupon.reward} · <span className="tabular-nums">{newsDate(coupon.expiresAt)}</span>
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  className="shrink-0"
                  onClick={() => copy(coupon)}
                >
                  {copiedId === coupon.id ? '복사됨!' : '복사'}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      </AsyncBoundary>
    </section>
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
        <ul className="-mx-2 space-y-0.5">
          {/* -mx-2 aligns each row's px-2 content with the "공지사항" heading's first char. */}
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
                <span className="text-muted-foreground text-xs tabular-nums">
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
