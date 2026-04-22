ALTER TABLE billing_events
    ADD COLUMN validation_status       VARCHAR(20)   DEFAULT 'PENDING',
    ADD COLUMN last_validated_at       TIMESTAMP,
    ADD COLUMN validation_override_reason  VARCHAR(1000),
    ADD COLUMN validation_overridden_by    VARCHAR(100),
    ADD COLUMN validation_overridden_at    TIMESTAMP;

CREATE INDEX idx_billing_events_validation_status ON billing_events(validation_status);
