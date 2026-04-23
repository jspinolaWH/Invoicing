CREATE TABLE property_groups (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255)  NOT NULL,
    description       VARCHAR(1000),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(255),
    created_at        TIMESTAMPTZ,
    last_modified_by  VARCHAR(255),
    last_modified_at  TIMESTAMPTZ
);

CREATE TABLE shared_service_participants (
    id                    BIGSERIAL PRIMARY KEY,
    property_group_id     BIGINT         NOT NULL REFERENCES property_groups(id),
    customer_number       VARCHAR(20)    NOT NULL,
    share_percentage      NUMERIC(5, 2)  NOT NULL,
    valid_from            DATE           NOT NULL,
    valid_to              DATE,
    include_if_zero_share BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by            VARCHAR(255),
    created_at            TIMESTAMPTZ,
    last_modified_by      VARCHAR(255),
    last_modified_at      TIMESTAMPTZ
);

CREATE INDEX idx_ssp_property_group ON shared_service_participants(property_group_id);
CREATE INDEX idx_ssp_customer_number ON shared_service_participants(customer_number);
