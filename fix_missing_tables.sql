-- invoice_runs: ensure simulation_mode column exists (base column not in migrations)
ALTER TABLE invoice_runs ADD COLUMN IF NOT EXISTS simulation_mode BOOLEAN NOT NULL DEFAULT FALSE;

-- V29: direct_debit_mandates
CREATE TABLE IF NOT EXISTS direct_debit_mandates (
    id                  BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT        NOT NULL UNIQUE REFERENCES customers(id),
    mandate_reference   VARCHAR(35)   NOT NULL,
    bank_account        VARCHAR(50)   NOT NULL,
    activated_at        TIMESTAMPTZ   NOT NULL,
    terminated_at       TIMESTAMPTZ,
    created_by          VARCHAR(255),
    created_at          TIMESTAMPTZ,
    last_modified_by    VARCHAR(255),
    last_modified_at    TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ddm_customer ON direct_debit_mandates(customer_id);

-- V30: company_invoicing_defaults
CREATE TABLE IF NOT EXISTS company_invoicing_defaults (
    id                    BIGINT       PRIMARY KEY,
    default_invoicing_mode VARCHAR(10) NOT NULL DEFAULT 'NET',
    updated_by            VARCHAR(100),
    updated_at            TIMESTAMP
);
INSERT INTO company_invoicing_defaults (id, default_invoicing_mode)
VALUES (1, 'NET') ON CONFLICT (id) DO NOTHING;

-- V31: linked_property_id on projects
ALTER TABLE projects ADD COLUMN IF NOT EXISTS linked_property_id VARCHAR(50);

-- V34: service_responsibility_change_log
CREATE TABLE IF NOT EXISTS service_responsibility_change_log (
    id                        BIGSERIAL    NOT NULL,
    change_run_id             VARCHAR(36)  NOT NULL,
    applied_by                VARCHAR(100) NOT NULL,
    applied_at                TIMESTAMP    NOT NULL,
    from_customer_number      VARCHAR(9)   NOT NULL,
    to_customer_number        VARCHAR(9)   NOT NULL,
    previous_responsibility   VARCHAR(100),
    new_responsibility        VARCHAR(100),
    change_effective_date     DATE,
    affected_count            INTEGER      NOT NULL DEFAULT 0,
    moved_in_progress_count   INTEGER      NOT NULL DEFAULT 0,
    correction_copies_created INTEGER      NOT NULL DEFAULT 0,
    reason                    VARCHAR(2000),
    CONSTRAINT pk_service_responsibility_change_log PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_src_log_change_run    ON service_responsibility_change_log (change_run_id);
CREATE INDEX IF NOT EXISTS idx_src_log_from_customer ON service_responsibility_change_log (from_customer_number);
CREATE INDEX IF NOT EXISTS idx_src_log_applied_at    ON service_responsibility_change_log (applied_at);
