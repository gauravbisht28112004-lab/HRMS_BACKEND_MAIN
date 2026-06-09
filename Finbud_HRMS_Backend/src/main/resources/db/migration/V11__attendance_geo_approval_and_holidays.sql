-- ---------------------------------------------------------------------------
-- V11: Attendance module Phase-2 — portal-based punch with TL/HR/Admin approval,
-- office geofence, nightly auto-Absent, and public-holiday CRUD.
--
-- Scope:
--   * Extend `attendance` with geo-coordinates per punch, approval workflow
--     columns, auto-absent / missing-punch flags, and a corrections audit
--     trail (manually_edited_by / manually_edited_at).
--   * New table `office_locations` (one or more offices with an optional
--     geofence). Seed a single "Finbud HQ" row with enforce_geofence=false
--     so existing deployments keep working until an HR explicitly turns the
--     geofence on.
--   * New FK `employees.office_location_id` pointing at `office_locations`.
--     The nightly scheduler uses this to decide whether a punch is in-range.
--   * New table `public_holidays` for admin CRUD (auto-marked as HOLIDAY,
--     scheduler skips these dates).
--   * New table `regularization_requests` so employees can request a
--     correction when they miss a punch (or punched from outside the
--     geofence). HR/Admin/TL approve or reject.
--
-- Constraint strategy:
--   * All new columns are NULL-able / defaulted so the migration is safe
--     on an existing dataset.
--   * CHECK constraints added NOT VALID, then validated, so they do not
--     block migration when old data doesn't yet satisfy them.
-- ---------------------------------------------------------------------------

-- ---------- 1. Office locations (seed one, geofence off by default) ---------

CREATE TABLE IF NOT EXISTS office_locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    address TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    geofence_radius_meters INTEGER NOT NULL DEFAULT 100,
    enforce_geofence BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Seed the default office. Coordinates are left NULL intentionally: HR
-- must set them (and flip enforce_geofence) before the geofence is active.
INSERT INTO office_locations (name, address, enforce_geofence, is_active)
SELECT 'Finbud HQ', 'Primary office — update address & coordinates in Admin → Offices', FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM office_locations WHERE name = 'Finbud HQ');

-- ---------- 2. Attach employees to an office ------------------------------

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS office_location_id BIGINT REFERENCES office_locations(id);

-- Back-fill: point every existing employee at the seeded default office.
UPDATE employees e
SET office_location_id = o.id
FROM office_locations o
WHERE o.name = 'Finbud HQ'
  AND e.office_location_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_employees_office_location ON employees(office_location_id);

-- ---------- 3. Public holidays (admin CRUD) -------------------------------

CREATE TABLE IF NOT EXISTS public_holidays (
    id BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_public_holidays_date ON public_holidays(holiday_date);

-- ---------- 4. Extend attendance with geo + approval + audit columns ------

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS punch_in_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS punch_in_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS punch_in_accuracy_meters DECIMAL(8, 2),
    ADD COLUMN IF NOT EXISTS punch_out_latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS punch_out_longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS punch_out_accuracy_meters DECIMAL(8, 2),
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS approved_by_id BIGINT REFERENCES employees(id),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS is_auto_absent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_missing_punch BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS manually_edited_by_id BIGINT REFERENCES employees(id),
    ADD COLUMN IF NOT EXISTS manually_edited_at TIMESTAMP;

-- Historical rows without an explicit flow get auto-approved so they
-- don't suddenly show up in HR's approval queue.
UPDATE attendance
SET approval_status = 'APPROVED'
WHERE approval_status = 'PENDING'
  AND created_at < CURRENT_TIMESTAMP - INTERVAL '1 day';

-- Guard rails. Added NOT VALID + VALIDATE so the migration stays cheap.
ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS chk_attendance_approval_status;

ALTER TABLE attendance
    ADD CONSTRAINT chk_attendance_approval_status
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')) NOT VALID;

ALTER TABLE attendance VALIDATE CONSTRAINT chk_attendance_approval_status;

CREATE INDEX IF NOT EXISTS idx_attendance_approval_status ON attendance(approval_status);
CREATE INDEX IF NOT EXISTS idx_attendance_date_status ON attendance(attendance_date, approval_status);
CREATE INDEX IF NOT EXISTS idx_attendance_approved_by ON attendance(approved_by_id);

-- ---------- 5. Regularization requests (portal self-service) --------------

CREATE TABLE IF NOT EXISTS regularization_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    attendance_id BIGINT REFERENCES attendance(id),
    attendance_date DATE NOT NULL,
    requested_punch_in TIMESTAMP,
    requested_punch_out TIMESTAMP,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_id BIGINT REFERENCES employees(id),
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE regularization_requests
    DROP CONSTRAINT IF EXISTS chk_regularization_status;

ALTER TABLE regularization_requests
    ADD CONSTRAINT chk_regularization_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')) NOT VALID;

ALTER TABLE regularization_requests VALIDATE CONSTRAINT chk_regularization_status;

CREATE INDEX IF NOT EXISTS idx_regularization_employee_date
    ON regularization_requests(employee_id, attendance_date);
CREATE INDEX IF NOT EXISTS idx_regularization_status ON regularization_requests(status);
