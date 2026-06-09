-- Align schema with current JPA entity model

-- BaseEntity audit/version columns
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE shift_types
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE salary_structures
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE shift_assignments
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payroll
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE system_config
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE audit_logs
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::text;

-- Role permissions as an element collection
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission)
);

INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN LATERAL jsonb_array_elements_text(COALESCE(r.permissions, '[]'::jsonb)) AS p(permission)
ON CONFLICT DO NOTHING;

-- Shift weekly off days as an element collection
CREATE TABLE IF NOT EXISTS shift_weekly_off_days (
    shift_type_id BIGINT NOT NULL REFERENCES shift_types(id) ON DELETE CASCADE,
    off_day INTEGER NOT NULL,
    PRIMARY KEY (shift_type_id, off_day)
);

INSERT INTO shift_weekly_off_days (shift_type_id, off_day)
SELECT st.id, d.off_day
FROM shift_types st
CROSS JOIN LATERAL unnest(COALESCE(st.weekly_off_days, ARRAY[]::INTEGER[])) AS d(off_day)
ON CONFLICT DO NOTHING;
