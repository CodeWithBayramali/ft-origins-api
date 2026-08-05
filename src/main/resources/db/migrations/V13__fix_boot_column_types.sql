-- V12 created the boot columns as SMALLINT, but the JPA mapping (Kotlin/Java Int) expects
-- INTEGER and ddl-auto=validate rejects the mismatch on startup — the same trap V6 fell into
-- (fixed by V7). Widen both columns to match the mapping.
ALTER TABLE players ALTER COLUMN boot_design_id TYPE INTEGER;
ALTER TABLE player_boots ALTER COLUMN design_id TYPE INTEGER;
