-- V5: Expand catalog with more categories and products inspired by typical
-- corporate gifting catalogs (offineeds.com etc.). Prices stored in paise
-- (column is named price_cents but we treat 100 = ₹1) so 271500 = ₹2,715.

INSERT INTO category (name, slug) VALUES
    ('Bags',             'bags'),
    ('Stationery',       'stationery'),
    ('Travel',           'travel'),
    ('Home & Living',    'home-and-living');

INSERT INTO product (category_id, name, slug, description, price_cents, image_url, in_stock) VALUES
    -- Bags
    ((SELECT id FROM category WHERE slug = 'bags'),
     'Canvas Laptop Backpack', 'canvas-laptop-backpack',
     'Tan canvas 23L backpack with padded 15.6" laptop sleeve, side pockets, and a debossed-leather logo patch. A clean, recipient-friendly daily carry.',
     271500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'bags'),
     'Executive Laptop Bag', 'executive-laptop-bag',
     'Slim vegan-leather messenger with a padded 14" laptop section, organiser panel, and detachable shoulder strap. Pairs well with CXO gifting.',
     304500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'bags'),
     'Drawstring Sport Bag', 'drawstring-sport-bag',
     'Lightweight drawstring backpack in matte black. Ideal for high-volume event giveaways and offsite kits.',
     30500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'bags'),
     'Travel Duffel 35L', 'travel-duffel-35l',
     'Cabin-friendly 35L duffel with a shoe compartment, water-resistant base, and a luggage trolley pass-through.',
     249000, NULL, TRUE),

    -- Stationery
    ((SELECT id FROM category WHERE slug = 'stationery'),
     'Hardbound Journal Set', 'hardbound-journal-set',
     'A5 dotted hardbound journal in saddle-stitched cloth, paired with a brass-tipped rollerball pen. Comes in a kraft gift sleeve.',
     89500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'stationery'),
     'Desk Organiser Kit', 'desk-organiser-kit',
     'Bamboo desk organiser with slots for stationery, phone, and cards. Includes a sticky-note cube and a metal letter opener.',
     139500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'stationery'),
     'Engraved Rollerball Pen', 'engraved-rollerball-pen',
     'Brushed-steel rollerball with personalised engraving (name or initials), presented in a magnetic gift box.',
     59500, NULL, TRUE),

    -- Travel
    ((SELECT id FROM category WHERE slug = 'travel'),
     'Cabin Trolley 20"', 'cabin-trolley-20',
     'Hardshell 20" 4-wheel cabin trolley with TSA lock and a built-in USB pass-through. Custom logo plate on request.',
     389000, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'travel'),
     'Vegan Leather Passport Wallet', 'vegan-leather-passport-wallet',
     'Slim passport holder with card slots, pen loop, and ticket pocket. Embossed-logo finish.',
     69500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'travel'),
     'Travel Tech Organiser', 'travel-tech-organiser',
     'Padded zip-around organiser for cables, chargers, SSDs, and a power bank. Keeps the carry-on tidy.',
     119500, NULL, TRUE),

    -- Home & Living
    ((SELECT id FROM category WHERE slug = 'home-and-living'),
     'Aromatherapy Candle Trio', 'aromatherapy-candle-trio',
     'Three soy-wax candles in vetiver, sandalwood, and white tea. Hand-poured in glass tumblers, presented in a wooden tray.',
     179500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'home-and-living'),
     'Indoor Plant Gift Box', 'indoor-plant-gift-box',
     'A low-maintenance indoor plant (Snake / ZZ / Pothos) in a ceramic planter, with a care card and a wooden coaster.',
     99500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'home-and-living'),
     'Artisanal Tea Sampler', 'artisanal-tea-sampler',
     'Six single-origin loose-leaf teas in tin caddies, with a bamboo infuser and a tasting guide.',
     149500, NULL, TRUE),

    -- A few additions to existing categories for variety
    ((SELECT id FROM category WHERE slug = 'tech-and-gadgets'),
     'Bamboo Wireless Power Bank', 'bamboo-wireless-power-bank',
     '10,000 mAh power bank with Qi wireless top and a bamboo finish. Engraving area for company logo.',
     229500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'gourmet-and-snacks'),
     'Diwali Sweets Hamper', 'diwali-sweets-hamper',
     'Festive hamper of kaju katli, dry fruit laddoos, and gulab jamun in a brass-finish gift box. Seasonal.',
     189500, NULL, TRUE),
    ((SELECT id FROM category WHERE slug = 'drinkware'),
     'Copper Bottle 1L', 'copper-bottle-1l',
     'Hand-hammered pure copper bottle, 1 litre, with engraved logo. Ayurveda-friendly.',
     89500, NULL, TRUE);
