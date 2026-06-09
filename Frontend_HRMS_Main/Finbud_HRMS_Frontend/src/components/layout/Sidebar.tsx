import {
  BarChart3,
  BellRing,
  Building2,
  CalendarClock,
  CalendarDays,
  ClipboardList,
  Clock3,
  FileCheck2,
  History,
  LayoutDashboard,
  LucideIcon,
  LogOut,
  MapPin,
  PlaneTakeoff,
  Target,
  Trophy,
  UploadCloud,
  UserRound,
  Users,
  Wallet,
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { navItems } from '@/constants/navigation';
import { useAuthStore } from '@/store/authStore';
import { cn } from '@/utils/cn';
import { getInitials } from '@/utils/format';

const iconMap: Record<string, LucideIcon> = {
  LayoutDashboard,
  UserRound,
  Users,
  Clock3,
  CalendarClock,
  CalendarDays,
  PlaneTakeoff,
  Wallet,
  Building2,
  BarChart3,
  Trophy,
  History,
  UploadCloud,
  FileCheck2,
  MapPin,
};

export const Sidebar = () => {
  const { user, logout } = useAuthStore();
  const role = user?.role;
  if (!role || !user) return null;

  if (role === 'Employee') {
    const employeeItems = [
      { label: 'Dashboard', path: '/', icon: LayoutDashboard },
      { label: 'Attendance', path: '/attendance', icon: CalendarDays },
      { label: 'Regularizations', path: '/regularizations', icon: FileCheck2 },
      { label: 'Leave Management', path: '/leave', icon: PlaneTakeoff },
      { label: 'Daily Commitment', path: '/daily-commitment', icon: Target },
      { label: 'Hourly Updates', path: '/hourly-updates', icon: Clock3 },
      { label: 'Monthly Targets', path: '/monthly-targets', icon: BarChart3 },
      { label: 'Payroll', path: '/employee-payroll', icon: Wallet },
      { label: 'Leaderboard', path: '/leaderboard', icon: Trophy },
    ];

    return (
      <aside className="hidden w-64 flex-col border-r border-slate-200 bg-[#fcfbf7] px-4 py-5 text-slate-700 lg:flex">
        <div className="flex items-center gap-3 border-b border-slate-200 pb-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#5b6957] text-white">
            <UserRound size={22} />
          </div>
          <div>
            <p className="text-lg font-semibold text-slate-900">FinBud</p>
            <p className="text-xs text-slate-500">Financial Portal</p>
          </div>
        </div>

        <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#5b6957] font-semibold text-white">
              {getInitials(user.name)}
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">{user.name}</p>
              <p className="text-xs text-slate-500">Employee</p>
            </div>
          </div>
        </div>

        <nav className="mt-5 flex flex-1 flex-col gap-1">
          {employeeItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={`${item.label}-${item.path}`}
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition',
                    isActive || (index === 0 && item.path === '/') ? 'bg-[#5b6957] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100',
                  )
                }
              >
                <Icon size={16} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <button
          onClick={() => void logout()}
          className="mt-4 flex items-center gap-3 rounded-xl border border-[#f3ddd1] bg-[#fff6f2] px-3 py-2.5 text-sm text-[#cf7e64] transition hover:bg-[#fff1ea]"
        >
          <LogOut size={16} />
          Logout
        </button>
      </aside>
    );
  }

  if (role === 'Team Leader') {
    const teamLeaderItems = [
      { label: 'Dashboard', path: '/', icon: LayoutDashboard },
      { label: 'Team Members', path: '/employees', icon: Users },
      { label: 'Team Attendance', path: '/attendance', icon: CalendarDays },
      { label: 'Regularizations', path: '/regularizations', icon: FileCheck2 },
      { label: 'Leave', path: '/leave', icon: PlaneTakeoff },
      { label: 'Daily Commitment', path: '/team-daily-commitment', icon: Target },
      { label: 'Monthly Commitment', path: '/team-monthly-commitment', icon: BarChart3 },
      { label: 'Team Reports', path: '/team-reports', icon: ClipboardList },
      { label: 'My Payroll', path: '/team-payroll', icon: Wallet },
      { label: 'Leaderboard', path: '/leaderboard', icon: Trophy },
    ];

    return (
      <aside className="hidden w-64 flex-col border-r border-slate-200 bg-[#fcfbf7] px-4 py-5 text-slate-700 lg:flex">
        <div className="flex items-center gap-3 border-b border-slate-200 pb-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-900 text-white">
            <Users size={22} />
          </div>
          <div>
            <p className="text-lg font-semibold text-slate-900">FinBud</p>
            <p className="text-xs text-slate-500">Team Leader Portal</p>
          </div>
        </div>

        <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-900 font-semibold text-white">
              {getInitials(user.name)}
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">{user.name}</p>
              <p className="text-xs text-slate-500">Team Leader</p>
            </div>
          </div>
        </div>

        <nav className="mt-5 flex flex-1 flex-col gap-1">
          {teamLeaderItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={`${item.label}-${item.path}`}
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition',
                    isActive ? 'bg-brand-900 text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100',
                  )
                }
              >
                <Icon size={16} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <button
          onClick={() => void logout()}
          className="mt-4 flex items-center gap-3 rounded-xl border border-[#f3ddd1] bg-[#fff6f2] px-3 py-2.5 text-sm text-[#cf7e64] transition hover:bg-[#fff1ea]"
        >
          <LogOut size={16} />
          Logout
        </button>
      </aside>
    );
  }

  const allowedItems = navItems.filter((item) => item.roles.includes(role));

  return (
    <aside className="hidden w-72 flex-col border-r border-white/70 bg-brand-900 px-5 py-6 text-white lg:flex">
      <div>
        <p className="text-xs uppercase tracking-[0.35em] text-brand-100/70">Finbud Financial</p>
        <h2 className="mt-3 text-2xl font-semibold">HRMS Control Tower</h2>
      </div>

      <nav className="mt-10 flex flex-1 flex-col gap-2">
        {allowedItems.map((item) => {
          const Icon = iconMap[item.icon];

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-2xl px-4 py-3 text-sm transition',
                  isActive ? 'bg-white text-brand-900 shadow-lg' : 'text-brand-100 hover:bg-white/10',
                )
              }
            >
              <Icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="rounded-3xl bg-white/10 p-4">
        <p className="text-sm font-medium">Policy Center</p>
        <p className="mt-1 text-xs text-brand-100/80">Quarter-end payroll review closes in 2 days.</p>
      </div>
    </aside>
  );
};
