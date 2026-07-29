-- =============================================================================
--  Finbud HRMS  —  PRE-GO-LIVE TRIAL DATA RESET
--  Target go-live : 25 June 2026
--  Prepared for review : 23 June 2026
--
--  WHAT THIS DOES (scope you confirmed)
--    attendance               -> WIPE
--    payroll (payslips)       -> WIPE
--    leave_requests           -> WIPE  (kept consistent with the balance reset)
--    leave_balances           -> WIPE, then RE-INITIALISE fresh for 2026
--    regularization_requests  -> WIPE  (forced: it is an FK child of attendance,
--                                       the attendance delete fails otherwise)
--
--  WHAT THIS KEEPS (your real go-live setup — untouched)
--    employees, users, roles, departments, salary_structures, shift_types,
--    office_locations, public_holidays, system_config
--
--  !! THIS IS A MANUAL OPERATIONS SCRIPT.
--     Do NOT put it in src/main/resources/db/migration — it is NOT a Flyway
--     migration and must never run automatically on app boot.
--
--  !! Run it in psql so the transaction works:
--        psql "$DATABASE_URL" -f reset_trial_data_pre_golive.sql
--
--  !! READ THE NOTES AT THE BOTTOM before running (backup, schedulers, Redis,
--     payslip files). The backup in STEP 0 is mandatory — there is no undo.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- STEP 0.  MANDATORY BACKUP  — run in your shell FIRST, not inside psql:
--
--     pg_dump "$DATABASE_URL" -Fc -f finbud_hrms_pre_reset_2026-06-24.dump
--
--   Confirm the .dump file exists and has a sensible size before continuing.
-- -----------------------------------------------------------------------------


BEGIN;

-- STEP 1.  Pre-counts — see exactly what you are about to remove.
SELECT 'attendance'              AS label, COUNT(*) AS row_count FROM attendance
UNION ALL SELECT 'regularization_requests', COUNT(*) FROM regularization_requests
UNION ALL SELECT 'payroll',                  COUNT(*) FROM payroll
UNION ALL SELECT 'leave_requests',           COUNT(*) FROM leave_requests
UNION ALL SELECT 'leave_balances',           COUNT(*) FROM leave_balances;


-- STEP 2.  regularization_requests  (must be deleted BEFORE attendance:
--          regularization_requests.attendance_id -> attendance.id, no cascade).
DELETE FROM regularization_requests;


-- STEP 3.  attendance
DELETE FROM attendance;


-- STEP 4.  payroll (payslips)
DELETE FROM payroll;


-- STEP 5.  leave_requests — trial leave applications. Removed so they stay
--          consistent with the balances we re-seed below (a leftover approved
--          request would otherwise contradict a "0 used" balance). No child
--          tables reference leave_requests, so this is a straight delete.
DELETE FROM leave_requests;


-- STEP 6.  leave_balances — wipe all trial rows, then re-seed clean opening
--          balances for the current year for every ACTIVE employee.
--
--          Finbud policy (read from the code, locked 2026-04-24):
--            casual + sick : 6.00 days/yr  (one shared pool)
--            paid / earned : 6.00 days/yr
--            LOP           : not pre-allocated (0)
--          Full annual allocation, NOT pro-rated (matches your mid-year
--          joiner policy). No carry-forward (trial history is discarded).
--          Only ACTIVE employees get a balance — same as the yearly scheduler.
DELETE FROM leave_balances;

INSERT INTO leave_balances (
    employee_id, year,
    casual_sick_allocated, casual_sick_used, casual_sick_carried_forward,
    paid_leave_allocated,  paid_leave_used,  paid_leave_carried_forward,
    lop_days,
    created_at, updated_at, version
)
SELECT
    e.id, 2026,
    6.00, 0.00, 0.00,
    6.00, 0.00, 0.00,
    0.00,
    NOW(), NOW(), 0
FROM employees e
WHERE e.status = 'ACTIVE';


-- STEP 7.  Post-counts — sanity check before you commit.
--          Expect: attendance = 0, regularization_requests = 0, payroll = 0,
--          leave_requests = 0, and leave_balances = your ACTIVE employee count.
SELECT 'attendance'                  AS label, COUNT(*) AS row_count FROM attendance
UNION ALL SELECT 'regularization_requests',       COUNT(*) FROM regularization_requests
UNION ALL SELECT 'payroll',                        COUNT(*) FROM payroll
UNION ALL SELECT 'leave_requests',                 COUNT(*) FROM leave_requests
UNION ALL SELECT 'leave_balances (re-seeded)',     COUNT(*) FROM leave_balances
UNION ALL SELECT 'active employees (expected)',    COUNT(*) FROM employees WHERE status = 'ACTIVE';


-- -----------------------------------------------------------------------------
-- DECISION POINT — review the post-counts above, then choose ONE:
--     • counts look right  ->  type:  COMMIT;
--     • anything is wrong  ->  type:  ROLLBACK;   (undoes everything above)
--
-- The script intentionally does NOT auto-commit. Nothing is permanent until
-- you run COMMIT yourself.
-- -----------------------------------------------------------------------------


-- =============================================================================
-- NOTES — things this script cannot do for you
--
-- 1. BACKUP (Step 0) is not optional. After COMMIT there is no undo.
--
-- 2. SCHEDULER TIMING. These jobs auto-create the exact data you are clearing:
--       auto-absent       00:30 IST daily  — marks YESTERDAY absent for anyone
--                                            with no punch. Run the reset on the
--                                            24th and the 00:30 job on the 25th
--                                            re-inserts "absent" rows for the
--                                            24th (a trial day).
--       missing-punch     21:00 IST daily
--       attendance close  23:59 IST daily
--       payroll           01:00 IST, 1st of month
--     Recommended: run this reset on the morning of the 25th, AFTER 00:30 and
--     BEFORE staff start punching — or temporarily disable the schedulers while
--     you reset. Coordinate with whoever owns the deploy.
--
-- 3. REDIS CACHE. The app caches some reads in Redis. After the wipe, flush it
--    (or restart the app) so stale values don't show:
--       redis-cli -h <host> -p <port> FLUSHDB
--
-- 4. PAYSLIP FILES. Deleting payroll rows does NOT delete payslip PDFs already
--    written to object storage (payroll.payslip_url). Clean those from your
--    bucket separately if trial payslips were generated.
--
-- 5. ID COUNTERS are left as-is (your "just delete rows" preference). New
--    attendance/payroll IDs continue from the current sequence value. Safe,
--    no action needed.
-- =============================================================================
