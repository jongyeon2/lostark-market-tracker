import { Outlet } from 'react-router-dom'

import { TopNav } from '@/components/layout/TopNav'

/*
  FND-03 app shell: persistent top nav + the route content area. Content is max-width
  constrained (max-w-7xl ≈ screen-xl) with xl(32px) side padding and is the primary visual
  focus of each route. The router renders the active page into <Outlet/>.
*/
export function AppLayout() {
  return (
    <div className="bg-background min-h-screen">
      <TopNav />
      <main className="mx-auto max-w-7xl px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}