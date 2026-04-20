CREATE TABLE contracts (
    id               BIGSERIAL    PRIMARY KEY,
    customer_number  VARCHAR(9)   NOT NULL,
    name             VARCHAR(200) NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_at TIMESTAMP
);

CREATE INDEX idx_contracts_customer ON contracts (customer_number);

CREATE TABLE contract_products (
    contract_id BIGINT NOT NULL REFERENCES contracts(id),
    product_id  BIGINT NOT NULL REFERENCES products(id),
    PRIMARY KEY (contract_id, product_id)
);
