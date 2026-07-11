-- Team kit pattern: the fabric design (solid, stripes, hoops, halves, ...) applied
-- to both the home and away kit, chosen by the founding player at registration.
-- Value is an id into the client-side pattern catalog (0..6). NULL = team created
-- before patterns existed (client falls back to solid / pattern 0).
ALTER TABLE regions ADD COLUMN kit_pattern INTEGER;
