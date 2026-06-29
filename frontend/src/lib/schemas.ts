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
export const eventTypeSchema = z.enum(['LOA_ON', 'MAJOR_UPDATE', 'SEASON_END', 'BALANCE_PATCH'])
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

export const timelineSchema = z.object({
  downsampled: z.boolean(),
  bucketWidth: z.string().nullable(), // e.g. "1h" when downsampled, null when raw
  snapshots: z.array(pricePointSchema),
  events: z.array(eventPointSchema),
  // Item enrichment as top-level metadata (controller fills via findById; range reads uncached).
  iconUrl: z.string().nullable(),
  itemGroup: z.string().nullable(),
  roleGroup: roleGroupSchema.nullable(),
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
  preAnchorAt: z.string().nullable(),
  postAnchorAt: z.string().nullable(),
  prePrice: z.number().nullable(),
  postPrice: z.number().nullable(),
  changeRate: z.number().nullable(),
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
