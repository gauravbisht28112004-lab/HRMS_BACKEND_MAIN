-- ---------------------------------------------------------------------------
-- V16: Hourly updates — Q1 Phase B.
--
-- Granular self-tracking. One row per employee per (work_date, hour_slot).
-- No approval workflow — these are pure activity logs that feed into the
-- daily/weekly reports. Daily commitment "actuals" remain a separate
-- entry the employee fills in once at end of day (Q1.C decision).
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS hourly_updates (
    id                       BIGSERIAL    PRIMARY KEY,
    employee_id              BIGINT       NOT NULL REFERENCES employees(id),
    work_date                DATE         NOT NULL,
    hour_slot                VARCHAR(15)  NOT NULL,
    calls_done               INTEGER      NOT NULL DEFAULT 0,
    otps_achieved            INTEGER      NOT NULL DEFAULT 0,
    interested_customers     INTEGER      NOT NULL DEFAULT 0,
    notes                    TEXT,

    -- BaseEntity columns.
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    version                  BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_hourly_update_per_slot UNIQUE (employee_id, work_date, hour_slot)
);

-- "Show me this employee's day" — drives the per-day employee view.
CREATE INDEX IF NOT EXISTS idx_hourly_updates_employee_date
    ON hourly_updates (employee_id, work_date DESC);
