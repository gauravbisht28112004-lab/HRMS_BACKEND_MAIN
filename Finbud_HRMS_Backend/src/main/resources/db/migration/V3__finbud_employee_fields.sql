-- V3: Finbud employee master data fields
-- Adds columns required by the Finbud Noida employee master import.
-- All new columns are nullable. Existing columns are only relaxed where needed.

-- ---------------------------------------------------------------------------
-- employees: personal & identification
-- ---------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS nick_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS father_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS spouse_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS gender VARCHAR(30),
    ADD COLUMN IF NOT EXISTS marital_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS marriage_date DATE,
    ADD COLUMN IF NOT EXISTS blood_group VARCHAR(20),
    ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS personal_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS official_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS country_of_origin VARCHAR(50),
    ADD COLUMN IF NOT EXISTS location VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_physical_challenged BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_international_employee BOOLEAN DEFAULT FALSE;

-- ---------------------------------------------------------------------------
-- employees: employment metadata
-- ---------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS emp_code_on_device INTEGER,
    ADD COLUMN IF NOT EXISTS login_username VARCHAR(50),
    ADD COLUMN IF NOT EXISTS employee_reference_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS extension_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS probation_period_days INTEGER,
    ADD COLUMN IF NOT EXISTS notice_period_days INTEGER,
    ADD COLUMN IF NOT EXISTS confirm_date DATE,
    ADD COLUMN IF NOT EXISTS date_of_resignation DATE,
    ADD COLUMN IF NOT EXISTS last_working_date DATE,
    ADD COLUMN IF NOT EXISTS employee_category VARCHAR(30),
    ADD COLUMN IF NOT EXISTS employee_series VARCHAR(50),
    ADD COLUMN IF NOT EXISTS producer_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS cost_center VARCHAR(100),
    ADD COLUMN IF NOT EXISTS division VARCHAR(100),
    ADD COLUMN IF NOT EXISTS grade VARCHAR(50),
    ADD COLUMN IF NOT EXISTS team_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS manager_name_text VARCHAR(150),
    ADD COLUMN IF NOT EXISTS branch_head VARCHAR(150),
    ADD COLUMN IF NOT EXISTS unit_head VARCHAR(150);

-- ---------------------------------------------------------------------------
-- employees: background verification
-- ---------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS background_check_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS background_verification_date DATE,
    ADD COLUMN IF NOT EXISTS background_agency_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS background_check_remarks TEXT;

-- ---------------------------------------------------------------------------
-- employees: banking extras
-- ---------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS bank_account_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS bank_branch VARCHAR(150),
    ADD COLUMN IF NOT EXISTS salary_payment_mode VARCHAR(30),
    ADD COLUMN IF NOT EXISTS dd_payable_at VARCHAR(150),
    ADD COLUMN IF NOT EXISTS name_as_per_bank VARCHAR(150),
    ADD COLUMN IF NOT EXISTS iban VARCHAR(50);

-- ---------------------------------------------------------------------------
-- employees: statutory (PF / ESI / LWF / Aadhaar / UAN)
-- ---------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS pf_eligible BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pf_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS pf_scheme VARCHAR(50),
    ADD COLUMN IF NOT EXISTS pf_joining_date DATE,
    ADD COLUMN IF NOT EXISTS excess_epf_eligible BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excess_eps_eligible BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS existing_pf_member BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS esi_eligible BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS esi_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS lwf_eligible BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS aadhaar_enrolment_no VARCHAR(30),
    ADD COLUMN IF NOT EXISTS aadhaar_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS uan_number VARCHAR(20);

-- ---------------------------------------------------------------------------
-- employees: operational / misc.
-- ---------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS target_info VARCHAR(255),
    ADD COLUMN IF NOT EXISTS employee_remarks TEXT,
    ADD COLUMN IF NOT EXISTS offer_letter_issued VARCHAR(10),
    ADD COLUMN IF NOT EXISTS id_card_status VARCHAR(10),
    ADD COLUMN IF NOT EXISTS punching_status VARCHAR(10);

-- ---------------------------------------------------------------------------
-- employees: relax NOT NULL on email (import can create rows without email
-- when employeeId + name are present; D2 decision).
-- Keep the existing UNIQUE constraint – Postgres treats multiple NULLs as distinct.
-- ---------------------------------------------------------------------------
ALTER TABLE employees ALTER COLUMN email DROP NOT NULL;

-- ---------------------------------------------------------------------------
-- Indexes (partial unique so multiple NULLs are allowed)
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS ux_employees_emp_code_on_device
    ON employees (emp_code_on_device)
    WHERE emp_code_on_device IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_employees_login_username
    ON employees (login_username)
    WHERE login_username IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_employees_location       ON employees(location);
CREATE INDEX IF NOT EXISTS idx_employees_cost_center    ON employees(cost_center);
CREATE INDEX IF NOT EXISTS idx_employees_structure_cat  ON employees(employee_category);
