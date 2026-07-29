# HRMS Module Removal — Teardown Plan

**Status: awaiting your approval. Nothing has been changed yet.**
Branch: `feature/manager-tl-hierarchy` · Date: 2026-07-29

---

## 0. Current state you should know about

- Your **414 uncommitted files are already a half-finished attendance/regularisation removal.** The entire Attendance + Regularization *backend* (controllers, entities, enums, mappers, repositories, scheduler, services — 24 files) is already deleted but **not committed**.
- `Employee.java` still references the deleted `Attendance` entity (line 402). **This means the backend does not compile right now.** Any build is currently broken until this teardown is finished.
- The **frontend attendance/regularisation code is untouched** and still calls backend endpoints that no longer exist (they would 404). This is consistent with attendance having moved to your new SaaS, but the cleanup was never finished.

So this task is really: **finish the attendance/regularisation removal AND remove 5 more modules (payroll, leave, shift, office location, holidays) plus salary, dashboards, and reports.**

---

## 1. Confirmed scope

**Remove (your instructions):** attendance, regularisation, payroll, leave, shift, office location, holidays.
**Also remove (your answers to my questions):** salary/compensation, dashboards, reports.
**Depth:** app code + UI + docs **and drop the database tables** (via a new migration).

**What remains after teardown (the end-state app):**
Login/auth · Employee directory · Departments · Targets & commitments (daily / hourly / monthly) · Hierarchy & ATL target views · Leaderboard · Announcements · Notifications · Audit logs · Employee import · AI assistant.

> This removes roughly two-thirds of the HRMS. It becomes a "targets + employee directory" app, not a full HRMS. Confirm that is the intent.

---

## 2. Backend — files DELETED (~82, plus the 24 already-deleted attendance/reg files)

- **Controllers (8):** Payroll, Salary, Leave, Shift, OfficeLocation, PublicHoliday, Dashboard, Report.
- **Services + impls (~21):** Payroll(+impl), PayrollCycle, SalaryCalculation(+impl), Salary(+impl), Leave(+impl), Shift(+impl), OfficeLocation(+impl), PublicHoliday(+impl), Dashboard(+impl), Report(+impl), and `service/payroll/PayslipPdfGenerator`.
- **Entities (8):** Payroll, SalaryStructure, LeaveRequest, LeaveBalance, ShiftType, ShiftAssignment, OfficeLocation, PublicHoliday.
- **Repositories (8):** matching the entities above.
- **DTO folders (~21 files):** `dto/payroll`, `dto/salary`, `dto/leave`, `dto/shift`, `dto/report`, `dto/dashboard`, and the remaining `dto/attendance` (OfficeLocation + PublicHoliday request/response).
- **Mappers (7):** Payroll, SalaryStructure, Leave, ShiftType, ShiftAssignment, OfficeLocation, PublicHoliday.
- **Enums (5):** PayrollStatus, SalaryStructureType, LeaveType, LeaveStatus, LeaveBalanceBucket.
- **Events (2):** LeaveEvents, LeaveNotificationListener.
- **Schedulers (2):** PayrollScheduler, LeaveAllocationScheduler.

## 3. Frontend — files DELETED

- **Pages (~12):** AttendancePage, RegularizationsPage, PayrollPage, EmployeePayrollPage, TeamLeaderPayrollPage, LeavePage, ShiftsPage, OfficeLocationsPage, HolidaysPage, ReportsPage, TeamLeaderReportsPage, DashboardPage.
- **Feature folders (6):** `features/attendance`, `features/payroll`, `features/leave`, `features/shifts`, `features/reports`, `features/dashboard`.
- **Employee sub-components:** `SalaryTab`, `SalarySnapshotCard` (salary), and `features/shifts/components/ShiftAssignmentCard` (embedded in the employee profile).

---

## 4. Files EDITED (kept, references stripped)

**Backend (~30 key files):**
- `entity/Employee.java` — remove fields: `attendances`, `leaveRequests`, `payrolls`, `leaveBalances`, `shiftType`, `shiftAssignments`, `officeLocation`, `salaryStructure`, and salary columns (`salaryPaymentMode`). *(This is the fix that makes the backend compile again.)*
- `security/Permission.java` — remove ATTENDANCE_*, LEAVE_*, PAYROLL_*, REPORT_* (and SALARY/SHIFT/HOLIDAY/OFFICE if present).
- `security/SecurityConfig.java` — remove route rules for the deleted endpoints.
- `security/AuthzService.java`, `security/DeviceApiKeyFilter.java` — strip attendance/leave/payroll logic (the device API key filter existed for attendance punch devices; likely removable entirely — flagged below).
- `config/DataInitializer.java` — remove seeding of deleted permissions/data.
- `entity/Notification.java`, `enums/NotificationType.java`, `service/NotificationService(+Impl)` — remove leave/payroll notification types and methods.
- `service/SystemConfigService.java` — remove payroll/attendance config keys.
- `service/AIService(+Impl)` — remove queries against deleted repositories (shrinks assistant capability).
- `service/EmployeeService(+Impl)`, `mapper/EmployeeMapper`, `dto/employee/*`, import services — remove salary/shift/leave fields from employee payloads.

**Frontend (~28 files):**
- `constants/navigation.ts`, `components/layout/Sidebar.tsx` — remove nav entries for all removed modules across every role block (Employee, Manager, Team Leader, ATL, Admin/HR); clean now-unused icon imports.
- `routes/AppRouter.tsx` — remove lazy imports + routes; **fix the index (home) route** (see confirmation #2).
- `services/api.ts`, `services/backendAdapters.ts`, `services/requestMappers.ts` — remove attendance/regularisation/payroll/leave/shift/office/holiday/report/dashboard/salary sections.
- `types/index.ts`, `constants/mockData.ts` — remove the corresponding types and mock data.
- `pages/EmployeeProfilePage.tsx`, `features/employee/components/EmployeeForm.tsx` — remove salary/shift cards and fields.
- `features/assistant/components/AssistantPanel.tsx`, `store/authStore.ts`, `services/roleMapping.ts` — strip references to removed modules/permissions.

---

## 5. Database — DESTRUCTIVE (needs care)

- **We do NOT delete the existing Flyway migrations** (V4 payroll, V10 shift, V11 attendance-geo+holidays, V12 leave+notifications, etc.). They are already applied to your live Supabase DB; deleting them breaks Flyway validation for every existing database.
- Instead I add **one new migration** (e.g. `V19__drop_removed_modules.sql`) that drops, in FK-safe order: `attendance`, `regularization_request`, `payroll`, `salary_structure`, `leave_request`, `leave_balance`, `shift_type`, `shift_assignment`, `office_location`, `public_holiday`, and the related `employee` FK columns (`shift_type_id`, `office_location_id`, salary columns).
- **You must take a Supabase backup before this migration runs, and you deploy it — I will not run it against your database.**

## 6. Docs & SQL cleaned

- **Delete:** `reset_attendance_only.sql`.
- **Trim:** `README.md`, `ARCHITECTURE.md`, `DATABASE_SCHEMA.md`, `Finbud_HRMS_API_Collection.json`, `HRMS_Portal_Documentation.docx`, `GO_LIVE_RESET_RUNBOOK.md`, `reset_trial_data_pre_golive.sql` — remove the sections describing the deleted modules.

---

## 7. Open confirmations (I need your yes/no before executing)

1. **Target dashboards:** The ATL dashboard, Hierarchy dashboard, and commitment reports power the **targets feature you're keeping**. I read "remove dashboards & reports" as the general HR/Employee dashboard + the Reports module — **not** the targets views. Keep the targets dashboards? *(Recommend: keep.)*
2. **Home page:** The app's home route currently loads the (removed) DashboardPage. Replace it with a redirect — Admin/HR → Employees, Employee → My Profile? *(Recommend: yes.)*
3. **Employee salary fields:** Remove salary columns from the Employee record too (pay mode, structure link)? *(Recommend: yes, since salary is going.)*
4. **Database:** Confirm you will back up Supabase and deploy the drop migration yourself.
5. **"On Leave" status:** Keep the employee status label "On Leave" (it's just a status value, independent of the leave module)? *(Recommend: keep.)*

---

## 8. Sequence & safety

- **Phase 0:** Commit your current 414 files as a WIP checkpoint, then create branch `chore/remove-hrms-modules`. Gives a clean rollback point.
- **Phase 1:** Backend deletions.
- **Phase 2:** Backend edits until `mvnw compile` passes.
- **Phase 3:** Frontend deletions + edits until `tsc --noEmit` / build passes.
- **Phase 4:** Write the DB drop migration (you deploy after backup).
- **Phase 5:** Docs cleanup.
- **Phase 6:** Verify — compile, build, and grep for any dangling references.

## 9. Risks

- **Destructive DB change** on a near-production system (you have go-live runbooks and roster-sync scripts).
- **Large blast radius** (~180 files) with **non-compiling intermediate states** — must be finished in one coherent pass.
- **AI assistant** loses any attendance/leave/payroll answers.
- **Tests** referencing removed modules (e.g. `SystemConfigServiceImplTest`) must be removed/updated.

## 10. What I will NOT do without you

- Run the drop migration against your database.
- Rewrite or force-push your existing branch history.
