import { NavItem } from '@/types';

export const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/', roles: ['Admin', 'HR', 'Team Leader', 'Employee'], icon: 'LayoutDashboard' },
  { label: 'My Profile', path: '/my-profile', roles: ['Employee'], icon: 'UserRound' },
  { label: 'Employees', path: '/employees', roles: ['Admin', 'HR', 'Team Leader'], icon: 'Users' },
  { label: 'Attendance', path: '/attendance', roles: ['Admin', 'HR', 'Team Leader', 'Employee'], icon: 'Clock3' },
  { label: 'Regularizations', path: '/regularizations', roles: ['Admin', 'HR', 'Team Leader', 'Employee'], icon: 'FileCheck2' },
  { label: 'Shifts', path: '/shifts', roles: ['Admin', 'HR'], icon: 'CalendarClock' },
  { label: 'Office Locations', path: '/office-locations', roles: ['Admin', 'HR'], icon: 'MapPin' },
  { label: 'Holidays', path: '/holidays', roles: ['Admin', 'HR'], icon: 'CalendarDays' },
  { label: 'Leave', path: '/leave', roles: ['Admin', 'HR', 'Team Leader', 'Employee'], icon: 'PlaneTakeoff' },
  { label: 'Payroll', path: '/payroll', roles: ['Admin', 'HR'], icon: 'Wallet' },
  { label: 'Departments', path: '/departments', roles: ['Admin', 'HR'], icon: 'Building2' },
  { label: 'Reports', path: '/reports', roles: ['Admin', 'HR'], icon: 'BarChart3' },
  { label: 'Leaderboard', path: '/leaderboard', roles: ['Admin', 'HR'], icon: 'Trophy' },
  { label: 'Audit Logs', path: '/audit-logs', roles: ['Admin', 'HR'], icon: 'History' },
  { label: 'Import Employees', path: '/admin/import', roles: ['Admin', 'HR'], icon: 'UploadCloud' },
];
