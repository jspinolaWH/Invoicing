-- Gap 3: transmission error reason stored on billing_event
ALTER TABLE billing_events ADD COLUMN IF NOT EXISTS transmission_error_reason TEXT;

-- Gap 1: persist the source billing-event FK on invoice_line_items so transmission
--        service can resolve which events belong to a given invoice
ALTER TABLE invoice_line_items ADD COLUMN IF NOT EXISTS source_event_id BIGINT REFERENCES billing_events(id);
