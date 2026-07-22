import type { UserRole } from '@/types';

export type BackendRole = 'ROLE_ADMIN' | 'ROLE_HR' | 'ROLE_MANAGER' | 'ROLE_ATL' | 'ROLE_EMPLOYEE';

const backendToUiRoleMap: Record<BackendRole, UserRole> = {
  ROLE_ADMIN: 'Admin',
  ROLE_HR: 'HR',
  ROLE_MANAGER: 'Team Leader',
  ROLE_ATL: 'ATL',
  ROLE_EMPLOYEE: 'Employee',
};

// An ATL who is also a MANAGER falls through to the more privileged Team
// Leader portal; a plain ATL (ATL + Employee) lands on the ATL portal.
const rolePriority: UserRole[] = ['Admin', 'HR', 'Team Leader', 'ATL', 'Employee'];

export const mapBackendRoleToUiRole = (role: string): UserRole | null =>
  backendToUiRoleMap[role as BackendRole] ?? null;

export const mapBackendRolesToUiRoles = (roles: string[]): UserRole[] =>
  roles
    .map(mapBackendRoleToUiRole)
    .filter((role): role is UserRole => role !== null);

export const pickPrimaryRole = (roles: UserRole[]): UserRole =>
  rolePriority.find((role) => roles.includes(role)) ?? 'Employee';
