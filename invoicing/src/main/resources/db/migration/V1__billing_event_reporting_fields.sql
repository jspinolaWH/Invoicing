-- Gap 1: waste type categorisation per billing event
ALTER TABLE billing_events ADD COLUMN IF NOT EXISTS waste_type VARCHAR(100);

-- Gap 2: receiving site per billing event
ALTER TABLE billing_events ADD COLUMN IF NOT EXISTS receiving_site VARCHAR(255);
