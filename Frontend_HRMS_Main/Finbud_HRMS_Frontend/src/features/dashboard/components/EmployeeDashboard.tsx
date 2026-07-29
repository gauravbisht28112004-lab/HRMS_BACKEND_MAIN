import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { AnnouncementBoard } from './AnnouncementBoard';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import type { CommitmentStatus, DailyCommitment, Employee, MonthlyTarget } from '@/types';

/**
 * Real-data Employee dashboard. Attendance, leave, and payroll widgets were
 * removed when those modules moved out of the HRMS; this view now focuses on
 * the sales-commitment workflow and profile.
 *
 * <p>Data sources (all real APIs):
 *   - Profile: api.employees.getByEmployeeId
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
        <p className="mt-1 text-sm text-slate-500">Today&apos;s snapshot — your commitments and targets.</p>
      </div>

      {/* Top KPI tiles */}
      <div className="grid gap-4 md:grid-cols-3">
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
          <p className="mt-3 text-3xl font-semibold text-slate-900">{monthlyTarget?.achievedPercent ?? 0}%</p>
          <p className="mt-2 text-sm text-slate-500">
            {formatCurrency(monthlyTarget?.achievedDisbursalAmount ?? 0)} of{' '}
            {formatCurrency(monthlyTarget?.targetDisbursalAmount ?? 0)}
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

      {/* Personal details */}
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
          </div>
        ) : (
          <p className="mt-4 text-sm text-slate-500">Loading your profile…</p>
        )}
      </Card>

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
