-- ---------------------------------------------------------------------------
-- V15: Daily commitments — Q1 Phase A.
--
-- One row per employee per workday. Employees create the row in the morning
-- with their *target* counts, fill in *actual* counts at end of day, then
-- submit for TL approval. The TL (or HR/Admin override) approves or rejects.
--
-- Design decisions (locked 2026-04-25):
--   * UNIQUE (employee_id, work_date) — exactly one commitment per day.
--   * Status workflow: DRAFT -> SUBMITTED -> APPROVED|REJECTED.
--   * Q3 leaderboard SUMs actual_disbursal_amount over APPROVED rows, so a
--     denormalised total isn't needed — read-time aggregation is fine for
--     the team sizes we're targeting.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS daily_commitments (
    id                              BIGSERIAL    PRIMARY KEY,
    employee_id                     BIGINT       NOT NULL REFERENCES employees(id),
    work_date                       DATE         NOT NULL,

    -- Targets — what the employee committed to at the start of the day.
    target_calls                    INTEGER      NOT NULL DEFAULT 0,
    target_otps                     INTEGER      NOT NULL DEFAULT 0,
    target_interested_customers     INTEGER      NOT NULL DEFAULT 0,
    target_disbursal_amount         NUMERIC(12, 2) NOT NULL DEFAULT 0,

    -- Actuals — what they achieved. Filled at end of day before submit.
    actual_calls                    INTEGER      DEFAULT 0,
    actual_otps                     INTEGER      DEFAULT 0,
    actual_interested_customers     INTEGER      DEFAULT 0,
    actual_disbursal_amount         NUMERIC(12, 2) DEFAULT 0,

    -- Workflow / audit.
    status                          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_at                    TIMESTAMP,
    approved_by_id                  BIGINT       REFERENCES employees(id),
    approved_at                     TIMESTAMP,
    rejection_reason                TEXT,
    notes                           TEXT,

    -- BaseEntity columns.
    created_at                      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP    NOT NULL DEFAULT NOW(),
    version                         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_daily_commitment_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT uk_daily_commitment_per_day UNIQUE (employee_id, work_date)
);

-- "Show me this employee's last 30 days" — most common employee query.
CREATE INDEX IF NOT EXISTS idx_daily_commitments_employee_date
    ON daily_commitments (employee_id, work_date DESC);

-- "Show me the team's pending approvals" + "leaderboard for the month".
CREATE INDEX IF NOT EXISTS idx_daily_commitments_status_date
    ON daily_commitments (status, work_date DESC);
