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
  ('Matti Virtanen',    'PRIVATE',      '123456',    'EMAIL',     'Mannerheimintie 1',   '00100', 'Helsinki', 'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Helsinki Oy',       'BUSINESS',     '987654321', 'E_INVOICE', 'Aleksanterinkatu 52', '00100', 'Helsinki', 'FI', 'FI12345678',  'fi', 'NET',   'system', NOW(), 'system', NOW()),
  ('Espoon kaupunki',   'MUNICIPALITY', '111222',    'E_INVOICE', 'Espoonkatu 1',        '02770', 'Espoo',    'FI', 'FI02073311',  'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Vantaa Municipality','MUNICIPALITY','333444',    'PAPER',     'Asematie 7',          '01300', 'Vantaa',   'FI', 'FI02068919',  'fi', 'GROSS', 'system', NOW(), 'system', NOW()),
  ('Keräys Finland Ab', 'BUSINESS',     '555666777', 'EMAIL',     'Teollisuuskatu 28',   '00510', 'Helsinki', 'FI', 'FI22334455',  'sv', 'NET',   'system', NOW(), 'system', NOW()),
  ('Turku Authority',   'AUTHORITY',    '888999',    'EMAIL',     'Yliopistonkatu 27a',  '20100', 'Turku',    'FI', NULL,          'fi', 'GROSS', 'system', NOW(), 'system', NOW());

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
