-- Coins get earning paths (the daily reward, match rewards) and a ledger; inactivity decay is softened.

-- 1) The coin ledger. Every change to a balance is a row; (user, reason, reference) names one grant
--    or one purchase — the day of a daily reward, the match a reward was for, the boot design bought —
--    so it can happen at most once however many times the code path runs.
CREATE TABLE coin_transactions (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount     INTEGER NOT NULL,
    reason     VARCHAR(24) NOT NULL,
    reference  VARCHAR(64) NOT NULL,
    CONSTRAINT uq_coin_transactions_grant UNIQUE (user_id, reason, reference)
);

CREATE INDEX idx_coin_transactions_user_created ON coin_transactions (user_id, created_at);

-- 2) The daily reward: the last (UTC) day it was claimed and the day of the 7-day ladder it paid.
ALTER TABLE users ADD COLUMN daily_reward_day DATE;
ALTER TABLE users ADD COLUMN daily_reward_streak INTEGER NOT NULL DEFAULT 0;

-- 3) Soft decay: how many points each attribute has lost to inactivity and not yet trained back.
--    Decay stops at 5 of them, and training wins them back twice as fast ("muscle memory").
ALTER TABLE players ADD COLUMN decay_debt_pace INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_shooting INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_passing INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_dribbling INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_defending INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_physical INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_goalkeeping INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN decay_debt_curve INTEGER NOT NULL DEFAULT 0;
