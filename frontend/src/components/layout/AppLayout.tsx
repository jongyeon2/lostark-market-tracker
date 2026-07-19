import { Outlet } from 'react-router-dom'

import { TopNav } from '@/components/layout/TopNav'

/*
  FND-03 app shell: persistent top nav + the route content area. Content is max-width constrained
  with 32px side padding and is the primary visual focus of each route. The router renders the active
  page into <Outlet/>.

  Width raised 90rem → 100rem (2026-07-20): the dashboard's left column grew 176px → 320px so it
  matches the 320px news column as a symmetric card (사용자 결정), which shrank the center. At 100rem
  the center is ~800px (1600 − 64 padding − 96 gaps − 320 left − 320 news), so the event-impact table
  (needs 754px) still fits without sideways scroll on wide screens. TopNav mirrors this value so the
  brand/menu stay aligned with the content below; keep the two in step. /timeline shares the shell and
  simply gets a wider chart.
*/
export function AppLayout() {
  return (
    <div className="bg-background min-h-screen">
      <TopNav />
      <main className="mx-auto max-w-[100rem] px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}