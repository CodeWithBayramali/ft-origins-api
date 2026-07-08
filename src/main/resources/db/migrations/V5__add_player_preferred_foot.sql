-- Preferred (dominant) foot, chosen at character creation. Drives which foot dribbles and
-- strikes the ball in match gameplay. Existing players default to right-footed.
ALTER TABLE players ADD COLUMN preferred_foot VARCHAR(8) NOT NULL DEFAULT 'RIGHT';
