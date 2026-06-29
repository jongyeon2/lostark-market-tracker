import { useState } from 'react'
import { FlaskConical, Package, ScrollText, type LucideIcon } from 'lucide-react'

import { cn } from '@/lib/utils'
import type { RoleGroup } from '@/features/_shared/roleGroup'

/*
  ItemIcon — the item's CDN icon in a FIXED-px slot with a role-color glyph fallback (ICON-01).
  The slot occupies identical space whether the <img> loads, fails (onError), or is absent
  (iconUrl == null) — so there is zero layout shift in any state (D-06). Real 각인서 icons (11
  share one glyph) are shown as-is; fallback is strictly the null/onError defense (D-05), never a
  deliberate substitution. lucide-react only — no external icon library (D-02).
*/

type IconSize = 'md' | 'sm'

// Fixed dimensions per size: slot footprint, intrinsic <img> px, and fallback glyph size.
const SIZE: Record<IconSize, { slot: string; px: number; glyph: string }> = {
  md: { slot: 'size-8', px: 32, glyph: 'size-5' },
  sm: { slot: 'size-5', px: 20, glyph: 'size-3' },
}

// Fallback glyph + tile per role (D-02): 각인서(딜러/서포터)=ScrollText, 융화재료=FlaskConical,
// 미상=Package on neutral bg. Role tiles wear white glyphs; the neutral tile uses muted text.
type FallbackMeta = { Icon: LucideIcon; tile: string; glyphColor: string }
const FALLBACK: Record<RoleGroup | 'null', FallbackMeta> = {
  DEALER: { Icon: ScrollText, tile: 'bg-role-dealer', glyphColor: 'text-white' },
  SUPPORT: { Icon: ScrollText, tile: 'bg-role-support', glyphColor: 'text-white' },
  MATERIAL: { Icon: FlaskConical, tile: 'bg-role-material', glyphColor: 'text-white' },
  null: { Icon: Package, tile: 'bg-muted', glyphColor: 'text-muted-foreground' },
}

export function ItemIcon({
  iconUrl,
  roleGroup,
  size = 'md',
}: {
  iconUrl: string | null
  roleGroup: RoleGroup | null
  size?: IconSize
}) {
  // One-shot error flag: once <img> fails, we unmount it and render the glyph tile — no retry loop.
  const [errored, setErrored] = useState(false)
  const dim = SIZE[size]
  const showFallback = iconUrl == null || errored

  const slot = cn(
    'shrink-0 rounded-md overflow-hidden inline-flex items-center justify-center',
    dim.slot,
  )

  if (showFallback) {
    const fb = FALLBACK[roleGroup ?? 'null']
    const Glyph = fb.Icon
    return (
      <span className={cn(slot, fb.tile)}>
        <Glyph aria-hidden="true" className={cn(dim.glyph, fb.glyphColor)} />
      </span>
    )
  }

  return (
    <span className={slot}>
      {/* alt="" — decorative: the item name text is always adjacent, so SR would double-read. */}
      <img
        src={iconUrl}
        width={dim.px}
        height={dim.px}
        loading="lazy"
        decoding="async"
        alt=""
        className="size-full object-cover"
        onError={() => setErrored(true)}
      />
    </span>
  )
}
