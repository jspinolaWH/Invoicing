ALTER TABLE billing_events
    ADD COLUMN IF NOT EXISTS pending_transfer_customer_number VARCHAR(9),
    ADD COLUMN IF NOT EXISTS pending_transfer_location_id     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS prior_customer_number            VARCHAR(9),
    ADD COLUMN IF NOT EXISTS prior_location_id                VARCHAR(100);

ALTER TABLE billing_event_audit_log
    ADD COLUMN IF NOT EXISTS confirmed_by VARCHAR(100);
