-- Squad size moves from 5 (GK+4) to 6 (GK+5): the Formation enum was replaced with 6-man shapes,
-- so persisted 5-man formation names must be mapped onto the new default. Only dev/test lineups
-- exist at this point; finished matches replay from their event timelines and participant rows,
-- which are squad-size agnostic, so history stays watchable.
UPDATE match_lineups
SET formation = 'F_2_2_1'
WHERE formation IN ('F_1_2_1', 'F_2_1_1', 'F_1_1_2', 'F_2_0_2');
