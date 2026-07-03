-- V6: hardening the ordering + inventory path
--
-- 1. Deterministic order numbers via a sequence (no more random suffix collisions).
-- 2. Real quantity-based inventory on product (stock_quantity), backfilled from in_stock.
-- 3. Idempotency key on orders so retried POST /api/checkout returns the same order.

CREATE SEQUENCE order_number_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE product
    ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 0
        CHECK (stock_quantity >= 0);

-- Backfill: anything currently in_stock=TRUE gets a generous default stock.
-- Out-of-stock rows stay at 0.
UPDATE product SET stock_quantity = 100 WHERE in_stock = TRUE;

ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(80);

-- Unique per user. NULL values are treated as distinct in both PostgreSQL and
-- H2's PostgreSQL mode, so orders without an idempotency key never collide.
CREATE UNIQUE INDEX ux_orders_user_idempotency_key
    ON orders (user_id, idempotency_key);
