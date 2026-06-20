# Task 0 — Lostark markets API verification findings

**Date:** 2026-06-20
**Endpoint base:** `https://developer-lostark.game.onstove.com`
**Auth:** `Authorization: bearer {JWT}` (key from env only; never committed)
**How captured:** `MarketsApiSpikeTest` (spike profile, `@Disabled`) via `LostarkSpikeClient` (RestClient, D-03), cross-checked with direct `curl`. Real calls made once.

---

## (a) Request format + items per request

**Search (list):** `POST /markets/items`
```json
{ "CategoryCode": 50010, "Sort": "CURRENT_MIN_PRICE", "PageNo": 1, "SortCondition": "ASC", "ItemName": "<optional>" }
```
- `CategoryCode` is **required and must be a leaf** category. A parent code (e.g. `50000` 강화 재료) returns `TotalCount: 0`; the leaf `50010` (재련 재료) returns items. Leaf codes come from `GET /markets/options` (`Categories[].Subs[].Code`).
- `ItemName` is an optional filter; omit it to list the whole category.
- **Items per request: `PageSize` = 10**, paged via `PageNo` (1-based in the response). Example: 재련 재료 → `TotalCount: 33` across 4 pages.

**Detail:** `GET /markets/items/{id}` → returns a JSON **array** of `{ Name, TradeRemainCount, BundleCount, Stats[], ToolTip }`.

**Rate-limit headers (present):** `x-ratelimit-limit: 100`, `x-ratelimit-remaining`, `x-ratelimit-reset` (epoch seconds).

---

## (b) Field-availability matrix

Real list item (`POST /markets/items`, 재련 재료):
```json
{"CurrentMinPrice":1,"Id":66102101,"Name":"수호석 조각","Grade":"일반",
 "Icon":"https://...","BundleCount":100,"TradeRemainCount":null,
 "YDayAvgPrice":0.0,"RecentPrice":1}
```
Real detail (`GET /markets/items/66102101`):
```json
[{"Name":"수호석 조각","TradeRemainCount":null,"BundleCount":100,
  "Stats":[{"Date":"2026-06-20","AvgPrice":1.0,"TradeCount":13},
           {"Date":"2026-06-16","AvgPrice":1.0,"TradeCount":896}, ...]}]
```

| Candidate field | Source | Present? | Actual JSON key | Notes |
|---|---|---|---|---|
| current lowest price | list | ✅ PRESENT | `CurrentMinPrice` | real-time per call → maps to `price_snapshot.min_price` |
| recent trade price | list | ✅ PRESENT | `RecentPrice` | last transaction price |
| stable item id | list | ✅ PRESENT | `Id` (int, e.g. `66102101`) | → `external_item_id` |
| display name | list / detail | ✅ PRESENT | `Name` | → `display_name` |
| grade | list | ✅ PRESENT | `Grade` | 일반/고급/… |
| bundle count | list / detail | ✅ PRESENT | `BundleCount` | price is **per bundle** (normalize per-unit when comparing) |
| purchase-limit remaining | list / detail | ⚠️ usually `null` | `TradeRemainCount` | a buy limit, **not** trade volume |
| **avg_price** | list (prior-day) | ✅ PRESENT | `YDayAvgPrice` | prior-day daily average; free with the list call |
| **avg_price** (per day series) | **detail** | ✅ PRESENT | `Stats[].AvgPrice` | **daily** granularity |
| **trade_count** | **detail** | ✅ PRESENT | `Stats[].TradeCount` | **daily** granularity (거래량) |
| avg_price (intraday / per-tick) | — | ❌ ABSENT | — | not provided at tick granularity |
| trade_count (intraday / per-tick) | — | ❌ ABSENT | — | not provided at tick granularity |

**D-06 verdict:** `avg_price` and `trade_count` **are both provided**, but at **DAILY** granularity via the per-item detail endpoint (`Stats[]`). The list/search endpoint gives only `YDayAvgPrice` (prior-day daily avg) + `CurrentMinPrice` + `RecentPrice` — **no intraday volume**. They were deferred, not cancelled → a follow-up migration is specified below.

---

## (c) external_item_id matching rule (D-05 / DATA-03)

**Decision: a stable numeric `Id` exists → store `external_item_id` + `display_name`.**
- `external_item_id` = `String.valueOf(Id)` (e.g. `"66102101"`), `display_name` = `Name`.
- Collection resolves a watchlisted item by its `Id` directly (`GET /markets/items/{id}`) or by `CategoryCode` + `ItemName` (list). No fuzzy name/category/grade/bundle combo is needed.
- **Risk:** low — `Id` is stable per item. The only normalization concern is `BundleCount` (price is per bundle), which is a collection-layer per-unit concern, not a matching concern.

This satisfies DATA-03 (unambiguous insert/lookup by `external_item_id` + `display_name`).

---

## (d) Rate-limit reality

- **100 requests / minute / key**, confirmed by `x-ratelimit-limit: 100` (matches the CLAUDE.md estimate).
- The API **does** return `x-ratelimit-limit`, `x-ratelimit-remaining`, `x-ratelimit-reset` — Phase 2's token bucket can be reconciled against real headers (and `Retry-After` on 429).
- A 20–30 item watchlist polled every 10 min (≤30 list calls/tick) sits comfortably under 100/min.

**Auth gotcha discovered:** the key value must be the **JWT only** ("bearer " is added by the client). Pasting a key copied from a wrapped display injected literal spaces into the token (3 mid-token `0x20`), corrupting the signature → `401 Authorization has been denied`. `LostarkSpikeClient` now strips a leading `bearer ` and **all whitespace** from the configured key.

---

## (e) Task 0 exit-gate verdict — **PASS**

- ✅ The collection unit works: one list call yields a watchlisted item's `CurrentMinPrice` (+ `YDayAvgPrice` for free); no heavy per-item paging required.
- ✅ Stable `Id` → unambiguous matching (D-05 resolved).
- ✅ Rate limit (100/min) comfortably covers the watchlist every 10 min.
- ✅ Volume + average are **not absent** — they are available at daily granularity (detail `Stats[]`).
- ✅ No negative-branch trigger (no multi-item-per-search ambiguity, no missing volume+avg).

---

## Model-lock recommendation (to ratify at the Task 3 checkpoint)

1. **`price_snapshot.min_price`** — keep, from `CurrentMinPrice` (real-time, per tick). ✅ already in V1.
2. **Add `avg_price`** to `price_snapshot` from **`YDayAvgPrice`** — free with the list call, no extra request. It is the prior-day daily average (constant within a day) kept alongside each snapshot as cheap context.
   → **Follow-up `V2__add_price_metrics.sql`**: `ALTER TABLE price_snapshot ADD COLUMN avg_price BIGINT;` + add `private Long avgPrice;` (`@Column(name="avg_price")`) to `PriceSnapshot`.
3. **`trade_count`** — **leave OUT of per-tick `price_snapshot`.** It is only available **daily** (detail `Stats[].TradeCount`); fetching it per tick means a 2nd call per item per tick (doubling rate-limit cost) for a value that only changes once a day.
   → If event-impact (Phase 5) needs volume, collect daily `Stats` into a **separate daily table** (e.g. `item_daily_stats(item_id, stat_date, avg_price, trade_count)`) on a once-daily schedule. Defer to Phase 5 / v2.
4. **Open decision for the developer:** ratify (2) + (3) as above, or choose to also persist daily `trade_count` now via the separate daily-stats table.

---

*Phase: 01-foundation-task-0 · Plan 01-03 · Task 0 spike*
