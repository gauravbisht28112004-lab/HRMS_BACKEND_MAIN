import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/layouts/AppLayout';
import { ProtectedRoute } from '@/routes/ProtectedRoute';

const LoginPage = lazy(() => import('@/pages/LoginPage').then((module) => ({ default: module.LoginPage })));
const ForceChangePasswordPage = lazy(() =>
  import('@/pages/ForceChangePasswordPage').then((module) => ({ default: module.ForceChangePasswordPage })),
);
const DashboardPage = lazy(() => import('@/pages/DashboardPage').then((module) => ({ default: module.DashboardPage })));
const EmployeesPage = lazy(() => import('@/pages/EmployeesPage').then((module) => ({ default: module.EmployeesPage })));
const EmployeeProfilePage = lazy(() =>
  import('@/pages/EmployeeProfilePage').then((module) => ({ default: module.EmployeeProfilePage })),
);
const AttendancePage = lazy(() => import('@/pages/AttendancePage').then((module) => ({ default: module.AttendancePage })));
const RegularizationsPage = lazy(() =>
  import('@/pages/RegularizationsPage').then((module) => ({ default: module.RegularizationsPage })),
);
const OfficeLocationsPage = lazy(() =>
  import('@/pages/OfficeLocationsPage').then((module) => ({ default: module.OfficeLocationsPage })),
);
const HolidaysPage = lazy(() => import('@/pages/HolidaysPage').then((module) => ({ default: module.HolidaysPage })));
const ReportsPage = lazy(() => import('@/pages/ReportsPage').then((module) => ({ default: module.ReportsPage })));
const ShiftsPage = lazy(() => import('@/pages/ShiftsPage').then((module) => ({ default: module.ShiftsPage })));
const PayrollPage = lazy(() => import('@/pages/PayrollPage').then((module) => ({ default: module.PayrollPage })));
const DepartmentsPage = lazy(() =>
  import('@/pages/DepartmentsPage').then((module) => ({ default: module.DepartmentsPage })),
);
const AuditLogsPage = lazy(() => import('@/pages/AuditLogsPage').then((module) => ({ default: module.AuditLogsPage })));
const AdminImportPage = lazy(() =>
  import('@/pages/AdminImportPage').then((module) => ({ default: module.AdminImportPage })),
);
const LeavePage = lazy(() => import('@/pages/LeavePage').then((module) => ({ default: module.LeavePage })));
const EmployeeDailyCommitmentPage = lazy(() =>
  import('@/pages/EmployeeDailyCommitmentPage').then((module) => ({ default: module.EmployeeDailyCommitmentPage })),
);
const EmployeeHourlyUpdatesPage = lazy(() =>
  import('@/pages/EmployeeHourlyUpdatesPage').then((module) => ({ default: module.EmployeeHourlyUpdatesPage })),
);
const EmployeeMonthlyTargetsPage = lazy(() =>
  import('@/pages/EmployeeMonthlyTargetsPage').then((module) => ({ default: module.EmployeeMonthlyTargetsPage })),
);
const EmployeePayrollPage = lazy(() =>
  import('@/pages/EmployeePayrollPage').then((module) => ({ default: module.EmployeePayrollPage })),
);
const LeaderboardPage = lazy(() => import('@/pages/LeaderboardPage').then((module) => ({ default: module.LeaderboardPage })));
const TeamLeaderPayrollPage = lazy(() =>
  import('@/pages/TeamLeaderPayrollPage').then((module) => ({ default: module.TeamLeaderPayrollPage })),
);
const TeamLeaderDailyCommitmentPage = lazy(() =>
  import('@/pages/TeamLeaderDailyCommitmentPage').then((module) => ({ default: module.TeamLeaderDailyCommitmentPage })),
);
const TeamLeaderMonthlyCommitmentPage = lazy(() =>
  import('@/pages/TeamLeaderMonthlyCommitmentPage').then((module) => ({ default: module.TeamLeaderMonthlyCommitmentPage })),
);
const TeamLeaderReportsPage = lazy(() =>
  import('@/pages/TeamLeaderReportsPage').then((module) => ({ default: module.TeamLeaderReportsPage })),
);
const AtlMonthlyTargetPage = lazy(() =>
  import('@/pages/AtlMonthlyTargetPage').then((module) => ({ default: module.AtlMonthlyTargetPage })),
);
const AtlTeamApprovalsPage = lazy(() =>
  import('@/pages/AtlTeamApprovalsPage').then((module) => ({ default: module.AtlTeamApprovalsPage })),
);
const HierarchyTargetsPage = lazy(() =>
  import('@/pages/HierarchyTargetsPage').then((module) => ({ default: module.HierarchyTargetsPage })),
);
const AllEmployeeTargetsPage = lazy(() =>
  import('@/pages/AllEmployeeTargetsPage').then((module) => ({ default: module.AllEmployeeTargetsPage })),
);

export const AppRouter = () => (
  <Suspense fallback={<div className="p-6 text-sm text-slate-500">Loading workspace...</div>}>
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      {/* Dedicated force-change-password slot. The gate lets users through
          even with `mustChangePassword=true`; the page itself redirects
          everyone else away. */}
      <Route
        element={
          <ProtectedRoute
            roles={['Admin', 'HR', 'Manager', 'Team Leader', 'ATL', 'Employee']}
            allowWhileMustChangePassword
          />
        }
      >
        <Route path="/force-change-password" element={<ForceChangePasswordPage />} />
      </Route>

      <Route element={<ProtectedRoute roles={['Admin', 'HR', 'Manager', 'Team Leader', 'ATL', 'Employee']} />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="/my-profile" element={<EmployeeProfilePage />} />

          <Route element={<ProtectedRoute roles={['Admin', 'HR', 'Manager']} />}>
            <Route path="/employees" element={<EmployeesPage />} />
            <Route path="/employees/:employeeId" element={<EmployeeProfilePage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['Admin', 'HR', 'Manager', 'Team Leader', 'ATL', 'Employee']} />}>
            <Route path="/attendance" element={<AttendancePage />} />
            <Route path="/regularizations" element={<RegularizationsPage />} />
            <Route path="/leaderboard" element={<LeaderboardPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['Admin', 'HR']} />}>
            <Route path="/shifts" element={<ShiftsPage />} />
            <Route path="/payroll" element={<PayrollPage />} />
            <Route path="/departments" element={<DepartmentsPage />} />
            <Route path="/audit-logs" element={<AuditLogsPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/admin/import" element={<AdminImportPage />} />
            <Route path="/office-locations" element={<OfficeLocationsPage />} />
            <Route path="/holidays" element={<HolidaysPage />} />
          </Route>

          <Route path="/leave" element={<LeavePage />} />
          <Route element={<ProtectedRoute roles={['Employee']} />}>
            <Route path="/daily-commitment" element={<EmployeeDailyCommitmentPage />} />
            <Route path="/hourly-updates" element={<EmployeeHourlyUpdatesPage />} />
            <Route path="/monthly-targets" element={<EmployeeMonthlyTargetsPage />} />
            <Route path="/employee-payroll" element={<EmployeePayrollPage />} />
          </Route>

          {/* Existing manager-level operational pages. These belong to the
              backend MANAGER role, now labelled "Manager" in the UI. */}
          <Route element={<ProtectedRoute roles={['Manager']} />}>
            <Route path="/team-daily-commitment" element={<TeamLeaderDailyCommitmentPage />} />
            <Route path="/team-monthly-commitment" element={<TeamLeaderMonthlyCommitmentPage />} />
            <Route path="/team-payroll" element={<TeamLeaderPayrollPage />} />
            <Route path="/team-reports" element={<TeamLeaderReportsPage />} />
          </Route>

          {/* Target-flow: every supervisor level assigns to its own direct
              reports and sees its whole-team disbursal. The dashboard (index)
              is the primary view; these give a dedicated menu entry too. */}
          <Route element={<ProtectedRoute roles={['Manager', 'Team Leader', 'ATL']} />}>
            <Route path="/my-team-targets" element={<HierarchyTargetsPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['Admin', 'HR']} />}>
            <Route path="/manager-overview" element={<HierarchyTargetsPage />} />
            <Route path="/all-employee-targets" element={<AllEmployeeTargetsPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['ATL']} />}>
            <Route path="/atl-monthly-target" element={<AtlMonthlyTargetPage />} />
            <Route path="/atl-daily-commitment" element={<AtlTeamApprovalsPage />} />
          </Route>
        </Route>
      </Route>
    </Routes>
  </Suspense>
);
