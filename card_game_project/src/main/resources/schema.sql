-- =====================================================================
-- Схема card_game — реестр карточной игры (в духе Magic: The Gathering)
-- Этот скрипт ИДЕМПОТЕНТЕН: пересоздаёт схему целиком при каждом запуске
-- приложения (если db.initSchema=true). Для ручной инициализации БД
-- смотрите скрипты в папке db/.
-- =====================================================================

DROP SCHEMA IF EXISTS card_game CASCADE;
CREATE SCHEMA card_game;

SET search_path TO card_game;

-- Коллекционеры (владельцы колод)
CREATE TABLE collector (
    id          int PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name        varchar(100) NOT NULL,
    surname     varchar(100) NOT NULL,
    patronymic  varchar(100)
);

-- Художники иллюстраций
CREATE TABLE painter (
    id          int PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name        varchar(100) NOT NULL,
    surname     varchar(100) NOT NULL,
    patronymic  varchar(100)
);

-- Сеты (выпуски карт)
CREATE TABLE card_set (
    id          int PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    code        varchar(20) NOT NULL UNIQUE,
    name        varchar(150) NOT NULL,
    release_year smallint
);

-- Колоды коллекционеров. Имя колоды уникально.
CREATE TABLE deck (
    id            int PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    collector_id  int NOT NULL,
    name          varchar(150) NOT NULL UNIQUE,
    CONSTRAINT fk_deck_collector
        FOREIGN KEY (collector_id) REFERENCES collector(id)
        ON DELETE CASCADE ON UPDATE RESTRICT
);

-- Способности (эффекты). У карты их может быть несколько.
CREATE TABLE ability (
    id          int PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    description text NOT NULL
);

-- Каталог карт. Карта принадлежит сету и нарисована художником.
CREATE TABLE card (
    id              bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    painter_id      int NOT NULL,
    set_id          int NOT NULL,
    name            varchar(150) NOT NULL,
    rarity          smallint NOT NULL DEFAULT 1
        CONSTRAINT chk_card_rarity_floor CHECK (rarity > 0)
        CONSTRAINT chk_card_rarity_top   CHECK (rarity <= 10),
    activation_cost smallint NOT NULL
        CONSTRAINT chk_card_activation_cost CHECK (activation_cost >= 0),
    magic_color     varchar(100) NOT NULL,
    type            varchar(100) NOT NULL,
    description     text,
    CONSTRAINT fk_card_painter
        FOREIGN KEY (painter_id) REFERENCES painter(id) ON UPDATE RESTRICT,
    CONSTRAINT fk_card_set
        FOREIGN KEY (set_id) REFERENCES card_set(id) ON UPDATE RESTRICT
);

-- Связь карта<->способность (у карты несколько эффектов)
CREATE TABLE card_ability (
    card_id    bigint NOT NULL,
    ability_id int NOT NULL,
    CONSTRAINT pk_card_ability PRIMARY KEY (card_id, ability_id),
    CONSTRAINT fk_card_ability_card
        FOREIGN KEY (card_id) REFERENCES card(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_card_ability_ability
        FOREIGN KEY (ability_id) REFERENCES ability(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- Состав колоды: сколько экземпляров конкретной карты лежит в колоде.
CREATE TABLE deck_card (
    deck_id  int NOT NULL,
    card_id  bigint NOT NULL,
    quantity int NOT NULL DEFAULT 1
        CONSTRAINT chk_deck_card_quantity CHECK (quantity > 0),
    CONSTRAINT pk_deck_card PRIMARY KEY (deck_id, card_id),
    CONSTRAINT fk_deck_card_deck
        FOREIGN KEY (deck_id) REFERENCES deck(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_deck_card_card
        FOREIGN KEY (card_id) REFERENCES card(id)
        ON DELETE CASCADE ON UPDATE RESTRICT
);
