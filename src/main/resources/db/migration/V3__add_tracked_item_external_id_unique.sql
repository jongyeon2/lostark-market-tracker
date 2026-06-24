-- Forward migration (Flyway owns the schema; ddl-auto=validate, D-02/D-14).
-- Enforces the uniqueness the app already ASSUMES on tracked_item.external_item_id: the read path
-- (findByExternalItemId -> Optional), the idempotent WatchlistSeeder upsert, and the admin item
-- create's 409-vs-reactivate branch (D-05) all treat external_item_id as unique, but V1 left it
-- unconstrained. This is a CONSTRAINT-only change (no column add/type change) so ddl-auto=validate
-- stays green and the entity needs no new annotation.
ALTER TABLE tracked_item ADD CONSTRAINT uq_tracked_item_external_id UNIQUE (external_item_id);