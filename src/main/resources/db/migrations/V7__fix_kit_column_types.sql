-- V6 created the kit columns as SMALLINT, but the JPA mapping (Kotlin Int?) expects
-- INTEGER and ddl-auto=validate rejects the mismatch. Widen the columns to match.
ALTER TABLE regions ALTER COLUMN kit_home TYPE INTEGER;
ALTER TABLE regions ALTER COLUMN kit_away TYPE INTEGER;
