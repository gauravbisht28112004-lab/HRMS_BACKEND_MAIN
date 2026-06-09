import type { UserRole } from '@/types';

export type BackendRole = 'ROLE_ADMIN' | 'ROLE_HR' | 'ROLE_MANAGER' | 'ROLE_EMPLOYEE';

const backendToUiRoleMap: Record<BackendRole, UserRole> = {
  ROLE_ADMIN: 'Admin',
  ROLE_HR: 'HR',
  ROLE_MANAGER: 'Team Leader',
  ROLE_EMPLOYEE: 'Employee',
};

const rolePriority: UserRole[] = ['Admin', 'HR', 'Team Leader', 'Employee'];

export const mapBackendRoleToUiRole = (role: string): UserRole | null =>
  backendToUiRoleMap[role as BackendRole] ?? null;

export const mapBackendRolesToUiRoles = (roles: string[]): UserRole[] =>
  roles
    .map(mapBackendRoleToUiRole)
    .filter((role): role is UserRole => role !== null);

export const pickPrimaryRole = (roles: UserRole[]): UserRole =>
  rolePriority.find((role) => roles.includes(role)) ?? 'Employee';
