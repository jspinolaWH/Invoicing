ALTER TABLE billing_events
    ADD COLUMN IF NOT EXISTS price_overridden              BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS original_waste_fee_price      NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS original_transport_fee_price  NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS original_eco_fee_price        NUMERIC(19,4);

CREATE TABLE billing_event_attachments (
    id               BIGSERIAL    PRIMARY KEY,
    billing_event_id BIGINT       NOT NULL REFERENCES billing_events(id),
    file_name        VARCHAR(255) NOT NULL,
    file_size        BIGINT       NOT NULL,
    content_type     VARCHAR(100) NOT NULL,
    content_base64   TEXT,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_at TIMESTAMP
);

CREATE INDEX idx_billing_event_attachments_event ON billing_event_attachments (billing_event_id);
