ALTER TABLE billing_events DROP CONSTRAINT IF EXISTS billing_events_status_check;

ALTER TABLE billing_events
    ADD CONSTRAINT billing_events_status_check
    CHECK (status IN ('DRAFT','PENDING_TRANSFER','IN_PROGRESS','FOR_CORRECTION','SENT','COMPLETED','ERROR'));
