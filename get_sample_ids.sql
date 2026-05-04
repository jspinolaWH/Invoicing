(SELECT 'billing_events'                    AS tbl, id::text           AS id FROM billing_events                    LIMIT 1)
UNION ALL
(SELECT 'billing_cycles',                          id::text                  FROM billing_cycles                    LIMIT 1)
UNION ALL
(SELECT 'billing_event_templates',                 id::text                  FROM billing_event_templates           LIMIT 1)
UNION ALL
(SELECT 'billing_restrictions',                    id::text                  FROM billing_restrictions              LIMIT 1)
UNION ALL
(SELECT 'accounting_accounts',                     id::text                  FROM accounting_accounts               LIMIT 1)
UNION ALL
(SELECT 'accounting_allocation_rules',             id::text                  FROM accounting_allocation_rules       LIMIT 1)
UNION ALL
(SELECT 'classification_rules',                    id::text                  FROM classification_rules              LIMIT 1)
UNION ALL
(SELECT 'cost_centers',                            id::text                  FROM cost_centers                      LIMIT 1)
UNION ALL
(SELECT 'contracts',                               id::text                  FROM contracts                         LIMIT 1)
UNION ALL
(SELECT 'customers',                               customer_id_number        FROM customers                         LIMIT 1)
UNION ALL
(SELECT 'invoice_runs',                            id::text                  FROM invoice_runs                      LIMIT 1)
UNION ALL
(SELECT 'invoice_templates',                       id::text                  FROM invoice_templates                 LIMIT 1)
UNION ALL
(SELECT 'invoice_number_series',                   id::text                  FROM invoice_number_series             LIMIT 1)
UNION ALL
(SELECT 'invoices',                                id::text                  FROM invoices                          LIMIT 1)
UNION ALL
(SELECT 'minimum_fee_config',                      id::text                  FROM minimum_fee_config                LIMIT 1)
UNION ALL
(SELECT 'price_lists',                             id::text                  FROM price_lists                       LIMIT 1)
UNION ALL
(SELECT 'products',                                id::text                  FROM products                          LIMIT 1)
UNION ALL
(SELECT 'projects',                                id::text                  FROM projects                          LIMIT 1)
UNION ALL
(SELECT 'properties',                              id::text                  FROM properties                        LIMIT 1)
UNION ALL
(SELECT 'property_groups',                         id::text                  FROM property_groups                   LIMIT 1)
UNION ALL
(SELECT 'seasonal_fee_configs',                    id::text                  FROM seasonal_fee_configs              LIMIT 1)
UNION ALL
(SELECT 'surcharge_config',                        id::text                  FROM surcharge_config                  LIMIT 1)
UNION ALL
(SELECT 'validation_rules',                        id::text                  FROM validation_rules                  LIMIT 1)
UNION ALL
(SELECT 'vat_rates',                               id::text                  FROM vat_rates                         LIMIT 1)
UNION ALL
(SELECT 'weighbridge_integration_configs',         id::text                  FROM weighbridge_integration_configs   LIMIT 1)
UNION ALL
(SELECT 'billing_threshold_config',                id::text                  FROM billing_threshold_config          LIMIT 1)
UNION ALL
(SELECT 'billing_threshold_triggers',              id::text                  FROM billing_threshold_triggers        LIMIT 1)
UNION ALL
(SELECT 'billing_threshold_tickets',               id::text                  FROM billing_threshold_tickets         LIMIT 1)
UNION ALL
(SELECT 'payment_reminders',                       id::text                  FROM payment_reminders                 LIMIT 1)
UNION ALL
(SELECT 'reporting_field_configs',                 id::text                  FROM reporting_field_configs           LIMIT 1)
UNION ALL
(SELECT 'service_responsibility_change_log',       id::text                  FROM service_responsibility_change_log LIMIT 1);
