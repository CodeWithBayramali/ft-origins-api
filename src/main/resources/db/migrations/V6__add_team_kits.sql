-- Team kits: two kit designs per team (region), chosen by the founding player at
-- registration. Values are ids into the client-side KitCatalog (0-9). NULL = the team
-- was created before kits existed (client falls back to the default kit).
ALTER TABLE regions ADD COLUMN kit_home SMALLINT;
ALTER TABLE regions ADD COLUMN kit_away SMALLINT;
