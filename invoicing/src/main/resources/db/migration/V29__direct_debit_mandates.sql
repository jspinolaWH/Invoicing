CREATE TABLE direct_debit_mandates (
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

CREATE INDEX idx_ddm_customer ON direct_debit_mandates(customer_id);
