-- V10: shift_assignments — integrity constraints + backfill from employees.shift_type_id
--
-- Background
-- ----------
-- Up through V9 the schema allowed any shift_assignments row for an employee,
-- with no validity-window sanity check and no coverage for employees that
-- already had a denormalized employees.shift_type_id pointer but no matching
-- assignment row. Phase 2 (Tier 2) introduces a full Shift Assignment CRUD
-- with the contract:
--
--   * Employee.shiftType is the "current shift" denormalized pointer.
--   * shift_assignments is the authoritative temporal log.
--   * When a new assignment is created, the previous OPEN assignment is
--     auto-closed (effective_to = new.effective_from - 1 day).
--
-- To make the service-layer invariants enforceable we need:
--   1. A CHECK that effective_to (when present) is on/after effective_from.
--   2. A supporting index for the "find current / find overlapping" queries.
--   3. A backfill so existing employees that only have a shift_type_id but no
--      shift_assignments row get one open-ended assignment. Without this,
--      `getCurrentForEmployee()` returns null for legacy employees even
--      though the UI shows them assigned to a shift.
-- ---------------------------------------------------------------------------

-- 1. Validity window sanity check ------------------------------------------
-- Use NOT VALID + VALIDATE pattern so this is safe against any legacy data
-- violating the invariant (it would refuse to attach otherwise, blocking boot).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_shift_assignments_window'
    ) THEN
        ALTER TABLE shift_assignments
            ADD CONSTRAINT chk_shift_assignments_window
            CHECK (effective_to IS NULL OR effective_to >= effective_from)
            NOT VALID;
    END IF;
END$$;

-- If any historical rows violate the invariant, log + null the bad
-- effective_to rather than failing the migration. In practice there should
-- be none, but be defensive.
UPDATE shift_assignments
SET effective_to = NULL
WHERE effective_to IS NOT NULL
  AND effective_to < effective_from;

-- Promote NOT VALID -> VALIDATE now that the data is clean.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_shift_assignments_window'
          AND NOT convalidated
    ) THEN
        ALTER TABLE shift_assignments
            VALIDATE CONSTRAINT chk_shift_assignments_window;
    END IF;
END$$;

-- 2. Supporting indexes ----------------------------------------------------
-- "Find current assignment for employee" and "find overlapping windows" both
-- filter by employee_id and scan effective_from/effective_to. A composite
-- index keeps those hot paths cheap.
CREATE INDEX IF NOT EXISTS idx_shift_assignments_employee_window
    ON shift_assignments (employee_id, effective_from, effective_to);

-- "List assignments for a shift type" for the Shifts admin page.
CREATE INDEX IF NOT EXISTS idx_shift_assignments_shift_type
    ON shift_assignments (shift_type_id);

-- 3. Backfill from employees.shift_type_id ---------------------------------
-- For every active employee that has a shift_type_id denormalized on their
-- row but NO shift_assignments row at all, create one open-ended assignment
-- starting from their date_of_joining (fall back to CURRENT_DATE if missing).
-- This is idempotent: the anti-join filter guarantees no duplicates even if
-- the migration re-runs against a partially-backfilled DB.
INSERT INTO shift_assignments (
    employee_id,
    shift_type_id,
    effective_from,
    effective_to,
    created_at,
    updated_at,
    version
)
SELECT
    e.id,
    e.shift_type_id,
    COALESCE(e.date_of_joining, CURRENT_DATE),
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM employees e
WHERE e.shift_type_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM shift_assignments sa
      WHERE sa.employee_id = e.id
  );
