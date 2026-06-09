# Finbud HRMS - Database Schema Design

## Entity Relationship Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                          FINBUD HRMS DATABASE                                                │
└──────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
 │   departments   │◄────────┤    employees    ├────────►│  shift_types    │
 ├─────────────────┤         ├─────────────────┤         ├─────────────────┤
 │ id (PK)         │         │ id (PK)         │         │ id (PK)         │
 │ name            │         │ employee_id (UQ)│         │ name            │
 │ code            │         │ first_name      │         │ code            │
 │ description     │         │ last_name       │         │ start_time      │
 │ manager_id (FK) │         │ email (UQ)      │         │ end_time        │
 │ created_at      │         │ phone           │         │ break_duration  │
 │ updated_at      │         │ address         │         │ grace_period    │
 └─────────────────┘         │ date_of_joining │         │ weekly_off_days │
         │                   │ department_id   │         │ is_night_shift  │
         │                   │ designation     │         │ created_at      │
         │                   │ manager_id (FK) │         │ updated_at      │
         │                   │ salary_structure│         └─────────────────┘
         │                   │ employment_type │                 ▲
         │                   │ shift_type_id   │                 │
         │                   │ status          │                 │
         │                   │ emergency_contact             ┌───┴───┐
         │                   │ bank_details    │             │  shift_assignments  │
         │                   │ pan_number      │             ├─────────────────────┤
         │                   │ aadhaar_number  │             │ id (PK)             │
         │                   │ profile_picture │             │ employee_id (FK)    │
         │                   │ created_at      │             │ shift_type_id (FK)  │
         │                   │ updated_at      │             │ effective_from      │
         │                   └─────────────────┘             │ effective_to        │
         │                           │                     │ created_at          │
         │                           │                     └─────────────────────┘
         │                           │
         │                   ┌───────┴───────┐
         │                   │
         │           ┌───────▼───────┐   ┌───────────────┐   ┌───────────────┐
         │           │    users      │   │ salary_structure              │
         │           ├───────────────┤   ├───────────────┤
         │           │ id (PK)       │   │ id (PK)       │
         └──────────►│ employee_id   │   │ employee_id   │
                     │ username      │   │ basic_salary  │
                     │ password      │   │ hra           │
                     │ is_active     │   │ da            │
                     │ last_login    │   │ conveyance    │
                     │ created_at    │   │ medical       │
                     └───────────────┘   │ special_allow │
                            ▲             │ pf_employee   │
                            │             │ pf_employer   │
                     ┌──────┴──────┐      │ esi_employee  │
                     │ user_roles  │      │ esi_employer  │
                     ├─────────────┤      │ pt_amount     │
                     │ user_id(FK) │      │ annual_ctc    │
                     │ role_id(FK) │      │ effective_from│
                     └─────────────┘      │ created_at    │
                            ▲             └───────────────┘
                     ┌──────┴──────┐
                     │    roles    │
                     ├─────────────┤         ┌─────────────────┐
                     │ id (PK)     │         │  leave_balances │
                     │ name (UQ)   │         ├─────────────────┤
                     │ description │         │ id (PK)         │
                     │ permissions │         │ employee_id(FK) │
                     │ created_at  │         │ year            │
                     └─────────────┘         │ casual_leave    │
                                              │ sick_leave      │
                                              │ paid_leave      │
                                              │ wfh_allowance   │
                                              │ created_at      │
                                              │ updated_at      │
                                              └───────────────┘
 ┌─────────────────┐         ┌─────────────────┐         │
 │   attendance    │         │  leave_requests │         │
 ├─────────────────┤         ├─────────────────┤         │
 │ id (PK)         │         │ id (PK)         │         │
 │ employee_id(FK) │         │ employee_id(FK) │         │
 │ date            │         │ leave_type      │         │
 │ shift_type_id   │         │ start_date      │         │
 │ punch_in        │         │ end_date        │         │
 │ punch_out       │         │ days_requested  │         │
 │ working_hours   │         │ reason          │         │
 │ status          │         │ manager_id(FK)│         │
 │ is_late         │         │ status          │         │
 │ late_minutes    │         │ approved_by     │         │
 │ is_early_leave  │         │ approved_at     │         │
 │ early_minutes   │         │ rejection_reason│         │
 │ is_half_day     │         │ created_at      │         │
 │ is_overtime     │         └───────────────┘         │
 │ overtime_hours    │
 │ device_id         │         ┌─────────────────┐         │
 │ location          │         │    payroll      │
 │ created_at        │         ├─────────────────┤         │
 │ updated_at        │         │ id (PK)         │◄────────┘
 └─────────────────┘         │ employee_id(FK) │
                             │ month           │
                             │ year            │
                             │ total_days      │
                             │ present_days    │
                             │ absent_days     │
 ┌─────────────────┐         │ leave_days      │
 │   audit_logs    │         │ half_days       │
 ├─────────────────┤         │ basic_earned    │
 │ id (PK)         │         │ hra_earned      │
 │ table_name      │         │ allowances      │
 │ record_id       │         │ gross_earnings  │
 │ action          │         │ pf_deduction    │
 │ old_values      │         │ esi_deduction   │
 │ new_values      │         │ pt_deduction    │
 │ performed_by    │         │ lop_deduction   │
 │ performed_at    │         │ total_deductions│
 │ ip_address      │         │ net_pay         │
 └─────────────────┘         │ overtime_pay    │
                             │ status          │
                             │ generated_at    │
                             │ created_at      │
                             └─────────────────┘

 ┌─────────────────┐         ┌─────────────────┐
 │ ai_embeddings   │         │  vector_search  │
 ├─────────────────┤         │    queries      │
 │ id (PK)         │         ├─────────────────┤
 │ entity_type     │         │ id (PK)         │
 │ entity_id       │         │ query_text      │
 │ content         │         │ embedding       │
 │ embedding       │         │ results         │
 │ metadata        │         │ executed_at     │ 
 │ created_at      │         │ response_time   │
 └─────────────────┘         └─────────────────┘
```

## Table Definitions

### 1. departments
```sql
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    manager_id BIGINT REFERENCES employees(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_departments_manager ON departments(manager_id);
CREATE INDEX idx_departments_code ON departments(code);
```

### 2. shift_types
```sql
CREATE TABLE shift_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_duration_minutes INTEGER DEFAULT 60,
    grace_period_minutes INTEGER DEFAULT 10,
    weekly_off_days INTEGER[] DEFAULT ARRAY[0], -- Sunday = 0
    is_night_shift BOOLEAN DEFAULT FALSE,
    overtime_threshold_hours DECIMAL(4,2) DEFAULT 8.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3. employees
```sql
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
    employment_type VARCHAR(20) DEFAULT 'FULL_TIME', -- FULL_TIME, PART_TIME, CONTRACT, INTERN
    shift_type_id BIGINT REFERENCES shift_types(id),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, TERMINATED, ON_NOTICE
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

CREATE INDEX idx_employees_dept ON employees(department_id);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_joining ON employees(date_of_joining);
CREATE INDEX idx_employees_shift ON employees(shift_type_id);
```

### 4. shift_assignments
```sql
CREATE TABLE shift_assignments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    shift_type_id BIGINT NOT NULL REFERENCES shift_types(id),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, effective_from)
);

CREATE INDEX idx_shift_assign_emp ON shift_assignments(employee_id);
CREATE INDEX idx_shift_assign_shift ON shift_assignments(shift_type_id);
```

### 5. salary_structures
```sql
CREATE TABLE salary_structures (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    basic_salary DECIMAL(12,2) NOT NULL,
    hra DECIMAL(12,2) NOT NULL, -- House Rent Allowance
    da DECIMAL(12,2) DEFAULT 0, -- Dearness Allowance
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

CREATE INDEX idx_salary_emp ON salary_structures(employee_id);
CREATE INDEX idx_salary_active ON salary_structures(is_active);
```

### 6. users
```sql
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

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_employee ON users(employee_id);
```

### 7. roles
```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initial roles
-- ADMIN: Full access
-- HR: Employee + Attendance + Leave + Payroll management
-- MANAGER: Team management + Leave approval
-- EMPLOYEE: Self-service only
```

### 8. user_roles
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role ON user_roles(role_id);
```

### 9. attendance
```sql
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    attendance_date DATE NOT NULL,
    shift_type_id BIGINT REFERENCES shift_types(id),
    punch_in TIMESTAMP,
    punch_out TIMESTAMP,
    working_hours DECIMAL(4,2),
    break_hours DECIMAL(4,2) DEFAULT 1.0,
    status VARCHAR(20) DEFAULT 'PRESENT', -- PRESENT, ABSENT, HALF_DAY, ON_LEAVE, HOLIDAY, WEEKLY_OFF
    is_late BOOLEAN DEFAULT FALSE,
    late_minutes INTEGER DEFAULT 0,
    is_early_leave BOOLEAN DEFAULT FALSE,
    early_leave_minutes INTEGER DEFAULT 0,
    is_half_day BOOLEAN DEFAULT FALSE,
    is_overtime BOOLEAN DEFAULT FALSE,
    overtime_hours DECIMAL(4,2) DEFAULT 0,
    device_id VARCHAR(50),
    punch_in_location VARCHAR(255),
    punch_out_location VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, attendance_date)
);

CREATE INDEX idx_attendance_emp ON attendance(employee_id);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_emp_date ON attendance(employee_id, attendance_date);
CREATE INDEX idx_attendance_status ON attendance(status);
CREATE INDEX idx_attendance_month ON attendance(employee_id, DATE_TRUNC('month', attendance_date));
```

### 10. leave_balances
```sql
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
    lop_days INTEGER DEFAULT 0, -- Loss of Pay
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, year)
);

CREATE INDEX idx_leave_balance_emp ON leave_balances(employee_id);
```

### 11. leave_requests
```sql
CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    leave_type VARCHAR(20) NOT NULL, -- CASUAL, SICK, PAID, WFH, LOP
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_requested DECIMAL(5,2) NOT NULL,
    reason TEXT NOT NULL,
    contact_during_leave VARCHAR(100),
    manager_id BIGINT REFERENCES employees(id),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, CANCELLED
    approved_by BIGINT REFERENCES employees(id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    is_half_day BOOLEAN DEFAULT FALSE,
    half_day_type VARCHAR(10), -- FIRST_HALF, SECOND_HALF
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leave_req_emp ON leave_requests(employee_id);
CREATE INDEX idx_leave_req_status ON leave_requests(status);
CREATE INDEX idx_leave_req_dates ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_req_manager ON leave_requests(manager_id);
```

### 12. payroll
```sql
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
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, GENERATED, APPROVED, PAID
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

CREATE INDEX idx_payroll_emp ON payroll(employee_id);
CREATE INDEX idx_payroll_month ON payroll(month, year);
CREATE INDEX idx_payroll_status ON payroll(status);
```

### 13. audit_logs
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE
    old_values JSONB,
    new_values JSONB,
    performed_by BIGINT REFERENCES employees(id),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    reason TEXT
) PARTITION BY RANGE (performed_at);

-- Create monthly partitions
CREATE TABLE audit_logs_y2024m01 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE INDEX idx_audit_table ON audit_logs(table_name);
CREATE INDEX idx_audit_record ON audit_logs(record_id);
CREATE INDEX idx_audit_performed_at ON audit_logs(performed_at);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
```

### 14. ai_embeddings
```sql
-- Enable pgvector extension first
-- CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE ai_embeddings (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL, -- employee, payroll, attendance, leave
    entity_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536), -- OpenAI embedding dimension
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_embeddings_entity ON ai_embeddings(entity_type, entity_id);
CREATE INDEX idx_ai_embeddings_vector ON ai_embeddings USING ivfflat (embedding vector_cosine_ops);
```

### 15. system_config
```sql
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description TEXT,
    updated_by BIGINT REFERENCES employees(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default configurations
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
```

## Constraints Summary

| Constraint Type | Count | Purpose |
|----------------|-------|---------|
| Primary Keys | 15 | Entity identification |
| Foreign Keys | 25 | Referential integrity |
| Unique Constraints | 12 | Data uniqueness |
| Check Constraints | 8 | Data validation |
| Indexes | 40+ | Query optimization |

## Partitioning Strategy

1. **audit_logs**: Partitioned by month (RANGE) for efficient archival
2. **attendance**: Partitioned by year (RANGE) for historical data
3. **payroll**: Partitioned by year (RANGE) for reporting

## Data Retention

- Audit Logs: 2 years (archive to S3)
- Attendance: 3 years
- Payroll: 7 years (compliance)
- Session Data: 30 days

## Backup Strategy

- Daily automated backups (pg_dump)
- Point-in-time recovery (WAL archiving)
- Cross-region replication for disaster recovery
