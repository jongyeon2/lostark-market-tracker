import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { ROLE_LABEL, type RoleGroup } from '@/features/_shared/roleGroup'

/*
  RoleBadge — the item's role bucket as a SOLID color pill (역할색 배경 + 흰 텍스트 + 한글 라벨).
  Following the StatusBadge precedent, it overrides the Badge cva via className (tailwind-merge
  lets bg/text win) so ui/badge.tsx stays 0-line (D-03). Color is never shown alone — the Korean
  label always accompanies it. roleGroup === null renders nothing (the item just has no badge).
*/

const ROLE_BG: Record<RoleGroup, string> = {
  DEALER: 'bg-role-dealer',
  SUPPORT: 'bg-role-support',
  MATERIAL: 'bg-role-material',
}

export function RoleBadge({ roleGroup }: { roleGroup: RoleGroup | null }) {
  if (roleGroup == null) return null

  // Inherit Badge's rounded-full / px-2 / py-0.5; override only color + typography.
  return (
    <Badge className={cn(ROLE_BG[roleGroup], 'text-white text-sm font-semibold')}>
      {ROLE_LABEL[roleGroup]}
    </Badge>
  )
}
