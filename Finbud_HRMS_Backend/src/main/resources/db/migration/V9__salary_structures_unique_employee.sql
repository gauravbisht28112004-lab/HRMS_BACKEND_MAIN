-- V9: enforce one-salary-structure-per-employee at the DB level.
--
-- Background
-- ----------
-- The JPA mapping on SalaryStructure.employee has @JoinColumn(unique = true),
-- and Employee.salaryStructure is @OneToOne(mappedBy="employee"). But the V1
-- CREATE TABLE for salary_structures never added a UNIQUE constraint on
-- employee_id, so duplicates were physically allowed. Hibernate's back-ref
-- fetch uses SingleUniqueKeyEntityLoaderStandard.load(), which fails hard
-- with:
--     HibernateException: More than one row with the given identifier was
--     found: <id>, for class: SalaryStructure
-- on the very first Employee load at startup (DataInitializer). We hit this
-- on an existing local DB where the import / salary flow had created two
-- rows with the same employee_id.
--
-- Fix
-- ---
-- 1. Deduplicate: keep exactly one row per employee_id, preferring the
--    active row, then the most recently-created (max(id)) row.
-- 2. Add a real UNIQUE constraint so the invariant is enforced going
--    forward and the JPA mapping becomes DB-accurate.
--
-- If your DB already has no duplicates, the CTE deletes zero rows — the
-- migration is idempotent-by-effect.
-- ---------------------------------------------------------------------------

-- 1. Deduplicate.
WITH ranked AS (
    SELECT
        id,
        employee_id,
        ROW_NUMBER() OVER (
            PARTITION BY employee_id
            ORDER BY
                CASE WHEN is_active THEN 0 ELSE 1 END,  -- active first
                id DESC                                 -- then newest
        ) AS rn
    FROM salary_structures
)
DELETE FROM salary_structures
WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

-- 2. Enforce the invariant.
-- Use ADD CONSTRAINT IF NOT EXISTS semantics via DO block so rerunning
-- against a partially-migrated DB is safe.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_salary_structures_employee'
    ) THEN
        ALTER TABLE salary_structures
            ADD CONSTRAINT uq_salary_structures_employee UNIQUE (employee_id);
    END IF;
END$$;
