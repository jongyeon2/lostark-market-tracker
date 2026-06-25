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
})
export type TrackedItem = z.infer<typeof trackedItemSchema>

export const trackedItemsSchema = z.array(trackedItemSchema)
export type TrackedItems = z.infer<typeof trackedItemsSchema>

// GET /api/items/{id}/latest
export const latestPriceSchema = z.object({
  itemId: z.number(),
  minPrice: z.number(),
  collectedAt: z.string(),
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

export const eventImpactSchema = z.object({
  itemId: z.number(),
  window: z.number(),
  events: z.array(eventImpactItemSchema),
})
export type EventImpact = z.infer<typeof eventImpactSchema>
