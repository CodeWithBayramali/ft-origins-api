-- Geographic reference data + spatial region (team) + player profile.

CREATE TABLE countries (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE cities (
    geoname_id  BIGINT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    admin1_code VARCHAR(20),
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    population  BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_cities_country ON cities (country_code);

-- A region = an H3 grid cell that behaves as a neighborhood team.
CREATE TABLE regions (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    h3_index        VARCHAR(20) NOT NULL,
    name            VARCHAR(60) NOT NULL,
    name_customized BOOLEAN NOT NULL DEFAULT FALSE,
    center_lat      DOUBLE PRECISION NOT NULL,
    center_lng      DOUBLE PRECISION NOT NULL,
    country_code    VARCHAR(2),
    city_name       VARCHAR(200),
    player_count    INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_regions_h3 UNIQUE (h3_index)
);

CREATE TABLE players (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id      UUID NOT NULL,
    region_id    UUID NOT NULL,
    home_lat     DOUBLE PRECISION NOT NULL,
    home_lng     DOUBLE PRECISION NOT NULL,
    h3_index     VARCHAR(20) NOT NULL,
    display_name VARCHAR(60),
    -- appearance (cosmetic catalog ids)
    skin_tone_id  INTEGER NOT NULL DEFAULT 0,
    hair_style_id INTEGER NOT NULL DEFAULT 0,
    hair_color_id INTEGER NOT NULL DEFAULT 0,
    beard_id      INTEGER NOT NULL DEFAULT 0,
    kit_number    INTEGER NOT NULL DEFAULT 10,
    -- body shape that affects effective attributes (height + weight, balanced trade-off)
    height_cm     INTEGER NOT NULL DEFAULT 178,
    weight_kg     INTEGER NOT NULL DEFAULT 75,
    -- position + base attributes (1-99)
    position      VARCHAR(8) NOT NULL DEFAULT 'MID',
    pace          INTEGER NOT NULL DEFAULT 50,
    shooting      INTEGER NOT NULL DEFAULT 50,
    passing       INTEGER NOT NULL DEFAULT 50,
    dribbling     INTEGER NOT NULL DEFAULT 50,
    defending     INTEGER NOT NULL DEFAULT 50,
    physical      INTEGER NOT NULL DEFAULT 50,
    goalkeeping   INTEGER NOT NULL DEFAULT 45,
    -- progression
    level         INTEGER NOT NULL DEFAULT 1,
    xp            INTEGER NOT NULL DEFAULT 0,
    energy        INTEGER NOT NULL DEFAULT 100,
    energy_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_players_user UNIQUE (user_id),
    CONSTRAINT fk_players_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_players_region FOREIGN KEY (region_id) REFERENCES regions (id)
);
CREATE INDEX idx_players_region ON players (region_id);
CREATE INDEX idx_players_h3 ON players (h3_index);
