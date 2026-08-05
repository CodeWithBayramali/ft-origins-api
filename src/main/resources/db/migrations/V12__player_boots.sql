-- Boot designs (client BootCatalog ids 0-19). Design 0 ("Classico") is the free default that every
-- player owns implicitly and wears from registration; the other designs are bought in the market
-- with coins and recorded per player in player_boots. Prices live in code (BootCatalog) so the
-- market can be tuned without touching data.

-- 1) Equipped boot on the player (client dresses the avatar with this id).
ALTER TABLE players ADD COLUMN boot_design_id SMALLINT NOT NULL DEFAULT 0;

-- 2) Ownership: one row per purchased design. Design 0 is never inserted (implicitly owned).
CREATE TABLE player_boots (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    player_id  UUID NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    design_id  SMALLINT NOT NULL,
    CONSTRAINT uq_player_boots UNIQUE (player_id, design_id)
);

CREATE INDEX idx_player_boots_player ON player_boots (player_id);
