-- ---------------------------------------------------------------------------
-- V18: Team Leader role — hierarchy target-flow.
--
-- Introduces ROLE_TEAM_LEADER as a distinct level in the reporting chain:
--
--     Admin  ->  Manager  ->  Team Leader  ->  ATL  ->  Employee
--
-- The target cascades down this chain: Admin sets each Manager's monthly
-- target, a Manager splits it across the Team Leaders under them, a Team
-- Leader splits across the ATLs under them, and an ATL splits across the
-- employees under them.
--
-- No schema change is needed for the hierarchy itself — reporting lines are
-- already modelled by employees.manager_id (a self-referencing tree), and
-- targets already live in monthly_targets. This migration only seeds the new
-- role row so existing databases pick it up even if the Java DataInitializer
-- (which also ensures every RoleType row exists) is skipped.
--
-- Idempotent: ON CONFLICT (name) DO NOTHING — safe to re-run and coexists
-- with the runtime seeding in DataInitializer.
-- ---------------------------------------------------------------------------

INSERT INTO roles (name, description, permissions)
VALUES (
    'ROLE_TEAM_LEADER',
    'Team Leader — reports to a Manager; assigns monthly targets to the ATLs under them and views their whole team''s disbursal',
    '["employee:read","attendance:read","leave:approve","commitment:read","target:write"]'::jsonb
)
ON CONFLICT (name) DO NOTHING;
