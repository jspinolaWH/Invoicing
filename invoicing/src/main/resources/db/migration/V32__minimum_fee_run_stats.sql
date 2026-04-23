ALTER TABLE invoice_runs ADD COLUMN IF NOT EXISTS minimum_fee_adjustment_count INTEGER;
ALTER TABLE invoice_runs ADD COLUMN IF NOT EXISTS minimum_fee_adjustment_total NUMERIC(19, 4);
ALTER TABLE invoice_runs ADD COLUMN IF NOT EXISTS minimum_fee_exempt_count INTEGER;
