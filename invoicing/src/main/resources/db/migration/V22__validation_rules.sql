CREATE TABLE validation_rules (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id       BIGINT       NOT NULL,
    rule_type        VARCHAR(30)  NOT NULL,
    rule_code        VARCHAR(80)  NOT NULL,
    config           TEXT,
    blocking         BOOLEAN      NOT NULL DEFAULT TRUE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    description      VARCHAR(500),
    created_by       VARCHAR(255),
    created_at       TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_at TIMESTAMP
);

CREATE INDEX idx_val_rule_company ON validation_rules(company_id, active);
