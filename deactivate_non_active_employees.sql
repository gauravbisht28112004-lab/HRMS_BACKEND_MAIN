-- =============================================================================
--  Finbud HRMS  —  DEACTIVATE NON-ROSTER EMPLOYEES (June active = 112)
--
--  WHAT THIS DOES
--    Sets status = 'INACTIVE' for every employee whose code is NOT in the
--    112-person June active roster. Only the 112 (plus any ADMIN/HR account,
--    protected below) stay ACTIVE.
--
--  NON-DESTRUCTIVE: no rows are deleted. All attendance, payroll, leave,
--  salary and login history is kept. Fully reversible (set status back).
--
--  RUN IN psql (transaction needs it):
--      psql "$DATABASE_URL" -f deactivate_non_active_employees.sql
--
--  BACKUP FIRST (mandatory, even though this is reversible):
--      pg_dump "$DATABASE_URL" -Fc -f finbud_pre_deactivate.dump
--
--  The script STOPS before COMMIT. Review the printed lists, then commit
--  yourself (uncomment COMMIT;) or type ROLLBACK; to undo everything.
-- =============================================================================

BEGIN;

-- 1. Load the 112 codes to KEEP ACTIVE -----------------------------------------
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
  ('ND33382'),
  ('ND33385'),
  ('ND33391'),
  ('ND33395'),
  ('ND33396'),
  ('ND33397'),
  ('ND33401'),
  ('ND33407'),
  ('ND33408'),
  ('ND33413'),
  ('ND33421'),
  ('ND33423'),
  ('ND33426'),
  ('ND33427'),
  ('ND33437'),
  ('ND33438'),
  ('ND33442'),
  ('ND33447'),
  ('ND33449'),
  ('ND33450'),
  ('ND33451'),
  ('ND33453'),
  ('ND33456'),
  ('ND33469'),
  ('ND33471'),
  ('ND33473'),
  ('ND33475'),
  ('ND33480'),
  ('ND33482'),
  ('ND33484'),
  ('ND33485'),
  ('ND33487'),
  ('ND33488'),
  ('ND33489'),
  ('ND33490'),
  ('ND33491'),
  ('ND33493'),
  ('ND33494'),
  ('ND33495'),
  ('ND33497'),
  ('ND33499'),
  ('ND33501'),
  ('ND33502'),
  ('ND33503'),
  ('ND33505'),
  ('ND33506'),
  ('ND33507'),
  ('ND33508'),
  ('ND33510'),
  ('ND33511'),
  ('ND33512'),
  ('ND33513'),
  ('ND33514'),
  ('ND33515'),
  ('ND33516'),
  ('ND33517'),
  ('ND33518'),
  ('ND33519'),
  ('ND33520'),
  ('ND33521'),
  ('ND33522'),
  ('ND33523'),
  ('ND33524'),
  ('ND33525'),
  ('ND33526'),
  ('ND33527'),
  ('ND33528'),
  ('ND33529'),
  ('ND33530'),
  ('ND33531'),
  ('ND33532'),
  ('ND33533'),
  ('ND33534'),
  ('ND33535'),
  ('ND33536'),
  ('ND33537'),
  ('ND33538'),
  ('ND33539'),
  ('ND33540'),
  ('ND33541'),
  ('ND33542'),
  ('ND33543');

-- sanity: must print 112
SELECT COUNT(*) AS keep_codes_loaded FROM keep_codes;

-- 2. Pre-counts ----------------------------------------------------------------
SELECT status, COUNT(*) AS n FROM employees GROUP BY status ORDER BY status;

-- 3. REVIEW: exactly who will be deactivated (read this before committing) ------
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

-- 3b. REVIEW: codes in your sheet that are NOT in the DB (won't be activated;
--     these are the ones you still need to IMPORT first) -----------------------
SELECT k.code AS sheet_code_missing_from_db
FROM keep_codes k
LEFT JOIN employees e ON UPPER(TRIM(e.employee_id)) = k.code
WHERE e.id IS NULL
ORDER BY k.code;

-- 4. The update ----------------------------------------------------------------
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

-- 5. Post-counts: ACTIVE should now equal your 112 (+ any protected admin/HR) --
SELECT status, COUNT(*) AS n FROM employees GROUP BY status ORDER BY status;
SELECT COUNT(*) AS active_after FROM employees WHERE status = 'ACTIVE';

-- 6. DECISION POINT ------------------------------------------------------------
--    counts look right  ->  uncomment and run:   COMMIT;
--    anything wrong      ->  run:                 ROLLBACK;
-- COMMIT;
