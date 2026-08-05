-- Match simulation foundation. Matches are simulated server-side as an event timeline (players
-- watch, they don't control); the team founder manages the lineup and, later, live changes.

-- 1) Founder: the player who picked the location and created the team. Manages match lineups.
--    Existing teams are backfilled with their earliest member (= the registration that founded them).
--    ON DELETE SET NULL: when a founder's account is deleted the seat opens and the next player to
--    join the team claims it (see PlayerService); meanwhile auto-lineups keep matches running.
ALTER TABLE regions ADD COLUMN founder_player_id UUID REFERENCES players (id) ON DELETE SET NULL;

UPDATE regions r
SET founder_player_id = (
    SELECT p.id FROM players p
    WHERE p.region_id = r.id
    ORDER BY p.created_at, p.id
    LIMIT 1
);

-- 2) Matches. One row per fixture; status walks SCHEDULED -> LIVE -> FINISHED (or CANCELLED).
--    seed + sim_minute belong to the engine: seed is the RNG seed fixed at kickoff (with the kickoff
--    stat snapshots it makes the whole match reproducible), sim_minute is the live frontier — clients
--    may only fetch events up to it, so nobody can read the future of a running match.
CREATE TABLE matches (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    home_region_id UUID NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    away_region_id UUID NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    match_type     VARCHAR(12) NOT NULL,
    status         VARCHAR(12) NOT NULL,
    kickoff_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    seed           BIGINT,
    sim_minute     INTEGER NOT NULL DEFAULT 0,
    home_score     INTEGER NOT NULL DEFAULT 0,
    away_score     INTEGER NOT NULL DEFAULT 0,
    finished_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_matches_distinct_teams CHECK (home_region_id <> away_region_id)
);

CREATE INDEX idx_matches_home_kickoff ON matches (home_region_id, kickoff_at);
CREATE INDEX idx_matches_away_kickoff ON matches (away_region_id, kickoff_at);
-- The scheduler's work queue: due SCHEDULED matches, and LIVE matches to advance.
CREATE INDEX idx_matches_status_kickoff ON matches (status, kickoff_at);

-- 3) One lineup per side. auto_generated marks lineups the system built because no founder submitted
--    one; submitted_by is kept for audit (SET NULL if that player's account goes away).
CREATE TABLE match_lineups (
    id                     UUID PRIMARY KEY,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    match_id               UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    region_id              UUID NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    formation              VARCHAR(8) NOT NULL,
    submitted_by_player_id UUID REFERENCES players (id) ON DELETE SET NULL,
    auto_generated         BOOLEAN NOT NULL,
    CONSTRAINT uq_match_lineups UNIQUE (match_id, region_id)
);

-- 4) Participants: the 5 slots per side (slot 0 = GK). player_id NULL = a standard-stat AI filler
--    (teams never play short-handed). The stat columns are the KICKOFF SNAPSHOT of the player's
--    EFFECTIVE attributes (base + body modifiers): frozen so mid-match training can't change a
--    running match, and stored so replays use exactly what the engine saw. NULL until kickoff.
CREATE TABLE match_participants (
    id             UUID PRIMARY KEY,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    match_id       UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    region_id      UUID NOT NULL REFERENCES regions (id) ON DELETE CASCADE,
    player_id      UUID REFERENCES players (id) ON DELETE SET NULL,
    slot           INTEGER NOT NULL,
    role           VARCHAR(4) NOT NULL,
    ai_fill        BOOLEAN NOT NULL,
    display_name   VARCHAR(60),
    kit_number     INTEGER,
    preferred_foot VARCHAR(8),
    pace           INTEGER,
    shooting       INTEGER,
    passing        INTEGER,
    dribbling      INTEGER,
    defending      INTEGER,
    physical       INTEGER,
    goalkeeping    INTEGER,
    curve          INTEGER,
    energy         INTEGER,
    CONSTRAINT uq_match_participants_slot UNIQUE (match_id, region_id, slot)
);

-- A real player appears at most once per match (AI fillers have no player_id and are exempt).
CREATE UNIQUE INDEX uq_match_participants_player
    ON match_participants (match_id, player_id) WHERE player_id IS NOT NULL;

-- 5) Event timeline: what happened, in order. The single source of truth for live viewing AND
--    replays — the Unity client only choreographs these rows, it never decides outcomes itself.
--    payload is per-type JSON (actors, positions, outcome details) kept schemaless on purpose:
--    the engine evolves faster than any column set would.
CREATE TABLE match_events (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    match_id   UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    seq        INTEGER NOT NULL,
    sim_minute INTEGER NOT NULL,
    sim_second INTEGER NOT NULL,
    event_type VARCHAR(24) NOT NULL,
    payload    JSONB,
    CONSTRAINT uq_match_events_seq UNIQUE (match_id, seq)
);
