import { Bell, BotMessageSquare, LogOut } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/common/Avatar';
import { StatusBadge } from '@/components/common/StatusBadge';

export const Topbar = () => {
  const { user, logout } = useAuthStore();
  const toggleAssistant = useUIStore((state) => state.toggleAssistant);

  if (!user) return null;

  return (
    <div className="flex flex-col gap-4 border-b border-slate-200/80 bg-white/70 px-4 py-4 backdrop-blur md:flex-row md:items-center md:justify-between lg:px-8">
      <div>
        <p className="text-sm text-slate-500">Welcome back</p>
        <h1 className="text-xl font-semibold text-slate-900">{user.name}</h1>
      </div>

      <div className="flex items-center gap-3">
        <StatusBadge label={user.role} tone="info" />

        <Button variant="secondary" onClick={toggleAssistant} className="gap-2">
          <BotMessageSquare size={16} />
          Assistant
        </Button>

        <button className="rounded-full border border-slate-200 bg-white p-3 text-slate-600">
          <Bell size={18} />
        </button>

        <Button variant="ghost" className="gap-2" onClick={() => void logout()}>
          <LogOut size={16} />
          Logout
        </Button>

        <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-2">
          <Avatar name={user.name} src={user.avatarUrl} size="md" />
          <div className="hidden md:block">
            <p className="text-sm font-medium text-slate-900">{user.name}</p>
            <p className="text-xs text-slate-500">{user.email}</p>
          </div>
        </div>
      </div>
    </div>
  );
};
