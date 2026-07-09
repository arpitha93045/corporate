-- V8: Product tags for the AI Gifting Agent.
--
-- The agent needs to filter the catalog by occasion, dietary, audience, and
-- price band. A single VARCHAR + a namespace prefix keeps the schema flat and
-- H2-compatible (jsonb would be nicer on Postgres but breaks the tests).
--
-- Tag format: "kind:value" where kind is one of:
--   occasion    (diwali, onboarding, anniversary, festival, holiday, offsite)
--   dietary     (vegetarian, vegan, jain, contains-nuts, contains-dairy)
--   audience    (clients, employees, executives, event-giveaway)
--   band        (under-500, 500-1500, 1500-3500, 3500-plus)  -- in rupees
--
-- Backfill is deliberately conservative: better to under-tag a product than
-- to mislead the model. Untagged products still show up in general searches.

CREATE TABLE product_tag (
    product_id BIGINT NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    tag        VARCHAR(64) NOT NULL,
    PRIMARY KEY (product_id, tag)
);

CREATE INDEX ix_product_tag_tag ON product_tag (tag);

-- Backfill: occasion + dietary + audience + price band per product.

-- Hampers / food
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:diwali'), ('occasion:festival'),
    ('dietary:vegetarian'), ('dietary:contains-nuts'), ('dietary:contains-dairy'),
    ('audience:clients'), ('audience:employees')
) AS t(tag) WHERE slug = 'diwali-sweets-hamper';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:festival'), ('occasion:onboarding'),
    ('dietary:vegetarian'), ('dietary:contains-nuts'),
    ('audience:clients'), ('audience:employees')
) AS t(tag) WHERE slug = 'premium-snack-hamper';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:festival'), ('occasion:anniversary'),
    ('dietary:vegetarian'), ('dietary:contains-dairy'),
    ('audience:clients'), ('audience:executives')
) AS t(tag) WHERE slug = 'artisan-chocolate-box';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:festival'), ('occasion:anniversary'),
    ('dietary:vegetarian'), ('dietary:vegan'), ('dietary:jain'),
    ('audience:clients'), ('audience:executives')
) AS t(tag) WHERE slug = 'artisanal-tea-sampler';

-- Drinkware
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:anniversary'),
    ('audience:employees'), ('audience:clients')
) AS t(tag) WHERE slug = 'ceramic-mug-duo';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:offsite'),
    ('audience:employees'), ('audience:event-giveaway')
) AS t(tag) WHERE slug = 'insulated-steel-tumbler';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'), ('occasion:onboarding'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'copper-bottle-1l';

-- Bags
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:anniversary'),
    ('audience:employees'), ('audience:clients')
) AS t(tag) WHERE slug = 'canvas-laptop-backpack';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'), ('occasion:onboarding'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'executive-laptop-bag';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:offsite'),
    ('audience:event-giveaway'), ('audience:employees')
) AS t(tag) WHERE slug = 'drawstring-sport-bag';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'), ('occasion:offsite'),
    ('audience:employees'), ('audience:executives')
) AS t(tag) WHERE slug = 'travel-duffel-35l';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'cabin-trolley-20';

-- Stationery
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'), ('occasion:onboarding'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'engraved-rollerball-pen';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:offsite'),
    ('audience:employees'), ('audience:clients')
) AS t(tag) WHERE slug = 'hardbound-journal-set';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'),
    ('audience:employees')
) AS t(tag) WHERE slug = 'desk-organiser-kit';

-- Tech
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:offsite'),
    ('audience:event-giveaway'), ('audience:employees')
) AS t(tag) WHERE slug = 'bamboo-wireless-power-bank';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'),
    ('audience:employees'), ('audience:clients')
) AS t(tag) WHERE slug = 'wireless-charging-pad';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'noise-cancelling-earbuds';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:anniversary'),
    ('audience:executives'), ('audience:employees')
) AS t(tag) WHERE slug = 'travel-tech-organiser';

-- Home & Living
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:diwali'), ('occasion:festival'), ('occasion:anniversary'),
    ('dietary:vegan'), ('dietary:jain'),
    ('audience:clients'), ('audience:executives')
) AS t(tag) WHERE slug = 'aromatherapy-candle-trio';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:festival'), ('occasion:anniversary'),
    ('dietary:vegan'), ('dietary:jain'),
    ('audience:clients'), ('audience:employees')
) AS t(tag) WHERE slug = 'indoor-plant-gift-box';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'),
    ('dietary:vegan'), ('dietary:jain'),
    ('audience:employees'), ('audience:executives')
) AS t(tag) WHERE slug = 'mindfulness-gift-set';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:offsite'),
    ('dietary:vegan'), ('dietary:jain'),
    ('audience:employees'), ('audience:event-giveaway')
) AS t(tag) WHERE slug = 'yoga-and-recovery-kit';

-- Apparel + welcome kits
INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:offsite'),
    ('audience:employees'), ('audience:event-giveaway')
) AS t(tag) WHERE slug = 'branded-hoodie';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:offsite'),
    ('audience:employees'), ('audience:event-giveaway')
) AS t(tag) WHERE slug = 'custom-polo-shirt';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'),
    ('audience:employees')
) AS t(tag) WHERE slug = 'new-hire-welcome-box';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:onboarding'), ('occasion:anniversary'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'executive-welcome-set';

INSERT INTO product_tag (product_id, tag)
SELECT id, t.tag FROM product, (VALUES
    ('occasion:anniversary'),
    ('audience:executives'), ('audience:clients')
) AS t(tag) WHERE slug = 'vegan-leather-passport-wallet';

-- Price bands: derived, so a single pass rather than 28 manual inserts.
-- price_cents is stored in paise; 1 rupee = 100 paise.
INSERT INTO product_tag (product_id, tag)
SELECT id, 'band:under-500'  FROM product WHERE price_cents <   50000;

INSERT INTO product_tag (product_id, tag)
SELECT id, 'band:500-1500'   FROM product WHERE price_cents >=  50000 AND price_cents < 150000;

INSERT INTO product_tag (product_id, tag)
SELECT id, 'band:1500-3500'  FROM product WHERE price_cents >= 150000 AND price_cents < 350000;

INSERT INTO product_tag (product_id, tag)
SELECT id, 'band:3500-plus'  FROM product WHERE price_cents >= 350000;
