import { useAuthStore } from '@/store/authStore';
import { AdminDashboard, EmployeeDashboard, HierarchyDashboard, HRDashboard } from '@/features/dashboard/components';

/**
 * Role -> home dashboard. Every supervisor level in the target chain
 * (Manager -> Team Leader -> ATL) lands on the unified HierarchyDashboard,
 * which shows their own target, their whole-team disbursal, and lets them
 * assign targets to their direct reports. Admin/HR/Employee keep their
 * existing dashboards untouched.
 */
export const DashboardPage = () => {
  const role = useAuthStore((state) => state.user?.role);

  if (role === 'Admin') return <AdminDashboard />;
  if (role === 'HR') return <HRDashboard />;
  if (role === 'Manager' || role === 'Team Leader' || role === 'ATL') return <HierarchyDashboard />;
  return <EmployeeDashboard />;
};
