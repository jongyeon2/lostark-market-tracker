import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

import { probeAdminSecret, setAdminUnauthorizedHandler } from '@/lib/api'

import { clearAdminSecret, getAdminSecret, setAdminSecret } from './adminSecret'

/*
  The admin session authority. Drives the whole /admin gate:
   - login(secret) runs the side-effect-free probe (D-02): 200 ⇒ persist secret + authenticated,
     401 ⇒ inline rejection copy (ADMINUI-02, stays anonymous), throw ⇒ connection-failure copy.
   - Re-hydrates from sessionStorage on mount so a refresh keeps the session (D-01, ADMINUI-01).
   - Registers the api.ts 401 hook so any admin-request 401 after login auto-logs-out (D-03).
  The secret value never leaves adminSecret.ts + the login input — it is not held in this context.
*/
type AdminStatus = 'anonymous' | 'authenticated'

type AdminAuthValue = {
  status: AdminStatus
  notice: string | null
  login: (secret: string) => Promise<void>
  logout: (reason?: string) => void
}

// UI-SPEC Copywriting Contract — verbatim login-gate copy.
const REJECTED_COPY = '시크릿이 올바르지 않습니다. 다시 확인해 주세요.'
const CONNECTION_COPY =
  '백엔드에 연결하지 못했어요. admin 백엔드(:8080)가 켜져 있는지 확인하고 다시 시도하세요.'
const SESSION_EXPIRED_COPY = '세션이 만료되어 로그아웃되었습니다. 시크릿을 다시 입력해 주세요.'

const AdminAuthContext = createContext<AdminAuthValue | null>(null)

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AdminStatus>(() =>
    getAdminSecret() ? 'authenticated' : 'anonymous',
  )
  const [notice, setNotice] = useState<string | null>(null)

  async function login(secret: string): Promise<void> {
    try {
      const ok = await probeAdminSecret(secret)
      if (ok) {
        setAdminSecret(secret)
        setStatus('authenticated')
        setNotice(null)
      } else {
        // Backend rejected the secret (401) — distinct from a connection failure.
        setNotice(REJECTED_COPY)
      }
    } catch {
      // Network error / 5xx from the probe — the backend is unreachable, not a wrong secret.
      setNotice(CONNECTION_COPY)
    }
  }

  function logout(reason?: string): void {
    clearAdminSecret()
    setStatus('anonymous')
    setNotice(reason ?? null)
  }

  useEffect(() => {
    // Global 401 interceptor (D-03): a rotated/expired secret drops the session back to login.
    setAdminUnauthorizedHandler(() => logout(SESSION_EXPIRED_COPY))
    return () => setAdminUnauthorizedHandler(() => {})
    // Registered once for the provider's lifetime; the handler closes over stable state setters.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <AdminAuthContext.Provider value={{ status, notice, login, logout }}>
      {children}
    </AdminAuthContext.Provider>
  )
}

export function useAdminAuth(): AdminAuthValue {
  const ctx = useContext(AdminAuthContext)
  if (!ctx) {
    throw new Error('useAdminAuth must be used within an AdminAuthProvider')
  }
  return ctx
}
