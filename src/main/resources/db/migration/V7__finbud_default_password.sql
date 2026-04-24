-- V7: Align the default password for imported employees with the Finbud policy.
--
-- V5 originally seeded auth.default_password = 'Welcome@123'. HR has since
-- standardised on 'finbud@123' for the initial roll-out. The EmployeeImport
-- pipeline reads this value at import time (see EmployeeImportServiceImpl),
-- so any employee imported after V7 is applied will get 'finbud@123' as
-- their initial password.
--
-- Employees already provisioned against the old default keep their current
-- password hash — we only change the policy for *future* provisioning.
-- Admins can rotate existing placeholder accounts manually via the admin UI
-- or by running DataInitializer (it leaves existing usernames alone).

UPDATE system_config
   SET config_value = 'finbud@123',
       description  = 'Default password assigned to newly imported employee users (Finbud rollout standard)'
 WHERE config_key = 'auth.default_password';
