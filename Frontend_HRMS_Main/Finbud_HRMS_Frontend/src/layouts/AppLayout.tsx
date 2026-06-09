import { Outlet } from 'react-router-dom';
import { AssistantPanel } from '@/features/assistant/components/AssistantPanel';
import { Sidebar } from '@/components/layout/Sidebar';
import { Topbar } from '@/components/layout/Topbar';
import { PasswordChangeWarningModal } from '@/components/security/PasswordChangeWarningModal';

export const AppLayout = () => (
  <div className="flex min-h-screen">
    <Sidebar />
    <div className="flex min-h-screen flex-1 flex-col">
      <Topbar />
      <main className="flex-1 px-4 py-6 lg:px-8">
        <Outlet />
      </main>
    </div>
    <AssistantPanel />
    {/* One-time nag when the user is still on the default password.
        Self-dismisses after acknowledgement; no effect once rotated. */}
    <PasswordChangeWarningModal />
  </div>
);
