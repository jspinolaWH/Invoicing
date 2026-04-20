-- Seed contracts for all customers except "No Contract Customer" (000001)
-- Run with: psql -U postgres -d invoicing -f seed_contracts.sql

DELETE FROM contract_products;
DELETE FROM contracts;

INSERT INTO contracts (customer_number, name, active, created_by, created_at)
VALUES
  ('123456',    'Matti Virtanen — Kotijätehuolto',              true, 'seeder', NOW()),
  ('987654321', 'Helsinki Oy — Yritysjätepalvelut',             true, 'seeder', NOW()),
  ('111222',    'Espoon kaupunki — Kunnalliset palvelut',        true, 'seeder', NOW()),
  ('333444',    'Vantaa Municipality — Waste Services',          true, 'seeder', NOW()),
  ('555666777', 'Keräys Finland — Kierrätyssopimus',             true, 'seeder', NOW()),
  ('888999',    'Turku Authority — Municipal Contract',          true, 'seeder', NOW()),
  ('444555',    'Tampereen kaupunki — Jätehuolto',              true, 'seeder', NOW()),
  ('666777888', 'Jyväskylä Services — Palvelusopimus',          true, 'seeder', NOW()),
  ('111999888', 'Lahti Industrial — Vaarallinen jäte',          true, 'seeder', NOW()),
  ('999111222', 'Aalto Kiinteistöt — Kiinteistöjätehuolto',    true, 'seeder', NOW()),
  ('222333',    'Pirjo Korhonen — Kotijätehuolto',              true, 'seeder', NOW()),
  ('777888',    'Kauniaisten kaupunki — Jätehuolto',            true, 'seeder', NOW()),
  ('456789',    'Oulun kaupunki — Kunnalliset palvelut',        true, 'seeder', NOW()),
  ('123789456', 'TechWaste Solutions — Teollisuusjäte',         true, 'seeder', NOW()),
  ('987321',    'Antti Mäkinen — Kotijätehuolto',               true, 'seeder', NOW()),
  ('765432',    'Rovaniemen kaupunki — Jätepalvelut',           true, 'seeder', NOW());

-- contract_products: link each contract to products by code
INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '123456'
  AND p.code IN ('WASTE-COLLECTION-240L','BIOWASTE-COLLECTION','RECYCLING-PAPER','ECO-FEE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '987654321'
  AND p.code IN ('WASTE-COLLECTION-660L','WASTE-COLLECTION-4000L','RECYCLING-CARDBOARD','TRANSPORT-FEE','BULKY-WASTE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '111222'
  AND p.code IN ('WASTE-COLLECTION-240L','WASTE-COLLECTION-660L','WASTE-COLLECTION-4000L','BIOWASTE-COLLECTION','ECO-FEE','LAND-RENT');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '333444'
  AND p.code IN ('WASTE-COLLECTION-240L','WASTE-COLLECTION-660L','BIOWASTE-COLLECTION','BULKY-WASTE','ECO-FEE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '555666777'
  AND p.code IN ('RECYCLING-PAPER','RECYCLING-CARDBOARD','RECYCLING-GLASS','HAZARDOUS-WASTE','TRANSPORT-FEE','CONTAINER-RENTAL-660L');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '888999'
  AND p.code IN ('WASTE-COLLECTION-240L','WASTE-COLLECTION-660L','ECO-FEE','LAND-RENT');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '444555'
  AND p.code IN ('WASTE-COLLECTION-660L','WASTE-COLLECTION-4000L','BIOWASTE-COLLECTION','LAND-RENT','ECO-FEE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '666777888'
  AND p.code IN ('WASTE-COLLECTION-240L','RECYCLING-PAPER','TRANSPORT-FEE','EXPERT-WORK','CONTAINER-RENTAL-240L');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '111999888'
  AND p.code IN ('HAZARDOUS-WASTE','TRANSPORT-FEE','CONTAINER-RENTAL-660L','EXPERT-WORK');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '999111222'
  AND p.code IN ('WASTE-COLLECTION-240L','WASTE-COLLECTION-660L','CONTAINER-RENTAL-240L','CONTAINER-RENTAL-660L','ECO-FEE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '222333'
  AND p.code IN ('WASTE-COLLECTION-240L','BIOWASTE-COLLECTION','RECYCLING-PAPER');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '777888'
  AND p.code IN ('WASTE-COLLECTION-240L','BIOWASTE-COLLECTION','ECO-FEE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '456789'
  AND p.code IN ('WASTE-COLLECTION-240L','WASTE-COLLECTION-660L','WASTE-COLLECTION-4000L','BIOWASTE-COLLECTION','LAND-RENT','ECO-FEE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '123789456'
  AND p.code IN ('HAZARDOUS-WASTE','TRANSPORT-FEE','EXPERT-WORK','BULKY-WASTE');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '987321'
  AND p.code IN ('WASTE-COLLECTION-240L','BIOWASTE-COLLECTION');

INSERT INTO contract_products (contract_id, product_id)
SELECT c.id, p.id FROM contracts c, products p
WHERE c.customer_number = '765432'
  AND p.code IN ('WASTE-COLLECTION-240L','WASTE-COLLECTION-660L','BIOWASTE-COLLECTION','ECO-FEE','LAND-RENT');
