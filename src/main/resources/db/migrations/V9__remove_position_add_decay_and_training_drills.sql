-- Training & stats overhaul:
--   1. Position is no longer a player attribute (every player can play anywhere, incl. GK; roles
--      are chosen per match).
--   2. Inactivity decay bookkeeping so attributes erode after a grace period without training.
--   3. Fair starting stats: everyone starts equal & low (35), no positional head start.
--   4. A data-driven training drill catalog (editable later via superadmin).
--   5. No RPG levels/XP — progression is pure stat-grinding, paced by diminishing returns + energy.

-- 1) Drop the fixed position and the RPG level/XP columns.
ALTER TABLE players DROP COLUMN position;
ALTER TABLE players DROP COLUMN level;
ALTER TABLE players DROP COLUMN xp;

-- 2) Inactivity decay tracking (see PlayerUpkeepService).
ALTER TABLE players ADD COLUMN last_trained_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE players ADD COLUMN decay_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

-- 3) Fair, low starting stats. New rows use these defaults; reset any pre-existing players so the
--    field stays fair after the rules change.
ALTER TABLE players ALTER COLUMN pace        SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN shooting    SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN passing     SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN dribbling   SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN defending   SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN physical    SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN goalkeeping SET DEFAULT 35;
ALTER TABLE players ALTER COLUMN curve       SET DEFAULT 35;

UPDATE players
   SET pace = 35, shooting = 35, passing = 35, dribbling = 35,
       defending = 35, physical = 35, goalkeeping = 35, curve = 35,
       energy = 100;

-- 4) Training drill catalog.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE training_drills (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    category    VARCHAR(16)  NOT NULL,
    difficulty  VARCHAR(8)   NOT NULL,
    energy_cost INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_training_drills_code UNIQUE (code)
);

CREATE TABLE training_drill_effects (
    drill_id   UUID        NOT NULL,
    attribute  VARCHAR(16) NOT NULL,
    base_gain  INTEGER     NOT NULL,
    CONSTRAINT fk_drill_effects_drill FOREIGN KEY (drill_id) REFERENCES training_drills (id) ON DELETE CASCADE
);
CREATE INDEX idx_drill_effects_drill ON training_drill_effects (drill_id);

-- Seed drills. Energy scales with difficulty (LIGHT ~10, MEDIUM ~18, HARD ~28, ELITE ~35).
INSERT INTO training_drills (id, created_at, updated_at, code, name, category, difficulty, energy_cost, active, sort_order) VALUES
    (gen_random_uuid(), now(), now(), 'FINISHING',           'Finishing',              'ATTACKING',   'MEDIUM', 18, TRUE,  1),
    (gen_random_uuid(), now(), now(), 'CONE_DRIBBLING',       'Cone Dribbling',         'ATTACKING',   'MEDIUM', 18, TRUE,  2),
    (gen_random_uuid(), now(), now(), 'RUN_IN_BEHIND',        'Run In Behind',          'ATTACKING',   'HARD',   28, TRUE,  3),
    (gen_random_uuid(), now(), now(), 'ONE_ON_ONE_FINISH',    'One-on-One Finishing',   'ATTACKING',   'HARD',   30, TRUE,  4),
    (gen_random_uuid(), now(), now(), 'FREEKICK_CURL',        'Free-Kick & Curl',       'ATTACKING',   'HARD',   28, TRUE,  5),
    (gen_random_uuid(), now(), now(), 'VOLLEY_FINISH',        'Volley Finishing',       'ATTACKING',   'ELITE',  36, TRUE,  6),
    (gen_random_uuid(), now(), now(), 'PASSING_RONDO',        'Passing Rondo',          'PLAYMAKING',  'LIGHT',  10, TRUE,  7),
    (gen_random_uuid(), now(), now(), 'THROUGH_BALL',         'Through-Ball Vision',    'PLAYMAKING',  'MEDIUM', 18, TRUE,  8),
    (gen_random_uuid(), now(), now(), 'LONG_SWITCH',          'Long-Ball Switch',       'PLAYMAKING',  'MEDIUM', 20, TRUE,  9),
    (gen_random_uuid(), now(), now(), 'LONG_RANGE_SHOT',      'Long-Range Shooting',    'PLAYMAKING',  'MEDIUM', 18, TRUE, 10),
    (gen_random_uuid(), now(), now(), 'ONE_TOUCH_COMBO',      'One-Touch Combination',  'PLAYMAKING',  'HARD',   26, TRUE, 11),
    (gen_random_uuid(), now(), now(), 'BOX_TO_BOX',           'Box-to-Box Conditioning','PLAYMAKING',  'ELITE',  36, TRUE, 12),
    (gen_random_uuid(), now(), now(), 'MARKING',              'Positioning & Marking',  'DEFENDING',   'LIGHT',  10, TRUE, 13),
    (gen_random_uuid(), now(), now(), 'BUILD_UP_PASS',        'Build-Up Passing',       'DEFENDING',   'MEDIUM', 16, TRUE, 14),
    (gen_random_uuid(), now(), now(), 'JOCKEYING',            '1v1 Jockeying',          'DEFENDING',   'MEDIUM', 18, TRUE, 15),
    (gen_random_uuid(), now(), now(), 'INTERCEPTION_SPRINT',  'Interception Sprint',    'DEFENDING',   'MEDIUM', 20, TRUE, 16),
    (gen_random_uuid(), now(), now(), 'TACKLING',             'Tackling',               'DEFENDING',   'HARD',   28, TRUE, 17),
    (gen_random_uuid(), now(), now(), 'AERIAL_DUELS',         'Aerial Duels',           'DEFENDING',   'HARD',   28, TRUE, 18),
    (gen_random_uuid(), now(), now(), 'HANDLING',             'Handling & Catching',    'GOALKEEPING', 'LIGHT',  10, TRUE, 19),
    (gen_random_uuid(), now(), now(), 'GK_FOOTWORK',          'Footwork & Positioning', 'GOALKEEPING', 'MEDIUM', 18, TRUE, 20),
    (gen_random_uuid(), now(), now(), 'GK_DISTRIBUTION',      'Distribution',           'GOALKEEPING', 'MEDIUM', 18, TRUE, 21),
    (gen_random_uuid(), now(), now(), 'NARROWING_ANGLES',     'Narrowing Angles',       'GOALKEEPING', 'MEDIUM', 20, TRUE, 22),
    (gen_random_uuid(), now(), now(), 'REFLEX_SAVES',         'Reflex Shot-Stopping',   'GOALKEEPING', 'HARD',   28, TRUE, 23),
    (gen_random_uuid(), now(), now(), 'DIVING_SAVES',         'Diving Saves',           'GOALKEEPING', 'ELITE',  34, TRUE, 24),
    (gen_random_uuid(), now(), now(), 'AGILITY_LADDER',       'Agility Ladder',         'PHYSICAL',    'MEDIUM', 18, TRUE, 25),
    (gen_random_uuid(), now(), now(), 'SPRINT_INTERVALS',     'Sprint Intervals',       'PHYSICAL',    'HARD',   26, TRUE, 26),
    (gen_random_uuid(), now(), now(), 'ENDURANCE_RUN',        'Endurance Run',          'PHYSICAL',    'HARD',   26, TRUE, 27),
    (gen_random_uuid(), now(), now(), 'STRENGTH_GYM',         'Strength & Gym',         'PHYSICAL',    'ELITE',  34, TRUE, 28);

-- Seed effects (base gains, pre-diminishing). Single-attribute drills use the difficulty base
-- (LIGHT 2 / MEDIUM 3 / HARD 4 / ELITE 5); two-attribute drills split lower so per-energy value
-- stays fair. Joined to drills by code.
INSERT INTO training_drill_effects (drill_id, attribute, base_gain)
SELECT d.id, e.attribute, e.base_gain
FROM (VALUES
    ('FINISHING',          'SHOOTING',    3),
    ('CONE_DRIBBLING',     'DRIBBLING',   3),
    ('RUN_IN_BEHIND',      'PACE',        4),
    ('ONE_ON_ONE_FINISH',  'SHOOTING',    3),
    ('ONE_ON_ONE_FINISH',  'DRIBBLING',   3),
    ('FREEKICK_CURL',      'CURVE',       3),
    ('FREEKICK_CURL',      'SHOOTING',    3),
    ('VOLLEY_FINISH',      'SHOOTING',    5),
    ('PASSING_RONDO',      'PASSING',     2),
    ('THROUGH_BALL',       'PASSING',     3),
    ('LONG_SWITCH',        'PASSING',     2),
    ('LONG_SWITCH',        'CURVE',       2),
    ('LONG_RANGE_SHOT',    'SHOOTING',    3),
    ('ONE_TOUCH_COMBO',    'PASSING',     3),
    ('ONE_TOUCH_COMBO',    'DRIBBLING',   3),
    ('BOX_TO_BOX',         'PHYSICAL',    4),
    ('BOX_TO_BOX',         'PACE',        4),
    ('MARKING',            'DEFENDING',   2),
    ('BUILD_UP_PASS',      'PASSING',     3),
    ('JOCKEYING',          'DEFENDING',   3),
    ('INTERCEPTION_SPRINT','DEFENDING',   2),
    ('INTERCEPTION_SPRINT','PACE',        2),
    ('TACKLING',           'DEFENDING',   3),
    ('TACKLING',           'PHYSICAL',    3),
    ('AERIAL_DUELS',       'DEFENDING',   3),
    ('AERIAL_DUELS',       'PHYSICAL',    3),
    ('HANDLING',           'GOALKEEPING', 2),
    ('GK_FOOTWORK',        'GOALKEEPING', 2),
    ('GK_FOOTWORK',        'PACE',        2),
    ('GK_DISTRIBUTION',    'GOALKEEPING', 2),
    ('GK_DISTRIBUTION',    'PASSING',     2),
    ('NARROWING_ANGLES',   'GOALKEEPING', 3),
    ('REFLEX_SAVES',       'GOALKEEPING', 4),
    ('DIVING_SAVES',       'GOALKEEPING', 4),
    ('DIVING_SAVES',       'PHYSICAL',    4),
    ('AGILITY_LADDER',     'PACE',        2),
    ('AGILITY_LADDER',     'DRIBBLING',   2),
    ('SPRINT_INTERVALS',   'PACE',        4),
    ('ENDURANCE_RUN',      'PHYSICAL',    4),
    ('STRENGTH_GYM',       'PHYSICAL',    5)
) AS e(code, attribute, base_gain)
JOIN training_drills d ON d.code = e.code;
