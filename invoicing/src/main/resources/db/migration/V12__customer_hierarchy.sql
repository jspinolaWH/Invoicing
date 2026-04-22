ALTER TABLE customers ADD COLUMN parent_customer_number VARCHAR(9);

CREATE INDEX idx_customers_parent ON customers (parent_customer_number);
