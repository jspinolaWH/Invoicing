-- ─────────────────────────────────────────────
-- VAT RATES
-- ─────────────────────────────────────────────
INSERT INTO vat_rates (code, rate, valid_from, valid_to, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('VAT_0',   0.00,  '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('VAT_10',  10.00, '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('VAT_14',  14.00, '2013-01-01', '2024-08-31', 'system', NOW(), 'system', NOW()),
  ('VAT_24',  24.00, '2013-01-01', '2024-08-31', 'system', NOW(), 'system', NOW()),
  ('VAT_255', 25.50, '2024-09-01', NULL,         'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- ACCOUNTING ACCOUNTS
-- ─────────────────────────────────────────────
INSERT INTO accounting_accounts (code, name, valid_from, valid_to, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('3001',        'Waste Collection Revenue',       '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('3002',        'Transport Fee Revenue',          '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('3003',        'Eco Fee Revenue',                '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('3004',        'Container Rental Revenue',       '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('3005',        'Additional Service Revenue',     '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('4001',        'Municipal Waste Services',       '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('4002',        'Private Waste Services',         '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('4003',        'Industrial Waste Services',      '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('5001',        'Surcharge — Paper Invoice',      '2018-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('5002',        'Surcharge — Direct Payment',     '2018-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('9001',        'Minimum Fee Adjustment',         '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('9002',        'Credit Note Adjustment',         '2013-01-01', NULL,         'system', NOW(), 'system', NOW()),
  ('LEGACY_3001', 'Legacy Waste Collection',        '2010-01-01', '2012-12-31', 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- COST CENTERS
-- ─────────────────────────────────────────────
INSERT INTO cost_centers (product_segment, reception_segment, responsibility_segment, composite_code, description, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('WASTE',  'HELSINKI-01', 'MUNICIPAL',   'WASTE-HELSINKI-01-MUNICIPAL',   'Helsinki municipal waste collection',       'system', NOW(), 'system', NOW()),
  ('WASTE',  'HELSINKI-02', 'MUNICIPAL',   'WASTE-HELSINKI-02-MUNICIPAL',   'Helsinki municipal waste — south district', 'system', NOW(), 'system', NOW()),
  ('WASTE',  'ESPOO-01',    'MUNICIPAL',   'WASTE-ESPOO-01-MUNICIPAL',      'Espoo municipal waste collection',          'system', NOW(), 'system', NOW()),
  ('WASTE',  'VANTAA-01',   'MUNICIPAL',   'WASTE-VANTAA-01-MUNICIPAL',     'Vantaa municipal waste collection',         'system', NOW(), 'system', NOW()),
  ('RECYCL', 'HELSINKI-01', 'PRIVATE',     'RECYCL-HELSINKI-01-PRIVATE',    'Helsinki private recycling services',       'system', NOW(), 'system', NOW()),
  ('RECYCL', 'ESPOO-01',    'PRIVATE',     'RECYCL-ESPOO-01-PRIVATE',       'Espoo private recycling services',          'system', NOW(), 'system', NOW()),
  ('HAZARD', 'HELSINKI-01', 'INDUSTRIAL',  'HAZARD-HELSINKI-01-INDUSTRIAL', 'Helsinki industrial hazardous waste',       'system', NOW(), 'system', NOW()),
  ('TRANSP', 'HELSINKI-01', 'MUNICIPAL',   'TRANSP-HELSINKI-01-MUNICIPAL',  'Helsinki municipal transport',              'system', NOW(), 'system', NOW()),
  ('TRANSP', 'ESPOO-01',    'MUNICIPAL',   'TRANSP-ESPOO-01-MUNICIPAL',     'Espoo municipal transport',                 'system', NOW(), 'system', NOW()),
  ('ECO',    'NATIONAL-01', 'MUNICIPAL',   'ECO-NATIONAL-01-MUNICIPAL',     'National eco fee — municipal',              'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- PRODUCTS
-- ─────────────────────────────────────────────
INSERT INTO products (code, pricing_unit, reverse_charge_vat, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('WASTE-COLLECTION-240L',  'PCS',  false, 'system', NOW(), 'system', NOW()),
  ('WASTE-COLLECTION-660L',  'PCS',  false, 'system', NOW(), 'system', NOW()),
  ('RECYCLING-PAPER',        'KG',   false, 'system', NOW(), 'system', NOW()),
  ('RECYCLING-CARDBOARD',    'KG',   false, 'system', NOW(), 'system', NOW()),
  ('HAZARDOUS-WASTE',        'TON',  true,  'system', NOW(), 'system', NOW()),
  ('TRANSPORT-FEE',          'HOUR', false, 'system', NOW(), 'system', NOW()),
  ('CONTAINER-RENTAL-240L',  'PCS',  false, 'system', NOW(), 'system', NOW()),
  ('ECO-FEE',                'PCS',  false, 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- PRODUCT TRANSLATIONS
-- ─────────────────────────────────────────────
INSERT INTO product_translations (product_id, locale, name, created_by, created_at, last_modified_by, last_modified_at)
SELECT id, 'fi', 'Jätteen keräys 240L',   'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-240L'
UNION ALL
SELECT id, 'sv', 'Sopinsamling 240L',      'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-240L'
UNION ALL
SELECT id, 'en', 'Waste Collection 240L',  'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-240L'
UNION ALL
SELECT id, 'fi', 'Jätteen keräys 660L',   'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-660L'
UNION ALL
SELECT id, 'sv', 'Sopinsamling 660L',      'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-660L'
UNION ALL
SELECT id, 'en', 'Waste Collection 660L',  'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-660L'
UNION ALL
SELECT id, 'fi', 'Paperinkeräys',          'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-PAPER'
UNION ALL
SELECT id, 'sv', 'Pappersinsamling',        'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-PAPER'
UNION ALL
SELECT id, 'en', 'Paper Recycling',         'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-PAPER'
UNION ALL
SELECT id, 'fi', 'Kartonginkeräys',         'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-CARDBOARD'
UNION ALL
SELECT id, 'sv', 'Kartongsinsamling',        'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-CARDBOARD'
UNION ALL
SELECT id, 'en', 'Cardboard Recycling',      'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-CARDBOARD'
UNION ALL
SELECT id, 'fi', 'Vaarallinen jäte',         'system', NOW(), 'system', NOW() FROM products WHERE code = 'HAZARDOUS-WASTE'
UNION ALL
SELECT id, 'sv', 'Farligt avfall',            'system', NOW(), 'system', NOW() FROM products WHERE code = 'HAZARDOUS-WASTE'
UNION ALL
SELECT id, 'en', 'Hazardous Waste',           'system', NOW(), 'system', NOW() FROM products WHERE code = 'HAZARDOUS-WASTE'
UNION ALL
SELECT id, 'fi', 'Kuljetusmaksu',             'system', NOW(), 'system', NOW() FROM products WHERE code = 'TRANSPORT-FEE'
UNION ALL
SELECT id, 'sv', 'Transportavgift',           'system', NOW(), 'system', NOW() FROM products WHERE code = 'TRANSPORT-FEE'
UNION ALL
SELECT id, 'en', 'Transport Fee',             'system', NOW(), 'system', NOW() FROM products WHERE code = 'TRANSPORT-FEE'
UNION ALL
SELECT id, 'fi', 'Astianvuokra 240L',         'system', NOW(), 'system', NOW() FROM products WHERE code = 'CONTAINER-RENTAL-240L'
UNION ALL
SELECT id, 'sv', 'Kärlavgift 240L',           'system', NOW(), 'system', NOW() FROM products WHERE code = 'CONTAINER-RENTAL-240L'
UNION ALL
SELECT id, 'en', 'Container Rental 240L',     'system', NOW(), 'system', NOW() FROM products WHERE code = 'CONTAINER-RENTAL-240L'
UNION ALL
SELECT id, 'fi', 'Ympäristömaksu',            'system', NOW(), 'system', NOW() FROM products WHERE code = 'ECO-FEE'
UNION ALL
SELECT id, 'sv', 'Miljöavgift',               'system', NOW(), 'system', NOW() FROM products WHERE code = 'ECO-FEE'
UNION ALL
SELECT id, 'en', 'Eco Fee',                   'system', NOW(), 'system', NOW() FROM products WHERE code = 'ECO-FEE';

-- ─────────────────────────────────────────────
-- INVOICE NUMBER SERIES
-- ─────────────────────────────────────────────
INSERT INTO invoice_number_series (name, prefix, format_pattern, current_counter, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('MAIN_2026',     'INV', '{PREFIX}-{YEAR}-{COUNTER:06d}', 0, 'system', NOW(), 'system', NOW()),
  ('CREDIT_2026',   'CR',  '{PREFIX}-{YEAR}-{COUNTER:06d}', 0, 'system', NOW(), 'system', NOW()),
  ('PROFORMA_2026', 'PRO', '{PREFIX}-{YEAR}-{COUNTER:06d}', 0, 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- CUSTOMERS
-- ─────────────────────────────────────────────
INSERT INTO customers (name, customer_type, customer_id_number, delivery_method, street_address, postal_code, city, country_code, business_id, language_code, invoicing_mode, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('Matti Virtanen',         'PRIVATE',      '123456',    'EMAIL',     'Mannerheimintie 1',      '00100', 'Helsinki', 'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Helsinki Oy',            'BUSINESS',     '987654321', 'E_INVOICE', 'Aleksanterinkatu 52',    '00100', 'Helsinki', 'FI', 'FI12345678',  'fi', 'NET',   'system', NOW(), 'system', NOW()),
  ('Espoon kaupunki',        'MUNICIPALITY', '111222',    'E_INVOICE', 'Espoonkatu 1',           '02770', 'Espoo',    'FI', 'FI02073311',  'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Vantaa Municipality',    'MUNICIPALITY', '333444',    'PAPER',     'Asematie 7',             '01300', 'Vantaa',   'FI', 'FI02068919',  'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Keräys Finland Ab',      'BUSINESS',     '555666777', 'EMAIL',     'Teollisuuskatu 28',      '00510', 'Helsinki', 'FI', 'FI22334455',  'sv', 'NET',   'system', NOW(), 'system', NOW()),
  ('Turku Authority',        'AUTHORITY',    '888999',    'EMAIL',     'Yliopistonkatu 27a',     '20100', 'Turku',    'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Liisa Korhonen',         'PRIVATE',      '246810',    'EMAIL',     'Hämeentie 14',           '00530', 'Helsinki', 'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Tampereen Jätehuolto',   'BUSINESS',     '112233445', 'E_INVOICE', 'Satakunnankatu 18',      '33200', 'Tampere',  'FI', 'FI33445566',  'fi', 'NET',   'system', NOW(), 'system', NOW()),
  ('Jyväskylä kaupunki',     'MUNICIPALITY', '776655',    'E_INVOICE', 'Vapaudenkatu 32',        '40100', 'Jyväskylä','FI', 'FI17321436',  'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Antti Leinonen',         'PRIVATE',      '918273',    'PAPER',     'Puistokatu 5',           '20200', 'Turku',    'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Oulu Recycle Ab',        'BUSINESS',     '334455667', 'EMAIL',     'Kauppurienkatu 22',      '90100', 'Oulu',     'FI', 'FI44556677',  'fi', 'NET',   'system', NOW(), 'system', NOW()),
  ('Pirkkalan kunta',        'MUNICIPALITY', '654321',    'E_INVOICE', 'Pirkkalaistentie 1',     '33960', 'Pirkkala', 'FI', 'FI02393050',  'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('SuomiWaste Oy',          'BUSINESS',     '223344556', 'EMAIL',     'Lautatarhankatu 6',      '00580', 'Helsinki', 'FI', 'FI55667788',  'fi', 'NET',   'system', NOW(), 'system', NOW()),
  ('Kari Mäkinen',           'PRIVATE',      '741852',    'PAPER',     'Länsimäentie 44',        '00750', 'Helsinki', 'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Lahden kaupunki',        'MUNICIPALITY', '369258',    'E_INVOICE', 'Harjukatu 33',           '15100', 'Lahti',    'FI', 'FI01634422',  'fi', 'GROSS', 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- PROPERTIES
-- ─────────────────────────────────────────────
INSERT INTO properties (property_id, street_address, city, postal_code, customer_number, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('PROP-HEL-001', 'Mannerheimintie 1',       'Helsinki',  '00100', '123456',    'system', NOW(), 'system', NOW()),
  ('PROP-HEL-002', 'Aleksanterinkatu 52',      'Helsinki',  '00100', '987654321', 'system', NOW(), 'system', NOW()),
  ('PROP-HEL-003', 'Hämeentie 14',             'Helsinki',  '00530', '246810',    'system', NOW(), 'system', NOW()),
  ('PROP-HEL-004', 'Teollisuuskatu 28',        'Helsinki',  '00510', '555666777', 'system', NOW(), 'system', NOW()),
  ('PROP-HEL-005', 'Lautatarhankatu 6',        'Helsinki',  '00580', '223344556', 'system', NOW(), 'system', NOW()),
  ('PROP-HEL-006', 'Länsimäentie 44',          'Helsinki',  '00750', '741852',    'system', NOW(), 'system', NOW()),
  ('PROP-ESP-001', 'Espoonkatu 1',             'Espoo',     '02770', '111222',    'system', NOW(), 'system', NOW()),
  ('PROP-ESP-002', 'Otaniementie 9',           'Espoo',     '02150', '111222',    'system', NOW(), 'system', NOW()),
  ('PROP-VAN-001', 'Asematie 7',              'Vantaa',    '01300', '333444',    'system', NOW(), 'system', NOW()),
  ('PROP-VAN-002', 'Tikkurilantie 12',         'Vantaa',    '01370', '333444',    'system', NOW(), 'system', NOW()),
  ('PROP-TRE-001', 'Satakunnankatu 18',        'Tampere',   '33200', '112233445', 'system', NOW(), 'system', NOW()),
  ('PROP-TRE-002', 'Hämeenkatu 41',            'Tampere',   '33200', '112233445', 'system', NOW(), 'system', NOW()),
  ('PROP-TKU-001', 'Yliopistonkatu 27a',       'Turku',     '20100', '888999',    'system', NOW(), 'system', NOW()),
  ('PROP-TKU-002', 'Puistokatu 5',             'Turku',     '20200', '918273',    'system', NOW(), 'system', NOW()),
  ('PROP-OUL-001', 'Kauppurienkatu 22',        'Oulu',      '90100', '334455667', 'system', NOW(), 'system', NOW()),
  ('PROP-OUL-002', 'Hallituskatu 18',          'Oulu',      '90100', '334455667', 'system', NOW(), 'system', NOW()),
  ('PROP-JYV-001', 'Vapaudenkatu 32',          'Jyväskylä', '40100', '776655',    'system', NOW(), 'system', NOW()),
  ('PROP-LAH-001', 'Harjukatu 33',             'Lahti',     '15100', '369258',    'system', NOW(), 'system', NOW()),
  ('PROP-PIR-001', 'Pirkkalaistentie 1',       'Pirkkala',  '33960', '654321',    'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- CLASSIFICATION RULES
-- ─────────────────────────────────────────────
INSERT INTO classification_rules (company_id, priority, customer_type_condition, product_code_condition, region_condition, result_classification, label, active, created_by, created_at, last_modified_by, last_modified_at) VALUES
  (1, 1, 'MUNICIPALITY', NULL, NULL, 'PUBLIC_LAW',  'Municipality — always public law', true, 'system', NOW(), 'system', NOW()),
  (1, 2, 'AUTHORITY',    NULL, NULL, 'PUBLIC_LAW',  'Authority — always public law',    true, 'system', NOW(), 'system', NOW()),
  (1, 3, 'BUSINESS',     NULL, NULL, 'PRIVATE_LAW', 'Business — private law',           true, 'system', NOW(), 'system', NOW()),
  (1, 4, 'PRIVATE',      NULL, NULL, 'PRIVATE_LAW', 'Private customer — private law',   true, 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- VALIDATION RULES
-- ─────────────────────────────────────────────
INSERT INTO validation_rules (company_id, rule_type, rule_code, config, blocking, active, description, created_by, created_at, last_modified_by, last_modified_at) VALUES
  (1, 'MANDATORY_FIELD', 'BUSINESS_ID_MANDATORY',   '{"field":"billingProfile.businessId"}',    true,  true, 'Business ID is mandatory for all customers',       'system', NOW(), 'system', NOW()),
  (1, 'MANDATORY_FIELD', 'INVOICING_MODE_MANDATORY', '{"field":"billingProfile.invoicingMode"}', false, true, 'Invoicing mode should be set (warning only)',       'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- BILLING EVENTS
-- ─────────────────────────────────────────────
INSERT INTO billing_events (customer_number, product_id, waste_fee_price, transport_fee_price, eco_fee_price, quantity, weight, vat_rate_0, vat_rate_24, event_date, municipality_id, location_id, vehicle_id, status, excluded, non_billable, office_review_required, legal_classification, origin, created_by, created_at, last_modified_by, last_modified_at)
SELECT '123456',    id, 12.50, 4.80,  0.50, 1,   0.24,  0, 25.50, '2025-11-05', 'MUN-01', 'LOC-001', 'ABC-123', 'IN_PROGRESS', false, false, false, 'PRIVATE_LAW', 'INTEGRATION', 'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-240L'
UNION ALL
SELECT '987654321', id, 28.00, 8.50,  1.00, 3,   0.72,  0, 25.50, '2025-11-07', 'MUN-01', 'LOC-002', 'XYZ-456', 'IN_PROGRESS', false, false, false, 'PRIVATE_LAW', 'INTEGRATION', 'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-660L'
UNION ALL
SELECT '111222',    id, 12.50, 4.80,  0.50, 1,   0.24,  0, 25.50, '2025-11-03', 'MUN-02', 'LOC-003', 'DEF-789', 'SENT',        false, false, false, 'PUBLIC_LAW',  'INTEGRATION', 'system', NOW(), 'system', NOW() FROM products WHERE code = 'WASTE-COLLECTION-240L'
UNION ALL
SELECT '333444',    id, 0.00,  45.00, 0.00, 2,   0.00,  0, 25.50, '2025-10-28', 'MUN-03', 'LOC-004', 'GHI-012', 'COMPLETED',   false, false, false, 'PUBLIC_LAW',  'INTEGRATION', 'system', NOW(), 'system', NOW() FROM products WHERE code = 'TRANSPORT-FEE'
UNION ALL
SELECT '555666777', id, 5.00,  2.50,  0.25, 120, 120.0, 0, 25.50, '2025-11-10', 'MUN-01', 'LOC-005', 'JKL-345', 'ERROR',       false, false, false, 'PRIVATE_LAW', 'INTEGRATION', 'system', NOW(), 'system', NOW() FROM products WHERE code = 'RECYCLING-PAPER'
UNION ALL
SELECT '888999',    id, 3.00,  0.00,  1.50, 1,   0.10,  0, 25.50, '2025-11-12', 'MUN-04', 'LOC-006', NULL,      'IN_PROGRESS', false, false, false, 'PUBLIC_LAW',  'MANUAL',      'system', NOW(), 'system', NOW() FROM products WHERE code = 'ECO-FEE';

-- ─────────────────────────────────────────────
-- EVENT TYPE CONFIGS
-- ─────────────────────────────────────────────
INSERT INTO event_type_configs (event_type_code, requires_office_review, unusual_quantity_threshold, unusual_weight_threshold, unusual_price_threshold, review_if_unknown_location, description, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('BIO_WASTE_EMPTYING',   false, 10.00, 500.00, 200.00, true, 'Bio waste container emptying — standard driver event', 'system', NOW(), 'system', NOW()),
  ('HAZARDOUS_WASTE_PICKUP', true, NULL,  NULL,   NULL,   true, 'Hazardous waste pickup — always requires office review', 'system', NOW(), 'system', NOW()),
  ('EXTRA_EMPTYING',       false, 5.00,  250.00, 150.00, true, 'Extra emptying requested by customer',                  'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- ACCOUNTING ALLOCATION RULES
-- ─────────────────────────────────────────────
INSERT INTO accounting_allocation_rules (product_id, accounting_account_id, region, municipality, specificity_score, description, active, created_by, created_at, last_modified_by, last_modified_at)
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for WASTE-COLLECTION-240L', true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'WASTE-COLLECTION-240L' AND a.code = '3001'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for WASTE-COLLECTION-660L', true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'WASTE-COLLECTION-660L' AND a.code = '3001'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for RECYCLING-PAPER',       true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'RECYCLING-PAPER'       AND a.code = '3001'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for RECYCLING-CARDBOARD',   true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'RECYCLING-CARDBOARD'   AND a.code = '3001'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for HAZARDOUS-WASTE',       true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'HAZARDOUS-WASTE'       AND a.code = '4003'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for TRANSPORT-FEE',         true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'TRANSPORT-FEE'         AND a.code = '3002'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for CONTAINER-RENTAL-240L', true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'CONTAINER-RENTAL-240L' AND a.code = '3004'
UNION ALL
SELECT p.id, a.id, NULL, NULL, 1, 'Default rule for ECO-FEE',               true, 'system', NOW(), 'system', NOW() FROM products p, accounting_accounts a WHERE p.code = 'ECO-FEE'               AND a.code = '3003';

-- ─────────────────────────────────────────────
-- COST CENTER COMPOSITION CONFIG
-- ─────────────────────────────────────────────
INSERT INTO cost_center_composition_config (separator, segment_order, product_segment_enabled, reception_point_segment_enabled, service_responsibility_segment_enabled, public_law_code, private_law_code, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('-', 'PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY', true, true, true, 'PL', 'PR', 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- SURCHARGE CONFIGS
-- ─────────────────────────────────────────────
INSERT INTO surcharge_config (delivery_method, amount, description, active, global_surcharge_enabled, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('PAPER',          5.00, 'Paper invoice surcharge',  true, true, 'system', NOW(), 'system', NOW()),
  ('EMAIL',          2.00, 'Email invoice surcharge',  true, true, 'system', NOW(), 'system', NOW()),
  ('DIRECT_PAYMENT', 3.50, 'Direct payment surcharge', true, true, 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- BILLING CYCLES
-- ─────────────────────────────────────────────
INSERT INTO billing_cycles (customer_number, frequency, next_billing_date, description, service_type, active, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('123456',    'MONTHLY',   '2026-02-01', 'Monthly bin emptying',             'CONTAINER_EMPTYING', true, 'system', NOW(), 'system', NOW()),
  ('987654321', 'MONTHLY',   '2026-02-01', 'Monthly recycling collection',     'RECYCLING',          true, 'system', NOW(), 'system', NOW()),
  ('111222',    'QUARTERLY', '2026-04-01', 'Quarterly municipal waste services','MUNICIPAL_WASTE',   true, 'system', NOW(), 'system', NOW()),
  ('123456',    'ANNUAL',    '2026-01-01', 'Annual base fee',                  'BASE_FEE',           true, 'system', NOW(), 'system', NOW());

-- ─────────────────────────────────────────────
-- MINIMUM FEE CONFIGS
-- ─────────────────────────────────────────────
INSERT INTO minimum_fee_config (customer_type, net_amount_threshold, period_type, contract_start_adjustment, contract_end_adjustment, adjustment_product_code, description, active, created_by, created_at, last_modified_by, last_modified_at) VALUES
  ('RESIDENTIAL', 50.00,  'ANNUAL', true, true, 'MIN_FEE_ADJ', 'Annual minimum fee for residential customers', true, 'system', NOW(), 'system', NOW()),
  ('BUSINESS',    200.00, 'ANNUAL', true, true, 'MIN_FEE_ADJ', 'Annual minimum fee for business customers',    true, 'system', NOW(), 'system', NOW());
