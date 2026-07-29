import type { UserRole } from '@/types';

export type BackendRole =
  | 'ROLE_ADMIN'
  | 'ROLE_HR'
  | 'ROLE_MANAGER'
  | 'ROLE_TEAM_LEADER'
  | 'ROLE_ATL'
  | 'ROLE_EMPLOYEE';

// Reporting chain: Admin -> Manager -> Team Leader -> ATL -> Employee.
// NOTE: ROLE_MANAGER maps to "Manager" (the level directly under Admin).
// ROLE_TEAM_LEADER is the distinct level below Manager.
const backendToUiRoleMap: Record<BackendRole, UserRole> = {
  ROLE_ADMIN: 'Admin',
  ROLE_HR: 'HR',
  ROLE_MANAGER: 'Manager',
  ROLE_TEAM_LEADER: 'Team Leader',
  ROLE_ATL: 'ATL',
  ROLE_EMPLOYEE: 'Employee',
};

// When a user holds several roles, they land on the most privileged portal.
const rolePriority: UserRole[] = ['Admin', 'HR', 'Manager', 'Team Leader', 'ATL', 'Employee'];

export const mapBackendRoleToUiRole = (role: string): UserRole | null =>
  backendToUiRoleMap[role as BackendRole] ?? null;

export const mapBackendRolesToUiRoles = (roles: string[]): UserRole[] =>
  roles
    .map(mapBackendRoleToUiRole)
    .filter((role): role is UserRole => role !== null);

export const pickPrimaryRole = (roles: UserRole[]): UserRole =>
  rolePriority.find((role) => roles.includes(role)) ?? 'Employee';
