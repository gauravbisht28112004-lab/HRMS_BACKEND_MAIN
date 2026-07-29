-- V4: CTC / NTH salary + payroll model
-- Extends salary_structures and payroll to support Finbud's CTC-to-NTH
-- compensation model (CONTRACT / MANAGEMENT / HIGHLY_SKILLED).
--
-- Strategy:
--   * Keep all existing component columns (basic_salary, hra, da, ...) but make
--     them nullable. They become deprecated — new records should not rely on them.
--   * Add CTC/NTH fields and structure_type.
--
-- Contract formula:   NTH = monthly_gross_ctc * 0.95,  tds = monthly_gross_ctc * 0.05
-- Management / HS:    NTH = monthly_gross_ctc - employer_pf - employee_pf - lwf_amount - tds_amount + adjustments
-- ---------------------------------------------------------------------------

-- ==== salary_structures ====================================================

ALTER TABLE salary_structures
    ALTER COLUMN basic_salary DROP NOT NULL,
    ALTER COLUMN hra          DROP NOT NULL,
    ALTER COLUMN annual_ctc   DROP NOT NULL;

ALTER TABLE salary_structures
    ADD COLUMN IF NOT EXISTS structure_type     VARCHAR(30),
    ADD COLUMN IF NOT EXISTS monthly_gross_ctc  DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS nth                DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS tds_amount         DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS tds_rate_percent   DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS employer_pf        DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS employee_pf        DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS employer_esi       DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS employee_esi       DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS lwf_amount         DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS incentives         DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS other_deductions   DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS num_of_months      INTEGER;

-- Widen annual_ctc to match the new precision used for monthly_gross_ctc
-- (existing DECIMAL(12,2) is fine for current data but let's align precision).
ALTER TABLE salary_structures
    ALTER COLUMN annual_ctc TYPE DECIMAL(19,4);

CREATE INDEX IF NOT EXISTS idx_salary_struct_type      ON salary_structures(structure_type);
CREATE INDEX IF NOT EXISTS idx_salary_struct_emp_active ON salary_structures(employee_id, is_active);

-- ==== payroll ==============================================================

-- Relax NOT NULL on legacy component-earned columns. New CTC payroll will not populate them.
ALTER TABLE payroll
    ALTER COLUMN basic_earned DROP NOT NULL,
    ALTER COLUMN hra_earned   DROP NOT NULL;

ALTER TABLE payroll
    ADD COLUMN IF NOT EXISTS structure_type     VARCHAR(30),
    ADD COLUMN IF NOT EXISTS monthly_gross_ctc  DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS working_days       INTEGER,
    ADD COLUMN IF NOT EXISTS lop_days           DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS employer_pf        DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS employee_pf        DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS employer_esi       DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS employee_esi       DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS lwf_amount         DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS tds_amount         DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS incentive_amount   DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS adjustments        DECIMAL(19,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS adjustment_reason  VARCHAR(500);

-- Widen money columns to 19,4 for consistency with CTC model
ALTER TABLE payroll
    ALTER COLUMN gross_earnings    TYPE DECIMAL(19,4),
    ALTER COLUMN pf_deduction      TYPE DECIMAL(19,4),
    ALTER COLUMN esi_deduction     TYPE DECIMAL(19,4),
    ALTER COLUMN pt_deduction      TYPE DECIMAL(19,4),
    ALTER COLUMN lop_deduction     TYPE DECIMAL(19,4),
    ALTER COLUMN other_deductions  TYPE DECIMAL(19,4),
    ALTER COLUMN total_deductions  TYPE DECIMAL(19,4),
    ALTER COLUMN net_pay           TYPE DECIMAL(19,4),
    ALTER COLUMN overtime_pay      TYPE DECIMAL(19,4);

CREATE INDEX IF NOT EXISTS idx_payroll_structure_type ON payroll(structure_type);
