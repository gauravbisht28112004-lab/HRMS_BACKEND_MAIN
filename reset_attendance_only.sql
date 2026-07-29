-- =============================================================================
--  Finbud HRMS  —  RESET ALL ATTENDANCE RECORDS
--  Prepared: 23 June 2026   |   Target go-live: 25 June 2026
--
--  Wipes every row in `attendance`. Nothing else is reset (payroll, leave,
--  employees and all master/config data are left untouched).
--
--  ONE UNAVOIDABLE CATCH: regularization_requests.attendance_id is a foreign
--  key to attendance(id). Postgres will not let you delete attendance while
--  those rows point at it. This script clears those trial regularization
--  requests first (they are attendance-correction requests from the same
--  trials). If you would rather KEEP them, use the commented alternative that
--  only detaches them.
--
--  !! MANUAL OPS SCRIPT — do NOT place in src/main/resources/db/migration.
--     It is NOT a Flyway migration and must never run automatically on boot.
--  !! Run it in psql:  psql "$DATABASE_URL" -f reset_attendance_only.sql
-- =============================================================================


-- STEP 0.  MANDATORY BACKUP — run in your shell FIRST, not inside psql:
--     pg_dump "$DATABASE_URL" -Fc -f finbud_hrms_pre_attendance_reset.dump
--   Confirm the .dump file exists and has a sensible size before continuing.


BEGIN;

-- Pre-counts.
SELECT 'attendance (before)'              AS label, COUNT(*) AS row_count FROM attendance
UNION ALL SELECT 'regularization_requests (before)', COUNT(*) FROM regularization_requests;


-- Clear the FK children: trial attendance-correction (regularization) requests.
DELETE FROM regularization_requests;

-- --- ALTERNATIVE (use INSTEAD of the DELETE above if you want to KEEP the
-- --- regularization_requests rows and only break their link to attendance):
-- UPDATE regularization_requests SET attendance_id = NULL WHERE attendance_id IS NOT NULL;


-- Wipe all attendance.
DELETE FROM attendance;


-- Post-count — expect attendance = 0.
SELECT 'attendance (after)' AS label, COUNT(*) AS row_count FROM attendance;


-- -----------------------------------------------------------------------------
-- DECISION POINT — review the count above, then choose ONE:
--     • count is 0 and that's right ->  type:  COMMIT;
--     • anything looks wrong        ->  type:  ROLLBACK;   (undoes everything)
-- Nothing is permanent until you run COMMIT yourself.
-- -----------------------------------------------------------------------------


-- =============================================================================
-- NOTES
-- 1. After COMMIT there is no undo — the Step 0 backup is your only safety net.
-- 2. Flush Redis (or restart the app) so cached attendance doesn't linger:
--      redis-cli -h <host> -p <port> FLUSHDB
-- 3. Scheduler timing: the 00:30 IST auto-absent job marks YESTERDAY absent for
--    anyone with no punch. Run this on the morning of the 25th, after 00:30 and
--    before staff start punching, so it doesn't immediately repopulate the
--    table with rows for the 24th.
-- =============================================================================
