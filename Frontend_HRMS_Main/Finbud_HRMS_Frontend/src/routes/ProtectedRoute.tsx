import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { UserRole } from '@/types';
import { UnauthorizedPage } from '@/pages/UnauthorizedPage';

interface ProtectedRouteProps {
  roles: UserRole[];
  /**
   * Historically this flag was used to exempt the force-change-password
   * screen from the password-rotation redirect. The rotation is now
   * advisory — users can continue to the app with the default password
   * after acknowledging a one-time warning (PasswordChangeWarningModal) —
   * so no route is gated on `mustChangePassword` any more. The prop is
   * kept for backwards compatibility with existing call sites in
   * AppRouter but is functionally a no-op.
   */
  allowWhileMustChangePassword?: boolean;
}

export const ProtectedRoute = ({ roles }: ProtectedRouteProps) => {
  const { user, isAuthenticated, isInitializing } = useAuthStore();
  const location = useLocation();

  if (isInitializing) {
    return <div className="p-6 text-sm text-slate-500">Checking access...</div>;
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return roles.includes(user.role) ? <Outlet /> : <UnauthorizedPage />;
};
