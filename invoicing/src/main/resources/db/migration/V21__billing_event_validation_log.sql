CREATE TABLE billing_event_validation_logs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    billing_event_id BIGINT        NOT NULL,
    rule_type        VARCHAR(30),
    rule_code        VARCHAR(80),
    severity         VARCHAR(20),
    field            VARCHAR(100),
    description      VARCHAR(1000),
    validated_at     TIMESTAMP     NOT NULL
);

CREATE INDEX idx_val_log_event_id ON billing_event_validation_logs(billing_event_id);
