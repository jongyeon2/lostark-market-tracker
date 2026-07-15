import { Outlet } from 'react-router-dom'

import { TopNav } from '@/components/layout/TopNav'

/*
  FND-03 app shell: persistent top nav + the route content area. Content is max-width constrained
  with 32px side padding and is the primary visual focus of each route. The router renders the active
  page into <Outlet/>.

  Width raised 80rem → 90rem (2026-07-15): the dashboard's event-impact table wants 754px of its own
  and the 3-column grid could only spare 641px at 80rem, so the table scrolled sideways. 90rem gives
  the center column 816px (1440 − 64 padding − 64 gaps − 176 nav − 320 news) and the table fits with
  the news panel untouched. TopNav mirrors this value so the brand/menu stay aligned with the content
  below; keep the two in step. /timeline shares the shell and simply gets a wider chart.
*/
export function AppLayout() {
  return (
    <div className="bg-background min-h-screen">
      <TopNav />
      <main className="mx-auto max-w-[90rem] px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}