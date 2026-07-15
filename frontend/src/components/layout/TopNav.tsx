import { NavLink } from 'react-router-dom'

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
]

/*
  Top fixed nav bar (UI-SPEC Layout: secondary surface, ~56px). Brand '로스트아크 시세 트래커'
  on the left + three text NavLinks; the active route is accent blue-600 (text + underline),
  the rest slate-500 Label — so the user always sees where they are.
*/
export function TopNav() {
  return (
    <header className="bg-card sticky top-0 z-40 h-14 border-b">
      {/* max-w는 AppLayout <main>과 반드시 같아야 브랜드·메뉴가 본문과 정렬된다. 함께 바꿀 것. */}
      <div className="mx-auto flex h-full max-w-[90rem] items-center justify-between px-8">
        <span className="text-base font-semibold">로스트아크 시세 트래커</span>
        <nav className="flex items-center gap-6">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'text-sm font-semibold transition-colors',
                  isActive
                    ? 'text-primary underline underline-offset-8'
                    : 'text-muted-foreground hover:text-foreground',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  )
}