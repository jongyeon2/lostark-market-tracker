-- Forward migration (Flyway owns the schema; ddl-auto=validate, D-02/D-14).
-- Adds a nullable categorical marker column to collection_run for run-level signals like
-- AUTH_ERROR / RATE_LIMITED. It NEVER stores the API key or any secret (D-08); status stays a
-- separate categorical enum-string.
ALTER TABLE collection_run ADD COLUMN summary_message VARCHAR(500);

-- DEFERRED (do NOT create yet): V3__add_price_metrics.sql would add price_snapshot.avg_price
-- (from the list call's YDayAvgPrice, Task 0 / D-06). Phase 2 does NOT collect avg_price, so no
-- V3 migration is created and no code references an avg_price column. Add V3 (not V2) when a
-- future phase actually persists avg_price.
