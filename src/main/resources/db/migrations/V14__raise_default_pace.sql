-- Starting pace floor: 35 -> 45. The client maps pace linearly to run speed, and a 35-pace starter
-- was unplayably slow on the pitch. Run-speed feel is tuned HERE (server-owned stat), never by
-- re-calibrating the client's mapping. Existing players are lifted to the same floor for fairness;
-- anyone already above it (trained pace) is untouched.
UPDATE players SET pace = 45 WHERE pace < 45;
