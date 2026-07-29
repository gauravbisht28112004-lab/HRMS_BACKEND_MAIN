-- ---------------------------------------------------------------------------
-- V17: Monthly targets — Q1 Phase C.
--
-- One row per employee per (year, month). Set by TL for direct reports;
-- HR/Admin can set or override for anyone. The "achieved" side is derived
-- from `daily_commitments.actual_disbursal_amount` summed over APPROVED
-- rows for the same period — no denormalised counter, computed at read time.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS monthly_targets (
    id                          BIGSERIAL    PRIMARY KEY,
    employee_id                 BIGINT       NOT NULL REFERENCES employees(id),
    year                        INTEGER      NOT NULL,
    month                       INTEGER      NOT NULL,
    target_disbursal_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    target_logins               INTEGER      NOT NULL DEFAULT 0,
    set_by_id                   BIGINT       REFERENCES employees(id),
    notes                       TEXT,

    -- BaseEntity columns.
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    version                     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_monthly_target_year  CHECK (year >= 2020 AND year <= 2100),
    CONSTRAINT chk_monthly_target_month CHECK (month >= 1 AND month <= 12),
    CONSTRAINT uk_monthly_target_emp_year_month UNIQUE (employee_id, year, month)
);

CREATE INDEX IF NOT EXISTS idx_monthly_targets_employee_period
    ON monthly_targets (employee_id, year DESC, month DESC);
