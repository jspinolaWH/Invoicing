CREATE TABLE IF NOT EXISTS company_invoicing_defaults (
    id BIGINT PRIMARY KEY,
    default_invoicing_mode VARCHAR(10) NOT NULL DEFAULT 'NET',
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

INSERT INTO company_invoicing_defaults (id, default_invoicing_mode)
VALUES (1, 'NET')
ON CONFLICT (id) DO NOTHING;
