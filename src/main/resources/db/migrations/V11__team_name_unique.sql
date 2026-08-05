-- The team name is now chosen by the FOUNDER at registration (not auto-generated), so it has to be
-- unique — otherwise two teams called "Yıldızspor" are indistinguishable in a league table.
--
-- Existing rows were auto-named ("Mavi Kartallar #FFFF") from a tiny 20x18 pool whose suffix never
-- actually varied (an H3 cell address ends in a run of 'f' at this resolution, so every suffix was
-- "FFFF"). Collisions are therefore already present in any populated DB and would make the unique
-- index fail to build. Deduplicate first: keep the oldest holder of each name, and suffix the rest.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY UPPER(name) ORDER BY created_at, id) AS rn
    FROM regions
)
UPDATE regions r
SET name = r.name || ' ' || ranked.rn
FROM ranked
WHERE r.id = ranked.id
  AND ranked.rn > 1;

-- Case-insensitive uniqueness: "Real Madrid" and "real madrid" are the same name. The index is on
-- UPPER(name) so it also BACKS Spring Data's existsByNameIgnoreCase, which generates UPPER(name) = UPPER(?)
-- — a LOWER() index would enforce uniqueness but leave that pre-check doing a full table scan.
CREATE UNIQUE INDEX uq_regions_name_ci ON regions (UPPER(name));
