import { Clock3, Landmark, PlaneTakeoff, Users } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common/PageHeader';
import { StatsCard } from '@/components/common/StatsCard';
import { Card } from '@/components/ui/Card';
import { api } from '@/services/api';
import { formatCurrency } from '@/utils/format';
import { AnnouncementBoard } from './AnnouncementBoard';

/**
 * HR home dashboard. KPIs come from the live `/api/dashboard/stats` endpoint
 * (gated to ADMIN/HR/MANAGER). The mock-data fallback was removed in the
 * Tier 1 cleanup — once the dashboard endpoint is available the UI must
 * never silently regress to fixtures or stakeholders see stale numbers.
 */
export const HRDashboard = () => {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: api.dashboard.stats,
    // Refetch on window focus so the HR ops view feels live without a manual reload.
    refetchOnWindowFocus: true,
    staleTime: 30_000,
  });

  const totalEmployees = data?.totalEmployees ?? 0;
  const presentToday = data?.presentToday ?? 0;
  const lateToday = data?.lateToday ?? 0;
  const absentToday = data?.absentToday ?? 0;
  const pendingLeaves = data?.pendingLeaves ?? 0;
  const approvedLeavesThisMonth = data?.approvedLeavesThisMonth ?? 0;
  const paidPayrollsThisMonth = data?.paidPayrollsThisMonth ?? 0;
  const pendingPayrolls = data?.pendingPayrolls ?? 0;
  const monthlyPayroll = data?.monthlyPayroll ?? 0;
  const onTimePct = Math.round(data?.onTimePercentage ?? 0);

  const fmt = (n: number) => (isLoading ? '—' : String(n));

  return (
    <div className="space-y-6">
      <PageHeader
        title="HR Operations Dashboard"
        description="Operational view across headcount, attendance, leave, compliance, and payroll for Finbud Financial."
      />

      {isError ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Couldn&apos;t load dashboard stats:{' '}
          {(error as Error)?.message || 'unknown error'}. The numbers below may be stale.
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatsCard
          label="Total Employees"
          value={fmt(totalEmployees)}
          meta="Across all departments"
          icon={<Users size={22} />}
        />
        <StatsCard
          label="Present Today"
          value={fmt(presentToday)}
          meta={`${onTimePct}% on time · ${lateToday} late · ${absentToday} absent`}
          icon={<Clock3 size={22} />}
        />
        <StatsCard
          label="Pending Leave"
          value={fmt(pendingLeaves)}
          meta={`${approvedLeavesThisMonth} approved this month`}
          icon={<PlaneTakeoff size={22} />}
        />
        <StatsCard
          label="Processed Payroll"
          value={fmt(paidPayrollsThisMonth)}
          meta={
            isLoading
              ? 'Loading…'
              : `${pendingPayrolls} pending · ${formatCurrency(monthlyPayroll)} disbursed`
          }
          icon={<Landmark size={22} />}
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <h3 className="text-lg font-semibold text-slate-900">HR Priorities</h3>
          <p className="mt-1 text-xs text-slate-500">
            Live across attendance, payroll, and leave queues.
          </p>
          <div className="mt-5 grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl bg-brand-50 p-4">
              <p className="text-sm text-brand-700">Attendance</p>
              <p className="mt-2 text-xl font-semibold text-slate-900">
                {fmt(lateToday)} late arrival{lateToday === 1 ? '' : 's'}
              </p>
            </div>
            <div className="rounded-2xl bg-accent-50 p-4">
              <p className="text-sm text-accent-700">Payroll</p>
              <p className="mt-2 text-xl font-semibold text-slate-900">
                {fmt(pendingPayrolls)} salar{pendingPayrolls === 1 ? 'y' : 'ies'} staged
              </p>
            </div>
            <div className="rounded-2xl bg-sky-50 p-4">
              <p className="text-sm text-sky-700">Leave</p>
              <p className="mt-2 text-xl font-semibold text-slate-900">
                {fmt(pendingLeaves)} request{pendingLeaves === 1 ? '' : 's'} pending
              </p>
            </div>
          </div>
        </Card>
        <AnnouncementBoard role="HR" />
      </div>
    </div>
  );
};
