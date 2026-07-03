/*
  The WRITE-side inverse of formatKst (D-08). The admin types 발생 시각 as a KST wall-clock in a native
  `datetime-local` input ('YYYY-MM-DDTHH:mm', zone-less); the backend GameEventRequest.occurredAt is a
  UTC OffsetDateTime. kstLocalToUtcIso subtracts the +9h KST offset and emits a UTC ISO string ending
  in 'Z'; utcIsoToKstLocal is the inverse for edit-form prefill. This closes the off-by-9h guard on the
  INPUT path the way formatKst closed it on display. KST has no DST, so the +9h offset is a fixed
  constant — matching the backend's UTC storage. The instant is the single conversion point (T-1503-01).
*/
const KST_OFFSET_MS = 9 * 60 * 60 * 1000

/** KST wall-clock ('YYYY-MM-DDTHH:mm') → UTC ISO string ('...Z') for submit. */
export function kstLocalToUtcIso(local: string): string {
  const [datePart, timePart] = local.split('T')
  const [year, month, day] = datePart.split('-').map(Number)
  const [hour, minute] = timePart.split(':').map(Number)
  // KST = UTC+9 ⇒ the UTC instant is the KST wall-clock minus 9h; Date.UTC normalizes any underflow.
  return new Date(Date.UTC(year, month - 1, day, hour - 9, minute)).toISOString()
}

/** UTC ISO string → KST 'YYYY-MM-DDTHH:mm' for prefilling the edit form's datetime-local input. */
export function utcIsoToKstLocal(iso: string): string {
  const kst = new Date(new Date(iso).getTime() + KST_OFFSET_MS)
  const yyyy = kst.getUTCFullYear()
  const mm = String(kst.getUTCMonth() + 1).padStart(2, '0')
  const dd = String(kst.getUTCDate()).padStart(2, '0')
  const hh = String(kst.getUTCHours()).padStart(2, '0')
  const mi = String(kst.getUTCMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`
}
