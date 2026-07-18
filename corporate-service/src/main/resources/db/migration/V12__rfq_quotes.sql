-- V12: RFQ (request-for-quote) workflow.
--
-- Extends the anonymous enquiry into a quote lifecycle. An admin responds to an
-- enquiry with an itemized, server-priced quote; the buyer views and accepts or
-- declines it via an opaque token (public, no login — same capability pattern as
-- draft_cart). Quote lines snapshot product name + price so an issued quote stays
-- correct even if the product is later edited or deleted (same as order_item).

-- The enquiry lifecycle gains QUOTED/ACCEPTED/DECLINED/EXPIRED. Existing rows used
-- NEW/CONTACTED/CLOSED; map CONTACTED to the new REVIEWING state.
UPDATE enquiry SET status = 'REVIEWING' WHERE status = 'CONTACTED';

CREATE TABLE quote (
    id          BIGSERIAL   PRIMARY KEY,
    enquiry_id  BIGINT      NOT NULL REFERENCES enquiry (id),
    token       VARCHAR(40) NOT NULL UNIQUE,
    total_cents BIGINT      NOT NULL,
    notes       TEXT,
    valid_until DATE,
    status      VARCHAR(40) NOT NULL DEFAULT 'SENT',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_quote_token ON quote (token);
CREATE INDEX ix_quote_enquiry ON quote (enquiry_id);

CREATE TABLE quote_line (
    id               BIGSERIAL    PRIMARY KEY,
    quote_id         BIGINT       NOT NULL REFERENCES quote (id) ON DELETE CASCADE,
    product_id       BIGINT       NOT NULL REFERENCES product (id),
    product_name     VARCHAR(200) NOT NULL,
    unit_price_cents BIGINT       NOT NULL,
    quantity         INTEGER      NOT NULL,
    line_total_cents BIGINT       NOT NULL
);

CREATE INDEX ix_quote_line_quote ON quote_line (quote_id);
