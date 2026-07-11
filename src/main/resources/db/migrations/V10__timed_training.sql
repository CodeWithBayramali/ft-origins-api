-- Timed training. A drill now takes real time to complete: START spends energy up-front and opens a
-- single active session (drill + finishesAt); CLAIM (after the duration) applies the stat gains;
-- CANCEL abandons it — no gain (energy stays spent). Energy does not regenerate while a session is
-- active (handled in PlayerUpkeepService). Higher energy + smaller base gains so a high OVR is a long
-- grind. Every value below is tunable from the DB without a redeploy.

-- 1) Drill duration + higher energy, by difficulty (2h / 4h / 8h / 12h).
ALTER TABLE training_drills ADD COLUMN duration_seconds INTEGER NOT NULL DEFAULT 3600;

UPDATE training_drills SET duration_seconds = 7200,  energy_cost = 20 WHERE difficulty = 'LIGHT';
UPDATE training_drills SET duration_seconds = 14400, energy_cost = 35 WHERE difficulty = 'MEDIUM';
UPDATE training_drills SET duration_seconds = 28800, energy_cost = 55 WHERE difficulty = 'HARD';
UPDATE training_drills SET duration_seconds = 43200, energy_cost = 75 WHERE difficulty = 'ELITE';

-- 2) Smaller per-drill gains (timed sessions gate volume; this slows the top end further).
UPDATE training_drill_effects SET base_gain = GREATEST(1, base_gain - 1);

-- 3) Active training session on the player (null = idle / not training).
ALTER TABLE players ADD COLUMN active_drill_id      UUID;
ALTER TABLE players ADD COLUMN training_started_at  TIMESTAMP WITH TIME ZONE;
ALTER TABLE players ADD COLUMN training_finishes_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE players ADD CONSTRAINT fk_players_active_drill
    FOREIGN KEY (active_drill_id) REFERENCES training_drills (id);
