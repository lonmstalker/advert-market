--liquibase formatted sql

--changeset enums-categories:010-01
--comment: Create categories table with JSONB localized names
CREATE TABLE categories (
    id              SERIAL        PRIMARY KEY,
    slug            VARCHAR(100)  NOT NULL UNIQUE,
    localized_name  JSONB         NOT NULL DEFAULT '{}',
    sort_order      INTEGER       DEFAULT 0,
    is_active       BOOLEAN       DEFAULT TRUE,
    created_at      TIMESTAMPTZ   DEFAULT now()
);

--changeset enums-categories:010-02
--comment: Create channel_categories junction table (many-to-many)
CREATE TABLE channel_categories (
    channel_id   BIGINT   NOT NULL REFERENCES channels(id),
    category_id  INTEGER  NOT NULL REFERENCES categories(id),
    PRIMARY KEY (channel_id, category_id)
);
CREATE INDEX idx_channel_categories_category ON channel_categories(category_id);

--changeset enums-categories:010-03
--comment: Migrate existing channels.category data into categories + junction
INSERT INTO categories (slug, localized_name, sort_order)
SELECT DISTINCT category,
       jsonb_build_object('en', category, 'ru', category),
       0
FROM channels
WHERE category IS NOT NULL AND category != ''
ON CONFLICT (slug) DO NOTHING;

INSERT INTO channel_categories (channel_id, category_id)
SELECT c.id, cat.id
FROM channels c
JOIN categories cat ON cat.slug = c.category
WHERE c.category IS NOT NULL AND c.category != '';

--changeset enums-categories:010-04
--comment: Seed standard categories with ru/en localization
INSERT INTO categories (slug, localized_name, sort_order) VALUES
    ('tech',          '{"ru":"Технологии","en":"Technology"}', 1),
    ('crypto',        '{"ru":"Криптовалюта","en":"Cryptocurrency"}', 2),
    ('business',      '{"ru":"Бизнес","en":"Business"}', 3),
    ('marketing',     '{"ru":"Маркетинг","en":"Marketing"}', 4),
    ('education',     '{"ru":"Образование","en":"Education"}', 5),
    ('entertainment', '{"ru":"Развлечения","en":"Entertainment"}', 6),
    ('news',          '{"ru":"Новости","en":"News"}', 7),
    ('lifestyle',     '{"ru":"Лайфстайл","en":"Lifestyle"}', 8),
    ('science',       '{"ru":"Наука","en":"Science"}', 9),
    ('health',        '{"ru":"Здоровье","en":"Health"}', 10),
    ('gaming',        '{"ru":"Игры","en":"Gaming"}', 11),
    ('sports',        '{"ru":"Спорт","en":"Sports"}', 12),
    ('travel',        '{"ru":"Путешествия","en":"Travel"}', 13),
    ('food',          '{"ru":"Еда","en":"Food"}', 14),
    ('fashion',       '{"ru":"Мода","en":"Fashion"}', 15),
    ('music',         '{"ru":"Музыка","en":"Music"}', 16),
    ('humor',         '{"ru":"Юмор","en":"Humor"}', 17),
    ('art',           '{"ru":"Искусство","en":"Art"}', 18)
ON CONFLICT (slug) DO NOTHING;

--changeset enums-categories:010-05
--comment: Drop indexes depending on channels.category, then drop column
DROP INDEX IF EXISTS idx_channels_topic;

--changeset enums-categories:010-05a failOnError:false
--comment: Drop BM25 index that includes category column (ParadeDB only)
DROP INDEX IF EXISTS idx_channels_bm25;

--changeset enums-categories:010-05b
--comment: Drop channels.category column (data migrated to junction)
ALTER TABLE channels DROP COLUMN category;

--changeset enums-categories:010-06
--comment: Create pricing_rule_post_types junction table
CREATE TABLE pricing_rule_post_types (
    pricing_rule_id  BIGINT      NOT NULL REFERENCES channel_pricing_rules(id) ON DELETE CASCADE,
    post_type        VARCHAR(30) NOT NULL,
    PRIMARY KEY (pricing_rule_id, post_type)
);

--changeset enums-categories:010-07
--comment: Migrate channel_pricing_rules.post_type into junction table
INSERT INTO pricing_rule_post_types (pricing_rule_id, post_type)
SELECT id, post_type
FROM channel_pricing_rules
WHERE post_type IS NOT NULL AND post_type != '';

--changeset enums-categories:010-08
--comment: Drop channel_pricing_rules.post_type column (data migrated to junction)
ALTER TABLE channel_pricing_rules DROP COLUMN post_type;

--changeset enums-categories:010-09
--comment: Add index for channel_categories by channel_id (for JOIN from channels)
CREATE INDEX idx_channel_categories_channel ON channel_categories(channel_id);

--changeset enums-categories:010-10 failOnError:false
--comment: Recreate BM25 index without category column (ParadeDB only)
CREATE INDEX idx_channels_bm25 ON channels
    USING bm25 (id, title, description)
    WITH (key_field = 'id');
