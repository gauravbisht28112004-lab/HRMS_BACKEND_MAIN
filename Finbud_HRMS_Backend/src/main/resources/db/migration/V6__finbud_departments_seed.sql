-- V6: Seed the departments that actually appear in the Finbud Noida master file.
-- Idempotent: uses ON CONFLICT DO NOTHING on the existing unique constraints.
-- Safe to run in every environment — no per-employee or per-salary data is seeded here.
-- Import will auto-create any other departments it encounters (C3 decision), this
-- migration simply ensures the known core three exist from day 1.

INSERT INTO departments (name, code, description) VALUES
    ('Sales',              'SALES', 'Sales department'),
    ('HR',                 'HR',    'Human Resources'),
    ('Digital Marketing',  'DGMKT', 'Digital Marketing')
ON CONFLICT (name) DO NOTHING;
