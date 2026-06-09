import { useMemo } from 'react';
import { useAuthStore } from '@/store/authStore';
import { UserRole } from '@/types';

export const useRoleAccess = (roles: UserRole[]) => {
  const role = useAuthStore((state) => state.user?.role);

  return useMemo(() => (role ? roles.includes(role) : false), [role, roles]);
};
