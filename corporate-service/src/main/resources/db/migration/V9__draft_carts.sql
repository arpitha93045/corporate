-- V9: Persisted draft carts for the AI Gifting Agent.
--
-- The agent's create_draft_cart tool server-prices a proposed selection and
-- stores it here; the buyer later adopts it into their (client-side) cart via
-- an opaque token. Line items snapshot slug/name/price so a draft stays correct
-- even if a product is edited or deleted afterwards — same pattern as order_item.

CREATE TABLE draft_cart (
    id          BIGSERIAL   PRIMARY KEY,
    token       VARCHAR(40) NOT NULL UNIQUE,
    total_cents BIGINT      NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_draft_cart_token ON draft_cart (token);

CREATE TABLE draft_cart_item (
    id               BIGSERIAL    PRIMARY KEY,
    draft_cart_id    BIGINT       NOT NULL REFERENCES draft_cart (id) ON DELETE CASCADE,
    product_id       BIGINT       NOT NULL REFERENCES product (id),
    product_slug     VARCHAR(200) NOT NULL,
    product_name     VARCHAR(200) NOT NULL,
    unit_price_cents BIGINT       NOT NULL,
    quantity         INTEGER      NOT NULL,
    line_total_cents BIGINT       NOT NULL
);

CREATE INDEX ix_draft_cart_item_cart ON draft_cart_item (draft_cart_id);
