INSERT INTO category (name, slug) VALUES
    ('Welcome Kits',      'welcome-kits'),
    ('Tech & Gadgets',    'tech-and-gadgets'),
    ('Gourmet & Snacks',  'gourmet-and-snacks'),
    ('Drinkware',         'drinkware'),
    ('Wellness',          'wellness'),
    ('Apparel',           'apparel');

INSERT INTO product (category_id, name, slug, description, price_cents, image_url, in_stock) VALUES
    ((SELECT id FROM category WHERE slug = 'welcome-kits'),
     'New Hire Welcome Box', 'new-hire-welcome-box',
     'A curated welcome kit with a branded notebook, pen, water bottle, and a handwritten card. Perfect for onboarding new team members.',
     499900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'welcome-kits'),
     'Executive Welcome Set', 'executive-welcome-set',
     'Premium leather portfolio, engraved pen, and a curated coffee selection. For senior hires and key partners.',
     1299900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'tech-and-gadgets'),
     'Wireless Charging Pad', 'wireless-charging-pad',
     'Sleek 15W wireless charger with custom-engraved logo. Works with all Qi-compatible devices.',
     349900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'tech-and-gadgets'),
     'Noise-Cancelling Earbuds', 'noise-cancelling-earbuds',
     'Premium wireless earbuds with active noise cancellation and a branded charging case.',
     899900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'gourmet-and-snacks'),
     'Artisan Chocolate Box', 'artisan-chocolate-box',
     'Hand-crafted Belgian chocolates in an elegant gift box. 24 pieces, individually wrapped.',
     279900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'gourmet-and-snacks'),
     'Premium Snack Hamper', 'premium-snack-hamper',
     'A generous mix of dry fruits, gourmet cookies, premium teas, and savoury snacks in a reusable basket.',
     449900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'drinkware'),
     'Insulated Steel Tumbler', 'insulated-steel-tumbler',
     'Double-walled, vacuum-insulated 600ml tumbler. Keeps drinks hot for 12 hours, cold for 24. Laser-engraved logo.',
     159900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'drinkware'),
     'Ceramic Mug Duo', 'ceramic-mug-duo',
     'A pair of matte-finish ceramic mugs in a gift-ready box. Microwave and dishwasher safe.',
     119900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'wellness'),
     'Mindfulness Gift Set', 'mindfulness-gift-set',
     'Aromatherapy candle, herbal tea sampler, and a guided journal. A thoughtful gift for team wellbeing initiatives.',
     249900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'wellness'),
     'Yoga & Recovery Kit', 'yoga-and-recovery-kit',
     'Premium yoga mat, massage ball, and resistance band in a branded carry bag.',
     399900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'apparel'),
     'Branded Hoodie', 'branded-hoodie',
     'Premium cotton-blend hoodie with embroidered logo. Available in multiple sizes; specify at order time.',
     229900, NULL, TRUE),

    ((SELECT id FROM category WHERE slug = 'apparel'),
     'Custom Polo Shirt', 'custom-polo-shirt',
     'Classic-fit polo in premium pique cotton with embroidered company logo. Sizes XS to XXL.',
     169900, NULL, TRUE);
