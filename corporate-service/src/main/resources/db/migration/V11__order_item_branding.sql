-- V11: per-line branding on order items.
--
-- Corporate buyers can attach a message/engraving and a logo URL per order line
-- (e.g. "engrave 'Welcome to the team' on the notebooks"). Both nullable: existing
-- rows and unbranded lines carry NULL. Snapshot on the line like product_name /
-- unit_price_cents — branding is descriptive metadata and never affects pricing.

ALTER TABLE order_item ADD COLUMN branding_message VARCHAR(500);
ALTER TABLE order_item ADD COLUMN branding_logo_url VARCHAR(1000);
