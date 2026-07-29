-- ============================================================================
-- HRMS module removal — database cleanup
-- ============================================================================
-- Drops the tables and employee columns for the modules removed from the app:
--   attendance, regularisation, payroll, salary, leave, shift,
--   office location, public holidays.
--
-- Target:   PostgreSQL / Supabase
-- WARNING:  Destructive and irreversible. TAKE A BACKUP FIRST.
-- Review before running. Kept modules (notifications, announcements,
-- commitments, targets, departments, audit, employees, users) are untouched.
-- ============================================================================

BEGIN;

-- 1. Drop the denormalised FK columns on employees that pointed at the
--    shift / office-location tables.
ALTER TABLE employees DROP COLUMN IF EXISTS shift_type_id;
ALTER TABLE employees DROP COLUMN IF EXISTS office_location_id;
DROP INDEX IF EXISTS idx_employees_office_location;

-- 2. Drop the module tables. CASCADE also removes dependent foreign keys and
--    child rows, so order does not matter.
DROP TABLE IF EXISTS regularization_requests CASCADE;
DROP TABLE IF EXISTS attendance              CASCADE;
DROP TABLE IF EXISTS payroll                 CASCADE;
DROP TABLE IF EXISTS salary_structures       CASCADE;
DROP TABLE IF EXISTS leave_requests          CASCADE;
DROP TABLE IF EXISTS leave_balances          CASCADE;
DROP TABLE IF EXISTS shift_weekly_off_days   CASCADE;
DROP TABLE IF EXISTS shift_assignments       CASCADE;
DROP TABLE IF EXISTS shift_types             CASCADE;
DROP TABLE IF EXISTS office_locations        CASCADE;
DROP TABLE IF EXISTS public_holidays         CASCADE;

COMMIT;

-- ----------------------------------------------------------------------------
-- Notes
-- ----------------------------------------------------------------------------
--  * 'notifications' is KEPT — the notification feature remains in the app.
--  * Employee master fields (PAN, Aadhaar, UAN, PF, ESI, bank details,
--    salary_payment_mode) are KEPT in the app and in this script, because the
--    employee import and profile still use them as HR master data. If you also
--    want those columns dropped, say so and I'll add the ALTER TABLE lines.
--  * The Flyway migration files that CREATED these tables are intentionally
--    left in place — deleting them would break Flyway checksum validation on
--    any existing database. This script drops the objects they created.
