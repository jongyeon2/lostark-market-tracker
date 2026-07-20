import { NavLink } from 'react-router-dom'

import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { cn } from '@/lib/utils'

/*
  FND-03: the top-level routes as text labels (UI-SPEC nav copy).

  Phase 28 dropped TWO entries into the dashboard (DASH-01/DASH-02):
  - '보석' got its own route in Phase 26 because gems carry no id and no time series, so they could not
    live in a grid whose cards deep-link to /timeline. That reasoning expired — the cards are no longer
    links (28-02 made the card a selector with a separate 차트 link), so a non-navigating gem card fits
    fine. Six values never justified a route.
  - '이벤트 영향' made you navigate away and re-pick, with a selector, the item you were already looking
    at. It now renders under the item you click.
  Both paths still resolve (main.tsx redirects them) — only the nav entries are gone.
*/
const navItems = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/timeline', label: '품목 타임라인' },
  { to: '/avatar', label: '아바타' },
  { to: '/adventure', label: '모험의 서' },
]

/*
  Top fixed nav bar (UI-SPEC Layout: secondary surface, 72px). Brand block on the left =
  'Loaket' 워드마크(Fredoka + 그라데이션 초록, .brand-wordmark) + 부제(옛 이름 '로스트아크 시세 트래커');
  text NavLinks on the right, the active route is accent blue-600 (text + underline).

  로고 이미지는 뺐다(사용자 결정) — 워드마크가 브랜드를 충분히 지고, 아이콘은 탭 파비콘으로 남는다.
  브랜드는 링크다(→ /). 부제를 남기는 건 Loaket이 처음 보는 사람에게 무슨 사이트인지 안 보이기
  때문 — 이름은 브랜드, 부제는 설명이다. 헤더를 72px로 키우고 py로 위아래 여백을 줬다(로고가 위아래
  선에 붙어 답답하다는 피드백).
*/
export function TopNav() {
  return (
    <header className="bg-card sticky top-0 z-40 h-[4.5rem] border-b">
      {/* max-w는 AppLayout <main>과 반드시 같아야 브랜드·메뉴가 본문과 정렬된다. 함께 바꿀 것. */}
      <div className="mx-auto flex h-full max-w-[100rem] items-center justify-between px-8 py-3">
        <NavLink to="/" className="flex min-w-0 flex-col leading-tight" aria-label="Loaket 홈">
          <span className="brand-wordmark text-3xl">Loaket</span>
          {/* 부제는 좁은 화면에서 숨긴다 — nav 링크 자리를 확보(브랜드 이름은 항상 보임). */}
          <span className="text-muted-foreground hidden text-xs sm:block">로스트아크 시세 트래커</span>
        </NavLink>
        {/* 좁은 화면(<sm)에선 링크가 줄바꿈돼 지저분해지므로 gap을 줄이고 줄바꿈을 막는다.
            브랜드 부제도 <sm에선 숨겨 폭을 브랜드가 독점하지 않게 한다. */}
        <nav className="flex shrink-0 items-center gap-4 sm:gap-6">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'text-sm font-semibold whitespace-nowrap transition-colors',
                  isActive
                    ? 'text-primary underline underline-offset-8'
                    : 'text-muted-foreground hover:text-foreground',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
          {/* 테마 토글은 라우트가 아니므로 링크 목록 뒤, 살짝 띄워 붙인다. */}
          <ThemeToggle />
        </nav>
      </div>
    </header>
  )
}