-- V5: Seed Finbud payroll / auth defaults into the existing system_config table.
-- The table itself was created in V1 and extended in V2 — we only INSERT here.
-- ON CONFLICT (config_key) DO NOTHING makes this idempotent and safe to re-run.

INSERT INTO system_config (config_key, config_value, description) VALUES
    ('payroll.pf.employer_default',       '1950',        'Default employer PF contribution for Management / Highly Skilled (INR, flat)'),
    ('payroll.pf.employee_default',       '1950',        'Default employee PF contribution for Management / Highly Skilled (INR, flat)'),
    ('payroll.lwf.default',               '0',           'Default LWF amount; set per state/policy if applicable (INR)'),
    ('payroll.tds.contract_rate_percent', '5.00',        'Flat TDS rate applied to CONTRACT salary structure'),
    ('payroll.calc.precision_scale',      '4',           'BigDecimal calculation scale before final rounding'),
    ('payroll.calc.output_scale',         '2',           'BigDecimal scale used for persisted payroll / response values'),
    ('auth.default_password',             'Welcome@123', 'Default password assigned to newly imported employee users'),
    ('auth.default_role',                 'ROLE_EMPLOYEE','Default role granted to newly imported employee users'),
    ('import.employee.default_country',   'India',       'Country of origin used when the import row does not specify one'),
    ('import.employee.default_location',  'Noida',       'Location used when the import row does not specify one'),
    ('attendance.device.api_key_header',  'X-Device-Key','Request header name carrying the device API key for /attendance/punch-*'),
    ('attendance.device.api_key',         '',            'Device API key for biometric punch endpoints (blank = disabled, regenerate in admin)')
ON CONFLICT (config_key) DO NOTHING;
