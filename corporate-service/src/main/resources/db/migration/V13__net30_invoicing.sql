-- V13: net-30 / PO invoicing.
--
-- A buyer may place an order on invoice terms (net-30) instead of paying by card.
-- The order is a normal PLACED order — payment_terms distinguishes the two paths
-- and the net-30 order carries a buyer PO number, a generated invoice number, and
-- a due date. Payment settles out-of-band; an admin marks the invoice paid, which
-- flips it PLACED -> PAID (the Stripe webhook is never involved for net-30).

-- Deterministic invoice numbers, mirroring order_number_seq (V6).
CREATE SEQUENCE invoice_number_seq START WITH 1 INCREMENT BY 1;

-- Every existing order is card/IMMEDIATE.
ALTER TABLE orders
    ADD COLUMN payment_terms VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE';

ALTER TABLE orders ADD COLUMN po_number      VARCHAR(80);
ALTER TABLE orders ADD COLUMN invoice_number VARCHAR(40);
ALTER TABLE orders ADD COLUMN due_date       DATE;

-- Invoice numbers are unique when present. NULLs are treated as distinct in both
-- PostgreSQL and H2's PostgreSQL mode, so card orders (no invoice) never collide.
CREATE UNIQUE INDEX ux_orders_invoice_number ON orders (invoice_number);
