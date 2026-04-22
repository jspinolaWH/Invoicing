-- billing_events: component billing flags (PD-318)
ALTER TABLE billing_events
    ADD COLUMN IF NOT EXISTS include_waste_fee               BOOLEAN       NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS include_transport_fee           BOOLEAN       NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS include_eco_fee                 BOOLEAN       NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS contractor_payment_status       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contractor_payment_notes        VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS contractor_payment_recorded_by  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contractor_payment_recorded_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS internal_comments               VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS registration_number             VARCHAR(50);

-- customers: billing profile fields added by Ralph (PD-307, PD-289, PD-273)
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS allow_external_recall        BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS invoice_template_id          BIGINT,
    ADD COLUMN IF NOT EXISTS default_legal_classification VARCHAR(20),
    ADD COLUMN IF NOT EXISTS default_ledger_code          VARCHAR(20);

-- invoice_runs: new filter and operational columns (PD-293, PD-304, PD-273)
ALTER TABLE invoice_runs
    ADD COLUMN IF NOT EXISTS filter_min_amount                NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS filter_customer_type             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS filter_service_type              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS filter_location                  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS filter_service_responsibility    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS batch_attachment_identifier      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS batch_attachment_filename        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS batch_attachment_mime_type       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS batch_attachment_security_class  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cancellation_reason              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cancelled_by                     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cancelled_at                     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS locked_customer_count            INTEGER,
    ADD COLUMN IF NOT EXISTS report_total_checked             INTEGER,
    ADD COLUMN IF NOT EXISTS report_passed                    INTEGER,
    ADD COLUMN IF NOT EXISTS report_blocking_count            INTEGER,
    ADD COLUMN IF NOT EXISTS report_warning_count             INTEGER,
    ADD COLUMN IF NOT EXISTS report_failures_json             TEXT;

-- invoices: billing type and internal comment (PD-292, PD-269)
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS billing_type       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS internal_comment   VARCHAR(2000);

-- invoice_line_items: shared service total (PD-280)
ALTER TABLE invoice_line_items
    ADD COLUMN IF NOT EXISTS shared_service_total_net NUMERIC(19,4);
