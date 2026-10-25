-- The tallest a player can be is now 190 cm (was 210). Players already taller are brought down to it,
-- so no stored height lies outside what the character screen offers or the API accepts.
UPDATE players SET height_cm = 190 WHERE height_cm > 190;
