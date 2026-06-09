-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create tables
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    manager_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shift_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_duration_minutes INTEGER DEFAULT 60,
    grace_period_minutes INTEGER DEFAULT 10,
    weekly_off_days INTEGER[],
    is_night_shift BOOLEAN DEFAULT FALSE,
    overtime_threshold_hours DECIMAL(4,2) DEFAULT 8.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(10),
    date_of_joining DATE NOT NULL,
    department_id BIGINT REFERENCES departments(id),
    designation VARCHAR(100),
    manager_id BIGINT REFERENCES employees(id),
    employment_type VARCHAR(20) DEFAULT 'FULL_TIME',
    shift_type_id BIGINT REFERENCES shift_types(id),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(50),
    bank_account_number VARCHAR(30),
    bank_ifsc_code VARCHAR(20),
    bank_name VARCHAR(100),
    pan_number VARCHAR(10),
    aadhaar_number VARCHAR(12),
    profile_picture_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraint for manager
ALTER TABLE departments ADD CONSTRAINT fk_dept_manager
    FOREIGN KEY (manager_id) REFERENCES employees(id);

CREATE TABLE salary_structures (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    basic_salary DECIMAL(12,2) NOT NULL,
    hra DECIMAL(12,2) NOT NULL,
    da DECIMAL(12,2) DEFAULT 0,
    conveyance_allowance DECIMAL(12,2) DEFAULT 0,
    medical_allowance DECIMAL(12,2) DEFAULT 0,
    special_allowance DECIMAL(12,2) DEFAULT 0,
    pf_employee_percentage DECIMAL(5,2) DEFAULT 12.0,
    pf_employer_percentage DECIMAL(5,2) DEFAULT 12.0,
    esi_employee_percentage DECIMAL(5,2) DEFAULT 0.75,
    esi_employer_percentage DECIMAL(5,2) DEFAULT 3.25,
    professional_tax_amount DECIMAL(10,2) DEFAULT 200.00,
    annual_ctc DECIMAL(12,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shift_assignments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    shift_type_id BIGINT NOT NULL REFERENCES shift_types(id),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT UNIQUE REFERENCES employees(id),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    password_changed_at TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    attendance_date DATE NOT NULL,
    shift_type_id BIGINT REFERENCES shift_types(id),
    punch_in TIMESTAMP,
    punch_out TIMESTAMP,
    working_hours DECIMAL(4,2),
    break_hours DECIMAL(4,2) DEFAULT 1.0,
    status VARCHAR(20) DEFAULT 'PRESENT',
    is_late BOOLEAN DEFAULT FALSE,
    late_minutes INTEGER DEFAULT 0,
    is_early_leave BOOLEAN DEFAULT FALSE,
    early_leave_minutes INTEGER DEFAULT 0,
    is_half_day BOOLEAN DEFAULT FALSE,
    is_overtime BOOLEAN DEFAULT FALSE,
    overtime_hours DECIMAL(5,2) DEFAULT 0,
    device_id VARCHAR(50),
    punch_in_location VARCHAR(255),
    punch_out_location VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, attendance_date)
);

CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    year INTEGER NOT NULL,
    casual_leave_allocated DECIMAL(5,2) DEFAULT 12.0,
    casual_leave_used DECIMAL(5,2) DEFAULT 0.0,
    sick_leave_allocated DECIMAL(5,2) DEFAULT 12.0,
    sick_leave_used DECIMAL(5,2) DEFAULT 0.0,
    paid_leave_allocated DECIMAL(5,2) DEFAULT 0.0,
    paid_leave_used DECIMAL(5,2) DEFAULT 0.0,
    wfh_days_allocated INTEGER DEFAULT 12,
    wfh_days_used INTEGER DEFAULT 0,
    lop_days DECIMAL(5,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, year)
);

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_requested DECIMAL(5,2) NOT NULL,
    reason TEXT NOT NULL,
    contact_during_leave VARCHAR(100),
    manager_id BIGINT REFERENCES employees(id),
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT REFERENCES employees(id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    is_half_day BOOLEAN DEFAULT FALSE,
    half_day_type VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payroll (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    total_working_days INTEGER NOT NULL,
    present_days DECIMAL(5,2) NOT NULL,
    absent_days DECIMAL(5,2) DEFAULT 0,
    leave_days DECIMAL(5,2) DEFAULT 0,
    half_days DECIMAL(5,2) DEFAULT 0,
    weekly_off_days INTEGER DEFAULT 0,
    holidays INTEGER DEFAULT 0,
    basic_earned DECIMAL(12,2) NOT NULL,
    hra_earned DECIMAL(12,2) NOT NULL,
    da_earned DECIMAL(12,2) DEFAULT 0,
    conveyance_earned DECIMAL(12,2) DEFAULT 0,
    medical_earned DECIMAL(12,2) DEFAULT 0,
    special_earned DECIMAL(12,2) DEFAULT 0,
    total_allowances DECIMAL(12,2) DEFAULT 0,
    gross_earnings DECIMAL(12,2) NOT NULL,
    pf_deduction DECIMAL(12,2) DEFAULT 0,
    esi_deduction DECIMAL(12,2) DEFAULT 0,
    pt_deduction DECIMAL(12,2) DEFAULT 0,
    lop_deduction DECIMAL(12,2) DEFAULT 0,
    other_deductions DECIMAL(12,2) DEFAULT 0,
    total_deductions DECIMAL(12,2) NOT NULL,
    net_pay DECIMAL(12,2) NOT NULL,
    overtime_hours DECIMAL(5,2) DEFAULT 0,
    overtime_pay DECIMAL(12,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT REFERENCES employees(id),
    approved_at TIMESTAMP,
    paid_at TIMESTAMP,
    payslip_generated BOOLEAN DEFAULT FALSE,
    payslip_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, month, year)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    performed_by BIGINT REFERENCES employees(id),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    reason TEXT
);

CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description TEXT,
    updated_by BIGINT REFERENCES employees(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_embeddings (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_employees_dept ON employees(department_id);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_attendance_emp_date ON attendance(employee_id, attendance_date);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_leave_requests_emp ON leave_requests(employee_id);
CREATE INDEX idx_payroll_emp ON payroll(employee_id);
CREATE INDEX idx_payroll_month ON payroll(month, year);
CREATE INDEX idx_ai_embeddings_entity ON ai_embeddings(entity_type, entity_id);
CREATE INDEX idx_ai_embeddings_vector ON ai_embeddings USING ivfflat (embedding vector_cosine_ops);

-- Insert default roles
INSERT INTO roles (name, description, permissions) VALUES
('ROLE_ADMIN', 'Administrator with full access', '["ALL"]'::jsonb),
('ROLE_HR', 'HR Manager with employee and payroll access', '["EMPLOYEE_READ","EMPLOYEE_WRITE","ATTENDANCE_READ","ATTENDANCE_WRITE","LEAVE_READ","LEAVE_APPROVE","PAYROLL_READ","PAYROLL_GENERATE","PAYROLL_APPROVE","REPORT_READ","REPORT_EXPORT"]'::jsonb),
('ROLE_MANAGER', 'Department Manager with team access', '["EMPLOYEE_READ","ATTENDANCE_READ","LEAVE_READ","LEAVE_APPROVE","REPORT_READ"]'::jsonb),
('ROLE_EMPLOYEE', 'Regular Employee with self access', '["EMPLOYEE_READ","ATTENDANCE_READ","LEAVE_APPLY"]'::jsonb);

-- Insert default system config
INSERT INTO system_config (config_key, config_value, description) VALUES
('attendance.grace_period', '10', 'Grace period in minutes for late coming'),
('attendance.half_day_threshold', '4', 'Hours below which is considered half day'),
('attendance.overtime_threshold', '8', 'Hours after which overtime is calculated'),
('attendance.late_cutoff_minutes', '120', 'Minutes after which mark as half day'),
('payroll.pay_day', '1', 'Day of month when payroll is processed'),
('payroll.probation_months', '6', 'Probation period in months'),
('leave.casual_annual', '12', 'Annual casual leave allocation'),
('leave.sick_annual', '12', 'Annual sick leave allocation'),
('leave.carry_forward_max', '5', 'Maximum leave days to carry forward');

-- Insert default shift types
INSERT INTO shift_types (name, code, start_time, end_time, break_duration_minutes, grace_period_minutes, weekly_off_days) VALUES
('General Shift', 'GENERAL', '09:00:00', '18:00:00', 60, 10, ARRAY[0]),
('Morning Shift', 'MORNING', '06:00:00', '14:00:00', 30, 10, ARRAY[0]),
('Evening Shift', 'EVENING', '14:00:00', '22:00:00', 30, 10, ARRAY[0]),
('Night Shift', 'NIGHT', '22:00:00', '06:00:00', 30, 10, ARRAY[0, 6]);
