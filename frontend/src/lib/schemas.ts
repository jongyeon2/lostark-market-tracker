import { z } from 'zod'

/*
  zod is the SINGLE source of truth for the 5 read DTOs (D-05): the TypeScript types are
  derived with z.infer — types and validation never drift. Schemas are hand-written 1:1
  against the REQUIREMENTS "실제 API 계약" table (backend unchanged, no OpenAPI codegen).

  All timestamps are UTC ISO-8601 strings ("...Z") — formatting to KST is formatKst()'s job.
  Nullable backend fields are modeled with .nullable() so insufficient-data nulls fail
  loudly at the boundary instead of silently poisoning a later chart (D-06).
  No .strict(): unknown keys strip so a future backend field stays forward-compatible.
*/

// EventType enum — shared by timeline events and event-impact items.
export const eventTypeSchema = z.enum([
  'LOA_ON',
  'MAJOR_UPDATE',
  'SEASON_END',
  'BALANCE_PATCH',
  // v1.5(EVT-01) additive — 백엔드 EventType과 1:1
  'NEW_CLASS',
  'NEW_RAID',
  'GENERAL_PATCH',
])
export type EventType = z.infer<typeof eventTypeSchema>

// RoleGroup enum — the item's role bucket, shared by all 4 read DTOs' enrichment fields.
// Backend types it as a free String but only ever fills DEALER/SUPPORT/MATERIAL or null
// (13-02). Modeling it as z.enum makes an unknown value fail LOUDLY at the .parse boundary
// (D-06) rather than silently flowing into a badge/sort as a bad bucket.
export const roleGroupSchema = z.enum(['DEALER', 'SUPPORT', 'MATERIAL'])
export type RoleGroup = z.infer<typeof roleGroupSchema>

// GET /api/health/collection
export const collectionHealthSchema = z.object({
  lastRunAt: z.string().nullable(),
  startedAt: z.string().nullable(),
  itemsAttempted: z.number(),
  itemsSucceeded: z.number(),
  itemsFailed: z.number(),
  status: z.string(), // NO_RUNS | PARTIAL_SUCCESS | (그 외 정상)
  summaryMessage: z.string().nullable(), // AUTH_ERROR | RATE_LIMITED | null
})
export type CollectionHealth = z.infer<typeof collectionHealthSchema>

// GET /api/items  ->  array
export const trackedItemSchema = z.object({
  id: z.number(),
  externalItemId: z.string(),
  displayName: z.string(),
  category: z.string(),
  active: z.boolean(),
  // Static enrichment (Phase 13, top-level): CDN icon URL, freeform itemGroup ("각인서"…),
  // and the role bucket. null when the seed left them unset; roleGroup loud-fails on unknown.
  iconUrl: z.string().nullable(),
  itemGroup: z.string().nullable(),
  roleGroup: roleGroupSchema.nullable(),
})
export type TrackedItem = z.infer<typeof trackedItemSchema>

export const trackedItemsSchema = z.array(trackedItemSchema)
export type TrackedItems = z.infer<typeof trackedItemsSchema>

// GET /api/items/{id}/latest
export const latestPriceSchema = z.object({
  itemId: z.number(),
  minPrice: z.number(),
  collectedAt: z.string(),
  // Enrichment baked alongside the price on a cache MISS, so a HIT returns it with no extra
  // DB read (API-02 zero-DB showcase preserved).
  iconUrl: z.string().nullable(),
  itemGroup: z.string().nullable(),
  roleGroup: roleGroupSchema.nullable(),
})
export type LatestPrice = z.infer<typeof latestPriceSchema>

// GET /api/items/{id}/prices?from=&to=
export const pricePointSchema = z.object({
  collectedAt: z.string(),
  minPrice: z.number(),
  sampleCount: z.number().nullable(), // null on raw (non-downsampled) points
})
export type PricePoint = z.infer<typeof pricePointSchema>

export const eventPointSchema = z.object({
  occurredAt: z.string(),
  eventType: eventTypeSchema,
  title: z.string(),
})
export type EventPoint = z.infer<typeof eventPointSchema>

// Backfill daily-average point (Phase 17.4). A DIFFERENT metric from a snapshot: statDate is a
// calendar day "YYYY-MM-DD" (KST) and avgPrice is that day's traded AVG (not a min ask), tagged by
// source. source is z.enum so an unknown provenance fails loudly at .parse (roleGroup precedent).
export const dailyStatSourceSchema = z.enum(['YDAY_AVG', 'DETAIL_STATS'])
export type DailyStatSource = z.infer<typeof dailyStatSourceSchema>

export const dailyStatPointSchema = z.object({
  statDate: z.string(), // date-only "YYYY-MM-DD" (KST) — positioned at KST midnight, not via formatKst
  avgPrice: z.number(),
  source: dailyStatSourceSchema,
})
export type DailyStatPoint = z.infer<typeof dailyStatPointSchema>

export const timelineSchema = z.object({
  downsampled: z.boolean(),
  bucketWidth: z.string().nullable(), // e.g. "1h" when downsampled, null when raw
  snapshots: z.array(pricePointSchema),
  events: z.array(eventPointSchema),
  // Item enrichment as top-level metadata (controller fills via findById; range reads uncached).
  iconUrl: z.string().nullable(),
  itemGroup: z.string().nullable(),
  roleGroup: roleGroupSchema.nullable(),
  // Backfilled daily averages, a SEPARATE series from snapshots (D-02 no-mix). .default([]) keeps an
  // older backend (no backfill field) forward-compatible instead of failing the parse.
  backfill: z.array(dailyStatPointSchema).default([]),
})
export type Timeline = z.infer<typeof timelineSchema>

// GET /api/items/{id}/event-impact?window=N
export const eventImpactStatusSchema = z.enum(['ok', 'insufficient_data'])
export type EventImpactStatus = z.infer<typeof eventImpactStatusSchema>

export const eventImpactItemSchema = z.object({
  id: z.number(),
  eventType: eventTypeSchema,
  title: z.string(),
  occurredAt: z.string(),
  status: eventImpactStatusSchema,
  // All anchor/price/changeRate fields are null when status is insufficient_data —
  // modeled explicitly so a null can never masquerade as a real number downstream.
  // Anchor TIMES are also null on a DAILY_AVG row: a daily average belongs to a date, not an instant.
  preAnchorAt: z.string().nullable(),
  postAnchorAt: z.string().nullable(),
  prePrice: z.number().nullable(),
  postPrice: z.number().nullable(),
  changeRate: z.number().nullable(),
  // Which measurement changeRate came from (Phase 25): SNAPSHOT_MIN = 10분 최저 호가(기본),
  // DAILY_AVG = 백필 일별 체결 평균(스냅샷이 없는 과거 이벤트). z.enum so an unknown source fails
  // loudly at .parse rather than being rendered as an unlabeled number (roleGroup precedent).
  anchorSource: z.enum(['SNAPSHOT_MIN', 'DAILY_AVG']).nullable(),
})
export type EventImpactItem = z.infer<typeof eventImpactItemSchema>

// FLAT shape, aligned to the real backend EnrichedEventImpactResponse record
// ({ itemId, window, iconUrl, itemGroup, roleGroup, events }) — the controller wraps the
// unchanged EventImpactResponse with top-level enrichment. NOT a nested `enrichment` object
// (CONTEXT/UI-SPEC's nested wording is inaccurate; the backend record is the source of truth).
export const eventImpactSchema = z.object({
  itemId: z.number(),
  window: z.number(),
  iconUrl: z.string().nullable(),
  itemGroup: z.string().nullable(),
  roleGroup: roleGroupSchema.nullable(),
  events: z.array(eventImpactItemSchema),
})
export type EventImpact = z.infer<typeof eventImpactSchema>

// ---- Admin WRITE DTOs (Phase 15) ----
// The zod boundary (D-05/06) extended to the admin write path: a GameEventResponse element of
// GET /api/admin/events (and the POST/PUT return). All instants are UTC ISO '...Z' strings —
// formatKst renders them for display; description is nullable. reuses eventTypeSchema.
export const gameEventResponseSchema = z.object({
  id: z.number(),
  eventType: eventTypeSchema,
  title: z.string(),
  occurredAt: z.string(),
  description: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
})
export type GameEventResponse = z.infer<typeof gameEventResponseSchema>

export const gameEventsSchema = z.array(gameEventResponseSchema)
export type GameEvents = z.infer<typeof gameEventsSchema>

// POST/PUT body — only the 4 client-settable fields. id/createdAt/updatedAt are entity-stamped by
// the backend (bound as GameEventRequest, never the entity), so they are not sendable from the client.
export type AdminEventRequest = {
  eventType: EventType
  title: string
  occurredAt: string
  description?: string
}

// POST /api/admin/items body (ADMINUI-04) — create OR reactivate: the backend branches on
// externalItemId (same id → reactivate a soft-deleted row, D-13). id/active are entity-controlled
// (bound as TrackedItemRequest, never the entity). The response reuses trackedItemSchema/
// trackedItemsSchema — no new response schema.
export type AdminItemRequest = {
  externalItemId: string
  displayName: string
  category?: string
}

// ---- News panel read DTO (Phase 17.2) ----
// GET /api/news — Lostark official events + notices, served from the backend Redis cache. The
// frontend consumes ONLY this backend contract, never Lostark directly (D-06). Fields transcribe
// the live-locked schema (17.2-NEWS-SPIKE-FINDINGS): events carry title/link/기간/thumbnail, notices
// carry title/link/date/type. UNLIKE every other DTO here, these dates are the source's ISO-8601
// LOCAL (KST) strings WITHOUT a 'Z' offset — they are displayed as-is (string slice), NOT via
// formatKst (which assumes UTC and would add 9h). updatedAt is null on a cold/never-filled cache
// so the panel shows an honest empty/loading state (D-07). type is an opaque Korean category string
// (공지/점검/…) rendered as a neutral badge — deliberately NOT a z.enum (the value set is not locked).
export const newsEventSchema = z.object({
  title: z.string(),
  link: z.string(),
  startDate: z.string(),
  endDate: z.string(),
  thumbnail: z.string().nullable().optional(), // display-only; 1차 텍스트 표 미사용
})
export type NewsEvent = z.infer<typeof newsEventSchema>

export const newsNoticeSchema = z.object({
  title: z.string(),
  link: z.string(),
  date: z.string(),
  type: z.string(),
})
export type NewsNotice = z.infer<typeof newsNoticeSchema>

export const newsResponseSchema = z.object({
  events: z.array(newsEventSchema),
  notices: z.array(newsNoticeSchema),
  updatedAt: z.string().nullable(),
})
export type NewsResponse = z.infer<typeof newsResponseSchema>

// ---- Coupon read/write DTOs (Phase 17.3) ----
// GET /api/coupons (public, unexpired soonest-first) and GET /api/admin/coupons (admin, includes
// expired) return CouponResponse elements. UNLIKE the UTC '...Z' instants elsewhere, expiresAt is a
// date-only "YYYY-MM-DD" string (backend LocalDate, D-01) — it is displayed by string slice, NEVER
// via formatKst (which assumes a UTC instant and would add 9h). createdAt/updatedAt ARE UTC '...Z'.
export const couponSchema = z.object({
  id: z.number(),
  code: z.string(),
  reward: z.string(),
  // 기간. Both are date-only "YYYY-MM-DD" (D-01) — display via slice(0, 10), not formatKst.
  // startsAt is nullable: coupons registered before V8 have no start date and one is never invented.
  startsAt: z.string().nullable(),
  expiresAt: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
})
export type Coupon = z.infer<typeof couponSchema>

export const couponsSchema = z.array(couponSchema)
export type Coupons = z.infer<typeof couponsSchema>

// POST/PUT body — only the client-settable fields. id/createdAt/updatedAt are entity-stamped by the
// backend (bound as CouponRequest, never the entity). Dates are sent as the raw "YYYY-MM-DD" from the
// <input type="date"> value — NO KST↔UTC conversion (D-01). startsAt is optional; null clears it
// (PUT is a full replace, not a patch). The backend rejects startsAt after expiresAt with a 400.
export type AdminCouponRequest = {
  code: string
  reward: string
  startsAt: string | null
  expiresAt: string
}

// ---- 보석 현재가 read DTO (Phase 26, GEM-02) ----
// GET /api/gems — 티어4 보석 6종(겁화·작열 × 8/9/10)의 최저 즉시구매가, 백엔드 Redis 스냅샷에서 서빙.
// 프론트는 이 계약만 소비하고 경매장을 직접 호출하지 않는다(D-06).
//
// 거래소 DTO와 다른 점(Phase 24 실측): 보석엔 id가 없다 — 경매장 응답에 Id 필드 자체가 부재해서
// series+level이 키다. 그래서 시계열도 없고 타임라인 딥링크도 없다(행이 링크가 아닌 이유).
// updatedAt은 스냅샷을 만든 UTC 순간('...Z') — formatKst로 표시한다(뉴스/쿠폰의 오프셋 없는 로컬
// 문자열과 반대). 캐시 없이 fail-open으로 서빙되면 null일 수 있다.
// RATE_LIMITED는 고장이 아니라 설계된 양보다 — 경매장이 10분 수집과 레이트리밋 버킷을 공유하므로
// (Phase 24 §H2) 수집이 예산을 쓰는 중이면 보석이 조회를 포기한다. FETCH_FAILED와 분리해 안내 문구를
// 다르게 준다("잠시 후 다시" vs "불러오지 못함").
export const gemPriceStatusSchema = z.enum(['OK', 'NO_BUYOUT', 'RATE_LIMITED', 'FETCH_FAILED'])
export type GemPriceStatus = z.infer<typeof gemPriceStatusSchema>

export const gemPriceSchema = z.object({
  series: z.string(), // 겁화 | 작열 — 그룹 헤더용. 자유 문자열(계열 추가에 열려 있음)
  level: z.number(), // 보석 레벨 8·9·10 (경매장 응답의 Level=1640 아이템레벨이 아님)
  displayName: z.string(), // "8레벨" — 계열은 그룹 헤더가 말하므로 레벨만
  iconUrl: z.string(),
  // OK일 때만 non-null. status와 함께 움직여 null이 0인 척할 수 없다(insufficient_data 선례).
  minBuyPrice: z.number().nullable(),
  status: gemPriceStatusSchema, // z.enum → 미지의 상태는 .parse에서 loud-fail (roleGroup 선례)
})
export type GemPrice = z.infer<typeof gemPriceSchema>

export const gemsResponseSchema = z.object({
  gems: z.array(gemPriceSchema),
  updatedAt: z.string().nullable(),
})
export type GemsResponse = z.infer<typeof gemsResponseSchema>
