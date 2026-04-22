CREATE TABLE weighbridge_integration_configs (
    id                   BIGSERIAL    PRIMARY KEY,
    customer_number      VARCHAR(9)   NOT NULL UNIQUE,
    external_system_id   VARCHAR(100),
    default_product_code VARCHAR(100),
    site_reference       VARCHAR(200),
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by           VARCHAR(255),
    created_at           TIMESTAMP,
    last_modified_by     VARCHAR(255),
    last_modified_at     TIMESTAMP
);

CREATE INDEX idx_weighbridge_cfg_customer ON weighbridge_integration_configs (customer_number);
