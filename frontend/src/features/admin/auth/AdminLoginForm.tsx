import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'

import { useAdminAuth } from './AdminAuthContext'

/*
  The centered login gate (UI-SPEC Layout → 수직·수평 중앙 Card, max-w-sm). While anonymous this is
  ALL that mounts (D-05) — no console write UI exists in the DOM. The secret lives only in local state
  behind a type="password" input and is never rendered as text, logged, or echoed (invariant).
*/
export function AdminLoginForm() {
  const { notice, login } = useAdminAuth()
  const [secret, setSecret] = useState('')
  const [submitting, setSubmitting] = useState(false)

  return (
    <div className="bg-background flex min-h-screen items-center justify-center px-8 py-12">
      <Card className="w-full max-w-sm">
        <CardHeader className="gap-4">
          <CardTitle className="text-[28px] leading-tight font-semibold">관리자 콘솔</CardTitle>
          <p className="text-muted-foreground text-base">관리자 시크릿을 입력해 로그인하세요.</p>
        </CardHeader>
        <CardContent>
          {/* Enter submits (native form). Fields: label↔input sm/8px, elements md/16px. */}
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault()
              if (submitting) return
              setSubmitting(true)
              void login(secret).finally(() => setSubmitting(false))
            }}
          >
            <div className="space-y-2">
              <label htmlFor="admin-secret" className="text-sm font-semibold">
                관리자 시크릿
              </label>
              <Input
                id="admin-secret"
                type="password"
                autoComplete="current-password"
                placeholder="관리자 시크릿 입력"
                value={secret}
                onChange={(event) => setSecret(event.target.value)}
                disabled={submitting}
              />
            </div>
            {notice && <p className="text-destructive text-base">{notice}</p>}
            <Button type="submit" className="w-full" disabled={submitting || secret.length === 0}>
              {submitting ? '확인 중…' : '로그인'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
