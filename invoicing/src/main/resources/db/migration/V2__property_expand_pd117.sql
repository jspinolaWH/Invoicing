-- Gap 5: expand property table with PD-117 fields

-- Basic / Classification
ALTER TABLE properties ADD COLUMN IF NOT EXISTS country_code          VARCHAR(2);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS country               VARCHAR(100);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS municipality_code     VARCHAR(20);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS building_classification VARCHAR(40);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS number_of_apartments  INTEGER;

-- R1 — Building information
ALTER TABLE properties ADD COLUMN IF NOT EXISTS building_status       VARCHAR(20);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS building_identifier   VARCHAR(30);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS building_type         VARCHAR(50);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS construction_year     INTEGER;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS usage_type            VARCHAR(100);
ALTER TABLE properties ADD COLUMN IF NOT EXISTS number_of_floors      INTEGER;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS total_area            NUMERIC(10, 2);

-- R3 — Address validity
ALTER TABLE properties ADD COLUMN IF NOT EXISTS address_valid_from    DATE;
ALTER TABLE properties ADD COLUMN IF NOT EXISTS address_valid_to      DATE;

-- R9 — Oldest resident (birth year only)
ALTER TABLE properties ADD COLUMN IF NOT EXISTS oldest_resident_year  INTEGER;

-- R4 — Property owners (new table)
CREATE TABLE IF NOT EXISTS property_owners (
    id                   BIGSERIAL PRIMARY KEY,
    property_id          BIGINT       NOT NULL REFERENCES properties(id),
    owner_id             VARCHAR(50),
    owner_name           VARCHAR(200),
    owner_contact_info   VARCHAR(500),
    ownership_type       VARCHAR(50),
    ownership_percentage NUMERIC(5, 2),
    created_by           VARCHAR(100),
    created_at           TIMESTAMP,
    last_modified_by     VARCHAR(100),
    last_modified_at     TIMESTAMP
);
