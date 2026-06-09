import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { AnnouncementBoard } from './AnnouncementBoard';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import type {
  AttendanceRecord,
  CommitmentStatus,
  DailyCommitment,
  Employee,
  LeaveBalance,
  LeaveRequest,
  MonthlyTarget,
  PayrollEntry,
} from '@/types';

/**
 * Real-data Employee dashboard. Replaces the prior mock-data fallback that
 * bailed with "unavailable for this account" for any imported employee
 * whose code wasn't in the seed fixtures.
 *
 * <p>Data sources (all real APIs):
 *   - Profile: api.employees.getByEmployeeId
 *   - Today's attendance: api.attendance.getByEmployeeAndDate
 *   - Leave balance (per-year): api.leave.getLeaveBalance
 *   - Recent leaves: api.leave.listForEmployee
 *   - Latest payslip: api.payroll.listForEmployee (first row)
 *   - Today's commitment: api.commitments.daily.getMineForDate
 *   - Monthly target: api.commitments.monthlyTargets.getMine
 *   - Org monthly goal: api.systemConfig.getOrgMonthlyGoal
 */
const NOW = new Date();
const TODAY = NOW.toISOString().slice(0, 10);

const commitmentTone: Record<CommitmentStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

export const EmployeeDashboard = () => {
  const user = useAuthStore((state) => state.user);
  const employeeDbId = user?.employeeDbId;
  const employeeCode = user?.employeeId;
  const currentYear = NOW.getFullYear();
  const currentMonth = NOW.getMonth() + 1;

  // -- Profile --
  const { data: employee } = useQuery<Employee | null>({
    queryKey: ['my-profile', employeeCode],
    queryFn: () => (employeeCode ? api.employees.getByEmployeeId(employeeCode) : Promise.resolve(null)),
    enabled: Boolean(employeeCode),
  });

  // -- Today's attendance --
  const { data: todayAttendance } = useQuery<AttendanceRecord | null>({
    queryKey: ['my-attendance-today', employeeDbId, TODAY],
    queryFn: () => (employeeDbId ? api.attendance.getByEmployeeAndDate(employeeDbId, TODAY) : Promise.resolve(null)),
    enabled: Boolean(employeeDbId),
    staleTime: 30_000,
  });

  // -- Leave balance --
  const { data: leaveBalance } = useQuery<LeaveBalance | null>({
    queryKey: ['my-leave-balance', employeeDbId, currentYear],
    queryFn: () =>
      employeeDbId ? api.leave.getLeaveBalance(employeeDbId, currentYear) : Promise.resolve(null),
    enabled: Boolean(employeeDbId),
  });

  // -- Recent leaves --
  const { data: leaveHistory = [] } = useQuery<LeaveRequest[]>({
    queryKey: ['my-leave-history', employeeDbId],
    queryFn: () => (employeeDbId ? api.leave.listForEmployee(employeeDbId) : Promise.resolve([])),
    enabled: Boolean(employeeDbId),
  });

  // -- Latest payslip --
  const { data: payrollHistory = [] } = useQuery<PayrollEntry[]>({
    queryKey: ['my-payroll-snapshot', employeeDbId],
    queryFn: () => (employeeDbId ? api.payroll.listForEmployee(employeeDbId) : Promise.resolve([])),
    enabled: Boolean(employeeDbId),
  });
  const latestPayroll = payrollHistory[0];

  // -- Today's daily commitment (sales workflow) --
  const { data: todayCommitment } = useQuery<DailyCommitment | null>({
    queryKey: ['my-commitment-today', TODAY],
    queryFn: () => api.commitments.daily.getMineForDate(TODAY),
    enabled: Boolean(employeeDbId),
  });

  // -- Monthly target with achieved overlay --
  const { data: monthlyTarget } = useQuery<MonthlyTarget | null>({
    queryKey: ['my-monthly-target', currentYear, currentMonth],
    queryFn: () => api.commitments.monthlyTargets.getMine(currentYear, currentMonth),
    enabled: Boolean(employeeDbId),
  });

  // -- Org-wide monthly goal --
  const { data: orgMonthlyGoal } = useQuery({
    queryKey: ['org-monthly-goal'],
    queryFn: api.systemConfig.getOrgMonthlyGoal,
    staleTime: 60_000,
  });

  const greeting = useMemo(() => {
    const first = user?.name?.split(' ')[0] ?? 'there';
    const hour = NOW.getHours();
    const part = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    return `${part}, ${first}`;
  }, [user?.name]);

  if (!employeeDbId) {
    return (
      <Card className="border border-amber-200 bg-amber-50">
        <p className="text-sm font-semibold text-amber-800">Employee profile not linked</p>
        <p className="mt-1 text-sm text-amber-700">
          Your login isn&apos;t linked to an employee record yet. Ask HR to provision your profile.
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">{greeting}</h1>
        <p className="mt-1 text-sm text-slate-500">
          Today&apos;s snapshot — attendance, commitments, leave balance, and your latest payslip.
        </p>
      </div>

      {/* Top KPI tiles */}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Today&apos;s Punch In</p>
          <p className="mt-3 text-xl font-semibold text-slate-900">
            {todayAttendance?.punchIn && todayAttendance.punchIn !== '--' ? todayAttendance.punchIn : 'Not yet'}
          </p>
          <div className="mt-2">
            {todayAttendance?.approvalStatus ? (
              <StatusBadge
                label={todayAttendance.approvalStatus}
                tone={
                  todayAttendance.approvalStatus === 'APPROVED'
                    ? 'success'
                    : todayAttendance.approvalStatus === 'REJECTED'
                    ? 'danger'
                    : 'warning'
                }
              />
            ) : (
              <StatusBadge label="Mark in" tone="neutral" />
            )}
          </div>
        </Card>

        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Daily Commitment</p>
          <p className="mt-3 text-xl font-semibold text-slate-900">
            {todayCommitment ? `${todayCommitment.actualCalls}/${todayCommitment.targetCalls} calls` : 'Not set'}
          </p>
          <div className="mt-2">
            {todayCommitment ? (
              <StatusBadge label={todayCommitment.status} tone={commitmentTone[todayCommitment.status]} />
            ) : (
              <StatusBadge label="Create commitment" tone="neutral" />
            )}
          </div>
        </Card>

        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Monthly Target</p>
          <p className="mt-3 text-3xl font-semibold text-slate-900">
            {monthlyTarget?.achievedPercent ?? 0}%
          </p>
          <p className="mt-2 text-sm text-slate-500">
            {formatCurrency(monthlyTarget?.achievedDisbursalAmount ?? 0)} of{' '}
            {formatCurrency(monthlyTarget?.targetDisbursalAmount ?? 0)}
          </p>
        </Card>

        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Latest Payslip</p>
          <p className="mt-3 text-xl font-semibold text-slate-900">
            {latestPayroll ? latestPayroll.month : 'Not generated'}
          </p>
          <p className="mt-2 text-sm text-slate-500">
            {latestPayroll ? `Net ${formatCurrency(Number(latestPayroll.netSalary))}` : 'Wait for HR to run payroll'}
          </p>
        </Card>
      </div>

      {/* Leave balance + Org goal */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Casual + Sick Leave</p>
          <p className="mt-3 text-3xl font-semibold text-slate-900">
            {leaveBalance ? leaveBalance.casualSickBalance.toFixed(1) : '—'}
            <span className="ml-1 text-sm font-normal text-slate-500">days</span>
          </p>
          <p className="mt-2 text-xs text-slate-500">
            {leaveBalance
              ? `Used ${leaveBalance.casualSickUsed.toFixed(1)} of ${leaveBalance.casualSickAllocated.toFixed(1)}`
              : 'Awaiting allocation'}
          </p>
        </Card>
        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Paid / Earned Leave</p>
          <p className="mt-3 text-3xl font-semibold text-slate-900">
            {leaveBalance ? leaveBalance.paidLeaveBalance.toFixed(1) : '—'}
            <span className="ml-1 text-sm font-normal text-slate-500">days</span>
          </p>
          <p className="mt-2 text-xs text-slate-500">
            {leaveBalance
              ? `Used ${leaveBalance.paidLeaveUsed.toFixed(1)} of ${leaveBalance.paidLeaveAllocated.toFixed(1)}`
              : 'Awaiting allocation'}
          </p>
        </Card>
        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Org Monthly Goal</p>
          <p className="mt-3 text-3xl font-semibold text-slate-900">
            {formatCurrency(orgMonthlyGoal?.amount ?? 0)}
          </p>
          <p className="mt-2 text-sm text-slate-500">Set by Admin for the whole org</p>
        </Card>
      </div>

      {/* Personal details + recent leaves */}
      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <Card className="border border-slate-200 bg-white shadow-none">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-slate-900">Personal Details</h2>
            <Link to="/my-profile" className="text-xs font-medium text-brand-700 hover:underline">
              Edit profile →
            </Link>
          </div>
          {employee ? (
            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <Detail label="Employee ID" value={employee.id} />
              <Detail label="Name" value={`${employee.firstName} ${employee.lastName}`.trim() || '—'} />
              <Detail label="Email" value={employee.email || '—'} />
              <Detail label="Phone" value={employee.phone || '—'} />
              <Detail label="Department" value={employee.department || '—'} />
              <Detail label="Designation" value={employee.designation || '—'} />
              <Detail label="Date of Joining" value={employee.dateOfJoining || '—'} />
              <Detail label="Team Leader" value={employee.teamLeader || '—'} />
              <Detail label="Employment Type" value={employee.employmentType} />
              <Detail label="Shift" value={employee.shiftAssignment || '—'} />
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-500">Loading your profile…</p>
          )}
        </Card>

        <Card className="border border-slate-200 bg-white shadow-none">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-slate-900">Recent Leaves</h2>
            <Link to="/leave" className="text-xs font-medium text-brand-700 hover:underline">
              View all →
            </Link>
          </div>
          {leaveHistory.length === 0 ? (
            <p className="mt-4 text-sm text-slate-500">
              No leave requests yet. Apply from the Leave Management page when you need time off.
            </p>
          ) : (
            <ul className="mt-5 space-y-3">
              {leaveHistory.slice(0, 5).map((leave) => (
                <li key={leave.id} className="flex items-center justify-between rounded-2xl bg-[#f8f8f4] p-4">
                  <div>
                    <p className="text-sm font-medium text-slate-900">
                      {leave.leaveType} · {leave.days} day{leave.days === 1 ? '' : 's'}
                    </p>
                    <p className="text-xs text-slate-500">
                      {leave.from} → {leave.to}
                    </p>
                  </div>
                  <StatusBadge
                    label={leave.status}
                    tone={
                      leave.status === 'Approved'
                        ? 'success'
                        : leave.status === 'Rejected'
                        ? 'danger'
                        : 'warning'
                    }
                  />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <AnnouncementBoard role="Employee" />
    </div>
  );
};

const Detail = ({ label, value }: { label: string; value: string }) => (
  <div className="rounded-2xl bg-[#f8f8f4] p-4">
    <p className="text-sm text-slate-500">{label}</p>
    <p className="mt-2 font-medium text-slate-900">{value}</p>
  </div>
);
