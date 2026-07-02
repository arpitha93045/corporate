CREATE TABLE app_user (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    company_name  VARCHAR(200),
    phone         VARCHAR(60),
    role          VARCHAR(40)  NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE orders ADD COLUMN user_id BIGINT REFERENCES app_user(id);
CREATE INDEX idx_orders_user ON orders(user_id);
