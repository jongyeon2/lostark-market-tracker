import { NavLink } from 'react-router-dom'

import { cn } from '@/lib/utils'

// FND-03: the three top-level routes as text labels (UI-SPEC nav copy).
const navItems = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/timeline', label: '품목 타임라인' },
  { to: '/impact', label: '이벤트 영향' },
]

/*
  Top fixed nav bar (UI-SPEC Layout: secondary surface, ~56px). Brand '로스트아크 시세 트래커'
  on the left + three text NavLinks; the active route is accent blue-600 (text + underline),
  the rest slate-500 Label — so the user always sees where they are.
*/
export function TopNav() {
  return (
    <header className="bg-card sticky top-0 z-40 h-14 border-b">
      <div className="mx-auto flex h-full max-w-7xl items-center justify-between px-8">
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