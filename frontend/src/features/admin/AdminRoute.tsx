import { AdminAuthProvider, useAdminAuth } from './auth/AdminAuthContext'
import { AdminLoginForm } from './auth/AdminLoginForm'
import { AdminConsolePage } from './AdminConsolePage'

/*
  The /admin full DOM gate (D-05, ADMINUI-06). While anonymous, ONLY <AdminLoginForm/> mounts — the
  console subtree (write forms, tables, buttons) is not created in the DOM at all (not merely
  disabled). Only after a successful login does <AdminConsolePage/> mount. AdminAuthProvider wraps
  both so login/logout state is shared across the gate.
*/
export function AdminRoute() {
  return (
    <AdminAuthProvider>
      <AdminGate />
    </AdminAuthProvider>
  )
}

function AdminGate() {
  const { status } = useAdminAuth()
  return status === 'authenticated' ? <AdminConsolePage /> : <AdminLoginForm />
}
