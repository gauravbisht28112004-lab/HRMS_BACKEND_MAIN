import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type BackendUserAccountResponse } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { StatusBadge } from '@/components/common/StatusBadge';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { useAuthStore } from '@/store/authStore';

/**
 * Admin / HR role editor for the login account attached to an employee.
 *
 * Authorisation model (mirrors the backend — see AdminUserServiceImpl):
 *  - Admin sees + edits all four roles.
 *  - HR sees all roles but can only tick/untick Employee + Team Leader.
 *  - HR cannot reset password or change status on an Admin/HR account.
 *
 * Server-side checks are authoritative; the UI mirrors them to avoid
 * showing disabled controls that would always 403.
 */

// Backend role constants, kept in sync with com.financebuddha.finbud.hrms.enums.RoleType.
const ROLE_ADMIN = 'ROLE_ADMIN';
const ROLE_HR = 'ROLE_HR';
const ROLE_MANAGER = 'ROLE_MANAGER';
const ROLE_EMPLOYEE = 'ROLE_EMPLOYEE';

const ALL_ROLES = [ROLE_ADMIN, ROLE_HR, ROLE_MANAGER, ROLE_EMPLOYEE] as const;

const roleLabel: Record<string, string> = {
  [ROLE_ADMIN]: 'Admin',
  [ROLE_HR]: 'HR',
  [ROLE_MANAGER]: 'Team Leader',
  [ROLE_EMPLOYEE]: 'Employee',
};

const formatDateTime = (iso?: string | null): string => {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
};

interface UserAccessCardProps {
  /** Employee code (e.g. FBD260005, ND33454). Used to look up the linked login. */
  employeeCode: string;
}

export const UserAccessCard = ({ employeeCode }: UserAccessCardProps) => {
  const { user: currentUser } = useAuthStore();
  const queryClient = useQueryClient();

  const isAdmin = currentUser?.role === 'Admin';
  const isHr = currentUser?.role === 'HR';
  // Gate hooks themselves on role membership: callers without permission still
  // have all hooks evaluated (so React's hook order remains stable across
  // re-renders), but the network query is disabled to avoid a wasted 403.
  const hasAccess = isAdmin || isHr;

  const { data: account, isLoading, refetch } = useQuery({
    queryKey: ['admin-user-account', employeeCode],
    queryFn: () => api.admin.getUserByEmployeeCode(employeeCode),
    enabled: Boolean(employeeCode) && hasAccess,
  });

  // Selected roles local state — seeded from the server response.
  const [selectedRoles, setSelectedRoles] = useState<Set<string> | null>(null);
  const [resetResult, setResetResult] = useState<{ username: string; password: string } | null>(null);
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false);
  const [statusConfirm, setStatusConfirm] = useState<null | { nextActive: boolean }>(null);
  const [copyNotice, setCopyNotice] = useState<string | null>(null);

  // Re-hydrate the editor whenever the underlying account row identity changes
  // (a different employee, or a fresh fetch returned a different userId). We
  // intentionally don't depend on `account.roles` directly — the save mutation
  // already calls setSelectedRoles itself, and listing roles here would clobber
  // an in-flight edit if a background refetch happens to land mid-typing.
  useEffect(() => {
    if (account) {
      setSelectedRoles(new Set(account.roles));
    } else {
      setSelectedRoles(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [account?.userId, employeeCode]);

  const rolesFromServer = useMemo(
    () => new Set(account?.roles ?? []),
    [account?.roles],
  );
  const currentEditSet = selectedRoles ?? rolesFromServer;
  const hasPendingRoleChange = useMemo(() => {
    if (!account) return false;
    const server = new Set(account.roles);
    if (server.size !== currentEditSet.size) return true;
    for (const r of currentEditSet) if (!server.has(r)) return true;
    return false;
  }, [account, currentEditSet]);

  const targetIsPrivileged = useMemo(() => {
    const server = new Set(account?.roles ?? []);
    return server.has(ROLE_ADMIN) || server.has(ROLE_HR);
  }, [account?.roles]);

  const canResetPassword = isAdmin || (isHr && !targetIsPrivileged);
  const canToggleStatus = isAdmin || (isHr && !targetIsPrivileged);

  const updateRolesMutation = useMutation({
    mutationFn: async (roles: string[]) => {
      if (!account) throw new Error('No account loaded');
      return api.admin.updateUserRoles(account.userId, roles);
    },
    onSuccess: async (fresh: BackendUserAccountResponse) => {
      // Reset local edit state to match what the server now has — this
      // makes the "unsaved changes" banner disappear cleanly.
      setSelectedRoles(new Set(fresh.roles));
      queryClient.setQueryData(['admin-user-account', employeeCode], fresh);
      await refetch();
    },
  });

  const statusMutation = useMutation({
    mutationFn: async (isActive: boolean) => {
      if (!account) throw new Error('No account loaded');
      return api.admin.updateUserStatus(account.userId, isActive);
    },
    onSuccess: async (fresh: BackendUserAccountResponse) => {
      queryClient.setQueryData(['admin-user-account', employeeCode], fresh);
      await refetch();
    },
  });

  const resetPasswordMutation = useMutation({
    mutationFn: async () => {
      if (!account) throw new Error('No account loaded');
      return api.admin.resetUserPassword(account.userId);
    },
    onSuccess: async (result) => {
      setResetResult({ username: result.username, password: result.temporaryPassword });
      await refetch();
    },
  });

  /**
   * Repair flow for employees that exist in HR's directory but have no
   * matching `users` row — typically a legacy import or a manual create
   * that ran before {@code EmployeeServiceImpl.autoProvisionUser} shipped.
   *
   * Strategy: call admin.createUser (which doesn't return the plaintext
   * password) and then immediately call resetUserPassword on the freshly
   * created account. The reset endpoint:
   *   - returns the plaintext (so HR can hand it to the joiner),
   *   - sets passwordChangedAt=null (forces rotation on first login),
   *   - clears any lockout / failed-attempt counter.
   * That is exactly the state a fresh provisioning produces, so the second
   * call is essentially idempotent — it just exists to leak the plaintext
   * back to the UI without changing the LoginResponse contract.
   */
  const provisionMutation = useMutation({
    mutationFn: async () => {
      // Step 1: create the User row. Username/password/role all default on
      // the server side (employee.loginUsername || employeeId.toLowerCase(),
      // system_config.auth.default_password, ROLE_EMPLOYEE).
      await api.admin.createUser({ employeeId: employeeCode });

      // Step 2: read back the new userId.
      const fresh = await api.admin.getUserByEmployeeCode(employeeCode);
      if (!fresh) {
        throw new Error('Provisioned but the account lookup failed unexpectedly');
      }

      // Step 3: surface the plaintext via the reset endpoint.
      return api.admin.resetUserPassword(fresh.userId);
    },
    onSuccess: async (result) => {
      setResetResult({ username: result.username, password: result.temporaryPassword });
      await refetch();
    },
  });

  const toggleRole = (role: string) => {
    if (!account) return;
    const next = new Set(currentEditSet);
    if (next.has(role)) next.delete(role);
    else next.add(role);
    setSelectedRoles(next);
  };

  // Disable the Admin + HR rows for an HR caller. Also disable any row that
  // would leave the user with zero roles (requires at least one to save).
  const isRoleEditableByCaller = (role: string): boolean => {
    if (isAdmin) return true;
    // HR — only Employee and Manager are editable.
    return role === ROLE_MANAGER || role === ROLE_EMPLOYEE;
  };

  const handleSaveRoles = async () => {
    if (!hasPendingRoleChange) return;
    const rolesArray = Array.from(currentEditSet);
    if (rolesArray.length === 0) return;
    await updateRolesMutation.mutateAsync(rolesArray);
  };

  const handleCopy = (value: string, label: string) => {
    void navigator.clipboard.writeText(value).then(() => {
      setCopyNotice(`${label} copied`);
      window.setTimeout(() => setCopyNotice(null), 2000);
    });
  };

  // Gate is below the hook calls so React's hook order is stable across role
  // changes. The parent (EmployeeProfilePage) already gates on canHrEdit, but
  // this defensive check keeps the component independently safe.
  if (!hasAccess) return null;

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">Portal Access</h3>
          <p className="mt-1 text-sm text-slate-500">
            Manage the login account linked to this employee. Role changes take effect on their next login.
          </p>
        </div>
        {account ? (
          <div className="flex items-center gap-2">
            <StatusBadge
              label={account.isActive ? 'Active' : 'Deactivated'}
              tone={account.isActive ? 'success' : 'danger'}
            />
            {account.locked ? <StatusBadge label="Locked" tone="warning" /> : null}
            {account.mustChangePassword ? (
              <StatusBadge label="Must change password" tone="info" />
            ) : null}
          </div>
        ) : null}
      </div>

      {isLoading ? (
        <p className="mt-4 text-sm text-slate-500">Loading account…</p>
      ) : !account ? (
        <div className="mt-4 space-y-3 rounded-xl border border-dashed border-slate-200 bg-slate-50/70 p-4 text-sm text-slate-600">
          <p>
            No login account is provisioned for this employee yet. This usually
            means the record pre-dates auto-provisioning or was imported from an
            older system.
          </p>
          <p className="text-xs text-slate-500">
            Provisioning will create a login with username{' '}
            <span className="font-mono font-semibold text-slate-700">
              {employeeCode.toLowerCase()}
            </span>{' '}
            and the Finbud default password. The plaintext will be shown once
            for you to share with the employee, and they&apos;ll be forced to
            change it on first login.
          </p>
          <div className="flex flex-wrap items-center gap-2 pt-1">
            <Button
              variant="primary"
              disabled={provisionMutation.isPending}
              onClick={() => provisionMutation.mutate()}
            >
              {provisionMutation.isPending ? 'Provisioning…' : 'Provision login account'}
            </Button>
            {provisionMutation.isError ? (
              <span className="text-xs text-rose-600">
                {(provisionMutation.error as Error).message || 'Failed to provision account.'}
              </span>
            ) : null}
          </div>
        </div>
      ) : (
        <div className="mt-5 space-y-5">
          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Username</p>
              <p className="mt-1 font-mono text-sm font-semibold text-slate-900">{account.username}</p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Last login</p>
              <p className="mt-1 text-sm text-slate-700">{formatDateTime(account.lastLoginAt)}</p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Password changed</p>
              <p className="mt-1 text-sm text-slate-700">{formatDateTime(account.passwordChangedAt)}</p>
            </div>
          </div>

          <div>
            <p className="text-sm font-semibold text-slate-900">Roles</p>
            <p className="mt-1 text-xs text-slate-500">
              {isHr
                ? 'As HR you can grant or revoke Employee and Team Leader. Admin and HR roles can only be changed by an Admin.'
                : 'Admins may grant any role. At least one role is required.'}
            </p>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              {ALL_ROLES.map((role) => {
                const editable = isRoleEditableByCaller(role);
                const checked = currentEditSet.has(role);
                return (
                  <label
                    key={role}
                    className={`flex items-center gap-3 rounded-xl border px-3 py-2 text-sm ${
                      editable
                        ? 'border-slate-200 bg-white hover:bg-slate-50'
                        : 'border-slate-100 bg-slate-50/60 text-slate-400'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      disabled={!editable}
                      onChange={() => toggleRole(role)}
                      className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-300"
                    />
                    <span className="flex flex-col">
                      <span className="font-medium text-slate-800">{roleLabel[role]}</span>
                      <span className="text-xs text-slate-500">{role}</span>
                    </span>
                  </label>
                );
              })}
            </div>

            {hasPendingRoleChange ? (
              <p className="mt-3 text-xs text-amber-700">Unsaved role changes.</p>
            ) : null}
            {updateRolesMutation.isError ? (
              <p className="mt-2 text-xs text-rose-600">
                {(updateRolesMutation.error as Error).message || 'Failed to update roles.'}
              </p>
            ) : null}

            <div className="mt-3 flex items-center gap-2">
              <Button
                variant="primary"
                disabled={!hasPendingRoleChange || updateRolesMutation.isPending || currentEditSet.size === 0}
                onClick={handleSaveRoles}
              >
                {updateRolesMutation.isPending ? 'Saving…' : 'Save roles'}
              </Button>
              {hasPendingRoleChange ? (
                <Button
                  variant="ghost"
                  onClick={() => setSelectedRoles(new Set(account.roles))}
                  disabled={updateRolesMutation.isPending}
                >
                  Discard
                </Button>
              ) : null}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 border-t border-slate-100 pt-4">
            <Button
              variant="secondary"
              disabled={!canResetPassword || resetPasswordMutation.isPending}
              title={
                !canResetPassword
                  ? 'HR cannot reset the password of an Admin or HR account'
                  : 'Reset to system default and force rotation on next login'
              }
              onClick={() => setResetConfirmOpen(true)}
            >
              Reset password to default
            </Button>
            <Button
              variant={account.isActive ? 'danger' : 'primary'}
              disabled={!canToggleStatus || statusMutation.isPending}
              title={
                !canToggleStatus
                  ? 'HR cannot change the status of an Admin or HR account'
                  : account.isActive
                    ? 'Block this user from logging in'
                    : 'Re-enable login for this user'
              }
              onClick={() => setStatusConfirm({ nextActive: !account.isActive })}
            >
              {account.isActive ? 'Deactivate account' : 'Activate account'}
            </Button>
            {statusMutation.isError ? (
              <p className="text-xs text-rose-600">
                {(statusMutation.error as Error).message || 'Status change failed.'}
              </p>
            ) : null}
          </div>
        </div>
      )}

      {/* Reset password confirmation. */}
      <ConfirmModal
        open={resetConfirmOpen}
        title="Reset password to default?"
        description={
          `This will reset the account's password to the Finbud default and force the user to change it on their next login. ` +
          `The plaintext will be shown exactly once — make sure you can share it with the user before confirming.`
        }
        confirmLabel="Reset password"
        onClose={() => setResetConfirmOpen(false)}
        onConfirm={async () => {
          setResetConfirmOpen(false);
          try {
            await resetPasswordMutation.mutateAsync();
          } catch {
            // error surfaced via mutation.isError
          }
        }}
      />

      {/* Status toggle confirmation. */}
      <ConfirmModal
        open={statusConfirm !== null}
        title={statusConfirm?.nextActive ? 'Re-activate account?' : 'Deactivate account?'}
        description={
          statusConfirm?.nextActive
            ? 'The user will be able to log in again. Any lockout will also be cleared.'
            : 'The user will immediately be unable to log in. The Employee record is not affected.'
        }
        confirmLabel={statusConfirm?.nextActive ? 'Re-activate' : 'Deactivate'}
        onClose={() => setStatusConfirm(null)}
        onConfirm={async () => {
          const next = statusConfirm?.nextActive ?? false;
          setStatusConfirm(null);
          try {
            await statusMutation.mutateAsync(next);
          } catch {
            // error surfaced via mutation.isError
          }
        }}
      />

      {/* Generated-credentials modal after password reset. */}
      {resetResult ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-md">
            <h3 className="text-lg font-semibold text-slate-900">Password reset</h3>
            <p className="mt-2 text-sm text-slate-500">
              The user&apos;s password has been reset. This is the <strong>only</strong> time you&apos;ll see the
              plaintext — share it with the user via a secure channel now. They&apos;ll be forced to change it
              on their next login.
            </p>
            <div className="mt-4 space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm">
              <div className="flex items-center justify-between gap-3">
                <span className="text-slate-500">Username</span>
                <div className="flex items-center gap-2">
                  <span className="font-mono font-semibold text-slate-900">{resetResult.username}</span>
                  <button
                    type="button"
                    className="text-xs font-medium text-brand-700 hover:underline"
                    onClick={() => handleCopy(resetResult.username, 'Username')}
                  >
                    Copy
                  </button>
                </div>
              </div>
              <div className="flex items-center justify-between gap-3">
                <span className="text-slate-500">Temporary password</span>
                <div className="flex items-center gap-2">
                  <span className="font-mono font-semibold text-slate-900">{resetResult.password}</span>
                  <button
                    type="button"
                    className="text-xs font-medium text-brand-700 hover:underline"
                    onClick={() => handleCopy(resetResult.password, 'Password')}
                  >
                    Copy
                  </button>
                </div>
              </div>
            </div>
            {copyNotice ? <p className="mt-2 text-xs text-emerald-700">{copyNotice}</p> : null}
            <div className="mt-6 flex justify-end">
              <Button variant="primary" onClick={() => setResetResult(null)}>
                Done
              </Button>
            </div>
          </Card>
        </div>
      ) : null}
    </Card>
  );
};
