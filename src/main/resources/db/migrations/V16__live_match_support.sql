-- Live match support: founder in-match commands, and participants that change over time
-- (substitutions hand a lineup slot to a new player mid-match).

-- 1) Command log. Determinism depends on this: a match is reproducible from seed + kickoff
--    snapshots + THESE rows (each replayed at its apply_minute), so a rebuilt simulation after a
--    server restart walks the exact same path the live one did.
CREATE TABLE match_commands (
    id                   UUID PRIMARY KEY,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    match_id             UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    region_id            UUID NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    command_type         VARCHAR(16) NOT NULL,
    apply_minute         INTEGER NOT NULL,
    payload              JSONB,
    created_by_player_id UUID REFERENCES players (id) ON DELETE SET NULL
);

CREATE INDEX idx_match_commands_match ON match_commands (match_id, apply_minute);

-- 2) Slot occupancy over time. A participant row now covers [entered_minute, left_minute): starters
--    enter at 0; a substitute takes the SAME slot with entered_minute = the minute the sub applies,
--    while the outgoing row gets left_minute set. Event payloads reference side+slot, so the client
--    resolves "who is slot 3 at minute 70" through these windows.
ALTER TABLE match_participants ADD COLUMN entered_minute INTEGER NOT NULL DEFAULT 0;
ALTER TABLE match_participants ADD COLUMN left_minute INTEGER;

ALTER TABLE match_participants DROP CONSTRAINT uq_match_participants_slot;
ALTER TABLE match_participants
    ADD CONSTRAINT uq_match_participants_slot UNIQUE (match_id, region_id, slot, entered_minute);
