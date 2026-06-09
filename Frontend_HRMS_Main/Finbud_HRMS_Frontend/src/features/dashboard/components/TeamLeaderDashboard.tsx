import { useMemo } from 'react';
import { ClipboardList, IndianRupee, Target, Users } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatsCard } from '@/components/common/StatsCard';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import { AnnouncementBoard } from './AnnouncementBoard';
import type { CommitmentStatus, DailyCommitment, Employee, MonthlyTarget } from '@/types';

/**
 * Real-data Team Leader dashboard. Replaces the prior mock-data fallback
 * that filtered local fixtures by `teamLeader === user?.name`.
 *
 * <p>Data sources (all real APIs):
 *   - Direct reports: api.employees.listByManager
 *   - Pending approvals: api.commitments.daily.listPendingForManager
 *   - Today's team snapshot: api.commitments.daily.listTeamForDate
 *   - Monthly progress per team member: api.commitments.monthlyTargets.listForManager
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

export const TeamLeaderDashboard = () => {
  const user = useAuthStore((state) => state.user);
  const managerId = user?.employeeDbId;
  const currentYear = NOW.getFullYear();
  const currentMonth = NOW.getMonth() + 1;

  // -- Org goal (shared with Admin / Employee dashboards) --
  const { data: orgMonthlyGoal } = useQuery({
    queryKey: ['org-monthly-goal'],
    queryFn: api.systemConfig.getOrgMonthlyGoal,
    staleTime: 60_000,
  });

  // -- Team members (real query, not a mock filter) --
  const { data: teamMembers = [] } = useQuery<Employee[]>({
    queryKey: ['my-team', managerId],
    queryFn: () => (managerId ? api.employees.listByManager(managerId) : Promise.resolve([])),
    enabled: Boolean(managerId),
  });

  // -- Pending daily-commitment approvals --
  const { data: pending = [] } = useQuery<DailyCommitment[]>({
    queryKey: ['tl-pending-commitments', managerId],
    queryFn: () => (managerId ? api.commitments.daily.listPendingForManager(managerId) : Promise.resolve([])),
    enabled: Boolean(managerId),
    refetchInterval: 60_000,
  });

  // -- Today's team commitments snapshot --
  const { data: todayCommitments = [] } = useQuery<DailyCommitment[]>({
    queryKey: ['tl-team-today', managerId, TODAY],
    queryFn: () => (managerId ? api.commitments.daily.listTeamForDate(managerId, TODAY) : Promise.resolve([])),
    enabled: Boolean(managerId),
  });

  // -- Monthly target progress for each team member --
  const { data: monthlyProgress = [] } = useQuery<MonthlyTarget[]>({
    queryKey: ['tl-team-monthly', managerId, currentYear, currentMonth],
    queryFn: () =>
      managerId
        ? api.commitments.monthlyTargets.listForManager(managerId, currentYear, currentMonth)
        : Promise.resolve([]),
    enabled: Boolean(managerId),
  });

  const averageMonthCompletion = useMemo(() => {
    if (monthlyProgress.length === 0) return 0;
    const sum = monthlyProgress.reduce((acc, t) => acc + (t.achievedPercent ?? 0), 0);
    return Math.round(sum / monthlyProgress.length);
  }, [monthlyProgress]);

  if (!managerId) {
    return (
      <Card className="border border-amber-200 bg-amber-50">
        <p className="text-sm font-semibold text-amber-800">Team Leader profile not linked</p>
        <p className="mt-1 text-sm text-amber-700">
          Your login isn&apos;t linked to an employee record. Ask HR to provision your profile.
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Team Leader Dashboard"
        description="Real-time view of your direct reports — approvals, progress, and the org-wide goal."
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatsCard
          label="My Team"
          value={String(teamMembers.length)}
          meta="Active direct reports"
          icon={<Users size={22} />}
        />
        <StatsCard
          label="Pending Approvals"
          value={String(pending.length)}
          meta={pending.length ? 'Submitted commitments waiting on you' : 'Queue is clear'}
          icon={<ClipboardList size={22} />}
        />
        <StatsCard
          label="Avg. Monthly Progress"
          value={`${averageMonthCompletion}%`}
          meta="Across your team's monthly targets"
          icon={<Target size={22} />}
        />
        <StatsCard
          label="Org Monthly Goal"
          value={formatCurrency(orgMonthlyGoal?.amount ?? 0)}
          meta="Set by Admin for everyone"
          icon={<IndianRupee size={22} />}
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <Card className="border border-slate-200 bg-white shadow-none">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-slate-900">Today's Team Commitments</h2>
            <Link to="/team-daily-commitment" className="text-xs font-medium text-brand-700 hover:underline">
              Manage approvals →
            </Link>
          </div>
          <div className="mt-5">
            <DataTable
              data={todayCommitments}
              emptyTitle="Nobody's filed yet"
              emptyDescription="Your team hasn't created today's commitments. Nudge them via Slack / Teams."
              columns={[
                { key: 'employee', header: 'Member', render: (row) => row.employeeName ?? row.employeeCode },
                { key: 'targetCalls', header: 'Target Calls', render: (row) => row.targetCalls },
                { key: 'actualCalls', header: 'Actual Calls', render: (row) => row.actualCalls },
                {
                  key: 'targetDisbursal',
                  header: 'Target ₹',
                  render: (row) => formatCurrency(row.targetDisbursalAmount),
                },
                {
                  key: 'actualDisbursal',
                  header: 'Actual ₹',
                  render: (row) => formatCurrency(row.actualDisbursalAmount),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (row) => <StatusBadge label={row.status} tone={commitmentTone[row.status]} />,
                },
              ]}
            />
          </div>
        </Card>

        <Card className="border border-slate-200 bg-white shadow-none">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-slate-900">Monthly Progress</h2>
            <Link to="/team-monthly-commitment" className="text-xs font-medium text-brand-700 hover:underline">
              Manage targets →
            </Link>
          </div>
          {monthlyProgress.length === 0 ? (
            <p className="mt-4 text-sm text-slate-500">
              No monthly targets set for your team yet. Set them from the Monthly Commitment page.
            </p>
          ) : (
            <ul className="mt-5 space-y-3">
              {monthlyProgress.map((target) => (
                <li
                  key={target.employeeId}
                  className="flex items-center justify-between rounded-2xl bg-[#f8f8f4] p-4"
                >
                  <div>
                    <p className="text-sm font-medium text-slate-900">{target.employeeName}</p>
                    <p className="text-xs text-slate-500">
                      {formatCurrency(target.achievedDisbursalAmount)} of{' '}
                      {formatCurrency(target.targetDisbursalAmount)}
                    </p>
                  </div>
                  <StatusBadge
                    label={`${target.achievedPercent}%`}
                    tone={
                      target.achievedPercent >= 80
                        ? 'success'
                        : target.achievedPercent >= 50
                        ? 'info'
                        : 'warning'
                    }
                  />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <AnnouncementBoard role="Team Leader" />
    </div>
  );
};
