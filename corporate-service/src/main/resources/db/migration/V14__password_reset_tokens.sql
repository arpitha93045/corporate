CREATE TABLE password_reset_token (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user(id),
    token       VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_prt_token ON password_reset_token (token);
CREATE INDEX ix_prt_user  ON password_reset_token (user_id);
