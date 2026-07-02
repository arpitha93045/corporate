CREATE TABLE enquiry (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    email              VARCHAR(200) NOT NULL,
    company_name       VARCHAR(200),
    phone              VARCHAR(60),
    message            TEXT         NOT NULL,
    estimated_quantity INTEGER,
    occasion           VARCHAR(200),
    event_date         DATE,
    budget_range       VARCHAR(80),
    status             VARCHAR(40)  NOT NULL DEFAULT 'NEW',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_enquiry_status ON enquiry(status);
CREATE INDEX idx_enquiry_created_at ON enquiry(created_at);
