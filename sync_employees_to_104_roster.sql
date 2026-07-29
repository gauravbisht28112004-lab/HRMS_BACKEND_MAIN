-- =============================================================================
--  Finbud HRMS  —  SYNC EMPLOYEES TO JULY ACTIVE ROSTER (active = 104)
--
--  WHAT THIS DOES
--    Makes the 104-person roster in ActiveEmpFinbud.xlsx the ONLY ACTIVE
--    employees:
--      (A) DEACTIVATE  — every employee currently ACTIVE whose code is NOT in
--          the 104 roster is set to status = 'INACTIVE'.
--          ADMIN / HR login accounts are protected and stay ACTIVE.
--      (B) REACTIVATE  — any of the 104 that are currently NOT ACTIVE
--          (INACTIVE / RESIGNED / TERMINATED / SUSPENDED / ON_NOTICE) are set
--          back to status = 'ACTIVE', so the sheet becomes the exact active set.
--
--  NON-DESTRUCTIVE: no rows are deleted. All attendance, payroll, leave,
--  salary and login history is kept. Fully reversible (set status back).
--
--  RUN IN psql (transaction needs it):
--      psql "$DATABASE_URL" -f sync_employees_to_104_roster.sql
--
--  BACKUP FIRST (mandatory, even though this is reversible):
--      pg_dump "$DATABASE_URL" -Fc -f finbud_pre_sync_104.dump
--
--  The script STOPS before COMMIT. Review the printed lists, then commit
--  yourself (uncomment COMMIT;) or type ROLLBACK; to undo everything.
--
--  HEADS-UP (from GO_LIVE_RESET_RUNBOOK): only ACTIVE employees get a yearly
--  leave_balances row. Anyone this script REACTIVATES may be missing a 2026
--  leave_balances row and will hit errors applying for leave until one exists.
--  Check step 3c below and seed balances for reactivated staff if needed.
-- =============================================================================

BEGIN;

-- 1. Load the 104 codes that define the ACTIVE roster -------------------------
CREATE TEMP TABLE keep_codes (code VARCHAR(20) PRIMARY KEY) ON COMMIT DROP;
INSERT INTO keep_codes (code) VALUES
  ('ND33004'),
  ('ND33006'),
  ('ND33011'),
  ('ND33038'),
  ('ND33137'),
  ('ND33194'),
  ('ND33033'),
  ('ND33229'),
  ('ND33271'),
  ('ND33177'),
  ('ND33273'),
  ('ND33291'),
  ('ND33155'),
  ('ND33301'),
  ('ND33302'),
  ('ND33306'),
  ('ND33297'),
  ('ND33311'),
  ('ND33299'),
  ('ND33008'),
  ('ND33317'),
  ('ND33178'),
  ('ND33323'),
  ('ND33348'),
  ('ND33359'),
  ('ND33361'),
  ('ND33362'),
  ('ND33363'),
  ('ND33365'),
  ('ND33368'),
  ('ND33371'),
  ('ND33375'),
  ('ND33376'),
  ('ND33382'),
  ('ND33385'),
  ('ND33386'),
  ('ND33387'),
  ('ND33391'),
  ('ND33395'),
  ('ND33396'),
  ('ND33397'),
  ('ND33398'),
  ('ND33401'),
  ('ND33407'),
  ('ND33408'),
  ('ND33413'),
  ('ND33417'),
  ('ND33419'),
  ('ND33421'),
  ('ND33423'),
  ('ND33426'),
  ('ND33427'),
  ('ND33429'),
  ('ND33433'),
  ('ND33437'),
  ('ND33438'),
  ('ND33442'),
  ('ND33445'),
  ('ND33447'),
  ('ND33449'),
  ('ND33450'),
  ('ND33451'),
  ('ND33453'),
  ('ND33304'),
  ('ND33456'),
  ('ND33458'),
  ('ND33463'),
  ('ND33469'),
  ('ND33470'),
  ('ND33471'),
  ('ND33473'),
  ('ND33474'),
  ('ND33475'),
  ('ND33476'),
  ('ND33477'),
  ('ND33478'),
  ('ND33479'),
  ('ND33480'),
  ('ND33481'),
  ('ND33482'),
  ('ND33483'),
  ('ND33484'),
  ('ND33485'),
  ('ND33486'),
  ('ND33487'),
  ('ND33488'),
  ('ND33489'),
  ('ND33490'),
  ('ND33491'),
  ('ND33492'),
  ('ND33493'),
  ('ND33494'),
  ('ND33495'),
  ('ND33496'),
  ('ND33497'),
  ('ND33498'),
  ('ND33499'),
  ('ND33500'),
  ('ND33501'),
  ('ND33502'),
  ('ND33503'),
  ('ND33504'),
  ('ND33505'),
  ('ND33506');

-- sanity: must print 104
SELECT COUNT(*) AS keep_codes_loaded FROM keep_codes;

-- 2. Pre-counts ---------------------------------------------------------------
SELECT status, COUNT(*) AS n FROM employees GROUP BY status ORDER BY status;

-- 3a. REVIEW: who will be DEACTIVATED (currently ACTIVE, not on the sheet,
--     not a protected ADMIN/HR account). Read this before committing. --------
SELECT employee_id, first_name, last_name, status
FROM employees
WHERE status = 'ACTIVE'
  AND UPPER(TRIM(employee_id)) NOT IN (SELECT code FROM keep_codes)
  AND id NOT IN (
        SELECT u.employee_id FROM users u
        JOIN user_roles ur ON ur.user_id = u.id
        JOIN roles r       ON r.id = ur.role_id
        WHERE r.name IN ('ROLE_ADMIN','ADMIN','ROLE_HR','HR')
          AND u.employee_id IS NOT NULL
      )
ORDER BY employee_id;

-- 3b. REVIEW: codes on the sheet that are NOT in the DB. These cannot be
--     activated because the employee does not exist yet — IMPORT them first. -
SELECT k.code AS sheet_code_missing_from_db
FROM keep_codes k
LEFT JOIN employees e ON UPPER(TRIM(e.employee_id)) = k.code
WHERE e.id IS NULL
ORDER BY k.code;

-- 3c. REVIEW: roster members currently NOT ACTIVE that step (B) will flip to
--     ACTIVE (shows their current status so nothing is reactivated blindly). -
SELECT employee_id, first_name, last_name, status AS current_status
FROM employees
WHERE UPPER(TRIM(employee_id)) IN (SELECT code FROM keep_codes)
  AND status <> 'ACTIVE'
ORDER BY status, employee_id;

-- 4A. DEACTIVATE non-roster active employees (ADMIN/HR protected) ------------
UPDATE employees
SET status = 'INACTIVE', updated_at = NOW()
WHERE status = 'ACTIVE'
  AND UPPER(TRIM(employee_id)) NOT IN (SELECT code FROM keep_codes)
  AND id NOT IN (
        SELECT u.employee_id FROM users u
        JOIN user_roles ur ON ur.user_id = u.id
        JOIN roles r       ON r.id = ur.role_id
        WHERE r.name IN ('ROLE_ADMIN','ADMIN','ROLE_HR','HR')
          AND u.employee_id IS NOT NULL
      );

-- 4B. REACTIVATE roster members that are not currently ACTIVE ----------------
UPDATE employees
SET status = 'ACTIVE', updated_at = NOW()
WHERE UPPER(TRIM(employee_id)) IN (SELECT code FROM keep_codes)
  AND status <> 'ACTIVE';

-- 5. Post-counts: ACTIVE should now equal the number of the 104 that exist in
--    the DB, plus any protected ADMIN/HR account not on the sheet. -----------
SELECT status, COUNT(*) AS n FROM employees GROUP BY status ORDER BY status;
SELECT COUNT(*) AS active_after FROM employees WHERE status = 'ACTIVE';

-- how many of the 104 actually exist in the DB (active_after should be this
-- number + any protected admin/HR not on the sheet):
SELECT COUNT(*) AS roster_present_in_db
FROM keep_codes k JOIN employees e ON UPPER(TRIM(e.employee_id)) = k.code;

-- 6. DECISION POINT ----------------------------------------------------------
--    counts look right  ->  uncomment and run:   COMMIT;
--    anything wrong      ->  run:                 ROLLBACK;
-- COMMIT;
