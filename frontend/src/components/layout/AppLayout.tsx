import { Outlet } from 'react-router-dom'

import { TopNav } from '@/components/layout/TopNav'

/*
  FND-03 app shell: persistent top nav + the route content area. Content is max-width constrained
  with 32px side padding and is the primary visual focus of each route. The router renders the active
  page into <Outlet/>.

  Width raised 90rem → 100rem → 110rem. The 100rem step gave the dashboard's symmetric 320px side
  cards a center wide enough for the event-impact table (needs 754px). 110rem plus narrower 18rem side
  columns (2026-07-20, 사용자 결정) takes the center to ~1088px (1760 − 64 padding − 96 gaps − 288 − 288):
  the item cards and the event-impact section below them both read better wide, and the table gains
  headroom rather than sitting just above its minimum.

  No horizontal scroll comes from this: the dashboard grid's center is minmax(0,1fr), so on a narrower
  viewport the page shrinks to fit instead of overflowing. The real overflow risk is the event-impact
  TABLE's own minimum width, and widening the center moves away from it.

  TopNav mirrors this value so the brand/menu stay aligned with the content below; keep the two in
  step. /timeline shares the shell and simply gets a wider chart.
*/
export function AppLayout() {
  return (
    <div className="bg-background min-h-screen">
      <TopNav />
      <main className="mx-auto max-w-[110rem] px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}