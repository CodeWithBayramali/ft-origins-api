-- Curve / "falso": how much bend a player can put on a struck ball. Trainable attribute,
-- same 1-99 scale as the others. Existing players start at 50 (neutral).
ALTER TABLE players ADD COLUMN curve INT NOT NULL DEFAULT 50;
