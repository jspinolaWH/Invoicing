-- Gap 1: snapshot responsibility_area from cost center at event creation time
ALTER TABLE billing_events ADD COLUMN IF NOT EXISTS responsibility_area VARCHAR(100);

-- Gap 3: snapshot product_group from product at event creation time
ALTER TABLE billing_events ADD COLUMN IF NOT EXISTS product_group VARCHAR(100);

-- Gap 4: configurable reporting field framework
CREATE TABLE IF NOT EXISTS reporting_field_configs (
    id               BIGSERIAL    PRIMARY KEY,
    company_id       BIGINT       NOT NULL,
    field_name       VARCHAR(50)  NOT NULL,
    label_override   VARCHAR(100),
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INTEGER      NOT NULL DEFAULT 0,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_at TIMESTAMP,
    CONSTRAINT uq_reporting_field_company UNIQUE (company_id, field_name)
);

CREATE INDEX IF NOT EXISTS idx_reporting_field_configs_company ON reporting_field_configs(company_id);
