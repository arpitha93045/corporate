-- Newsletter subscription table
CREATE TABLE newsletter_subscription (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    subscribed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    unsubscribed_at TIMESTAMP WITH TIME ZONE
);

-- Index for faster lookups
CREATE INDEX idx_newsletter_email ON newsletter_subscription(email);
CREATE INDEX idx_newsletter_active ON newsletter_subscription(is_active) WHERE is_active = TRUE;

