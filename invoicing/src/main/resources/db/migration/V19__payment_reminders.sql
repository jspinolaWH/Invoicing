CREATE TABLE payment_reminders (
    id                BIGSERIAL PRIMARY KEY,
    invoice_id        BIGINT       NOT NULL REFERENCES invoices(id),
    customer_id       BIGINT       NOT NULL,
    reminder_number   INT          NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    recipient_address VARCHAR(500),
    delivery_method   VARCHAR(20),
    sent_at           TIMESTAMPTZ,
    message           VARCHAR(2000),
    created_by        VARCHAR(255),
    created_at        TIMESTAMPTZ,
    last_modified_by  VARCHAR(255),
    last_modified_at  TIMESTAMPTZ
);

CREATE INDEX idx_payment_reminders_invoice ON payment_reminders(invoice_id);
CREATE INDEX idx_payment_reminders_customer ON payment_reminders(customer_id, status);
