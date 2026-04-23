-- V33: Reference data tables for vehicles, drivers, locations, waste types, receiving sites

-- ─────────────────────────────────────────────────────────────────
-- 1. VEHICLES
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE vehicles (
    vehicle_id       VARCHAR(20)  NOT NULL,
    registration_plate VARCHAR(20) NOT NULL,
    vehicle_type     VARCHAR(100) NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_vehicles PRIMARY KEY (vehicle_id)
);

INSERT INTO vehicles (vehicle_id, registration_plate, vehicle_type) VALUES
('VH-001', 'ABC-123', 'COMPACTOR'),
('VH-002', 'DEF-456', 'COMPACTOR'),
('VH-003', 'GHI-789', 'ROLL_OFF'),
('VH-004', 'JKL-012', 'ROLL_OFF'),
('VH-005', 'MNO-345', 'SIDE_LOADER'),
('VH-006', 'PQR-678', 'SIDE_LOADER'),
('VH-007', 'STU-901', 'REAR_LOADER'),
('VH-008', 'VWX-234', 'REAR_LOADER'),
('VH-009', 'YZA-567', 'CONTAINER_TRUCK'),
('VH-010', 'BCD-890', 'CONTAINER_TRUCK'),
('VH-011', 'EFG-123', 'HOOKLIFT'),
('VH-012', 'HIJ-456', 'HOOKLIFT');

-- ─────────────────────────────────────────────────────────────────
-- 2. DRIVERS
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE drivers (
    driver_id      VARCHAR(50)  NOT NULL,
    name           VARCHAR(255) NOT NULL,
    license_number VARCHAR(50),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_drivers PRIMARY KEY (driver_id)
);

INSERT INTO drivers (driver_id, name, license_number) VALUES
('DRV-001', 'Mikko Mäkinen',    'LIC-FI-10001'),
('DRV-002', 'Pekka Virtanen',   'LIC-FI-10002'),
('DRV-003', 'Juhani Korhonen',  'LIC-FI-10003'),
('DRV-004', 'Eero Leinonen',    'LIC-FI-10004'),
('DRV-005', 'Antti Heikkinen',  'LIC-FI-10005'),
('DRV-006', 'Kari Nieminen',    'LIC-FI-10006'),
('DRV-007', 'Timo Hämäläinen',  'LIC-FI-10007'),
('DRV-008', 'Juha Koskinen',    'LIC-FI-10008'),
('DRV-009', 'Matti Järvinen',   'LIC-FI-10009'),
('DRV-010', 'Ville Lehtonen',   'LIC-FI-10010'),
('DRV-011', 'Sari Laitinen',    'LIC-FI-10011'),
('DRV-012', 'Hanna Turunen',    'LIC-FI-10012');

-- ─────────────────────────────────────────────────────────────────
-- 3. LOCATION MASTERS
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE location_masters (
    location_id       VARCHAR(50)  NOT NULL,
    name              VARCHAR(255) NOT NULL,
    municipality_id   VARCHAR(50)  NOT NULL,
    municipality_name VARCHAR(255) NOT NULL,
    address           VARCHAR(255),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_location_masters PRIMARY KEY (location_id)
);

INSERT INTO location_masters (location_id, name, municipality_id, municipality_name, address) VALUES
('LOC-001', 'Ämmässuo Transfer Station',        'ESP', 'Espoo',       'Ämmässuontie 1, 02820 Espoo'),
('LOC-002', 'Espoo Recycling Centre',           'ESP', 'Espoo',       'Juvanmalmintie 21, 02920 Espoo'),
('LOC-003', 'Kivikko Waste Station',            'HEL', 'Helsinki',    'Kivikontie 2, 00920 Helsinki'),
('LOC-004', 'Konala Recycling Point',           'HEL', 'Helsinki',    'Konalantie 47, 00370 Helsinki'),
('LOC-005', 'Ruskeasanta Collection Point',     'HEL', 'Helsinki',    'Ruskeasannantie 12, 00400 Helsinki'),
('LOC-006', 'Seutula Waste Centre',             'VAN', 'Vantaa',      'Seutulanranta 3, 01740 Vantaa'),
('LOC-007', 'Vantaa Transfer Station North',    'VAN', 'Vantaa',      'Myllymäentie 8, 01600 Vantaa'),
('LOC-008', 'Kauniainen Collection Depot',      'KAU', 'Kauniainen',  'Asematie 5, 02700 Kauniainen'),
('LOC-009', 'Tarastejärvi Station',             'TRE', 'Tampere',     'Tarasteentie 10, 33820 Tampere'),
('LOC-010', 'Tampere North Transfer',           'TRE', 'Tampere',     'Pitkäniemenkatu 3, 33270 Tampere'),
('LOC-011', 'Topinoja Sorting Facility',        'TKU', 'Turku',       'Topinojankaari 1, 20360 Turku'),
('LOC-012', 'Turku Harbour Collection',         'TKU', 'Turku',       'Satamakatu 12, 20100 Turku'),
('LOC-013', 'Ruskon Jätekeskus',                'OUL', 'Oulu',        'Ruskonkatu 1, 90620 Oulu'),
('LOC-014', 'Mustankorkea Station',             'JKL', 'Jyväskylä',   'Mustankorkeantie 2, 40800 Jyväskylä'),
('LOC-015', 'Kuopio Recycling Centre',          'KUO', 'Kuopio',      'Kuopionlahdenranta 4, 70100 Kuopio');

-- ─────────────────────────────────────────────────────────────────
-- 4. WASTE TYPE MASTERS
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE waste_type_masters (
    code     VARCHAR(50)  NOT NULL,
    name_fi  VARCHAR(255) NOT NULL,
    name_en  VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_waste_type_masters PRIMARY KEY (code)
);

INSERT INTO waste_type_masters (code, name_fi, name_en, category) VALUES
('MIXED_WASTE',    'Sekajäte',              'Mixed Waste',           'MUNICIPAL'),
('PAPER',          'Paperi',                'Paper',                 'RECYCLABLE'),
('BIO_WASTE',      'Biojäte',               'Biowaste',              'MUNICIPAL'),
('CARDBOARD',      'Kartonki',              'Cardboard',             'RECYCLABLE'),
('GLASS',          'Lasi',                  'Glass',                 'RECYCLABLE'),
('METAL',          'Metalli',               'Metal',                 'RECYCLABLE'),
('PLASTIC',        'Muovi',                 'Plastic',               'RECYCLABLE'),
('WOOD',           'Puu',                   'Wood',                  'SPECIAL'),
('HAZARDOUS',      'Vaarallinen jäte',      'Hazardous Waste',       'HAZARDOUS'),
('ELECTRONIC',     'Sähkö- ja elektroniikkajäte', 'Electronic Waste', 'SPECIAL'),
('CONSTRUCTION',   'Rakennusjäte',          'Construction Waste',    'SPECIAL'),
('BULKY',          'Suurikokoinen jäte',    'Bulky Waste',           'MUNICIPAL'),
('TEXTILE',        'Tekstiilijäte',         'Textile Waste',         'RECYCLABLE'),
('BATTERIES',      'Paristot ja akut',      'Batteries',             'HAZARDOUS'),
('GARDEN',         'Puutarhajäte',          'Garden Waste',          'MUNICIPAL');

-- ─────────────────────────────────────────────────────────────────
-- 5. RECEIVING SITES
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE receiving_sites (
    id                BIGSERIAL    NOT NULL,
    name              VARCHAR(255) NOT NULL,
    municipality_name VARCHAR(255) NOT NULL,
    address           VARCHAR(255),
    site_type         VARCHAR(100) NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_receiving_sites PRIMARY KEY (id),
    CONSTRAINT uq_receiving_sites_name UNIQUE (name)
);

INSERT INTO receiving_sites (name, municipality_name, address, site_type) VALUES
('Ämmässuo Waste Treatment Centre',  'Espoo',     'Ämmässuontie 1, 02820 Espoo',         'TREATMENT_PLANT'),
('Kivikko Sorting Facility',         'Helsinki',  'Kivikontie 2, 00920 Helsinki',         'SORTING_FACILITY'),
('Seutula Landfill',                 'Vantaa',    'Seutulanranta 3, 01740 Vantaa',        'LANDFILL'),
('Tarastejärvi Waste Centre',        'Tampere',   'Tarasteentie 10, 33820 Tampere',       'TREATMENT_PLANT'),
('Topinoja Waste Management Centre', 'Turku',     'Topinojankaari 1, 20360 Turku',        'SORTING_FACILITY'),
('Ruskon Jätekeskus',                'Oulu',      'Ruskonkatu 1, 90620 Oulu',             'TREATMENT_PLANT'),
('Mustankorkea Waste Centre',        'Jyväskylä', 'Mustankorkeantie 2, 40800 Jyväskylä',  'LANDFILL'),
('Kuopio Composting Plant',          'Kuopio',    'Kuopionlahdenranta 4, 70100 Kuopio',   'COMPOSTING'),
('Lahti Recycling Centre',           'Lahti',     'Paperitehtaankatu 5, 15140 Lahti',     'RECYCLING_CENTER'),
('Hyvinkää Transfer Station',        'Hyvinkää',  'Sveitsinrinne 3, 05800 Hyvinkää',      'SORTING_FACILITY');
