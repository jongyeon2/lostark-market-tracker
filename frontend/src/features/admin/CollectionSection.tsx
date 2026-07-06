import { HealthCard } from '@/features/dashboard/HealthCard'

/*
  API 상태 section (D-08, ADMINUI-05). Reuses the existing self-contained <HealthCard/>
  (GET /api/health/collection — no admin secret, no props, no backend change), which is itself a
  Card titled 'API 상태'. The former outer wrapper Card (a redundant section title) was removed to
  dissolve the section>card nesting — the admin console now shows a single 'API 상태' card. Zero backend touch.
*/
export function CollectionSection() {
  return <HealthCard />
}
