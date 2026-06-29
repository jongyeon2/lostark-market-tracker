import type { RoleGroup } from '@/lib/schemas'

/*
  Single source of truth (D-07) for everything role-bucket related that the 4 screens (14-02)
  reuse: the Korean labels, the canonical display order, and the sort helper. RoleGroup itself
  is owned by the zod layer (schemas.ts) and re-exported here so consumers import role concerns
  from one module.
*/

export type { RoleGroup }

// Korean labels for the 3 role buckets — color is never shown alone, always with this label.
export const ROLE_LABEL: Record<RoleGroup, string> = {
  DEALER: '딜러',
  SUPPORT: '서포터',
  MATERIAL: '융화재료',
}

// Display order: 딜러 → 서포터 → 융화재료, with null (unknown bucket) absorbed at the tail.
const ROLE_ORDER: Record<RoleGroup, number> = {
  DEALER: 0,
  SUPPORT: 1,
  MATERIAL: 2,
}
const NULL_ORDER = 3

function roleRank(role: RoleGroup | null): number {
  return role == null ? NULL_ORDER : ROLE_ORDER[role]
}

/*
  Sort by [role bucket, then displayName (ko-KR locale)] into a NEW array — the input is never
  mutated (D-07). Generic over any item carrying roleGroup + displayName, so the dashboard grid
  and the selector share one ordering. null roleGroup sorts last.
*/
export function sortByRole<T extends { roleGroup: RoleGroup | null; displayName: string }>(
  items: readonly T[],
): T[] {
  return [...items].sort((a, b) => {
    const byRole = roleRank(a.roleGroup) - roleRank(b.roleGroup)
    if (byRole !== 0) return byRole
    return a.displayName.localeCompare(b.displayName, 'ko-KR')
  })
}
