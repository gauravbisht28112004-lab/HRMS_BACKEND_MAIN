import { IndianRupee, Megaphone, UserCheck, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatsCard } from '@/components/common/StatsCard';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { api } from '@/services/api';
import { formatCurrency } from '@/utils/format';
import type { Announcement } from '@/types';
import { AnnouncementBoard } from './AnnouncementBoard';

/**
 * Admin home dashboard. Every tile and table is now backed by a real API:
 *   - Active Employees / Present Today  -> /api/dashboard/stats
 *   - Live Announcements                 -> /api/announcements (active)
 *   - Monthly Target                     -> /api/system-config/org-monthly-goal
 *   - Department distribution table      -> /api/dashboard/stats.departmentStats
 *
 * The "Publish Monthly Target" form below calls the Admin-only PUT and
 * invalidates the query so every other dashboard reading the same value
 * refreshes on next focus.
 *
 * Productivity / quality / disbursal scoring per employee was previously
 * shown here from a mock array; that's been removed pending a proper
 * analytics rollup endpoint on the backend.
 */
export const AdminDashboard = () => {
  const queryClient = useQueryClient();

  const { data: monthlyGoal } = useQuery({
    queryKey: ['org-monthly-goal'],
    queryFn: api.systemConfig.getOrgMonthlyGoal,
    staleTime: 60_000,
  });
  const monthlyTargetAmount = monthlyGoal?.amount ?? 0;
  const [draftTarget, setDraftTarget] = useState('0');

  // Sync the input with the persisted value on first load and after a save.
  useEffect(() => {
    setDraftTarget(String(monthlyTargetAmount));
  }, [monthlyTargetAmount]);

  const setGoalMutation = useMutation({
    mutationFn: (amount: number) => api.systemConfig.setOrgMonthlyGoal(amount),
    onSuccess: () => {
      // Refresh the value across every dashboard reading the same query key.
      queryClient.invalidateQueries({ queryKey: ['org-monthly-goal'] });
    },
  });

  const { data: stats, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: api.dashboard.stats,
    refetchOnWindowFocus: true,
    staleTime: 30_000,
  });

  // Same query key the AnnouncementBoard uses, so we share the cache —
  // we don't double-fetch when both components mount on the same page.
  const { data: liveAnnouncements = [] } = useQuery<Announcement[]>({
    queryKey: ['announcements-active'],
    queryFn: () => api.announcements.listActive(),
    staleTime: 30_000,
  });

  const fmt = (n?: number) => (isLoading ? '—' : String(n ?? 0));
  const activeEmployees = stats?.activeEmployees ?? 0;
  const totalEmployees = stats?.totalEmployees ?? 0;
  const newThisMonth = stats?.newEmployeesThisMonth ?? 0;
  const presentToday = stats?.presentToday ?? 0;
  const departmentStats = stats?.departmentStats ?? [];
  const totalDepartmentEmployees = departmentStats.reduce(
    (sum, dept) => sum + (dept.employeeCount ?? 0),
    0,
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Admin Command Dashboard"
        description="Publish org-wide announcements, monitor workforce performance, and set the monthly amount target for all business dashboards."
      />

      {isError ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Couldn&apos;t load dashboard stats:{' '}
          {(error as Error)?.message || 'unknown error'}.
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatsCard
          label="Active Employees"
          value={fmt(activeEmployees)}
          meta={
            isLoading
              ? 'Loading…'
              : `${totalEmployees} on rolls · ${newThisMonth} joined this month`
          }
          icon={<Users size={22} />}
        />
        <StatsCard
          label="Live Announcements"
          value={String(liveAnnouncements.length)}
          meta="Visible across dashboards"
          icon={<Megaphone size={22} />}
        />
        <StatsCard
          label="Monthly Target"
          value={formatCurrency(monthlyTargetAmount)}
          meta="Set by admin in rupees"
          icon={<IndianRupee size={22} />}
        />
        <StatsCard
          label="Present Today"
          value={fmt(presentToday)}
          meta={
            isLoading
              ? 'Loading…'
              : totalEmployees > 0
                ? `${Math.round((presentToday / totalEmployees) * 100)}% of ${totalEmployees} on rolls`
                : 'No employees on rolls'
          }
          icon={<UserCheck size={22} />}
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">Department Distribution</h3>
              <p className="mt-1 text-sm text-slate-600">
                Headcount by department. Numbers refresh every 30 seconds.
              </p>
            </div>
            <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700">
              {totalDepartmentEmployees} total
            </span>
          </div>
          <div className="mt-5">
            {isLoading ? (
              <p className="text-sm text-slate-500">Loading department data…</p>
            ) : departmentStats.length === 0 ? (
              <p className="text-sm text-slate-500">No department data available yet.</p>
            ) : (
              <DataTable
                data={departmentStats}
                columns={[
                  { key: 'department', header: 'Department', render: (item) => item.departmentName },
                  { key: 'count', header: 'Employees', render: (item) => String(item.employeeCount) },
                  {
                    key: 'share',
                    header: '% of Total',
                    render: (item) =>
                      totalDepartmentEmployees > 0
                        ? `${((item.employeeCount / totalDepartmentEmployees) * 100).toFixed(1)}%`
                        : '0%',
                  },
                ]}
              />
            )}
          </div>
        </Card>

        <Card>
          <h3 className="text-lg font-semibold text-slate-900">Admin Monthly Target Control</h3>
          <p className="mt-2 text-sm text-slate-600">Set the rupee target that appears on Admin, Team Leader, and Employee dashboards.</p>
          <div className="mt-5 space-y-4">
            <Input
              label="Monthly Target Amount (INR)"
              type="number"
              value={draftTarget}
              onChange={(event) => setDraftTarget(event.target.value)}
            />
            <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
              Current published target: <span className="font-semibold text-slate-900">{formatCurrency(monthlyTargetAmount)}</span>
            </div>
            <Button
              onClick={() => setGoalMutation.mutate(Number(draftTarget) || 0)}
              disabled={setGoalMutation.isPending}
            >
              {setGoalMutation.isPending ? 'Publishing…' : 'Publish Monthly Target'}
            </Button>
            {setGoalMutation.isError ? (
              <p className="text-xs text-rose-600">
                Could not save: {(setGoalMutation.error as Error)?.message ?? 'try again'}
              </p>
            ) : null}
          </div>
        </Card>
      </div>

      <AnnouncementBoard role="Admin" canCreate />
    </div>
  );
};
