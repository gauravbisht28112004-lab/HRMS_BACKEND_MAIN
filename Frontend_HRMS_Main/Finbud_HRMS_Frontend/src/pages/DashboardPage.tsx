import { useAuthStore } from '@/store/authStore';
import { AdminDashboard, EmployeeDashboard, HRDashboard, TeamLeaderDashboard } from '@/features/dashboard/components';

export const DashboardPage = () => {
  const role = useAuthStore((state) => state.user?.role);

  if (role === 'Admin') return <AdminDashboard />;
  if (role === 'HR') return <HRDashboard />;
  if (role === 'Team Leader') return <TeamLeaderDashboard />;
  return <EmployeeDashboard />;
};
