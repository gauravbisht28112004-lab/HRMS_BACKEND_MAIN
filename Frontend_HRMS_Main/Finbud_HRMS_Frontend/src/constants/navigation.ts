import { NavItem } from '@/types';

export const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/', roles: ['Admin', 'HR', 'Team Leader', 'Employee'], icon: 'LayoutDashboard' },
  { label: 'My Profile', path: '/my-profile', roles: ['Employee'], icon: 'UserRound' },
  { label: 'Employees', path: '/employees', roles: ['Admin', 'HR', 'Manager'], icon: 'Users' },
  { label: 'Managers Overview', path: '/manager-overview', roles: ['Admin', 'HR'], icon: 'Target' },
  { label: 'All Employee Targets', path: '/all-employee-targets', roles: ['Admin', 'HR'], icon: 'BarChart3' },
  { label: 'Departments', path: '/departments', roles: ['Admin', 'HR'], icon: 'Building2' },
  { label: 'Leaderboard', path: '/leaderboard', roles: ['Admin', 'HR'], icon: 'Trophy' },
  { label: 'Audit Logs', path: '/audit-logs', roles: ['Admin', 'HR'], icon: 'History' },
  { label: 'Import Employees', path: '/admin/import', roles: ['Admin', 'HR'], icon: 'UploadCloud' },
];
