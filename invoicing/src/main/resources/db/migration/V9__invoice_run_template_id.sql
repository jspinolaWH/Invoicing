-- PD-309 Gap 1: template selection on invoice runs
ALTER TABLE invoice_runs ADD COLUMN IF NOT EXISTS template_id BIGINT REFERENCES invoice_templates(id);
