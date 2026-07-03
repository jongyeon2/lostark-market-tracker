import { Button } from '@/components/ui/button'

import { useAdminAuth } from './auth/AdminAuthContext'
import { CollectionSection } from './CollectionSection'
import { EventSection } from './EventSection'
import { WatchlistSection } from './WatchlistSection'

/*
  The authenticated console shell (D-06). A header bar (Display '관리자 콘솔' left, '로그아웃' right — the
  public TopNav is deliberately NOT reproduced, admin context is explicit) over a single-page vertical
  stack of the three section cards (section gap xl/32). 15-03 / 15-04 replace EventSection /
  WatchlistSection WITHOUT editing this file, so the wave-2/3 plans never touch the shell.
*/
export function AdminConsolePage() {
  const { logout } = useAdminAuth()

  return (
    <div className="bg-background min-h-screen">
      <header className="bg-card border-b">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-8 py-4">
          <span className="text-[28px] leading-tight font-semibold">관리자 콘솔</span>
          <Button variant="outline" onClick={() => logout()}>
            로그아웃
          </Button>
        </div>
      </header>
      <main className="mx-auto max-w-3xl space-y-8 px-8 py-8">
        <EventSection />
        <WatchlistSection />
        <CollectionSection />
      </main>
    </div>
  )
}
