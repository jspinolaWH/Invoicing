-- customers: invoice_per_project (BillingProfile embeddable)
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS invoice_per_project BOOLEAN NOT NULL DEFAULT FALSE;

-- surcharge_config: boolean flags added by Ralph
ALTER TABLE surcharge_config
    ADD COLUMN IF NOT EXISTS global_surcharge_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS exempt_first_invoice       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_tariff_inclusion  BOOLEAN NOT NULL DEFAULT FALSE;
