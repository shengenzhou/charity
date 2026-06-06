ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username_configured boolean NOT NULL DEFAULT false;

ALTER TABLE wordle_match
    ADD COLUMN IF NOT EXISTS game_type smallint;

ALTER TABLE charity_donation
    ADD COLUMN IF NOT EXISTS wordle_match_id bigint;

ALTER TABLE charity_donation
    ALTER COLUMN trade_id DROP NOT NULL;
