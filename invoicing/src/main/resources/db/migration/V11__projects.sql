CREATE TABLE projects (
    id               BIGSERIAL    PRIMARY KEY,
    customer_number  VARCHAR(9)   NOT NULL,
    name             VARCHAR(200) NOT NULL,
    description      VARCHAR(1000),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(255),
    created_at       TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_at TIMESTAMP
);

CREATE INDEX idx_projects_customer ON projects (customer_number);
