-- ---------------------------------------------------------------------------
-- V12: Leave module Phase-2 — Finbud policy alignment + in-app notifications
--
-- Scope:
--   * Collapse `casual_leave_*` and `sick_leave_*` into a single combined
--     "casual_sick_*" pool (Finbud policy: 6 days/year combined). Track
--     carry-forward separately so reports can show "you carried over X,
--     fresh allocation Y".
--   * Drop WFH columns and migrate any existing WFH leave_requests to PAID
--     — WFH is a working arrangement, not an absence (decision locked
--     with user 2026-04-24).
--   * Default `paid_leave_allocated` to 6.00 to match Finbud policy.
--   * New `notifications` table for in-app notification feed (leave
--     approvals/rejections/overrides). Email is a later phase.
--
-- Constraint strategy:
--   * All new columns are NULL-able / defaulted so the migration is safe
--     on an existing dataset.
--   * Backfill the combined pool from old casual+sick values before dropping
--     the old columns, so no allocated/used days are lost.
--   * Existing WFH leave rows are remapped to PAID so the LeaveType enum
--     can drop the WFH value without orphan rows.
-- ---------------------------------------------------------------------------

-- ---------- 1. leave_balances: add combined pool + carry-forward ------------

ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS casual_sick_allocated NUMERIC(5, 2) DEFAULT 6.00;

ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS casual_sick_used NUMERIC(5, 2) DEFAULT 0.00;

ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS casual_sick_carried_forward NUMERIC(5, 2) DEFAULT 0.00;

ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS paid_leave_carried_forward NUMERIC(5, 2) DEFAULT 0.00;

-- Backfill combined pool from the old separate casual + sick columns so
-- no pre-existing data is silently dropped. `COALESCE` handles the case
-- where one of the columns is NULL on older rows.
UPDATE leave_balances
SET casual_sick_allocated = COALESCE(casual_leave_allocated, 0) + COALESCE(sick_leave_allocated, 0),
    casual_sick_used      = COALESCE(casual_leave_used, 0) + COALESCE(sick_leave_used, 0)
WHERE casual_sick_allocated IS NULL
   OR casual_sick_used IS NULL
   OR casual_sick_allocated = 6.00; -- default we just inserted — safe to overwrite

-- Now drop the old columns we just folded into the combined pool.
ALTER TABLE leave_balances DROP COLUMN IF EXISTS casual_leave_allocated;
ALTER TABLE leave_balances DROP COLUMN IF EXISTS casual_leave_used;
ALTER TABLE leave_balances DROP COLUMN IF EXISTS sick_leave_allocated;
ALTER TABLE leave_balances DROP COLUMN IF EXISTS sick_leave_used;

-- WFH is going away — drop its columns too.
ALTER TABLE leave_balances DROP COLUMN IF EXISTS wfh_days_allocated;
ALTER TABLE leave_balances DROP COLUMN IF EXISTS wfh_days_used;

-- Finbud policy: paid/earned leave = 6 days/year (was 0 in V1). Existing
-- rows keep whatever they currently have — only the DEFAULT for NEW rows
-- changes.
ALTER TABLE leave_balances ALTER COLUMN paid_leave_allocated SET DEFAULT 6.00;

-- ---------- 2. Migrate WFH leave_requests to PAID --------------------------

-- Remap any historical WFH leave rows so the enum can safely drop WFH.
-- In a fresh dev DB this UPDATE is a no-op.
UPDATE leave_requests
SET leave_type = 'PAID'
WHERE leave_type = 'WFH';

-- ---------- 3. notifications table ------------------------------------------

-- In-app notifications feed. Recipient is an employee (not a User) so the
-- FK mirrors how leave/attendance already reference employees. `entity_*`
-- is a loose polymorphic pointer — the listing UI uses it to deep-link
-- back into the source record (e.g. a leave row).
CREATE TABLE IF NOT EXISTS notifications (
    id                      BIGSERIAL    PRIMARY KEY,
    recipient_employee_id   BIGINT       NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    type                    VARCHAR(40)  NOT NULL,
    title                   VARCHAR(200) NOT NULL,
    body                    TEXT,
    entity_type             VARCHAR(40),
    entity_id               BIGINT,
    is_read                 BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    read_at                 TIMESTAMP
);

-- Hot path: "give me my unread notifications, newest first". The
-- composite index covers the filter + ORDER BY in a single index scan.
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread
    ON notifications (recipient_employee_id, is_read, created_at DESC);

-- Secondary index for polling the unread count cheaply.
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications (recipient_employee_id, created_at DESC);
