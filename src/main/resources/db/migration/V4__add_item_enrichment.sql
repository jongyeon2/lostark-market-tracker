-- V4: Read-path additive enrichment for tracked_item (Item Visual/Data Enrichment, v1.2).
-- Adds nullable icon_url / item_group / role_group transcribed from the Phase 12 spike-locked
-- 6-field curation (12-SPIKE-FINDINGS.md (c)). V1–V3 stay immutable (Flyway checksums unchanged);
-- this is a pure additive ADD COLUMN so ddl-auto=validate (D-02) stays green once the entity maps them.
-- All three columns are NULLABLE: enrichment is optional display metadata, never required by the
-- collect / cache / event-impact paths. PostgreSQL types only (no MySQL types).
ALTER TABLE tracked_item
    ADD COLUMN icon_url   VARCHAR(300),
    ADD COLUMN item_group VARCHAR(50),
    ADD COLUMN role_group VARCHAR(20);
