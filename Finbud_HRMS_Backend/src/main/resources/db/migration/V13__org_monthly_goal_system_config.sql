-- ---------------------------------------------------------------------------
-- V13: Seed org-wide monthly goal amount in system_config
--
-- Used by every dashboard's "Org Monthly Goal" tile. Settable by Admin via
-- the new SystemConfigController PUT endpoint. Initial value 0 — Admin sets
-- it for the first time after deployment.
-- ---------------------------------------------------------------------------

INSERT INTO system_config (config_key, config_value, description)
VALUES ('org.monthly_goal_amount', '0', 'Org-wide monthly disbursal goal in INR, displayed on every dashboard.')
ON CONFLICT (config_key) DO NOTHING;
