/*
  formatKst — UTC ISO-8601 → KST for DISPLAY only. The off-by-9h guard carried from the
  backend's honest-data discipline to the UI (FND-05, D-09):

    DISPLAY = KST via native Intl.DateTimeFormat({ timeZone: 'Asia/Seoul' })
    MATH    = raw UTC epoch ms (toEpochMs) — chart x-axis position & sorting stay on the
              UTC instant; only the visible label is KST-formatted.

  The instant is NEVER hand-shifted by +9h — Intl owns the conversion (DST/edge-correct).
  No third-party timezone library is imported; the native Intl API is the only dependency.
*/

const kstFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

/** Format a UTC ISO-8601 string (or epoch ms) as a readable KST timestamp. */
export function formatKst(value: string | number): string {
  return kstFormatter.format(new Date(value))
}

/**
 * Raw UTC epoch ms for chart x-axis positioning and sorting. Position math MUST stay on
 * this UTC instant (never the KST-shifted value) so axes never drift by 9 hours.
 */
export function toEpochMs(iso: string): number {
  return new Date(iso).getTime()
}
