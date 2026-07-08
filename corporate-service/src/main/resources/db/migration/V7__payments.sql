-- V7: Stripe payment integration
--
-- Orders start in PLACED with no payment. When the client creates a
-- PaymentIntent we store its id + status; when Stripe's webhook confirms
-- payment_intent.succeeded we flip the order to PAID and stamp paid_at.
--
-- Each column added in its own ALTER for H2 compatibility in tests
-- (H2 doesn't accept the comma-separated multi-column form).

ALTER TABLE orders ADD COLUMN payment_intent_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN payment_status    VARCHAR(40);
ALTER TABLE orders ADD COLUMN paid_at           TIMESTAMP;

-- One PaymentIntent maps to at most one order. NULLs are distinct so
-- orders without a PI never collide.
CREATE UNIQUE INDEX ux_orders_payment_intent_id
    ON orders (payment_intent_id);

