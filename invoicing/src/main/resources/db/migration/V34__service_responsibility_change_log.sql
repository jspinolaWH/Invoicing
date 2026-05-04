CREATE TABLE IF NOT EXISTS service_responsibility_change_log (
    id                       BIGSERIAL     NOT NULL,
    change_run_id            VARCHAR(36)   NOT NULL,
    applied_by               VARCHAR(100)  NOT NULL,
    applied_at               TIMESTAMP     NOT NULL,
    from_customer_number     VARCHAR(9)    NOT NULL,
    to_customer_number       VARCHAR(9)    NOT NULL,
    previous_responsibility  VARCHAR(100),
    new_responsibility       VARCHAR(100),
    change_effective_date    DATE,
    affected_count           INTEGER       NOT NULL DEFAULT 0,
    moved_in_progress_count  INTEGER       NOT NULL DEFAULT 0,
    correction_copies_created INTEGER      NOT NULL DEFAULT 0,
    reason                   VARCHAR(2000),
    CONSTRAINT pk_service_responsibility_change_log PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_src_log_change_run    ON service_responsibility_change_log (change_run_id);
CREATE INDEX IF NOT EXISTS idx_src_log_from_customer ON service_responsibility_change_log (from_customer_number);
CREATE INDEX IF NOT EXISTS idx_src_log_applied_at    ON service_responsibility_change_log (applied_at);
