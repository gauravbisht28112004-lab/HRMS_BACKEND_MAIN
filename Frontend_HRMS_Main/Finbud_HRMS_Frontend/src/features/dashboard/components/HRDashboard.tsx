import { Building2, UserCheck, UserPlus, Users } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common/PageHeader';
import { StatsCard } from '@/components/common/StatsCard';
import { api } from '@/services/api';
import { AnnouncementBoard } from './AnnouncementBoard';

/**
 * HR home dashboard. KPIs come from the live `/api/dashboard/stats` endpoint
 * (gated to ADMIN/HR/MANAGER). Attendance, leave, and payroll widgets were
 * removed when those modules moved out of the HRMS.
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
  const activeEmployees = data?.activeEmployees ?? 0;
  const newEmployeesThisMonth = data?.newEmployeesThisMonth ?? 0;
  const totalDepartments = data?.totalDepartments ?? 0;

  const fmt = (n: number) => (isLoading ? '—' : String(n));

  return (
    <div className="space-y-6">
      <PageHeader
        title="HR Operations Dashboard"
        description="Headcount and organisation overview for Finbud Financial."
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
          label="Active Employees"
          value={fmt(activeEmployees)}
          meta="Currently active"
          icon={<UserCheck size={22} />}
        />
        <StatsCard
          label="New This Month"
          value={fmt(newEmployeesThisMonth)}
          meta="Joined this month"
          icon={<UserPlus size={22} />}
        />
        <StatsCard
          label="Departments"
          value={fmt(totalDepartments)}
          meta="Total departments"
          icon={<Building2 size={22} />}
        />
      </div>

      <AnnouncementBoard role="HR" />
    </div>
  );
};
