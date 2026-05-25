-- БД: облік результатів змагань з біатлону (курсова)
-- СУБД: сумісний синтаксис з PostgreSQL / SQLite 3.37+ (DROP IF EXISTS)

PRAGMA foreign_keys = ON; -- SQLite

CREATE TABLE IF NOT EXISTS country (
    country_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    iso_code     CHAR(3) NOT NULL UNIQUE,
    name_uk      TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS athlete (
    athlete_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    country_id   INTEGER NOT NULL REFERENCES country (country_id),
    last_name    TEXT NOT NULL,
    first_name   TEXT NOT NULL,
    birth_date   TEXT NOT NULL, -- ISO date YYYY-MM-DD
    gender       TEXT NOT NULL CHECK (gender IN ('M', 'W')),
    UNIQUE (last_name, first_name, birth_date)
);

CREATE TABLE IF NOT EXISTS competition (
    competition_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT NOT NULL,
    venue          TEXT NOT NULL,
    start_date     TEXT NOT NULL,
    end_date       TEXT,
    season_year    INTEGER NOT NULL
);

-- Тип штрафу згідно з правилами IBU для різних форматів
CREATE TABLE IF NOT EXISTS discipline (
    discipline_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name_uk          TEXT NOT NULL,
    distance_km      REAL NOT NULL,
    gender           TEXT NOT NULL CHECK (gender IN ('M', 'W', 'X')),
    rounds_prone     INTEGER NOT NULL DEFAULT 0,
    rounds_standing  INTEGER NOT NULL DEFAULT 0,
    shooting_order   TEXT,
    penalty_type     TEXT NOT NULL CHECK (
        penalty_type IN ('TIME_1MIN', 'TIME_45SEC', 'PENALTY_LOOP', 'RELAY_RULES')
    ),
    UNIQUE (name_uk, distance_km, gender)
);

CREATE TABLE IF NOT EXISTS result (
    result_id             INTEGER PRIMARY KEY AUTOINCREMENT,
    athlete_id            INTEGER NOT NULL REFERENCES athlete (athlete_id),
    competition_id        INTEGER NOT NULL REFERENCES competition (competition_id),
    discipline_id         INTEGER NOT NULL REFERENCES discipline (discipline_id),
    ski_time_seconds      INTEGER NOT NULL,
    misses_prone          INTEGER NOT NULL DEFAULT 0,
    misses_standing       INTEGER NOT NULL DEFAULT 0,
    penalty_time_seconds  INTEGER NOT NULL DEFAULT 0,
    penalty_loops         INTEGER NOT NULL DEFAULT 0,
    final_time_seconds    INTEGER NOT NULL,
    place                 INTEGER,
    leg_number            INTEGER CHECK (leg_number IS NULL OR (leg_number >= 1 AND leg_number <= 4)),
    UNIQUE (athlete_id, competition_id, discipline_id)
);

CREATE INDEX IF NOT EXISTS idx_result_competition ON result (competition_id);
CREATE INDEX IF NOT EXISTS idx_result_athlete ON result (athlete_id);
CREATE INDEX IF NOT EXISTS idx_result_discipline ON result (discipline_id);
