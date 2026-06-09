import { useEffect, useMemo, useState } from 'react';
import { Pencil, Trash2, UserPlus } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { DepartmentForm } from '@/features/departments/components/DepartmentForm';
import { EmployeeForm } from '@/features/employee/components/EmployeeForm';
import { api, type EmployeeCreateResult } from '@/services/api';
import { ApiError } from '@/services/contracts';
import {
  createEmptyEmployeeFormValues,
  mapDepartmentFormToRequest,
  mapEmployeeFormToRequest,
  type DepartmentFormValues,
  type EmployeeFormValues,
} from '@/services/requestMappers';
import { useAuthStore } from '@/store/authStore';
import type { Department } from '@/types';

/**
 * Format a backend / network error into a single human-readable line for
 * inline banners. Mirrors the helper on EmployeesPage so the two pages
 * surface failures identically — when we extract a shared helper this is
 * the canonical shape.
 */
const getMutationErrorMessage = (error: unknown) => {
  if (error instanceof ApiError) {
    return error.errors?.length ? `${error.message} ${error.errors.join(' ')}` : error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
};

interface EditFormState {
  name: string;
  code: string;
  managerId: string;
}

export const DepartmentsPage = () => {
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const canManage = user?.role === 'Admin' || user?.role === 'HR';

  // ---- Source data ----
  const { data: departments = [] } = useQuery({
    queryKey: ['departments'],
    queryFn: api.departments.list,
  });
  const { data: employees = [] } = useQuery({
    queryKey: ['department-managers'],
    queryFn: api.employees.list,
  });
  // Shifts are needed for the inline EmployeeForm (Add Employee from row).
  // Same query key as EmployeesPage so the cache is shared.
  const { data: shifts = [] } = useQuery({
    queryKey: ['shifts-options'],
    queryFn: api.shifts.list,
  });

  // ---- Row-action state ----
  const [editTarget, setEditTarget] = useState<Department | null>(null);
  const [editForm, setEditForm] = useState<EditFormState>({ name: '', code: '', managerId: '' });
  const [editError, setEditError] = useState<string | null>(null);
  const [editLeaderUnsetWarning, setEditLeaderUnsetWarning] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<Department | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const [addEmployeeTarget, setAddEmployeeTarget] = useState<Department | null>(null);

  // Post-create credentials modal — same shape as EmployeesPage. Backend
  // only returns the temporary password once; if HR misses it they have
  // to reset via Admin > Users. So we surface it loudly here too.
  const [createdCreds, setCreatedCreds] = useState<{
    employeeId: string;
    fullName: string;
    userProvisioned: boolean;
    username?: string | null;
    password?: string | null;
    skippedReason?: string | null;
  } | null>(null);
  const [credsCopyNotice, setCredsCopyNotice] = useState<string | null>(null);

  // Sync the edit form whenever the user opens a different row.
  useEffect(() => {
    if (editTarget) {
      setEditForm({
        name: editTarget.name,
        code: editTarget.code ?? '',
        managerId: editTarget.managerId ? String(editTarget.managerId) : '',
      });
      setEditError(null);
      setEditLeaderUnsetWarning(false);
    }
  }, [editTarget]);

  // ---- Options for selects ----
  const managerOptions = useMemo(
    () => [
      { label: 'Select team leader', value: '' },
      ...employees.map((employee) => ({
        label: `${employee.firstName} ${employee.lastName}`,
        value: String(employee.backendId ?? ''),
      })),
    ],
    [employees],
  );

  const departmentOptions = useMemo(
    () => [
      { label: 'Select department', value: '' },
      ...departments.map((item) => ({
        label: item.name,
        value: String(item.backendId ?? ''),
      })),
    ],
    [departments],
  );

  const shiftOptions = useMemo(
    () => [
      { label: 'Select shift', value: '' },
      ...shifts.map((item) => ({
        label: item.name,
        value: String(item.backendId ?? ''),
      })),
    ],
    [shifts],
  );

  const employeeManagerOptions = useMemo(
    () => [
      { label: 'Select manager', value: '' },
      ...employees.map((item) => ({
        label: `${item.firstName} ${item.lastName}`,
        value: String(item.backendId ?? ''),
      })),
    ],
    [employees],
  );

  // ---- Mutations ----
  const createDepartmentMutation = useMutation({
    mutationFn: (values: DepartmentFormValues) =>
      api.departments.create(mapDepartmentFormToRequest(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['departments'] });
    },
  });

  /**
   * Edit flow uses BOTH endpoints intentionally:
   *   - PUT /departments/{id} for name/code edits (manager omitted from
   *     payload so the backend leaves it alone — see DepartmentServiceImpl
   *     which only re-binds manager when `managerId != null`).
   *   - POST /departments/{id}/manager/{managerId} only when the user
   *     actually picked a different leader. Cleaner audit trail than
   *     bundling everything into the update.
   *
   * Limitation: the current backend cannot *unset* a leader (passing null
   * is interpreted as "leave alone"). If the user tries to clear it we
   * surface an inline warning instead of silently no-op'ing.
   */
  const updateDepartmentMutation = useMutation({
    mutationFn: async (input: { target: Department; form: EditFormState }) => {
      const { target, form } = input;
      if (!target.backendId) {
        throw new Error('Missing backend department id.');
      }

      const trimmedName = form.name.trim();
      const trimmedCode = form.code.trim().toUpperCase();
      if (!trimmedName || !trimmedCode) {
        throw new Error('Name and code are required.');
      }

      // 1) Patch name / code (managerId omitted on purpose).
      await api.departments.update(target.backendId, {
        name: trimmedName,
        code: trimmedCode,
        description: '',
      });

      // 2) Reassign the manager only if it actually changed and the new
      //    value is non-empty. Empty → do nothing (see limitation above).
      const previousManagerId = target.managerId ? String(target.managerId) : '';
      const nextManagerId = form.managerId.trim();
      if (nextManagerId && nextManagerId !== previousManagerId) {
        await api.departments.assignManager(target.backendId, Number(nextManagerId));
      }
    },
    onSuccess: async () => {
      setEditTarget(null);
      await queryClient.invalidateQueries({ queryKey: ['departments'] });
      // Employees query carries a `teamLeader` string per row that depends
      // on the department's manager — refresh it too so the Employees page
      // doesn't show stale leader names.
      await queryClient.invalidateQueries({ queryKey: ['employees'] });
    },
    onError: (err) => {
      setEditError(getMutationErrorMessage(err));
    },
  });

  const deleteDepartmentMutation = useMutation({
    mutationFn: async (target: Department) => {
      if (!target.backendId) {
        throw new Error('Missing backend department id.');
      }
      await api.departments.remove(target.backendId);
    },
    onSuccess: async () => {
      setDeleteTarget(null);
      setDeleteError(null);
      await queryClient.invalidateQueries({ queryKey: ['departments'] });
    },
    onError: (err) => {
      // Most common failure: backend rejects because the department still
      // has employees. Surface that exact message inline so HR understands
      // the next step (move employees first).
      setDeleteError(getMutationErrorMessage(err));
    },
  });

  /**
   * Add Employee flow — opens the existing EmployeeForm pre-filled with
   * this row's department + leader. We reuse api.employees.create so the
   * backend's auto-provisioning of a User row + temp password works the
   * same as it does on the Employees page.
   */
  const createEmployeeMutation = useMutation({
    mutationFn: async (values: EmployeeFormValues): Promise<EmployeeCreateResult> => {
      return api.employees.create(mapEmployeeFormToRequest(values));
    },
    onSuccess: async (result) => {
      setAddEmployeeTarget(null);
      // Headcount on this page comes from BackendDepartmentResponse.employeeCount,
      // so we have to refetch departments to see the bump. Employees list also
      // gets invalidated so it picks up the new row.
      await queryClient.invalidateQueries({ queryKey: ['departments'] });
      await queryClient.invalidateQueries({ queryKey: ['employees'] });
      await queryClient.invalidateQueries({ queryKey: ['department-managers'] });

      const created = result.employee;
      setCreatedCreds({
        employeeId: created.id,
        fullName: `${created.firstName} ${created.lastName}`.trim() || created.id,
        userProvisioned: result.userProvisioned,
        username: result.generatedUsername,
        password: result.generatedTemporaryPassword,
        skippedReason: result.provisioningSkippedReason,
      });
    },
  });

  const handleCredsCopy = (value: string, label: string) => {
    void navigator.clipboard.writeText(value).then(() => {
      setCredsCopyNotice(`${label} copied`);
      window.setTimeout(() => setCredsCopyNotice(null), 2000);
    });
  };

  // Pre-filled form values when launching Add Employee from a row.
  const addEmployeeInitialValues = useMemo<EmployeeFormValues | undefined>(() => {
    if (!addEmployeeTarget) return undefined;
    const base = createEmptyEmployeeFormValues();
    return {
      ...base,
      departmentId: addEmployeeTarget.backendId ? String(addEmployeeTarget.backendId) : '',
      managerId: addEmployeeTarget.managerId ? String(addEmployeeTarget.managerId) : '',
    };
  }, [addEmployeeTarget]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Department Management"
        description="Create departments, assign team leaders, and track headcount distribution."
      />

      <DepartmentForm
        onSubmit={(values) => createDepartmentMutation.mutateAsync(values)}
        isSubmitting={createDepartmentMutation.isPending}
        managerOptions={managerOptions}
      />

      <DataTable
        data={departments}
        columns={[
          { key: 'name', header: 'Department', render: (department) => department.name },
          { key: 'teamLeader', header: 'Team Leader', render: (department) => department.teamLeader },
          {
            key: 'employees',
            header: 'Employees',
            render: (department) => department.totalEmployees,
          },
          {
            key: 'actions',
            header: 'Actions',
            render: (department) => (
              <div className="flex items-center gap-2">
                {canManage ? (
                  <Button variant="secondary" onClick={() => setEditTarget(department)}>
                    <Pencil size={14} className="mr-1" /> Edit
                  </Button>
                ) : null}
                {canManage ? (
                  <Button variant="ghost" onClick={() => setAddEmployeeTarget(department)}>
                    <UserPlus size={14} className="mr-1" /> Add Employee
                  </Button>
                ) : null}
                {canManage ? (
                  <button
                    className="rounded-lg p-2 text-rose-600 hover:bg-rose-50"
                    onClick={() => {
                      setDeleteError(null);
                      setDeleteTarget(department);
                    }}
                    aria-label={`Delete ${department.name}`}
                  >
                    <Trash2 size={16} />
                  </button>
                ) : null}
              </div>
            ),
          },
        ]}
      />

      {/* Edit department modal */}
      {editTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-lg">
            <h3 className="text-lg font-semibold text-slate-900">Edit department</h3>
            <p className="mt-1 text-sm text-slate-500">
              Updating <strong>{editTarget.name}</strong>. Changing the team leader reassigns every
              direct report on the next page refresh.
            </p>

            {editError ? (
              <div className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {editError}
              </div>
            ) : null}

            {editLeaderUnsetWarning ? (
              <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-800">
                Heads up: the current backend doesn&apos;t support unsetting the team leader from this
                screen. Pick a different leader or leave the existing one in place.
              </div>
            ) : null}

            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <Input
                label="Department Name"
                value={editForm.name}
                onChange={(event) => setEditForm({ ...editForm, name: event.target.value })}
              />
              <Input
                label="Department Code"
                value={editForm.code}
                onChange={(event) => setEditForm({ ...editForm, code: event.target.value })}
              />
              <div className="md:col-span-2">
                <Select
                  label="Assign Team Leader"
                  value={editForm.managerId}
                  onChange={(event) => {
                    const next = event.target.value;
                    setEditForm({ ...editForm, managerId: next });
                    setEditLeaderUnsetWarning(Boolean(editTarget.managerId) && next === '');
                  }}
                  options={managerOptions}
                />
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <Button
                variant="secondary"
                onClick={() => setEditTarget(null)}
                disabled={updateDepartmentMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                onClick={() => {
                  setEditError(null);
                  updateDepartmentMutation.mutate({ target: editTarget, form: editForm });
                }}
                disabled={updateDepartmentMutation.isPending}
              >
                {updateDepartmentMutation.isPending ? 'Saving…' : 'Save changes'}
              </Button>
            </div>
          </Card>
        </div>
      ) : null}

      {/* Delete confirm */}
      <ConfirmModal
        open={Boolean(deleteTarget)}
        title="Delete department?"
        description={
          deleteError
            ? deleteError
            : deleteTarget
            ? `This will remove "${deleteTarget.name}" (${deleteTarget.totalEmployees} employee${
                deleteTarget.totalEmployees === 1 ? '' : 's'
              }). The backend rejects deletion when employees > 0 — move them first.`
            : ''
        }
        onClose={() => {
          setDeleteTarget(null);
          setDeleteError(null);
        }}
        onConfirm={() => {
          if (deleteTarget) {
            deleteDepartmentMutation.mutate(deleteTarget);
          }
        }}
        confirmLabel={deleteDepartmentMutation.isPending ? 'Deleting…' : 'Delete'}
      />

      {/* Add Employee modal — reuses the EmployeeForm from the Employees page. */}
      {addEmployeeTarget ? (
        <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-950/40 p-4 backdrop-blur-sm">
          <div className="w-full max-w-5xl py-6">
            {createEmployeeMutation.isError ? (
              <div className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {getMutationErrorMessage(createEmployeeMutation.error)}
              </div>
            ) : null}

            <div className="mb-3 flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm">
              <span className="font-medium text-slate-900">
                Add employee to <strong>{addEmployeeTarget.name}</strong>
                {addEmployeeTarget.teamLeader && addEmployeeTarget.teamLeader !== 'Not Assigned'
                  ? ` (Reporting to ${addEmployeeTarget.teamLeader})`
                  : ''}
              </span>
              <Button
                variant="ghost"
                onClick={() => setAddEmployeeTarget(null)}
                disabled={createEmployeeMutation.isPending}
              >
                Close
              </Button>
            </div>

            <EmployeeForm
              initialValues={addEmployeeInitialValues}
              onSubmit={(values) => createEmployeeMutation.mutateAsync(values)}
              onCancel={() => setAddEmployeeTarget(null)}
              isSubmitting={createEmployeeMutation.isPending}
              isEditMode={false}
              departmentOptions={departmentOptions}
              managerOptions={employeeManagerOptions}
              shiftOptions={shiftOptions}
              backendEmployeeId={null}
              canEditSalary={canManage}
            />
          </div>
        </div>
      ) : null}

      {/* Post-create credentials modal — duplicated from EmployeesPage on
       * purpose to keep this change scoped. When this pattern shows up a
       * third time, extract it into a shared component. */}
      {createdCreds ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-md">
            {createdCreds.userProvisioned ? (
              <>
                <h3 className="text-lg font-semibold text-slate-900">Employee created</h3>
                <p className="mt-2 text-sm text-slate-500">
                  A login account has been provisioned for{' '}
                  <strong>{createdCreds.fullName}</strong> ({createdCreds.employeeId}). This is the{' '}
                  <strong>only</strong> time you&apos;ll see the plaintext password — share it with
                  the employee via a secure channel now. They&apos;ll be forced to change it on first
                  login.
                </p>
                <div className="mt-4 space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm">
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-slate-500">Username</span>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-semibold text-slate-900">
                        {createdCreds.username ?? '—'}
                      </span>
                      {createdCreds.username ? (
                        <button
                          type="button"
                          className="text-xs font-medium text-brand-700 hover:underline"
                          onClick={() => handleCredsCopy(createdCreds.username!, 'Username')}
                        >
                          Copy
                        </button>
                      ) : null}
                    </div>
                  </div>
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-slate-500">Temporary password</span>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-semibold text-slate-900">
                        {createdCreds.password ?? '—'}
                      </span>
                      {createdCreds.password ? (
                        <button
                          type="button"
                          className="text-xs font-medium text-brand-700 hover:underline"
                          onClick={() => handleCredsCopy(createdCreds.password!, 'Password')}
                        >
                          Copy
                        </button>
                      ) : null}
                    </div>
                  </div>
                </div>
                {credsCopyNotice ? (
                  <p className="mt-2 text-xs text-emerald-700">{credsCopyNotice}</p>
                ) : null}
              </>
            ) : (
              <>
                <h3 className="text-lg font-semibold text-slate-900">Employee created</h3>
                <p className="mt-2 text-sm text-amber-700">
                  <strong>{createdCreds.fullName}</strong> ({createdCreds.employeeId}) was created,
                  but a login account was not auto-provisioned.
                </p>
                {createdCreds.skippedReason ? (
                  <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
                    Reason: {createdCreds.skippedReason}
                  </div>
                ) : null}
                <p className="mt-3 text-sm text-slate-600">
                  You can provision a login manually from the employee&apos;s profile under
                  <em> Portal Access</em>.
                </p>
              </>
            )}
            <div className="mt-6 flex justify-end">
              <Button variant="primary" onClick={() => setCreatedCreds(null)}>
                Done
              </Button>
            </div>
          </Card>
        </div>
      ) : null}
    </div>
  );
};
