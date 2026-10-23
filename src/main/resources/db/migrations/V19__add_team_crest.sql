-- Team crest ("arma"): four ids into the client-side crest catalogs (the catalog sizes live in CrestDto
-- only) — the shape, the symbol drawn on it, the base colour and the detail colour the symbol is drawn
-- in. Chosen by the founding player at registration, later changeable by the founder only. All NULL =
-- no crest (teams founded before crests existed, or a founder who skipped it).
-- SMALLINT on purpose: Region pins these columns with @JdbcTypeCode(SMALLINT), so ddl-auto=validate
-- accepts them (a plain Int mapping expects INTEGER — the trap V7 and V13 had to undo).
ALTER TABLE regions ADD COLUMN crest_shape SMALLINT;
ALTER TABLE regions ADD COLUMN crest_symbol SMALLINT;
ALTER TABLE regions ADD COLUMN crest_base SMALLINT;
ALTER TABLE regions ADD COLUMN crest_detail SMALLINT;
