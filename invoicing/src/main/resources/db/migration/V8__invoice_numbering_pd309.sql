-- PD-309: Invoice numbering sequence determination

-- Gap 3: category field on invoice_number_series
ALTER TABLE invoice_number_series ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Gap 1: selected number series ID on invoice_runs
ALTER TABLE invoice_runs ADD COLUMN IF NOT EXISTS number_series_id BIGINT;

-- Gap 2: invoice_templates table
CREATE TABLE IF NOT EXISTS invoice_templates (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(200) NOT NULL UNIQUE,
    code              VARCHAR(100) NOT NULL UNIQUE,
    number_series_id  BIGINT REFERENCES invoice_number_series(id),
    created_by        VARCHAR(200),
    created_at        TIMESTAMP,
    last_modified_by  VARCHAR(200),
    last_modified_at  TIMESTAMP
);
