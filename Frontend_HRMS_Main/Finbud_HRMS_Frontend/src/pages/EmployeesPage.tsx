import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil, Trash2 } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Avatar } from '@/components/common/Avatar';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { DataTable } from '@/components/common/DataTable';
import { FilterBar } from '@/components/common/FilterBar';
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Card } from '@/components/ui/Card';
import { EmployeeForm } from '@/features/employee/components/EmployeeForm';
import { api, type EmployeeCreateResult } from '@/services/api';
import { ApiError } from '@/services/contracts';
import {
  createEmployeeFormValuesFromEmployee,
  createEmptyEmployeeFormValues,
  mapEmployeeFormToRequest,
  type EmployeeFormValues,
} from '@/services/requestMappers';
import { useAuthStore } from '@/store/authStore';
import type { Employee } from '@/types';

type FormMode = 'closed' | 'create' | 'edit';

const getMutationErrorMessage = (error: unknown) => {
  if (error instanceof ApiError) {
    return error.errors?.length ? `${error.message} ${error.errors.join(' ')}` : error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Unable to save the employee right now. Please try again.';
};

export const EmployeesPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const formCardRef = useRef<HTMLDivElement | null>(null);

  const [search, setSearch] = useState('');
  const [department, setDepartment] = useState('All');
  const [shift, setShift] = useState('All');
  const [formMode, setFormMode] = useState<FormMode>('closed');
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Employee | null>(null);
  /**
   * One-time credentials modal shown after a successful employee create.
   *
   * Backend auto-provisions a User row inside the same transaction as the
   * Employee insert (see EmployeeServiceImpl#autoProvisionUser). The plaintext
   * temporary password is in the response payload and is the *only* time we
   * ever see it — so we surface it in a dismissable modal with copy buttons
   * and let HR forward it to the new joiner via a secure channel.
   *
   * If provisioning was skipped (collision, etc.) we still show the modal so
   * HR understands why the new hire can't log in yet — the `skippedReason`
   * branch turns the modal into an informational notice instead of a
   * credential handoff.
   */
  const [createdCreds, setCreatedCreds] = useState<{
    employeeId: string;
    fullName: string;
    userProvisioned: boolean;
    username?: string | null;
    password?: string | null;
    skippedReason?: string | null;
  } | null>(null);
  const [credsCopyNotice, setCredsCopyNotice] = useState<string | null>(null);

  const canManage = user?.role === 'Admin' || user?.role === 'HR';

  const { data = [], isLoading } = useQuery<Employee[]>({
    queryKey: ['employees', user?.role, user?.employeeDbId],
    queryFn: () =>
      user?.role === 'Team Leader' && user.employeeDbId
        ? api.employees.listByManager(user.employeeDbId)
        : api.employees.list(),
    enabled: Boolean(user),
  });

  const { data: departments = [] } = useQuery({
    queryKey: ['departments-options'],
    queryFn: api.departments.list,
    enabled: user?.role !== 'Team Leader',
  });

  const { data: shifts = [] } = useQuery({
    queryKey: ['shifts-options'],
    queryFn: api.shifts.list,
    enabled: user?.role !== 'Team Leader',
  });

  const { data: employeeDetails, isFetching: isDetailsFetching } = useQuery({
    queryKey: ['employee-details', editingEmployee?.backendId],
    queryFn: () => api.employees.getDetails(editingEmployee!.backendId!),
    enabled: formMode === 'edit' && Boolean(editingEmployee?.backendId),
  });

  const saveEmployeeMutation = useMutation({
    mutationFn: async (
      values: EmployeeFormValues,
    ): Promise<EmployeeCreateResult | { kind: 'updated' }> => {
      const payload = mapEmployeeFormToRequest(values);
      if (formMode === 'edit' && editingEmployee?.backendId) {
        await api.employees.update(editingEmployee.backendId, payload);
        // We deliberately discriminate the result so onSuccess can tell
        // create-vs-update apart without re-reading formMode (which has
        // already been reset by the time the mutation resolves).
        return { kind: 'updated' };
      }
      return api.employees.create(payload);
    },
    onSuccess: async (result) => {
      setFormMode('closed');
      setEditingEmployee(null);
      await queryClient.invalidateQueries({ queryKey: ['employees'] });

      // Only the create path returns an EmployeeCreateResult (it has an
      // `employee` property). The update path returns our sentinel.
      if ('employee' in result) {
        const created = result.employee;
        setCreatedCreds({
          employeeId: created.id,
          fullName: `${created.firstName} ${created.lastName}`.trim() || created.id,
          userProvisioned: result.userProvisioned,
          username: result.generatedUsername,
          password: result.generatedTemporaryPassword,
          skippedReason: result.provisioningSkippedReason,
        });
      }
    },
  });

  const handleCredsCopy = (value: string, label: string) => {
    void navigator.clipboard.writeText(value).then(() => {
      setCredsCopyNotice(`${label} copied`);
      window.setTimeout(() => setCredsCopyNotice(null), 2000);
    });
  };

  const deleteEmployeeMutation = useMutation({
    mutationFn: async (employee: Employee) => {
      if (!employee.backendId) {
        throw new Error('Missing backend employee id.');
      }
      await api.employees.remove(employee.backendId);
    },
    onSuccess: async () => {
      setDeleteTarget(null);
      await queryClient.invalidateQueries({ queryKey: ['employees'] });
    },
  });

  useEffect(() => {
    if (formMode !== 'closed') {
      // Smoothly bring the editor into view so the user knows it's active.
      formCardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [formMode, editingEmployee?.backendId]);

  useEffect(() => {
    // Clear any stale mutation error when the user switches records
    // or closes the form. Otherwise the red banner "sticks" visually.
    saveEmployeeMutation.reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formMode, editingEmployee?.backendId]);

  const filtered = useMemo(
    () =>
      data.filter((employee) => {
        const withinScope = user?.role !== 'Team Leader' || employee.managerId === user.employeeDbId;
        const matchesSearch = `${employee.firstName} ${employee.lastName} ${employee.id}`
          .toLowerCase()
          .includes(search.toLowerCase());
        const matchesDepartment = department === 'All' || employee.department === department;
        const matchesShift = shift === 'All' || employee.shiftAssignment === shift;
        return withinScope && matchesSearch && matchesDepartment && matchesShift;
      }),
    [data, department, search, shift, user],
  );

  const departmentOptions = useMemo(
    () => [
      { label: 'Select department', value: '' },
      ...departments.map((item) => ({ label: item.name, value: String(item.backendId ?? '') })),
    ],
    [departments],
  );

  const shiftOptions = useMemo(
    () => [
      { label: 'Select shift', value: '' },
      ...shifts.map((item) => ({ label: item.name, value: String(item.backendId ?? '') })),
    ],
    [shifts],
  );

  const managerOptions = useMemo(
    () => [
      { label: 'Select manager', value: '' },
      ...data.map((item) => ({
        label: `${item.firstName} ${item.lastName}`,
        value: String(item.backendId ?? ''),
      })),
    ],
    [data],
  );

  const initialFormValues = useMemo(() => {
    if (formMode === 'edit' && editingEmployee) {
      return createEmployeeFormValuesFromEmployee(editingEmployee, employeeDetails);
    }
    if (formMode === 'create') {
      return createEmptyEmployeeFormValues();
    }
    return undefined;
  }, [formMode, editingEmployee, employeeDetails]);

  const openCreate = () => {
    setEditingEmployee(null);
    setFormMode('create');
  };

  const openEdit = (employee: Employee) => {
    setEditingEmployee(employee);
    setFormMode('edit');
  };

  const closeForm = () => {
    setFormMode('closed');
    setEditingEmployee(null);
  };

  const editingLabel = editingEmployee
    ? `${editingEmployee.firstName} ${editingEmployee.lastName} — ${editingEmployee.id}`
    : undefined;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Employee Management"
        description={
          user?.role === 'Team Leader'
            ? 'View your team members. Edit access is restricted to HR and Admin.'
            : 'Create, edit, search, filter, and audit employee records with a live backend workflow.'
        }
        action={
          canManage && formMode === 'closed' ? (
            <Button onClick={openCreate}>+ Add Employee</Button>
          ) : canManage ? (
            <Button variant="secondary" onClick={closeForm}>
              Close form
            </Button>
          ) : undefined
        }
      />

      <div ref={formCardRef}>
        {canManage && formMode !== 'closed' ? (
          <>
            {saveEmployeeMutation.isError ? (
              <div className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {getMutationErrorMessage(saveEmployeeMutation.error)}
              </div>
            ) : null}

            {formMode === 'edit' && isDetailsFetching ? (
              <div className="mb-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-800">
                Loading secure banking / statutory details for {editingLabel}…
              </div>
            ) : null}

            <EmployeeForm
              initialValues={initialFormValues}
              onSubmit={(values) => saveEmployeeMutation.mutateAsync(values)}
              onCancel={closeForm}
              isSubmitting={saveEmployeeMutation.isPending}
              isEditMode={formMode === 'edit'}
              editingLabel={editingLabel}
              employeeCode={formMode === 'edit' ? editingEmployee?.id : undefined}
              onAvatarChange={() => {
                // Refresh the employee list so the table row shows the new picture.
                void queryClient.invalidateQueries({ queryKey: ['employees'] });
                if (editingEmployee?.backendId) {
                  void queryClient.invalidateQueries({
                    queryKey: ['employee-details', editingEmployee.backendId],
                  });
                }
              }}
              departmentOptions={departmentOptions}
              managerOptions={managerOptions}
              shiftOptions={shiftOptions}
              backendEmployeeId={editingEmployee?.backendId ?? null}
              canEditSalary={canManage}
            />
          </>
        ) : null}
      </div>

      <FilterBar>
        <Input
          placeholder="Search by name or employee ID"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <Select
          value={department}
          onChange={(event) => setDepartment(event.target.value)}
          options={[
            { label: 'All Departments', value: 'All' },
            ...departments.map((item) => ({ label: item.name, value: item.name })),
          ]}
        />
        <Select
          value={shift}
          onChange={(event) => setShift(event.target.value)}
          options={[
            { label: 'All Shifts', value: 'All' },
            ...shifts.map((item) => ({ label: item.name, value: item.name })),
          ]}
        />
        <div className="flex items-center justify-end text-sm text-slate-500">
          Showing {filtered.length} employee{filtered.length === 1 ? '' : 's'}
        </div>
      </FilterBar>

      {isLoading ? (
        <LoadingSkeleton />
      ) : (
        <DataTable
          data={filtered}
          columns={[
            {
              key: 'name',
              header: 'Employee',
              render: (employee) => (
                <button
                  className="flex items-center gap-3 text-left font-medium text-brand-700"
                  onClick={() => navigate(`/employees/${employee.id}`)}
                >
                  <Avatar
                    name={`${employee.firstName} ${employee.lastName}`}
                    src={employee.profilePicture}
                    size="sm"
                  />
                  <span>
                    {employee.firstName} {employee.lastName}
                  </span>
                </button>
              ),
            },
            { key: 'employeeId', header: 'Employee ID', render: (employee) => employee.id },
            { key: 'department', header: 'Department', render: (employee) => employee.department },
            { key: 'shift', header: 'Shift', render: (employee) => employee.shiftAssignment },
            { key: 'teamLeader', header: 'Team Leader', render: (employee) => employee.teamLeader },
            {
              key: 'status',
              header: 'Status',
              render: (employee) => (
                <StatusBadge
                  label={employee.status}
                  tone={employee.status === 'Active' ? 'success' : employee.status === 'On Leave' ? 'warning' : 'danger'}
                />
              ),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (employee) => (
                <div className="flex gap-2">
                  {canManage ? (
                    <Button variant="secondary" onClick={() => openEdit(employee)}>
                      <Pencil size={14} className="mr-1" /> Edit
                    </Button>
                  ) : null}
                  <Button variant="ghost" onClick={() => navigate(`/employees/${employee.id}`)}>
                    View
                  </Button>
                  {canManage ? (
                    <button
                      className="rounded-lg p-2 text-rose-600 hover:bg-rose-50"
                      onClick={() => setDeleteTarget(employee)}
                      aria-label={`Delete ${employee.firstName}`}
                    >
                      <Trash2 size={16} />
                    </button>
                  ) : null}
                </div>
              ),
            },
          ]}
        />
      )}

      <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">
        <span>{filtered.length} employee record(s) loaded from backend.</span>
        <div className="flex gap-2">
          <Button variant="secondary" disabled>
            Live Sync
          </Button>
        </div>
      </div>

      <ConfirmModal
        open={Boolean(deleteTarget)}
        title="Delete employee record?"
        description={`This confirms deletion for ${deleteTarget?.firstName ?? 'the selected employee'}.`}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            void deleteEmployeeMutation.mutateAsync(deleteTarget);
          }
        }}
        confirmLabel="Delete"
      />

      {/*
       * Post-create credentials modal. Two variants:
       *  1. userProvisioned === true → render username + temporary password
       *     with copy buttons. This is the *only* time we see the plaintext.
       *  2. userProvisioned === false → render an informational notice with
       *     the skipped reason so HR knows why the employee can't log in yet
       *     and which manual fallback (Admin > Users) to use.
       */}
      {createdCreds ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-md">
            {createdCreds.userProvisioned ? (
              <>
                <h3 className="text-lg font-semibold text-slate-900">Employee created</h3>
                <p className="mt-2 text-sm text-slate-500">
                  A login account has been provisioned for{' '}
                  <strong>{createdCreds.fullName}</strong> ({createdCreds.employeeId}). This is the{' '}
                  <strong>only</strong> time you&apos;ll see the plaintext password — share it with the
                  employee via a secure channel now. They&apos;ll be forced to change it on their first login.
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
                  <strong>{createdCreds.fullName}</strong> ({createdCreds.employeeId}) was created, but a
                  login account was not auto-provisioned.
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
