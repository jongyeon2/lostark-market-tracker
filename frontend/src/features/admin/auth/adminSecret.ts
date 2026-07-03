/*
  The ONLY module that reads or writes the admin secret in sessionStorage (D-01, ADMINUI-01).
  sessionStorage (NOT localStorage) is deliberate: a page refresh re-hydrates the session, while
  closing the tab drops the secret — minimizing the exposure window. The value is written only via
  setAdminSecret and is never logged or rendered anywhere (invariant).
*/
const ADMIN_SECRET_KEY = 'lostark.admin.secret'

export function getAdminSecret(): string | null {
  return window.sessionStorage.getItem(ADMIN_SECRET_KEY)
}

export function setAdminSecret(secret: string): void {
  window.sessionStorage.setItem(ADMIN_SECRET_KEY, secret)
}

export function clearAdminSecret(): void {
  window.sessionStorage.removeItem(ADMIN_SECRET_KEY)
}
