CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(140) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES category(id),
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(220) NOT NULL UNIQUE,
    description TEXT         NOT NULL,
    price_cents BIGINT       NOT NULL CHECK (price_cents >= 0),
    image_url   VARCHAR(500),
    in_stock    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_category ON product(category_id);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(40)  NOT NULL UNIQUE,
    company_name    VARCHAR(200) NOT NULL,
    contact_name    VARCHAR(200) NOT NULL,
    email           VARCHAR(200) NOT NULL,
    phone           VARCHAR(60),
    address_line1   VARCHAR(200) NOT NULL,
    address_line2   VARCHAR(200),
    city            VARCHAR(120) NOT NULL,
    state           VARCHAR(120),
    postal_code     VARCHAR(40)  NOT NULL,
    country         VARCHAR(120) NOT NULL,
    subtotal_cents  BIGINT       NOT NULL CHECK (subtotal_cents >= 0),
    status          VARCHAR(40)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_item (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id       BIGINT       NOT NULL REFERENCES product(id),
    product_name     VARCHAR(200) NOT NULL,
    unit_price_cents BIGINT       NOT NULL CHECK (unit_price_cents >= 0),
    quantity         INTEGER      NOT NULL CHECK (quantity > 0),
    line_total_cents BIGINT       NOT NULL CHECK (line_total_cents >= 0)
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
