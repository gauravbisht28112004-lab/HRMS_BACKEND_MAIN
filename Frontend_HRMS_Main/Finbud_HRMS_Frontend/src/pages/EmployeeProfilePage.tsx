import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { AvatarUpload } from '@/components/common/AvatarUpload';
import { AuditTimeline } from '@/features/audit/components/AuditTimeline';
import { auditLogs } from '@/constants/mockData';
import { useAuthStore } from '@/store/authStore';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { createEmployeeFormValuesFromEmployee, mapEmployeeFormToRequest } from '@/services/requestMappers';
import { SalarySnapshotCard } from '@/features/employee/components/SalarySnapshotCard';
import { UserAccessCard } from '@/features/employee/components/UserAccessCard';
import { ShiftAssignmentCard } from '@/features/shifts/components/ShiftAssignmentCard';

export const EmployeeProfilePage = () => {
  const { employeeId = '' } = useParams();
  const { user, markSelfServiceEditUsed, setAvatarUrl } = useAuthStore();
  const queryClient = useQueryClient();
  const resolvedEmployeeId = employeeId || user?.employeeId || '';
  const { data: employee } = useQuery({
    queryKey: ['employee', resolvedEmployeeId],
    queryFn: () => (/^\d+$/.test(resolvedEmployeeId) ? api.employees.getById(resolvedEmployeeId) : api.employees.getByEmployeeId(resolvedEmployeeId)),
    enabled: Boolean(resolvedEmployeeId),
  });
  const { data: employeeDetails } = useQuery({
    queryKey: ['employee-profile-details', employee?.backendId],
    queryFn: () => api.employees.getDetails(employee!.backendId!),
    enabled: Boolean(employee?.backendId),
  });
  const [form, setForm] = useState({
    email: '',
    phone: '',
    address: '',
    emergencyContact: '',
    bankDetails: '',
  });

  const isSelfProfile = Boolean(user?.role === 'Employee' && user.employeeId === resolvedEmployeeId);
  const canUseSelfServiceEdit = Boolean(isSelfProfile && user && !user.hasUsedSelfServiceEdit);
  const canHrEdit = user?.role === 'HR' || user?.role === 'Admin';
  const isAllowedTeamLeaderView = user?.role !== 'Team Leader' || employee?.teamLeader === user.name;

  const updateProfileMutation = useMutation({
    mutationFn: async () => {
      if (!employee?.backendId) {
        throw new Error('Missing backend employee id.');
      }

      const baseForm = createEmployeeFormValuesFromEmployee(employee, employeeDetails);
      const [emergencyContactName = '', emergencyContactPhone = ''] = form.emergencyContact.split(' - ');

      const payload = mapEmployeeFormToRequest({
        ...baseForm,
        email: form.email,
        phone: form.phone,
        address: form.address,
        emergencyContactName: emergencyContactName.trim(),
        emergencyContactPhone: emergencyContactPhone.trim(),
        bankAccountNumber: form.bankDetails,
      });

      await api.employees.update(employee.backendId, payload);
    },
    onSuccess: async () => {
      markSelfServiceEditUsed();
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['employee', resolvedEmployeeId] }),
        queryClient.invalidateQueries({ queryKey: ['employee-profile-details', employee?.backendId] }),
      ]);
    },
  });

  useEffect(() => {
    if (!employee) return;
    setForm({
      email: employee.email,
      phone: employee.phone,
      address: employee.address,
      emergencyContact: employee.emergencyContact,
      bankDetails: employee.bankDetails,
    });
  }, [employee]);

  if (!employee) {
    return <div className="text-sm text-slate-500">Employee not found.</div>;
  }

  if (!isAllowedTeamLeaderView) {
    return <div className="text-sm text-slate-500">Access restricted to your own team members.</div>;
  }

  const handleSelfServiceSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await updateProfileMutation.mutateAsync();
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${employee.firstName} ${employee.lastName}`}
        description={
          isSelfProfile
            ? 'Your employee profile with one-time self-service edit access.'
            : 'Employee profile, compliance details, and recent audit activity.'
        }
      />

      <Card>
        <div className="flex flex-col gap-4">
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-brand-700">Profile Picture</h3>
            <p className="mt-1 text-xs text-slate-500">
              {isSelfProfile
                ? 'This is how you appear across the portal. Upload a clear, front-facing photo.'
                : canHrEdit
                  ? 'Update the employee’s profile picture. Changes are visible immediately across the portal.'
                  : 'Profile picture shown across the portal.'}
            </p>
          </div>
          <AvatarUpload
            employeeCode={employee.id}
            name={`${employee.firstName} ${employee.lastName}`}
            currentUrl={employee.profilePicture}
            editable={isSelfProfile || canHrEdit}
            onChange={(newUrl) => {
              // Refresh the cached employee payload so the photo updates on
              // this page and in the employees list. Also mirror the change
              // into the auth store when the signed-in user is editing their
              // own avatar so the Topbar refreshes instantly.
              void queryClient.invalidateQueries({ queryKey: ['employee', resolvedEmployeeId] });
              void queryClient.invalidateQueries({ queryKey: ['employees'] });
              if (isSelfProfile) {
                setAvatarUrl(newUrl);
              }
            }}
          />
        </div>
      </Card>

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <p className="text-sm text-slate-500">Employee ID</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.id}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Status</p>
              <div className="mt-2">
                <StatusBadge
                  label={employee.status}
                  tone={employee.status === 'Active' ? 'success' : employee.status === 'On Leave' ? 'warning' : 'danger'}
                />
              </div>
            </div>
            <div>
              <p className="text-sm text-slate-500">Department</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.department}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Designation</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.designation}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Team Leader</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.teamLeader}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Date of Joining</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.dateOfJoining}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Bank Details</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.bankDetails}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">PAN / Aadhaar</p>
              <p className="mt-1 font-semibold text-slate-900">{employee.panAadhaar}</p>
            </div>
          </div>
        </Card>
        <AuditTimeline logs={auditLogs.filter((log) => log.entity.includes(employee.id))} />
      </div>

      {isSelfProfile ? (
        <Card>
          <div className="flex items-center justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">Employee Self-Service Edit</h3>
              <p className="mt-1 text-sm text-slate-500">
                Employees can update their own details only once. After that, only HR can make changes.
              </p>
            </div>
            <StatusBadge label={user?.hasUsedSelfServiceEdit ? 'Edit Used' : 'One Edit Available'} tone={user?.hasUsedSelfServiceEdit ? 'danger' : 'success'} />
          </div>

          <form className="mt-6 grid gap-4 md:grid-cols-2" onSubmit={handleSelfServiceSubmit}>
            <Input label="Email" value={form.email} disabled={!canUseSelfServiceEdit} onChange={(event) => setForm({ ...form, email: event.target.value })} />
            <Input label="Phone" value={form.phone} disabled={!canUseSelfServiceEdit} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
            <Input
              label="Emergency Contact"
              value={form.emergencyContact}
              disabled={!canUseSelfServiceEdit}
              onChange={(event) => setForm({ ...form, emergencyContact: event.target.value })}
            />
            <Input
              label="Bank Details"
              value={form.bankDetails}
              disabled={!canUseSelfServiceEdit}
              onChange={(event) => setForm({ ...form, bankDetails: event.target.value })}
            />
            <div className="md:col-span-2">
              <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
                <span>Address</span>
                <textarea
                  rows={4}
                  value={form.address}
                  disabled={!canUseSelfServiceEdit}
                  onChange={(event) => setForm({ ...form, address: event.target.value })}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100 disabled:bg-slate-50"
                />
              </label>
            </div>
            <div className="md:col-span-2 flex items-center justify-between gap-4">
              <p className="text-sm text-slate-500">
                {canUseSelfServiceEdit
                  ? 'Submitting this will lock further self-edits for the employee account.'
                  : 'Your one-time self-edit has already been used. Please contact HR for further changes.'}
              </p>
              <Button type="submit" disabled={!canUseSelfServiceEdit || updateProfileMutation.isPending}>
                {updateProfileMutation.isPending ? 'Saving...' : 'Save My Details'}
              </Button>
            </div>
          </form>
        </Card>
      ) : null}

      {canHrEdit ? <SalarySnapshotCard employeeBackendId={employee.backendId} /> : null}

      {canHrEdit ? <ShiftAssignmentCard employeeBackendId={employee.backendId} /> : null}

      {canHrEdit ? <UserAccessCard employeeCode={employee.id} /> : null}

      {canHrEdit ? (
        <Card>
          <h3 className="text-lg font-semibold text-slate-900">HR Edit Authority</h3>
          <p className="mt-2 text-sm text-slate-500">
            HR and Admin can continue editing employee master data even after the employee has used their one-time self-service update.
          </p>
        </Card>
      ) : null}
    </div>
  );
};
