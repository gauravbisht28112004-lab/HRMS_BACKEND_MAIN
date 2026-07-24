import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { LeaveCalendar } from '@/features/leave/components/LeaveCalendar';
import { api } from '@/services/api';
import { mapLeaveFormToRequest } from '@/services/requestMappers';
import { useAuthStore } from '@/store/authStore';
import { cn } from '@/utils/cn';
import type { LeaveRequest } from '@/types';

// Must stay in sync with the backend enum
// com.financebuddha.finbud.hrms.enums.LeaveType (CASUAL, SICK, PAID, LOP).
// `value` is exactly what we POST to /leaves/apply — any string outside this
// set fails Jackson enum binding and the backend rejects it with HTTP 400,
// which is what used to make the form "never submit".
const LEAVE_TYPE_OPTIONS = [
  { value: 'CASUAL', label: 'Casual', hint: 'Casual leave' },
  { value: 'PAID', label: 'Paid', hint: 'Earned / privilege leave' },
  { value: 'SICK', label: 'Sick', hint: 'Sick leave' },
  { value: 'LOP', label: 'LOP', hint: 'Loss of pay (unpaid)' },
] as const;

const emptyForm = { leaveType: '', from: '', to: '', reason: '', contactDuringLeave: '' };

// Client-side guard so we never send a request the backend is guaranteed to
// reject (empty enum, empty dates, blank reason, reversed date range).
const validateLeaveForm = (form: typeof emptyForm): string | null => {
  if (!form.leaveType) return 'Please select a leave type.';
  if (!form.from) return 'Please choose a start date.';
  if (!form.to) return 'Please choose an end date.';
  if (form.to < form.from) return 'The "To" date cannot be earlier than the "From" date.';
  if (!form.reason.trim()) return 'Please enter a reason for your leave.';
  return null;
};

export const LeavePage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const [form, setForm] = useState(emptyForm);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const canApproveLeave = user?.role === 'Admin' || user?.role === 'HR' || user?.role === 'Manager';

  const { data = [] } = useQuery<LeaveRequest[]>({
    queryKey: ['leave', user?.role, user?.employeeDbId],
    queryFn: async () => {
      if (!user) {
        return [];
      }

      if (user.role === 'Employee' && user.employeeDbId) {
        return api.leave.listForEmployee(user.employeeDbId);
      }

      if (user.role === 'Manager' && user.employeeDbId) {
        return api.leave.listForManager(user.employeeDbId);
      }

      const employees = await api.employees.list();
      const employeeIds = employees.flatMap((employee) => (employee.backendId ? [employee.backendId] : []));
      return api.leave.listByEmployeeIds(employeeIds);
    },
    enabled: Boolean(user),
  });

  const applyLeaveMutation = useMutation({
    mutationFn: () => api.leave.apply(mapLeaveFormToRequest(form)),
    onSuccess: async () => {
      setForm(emptyForm);
      setNotice({ type: 'success', text: 'Leave request submitted successfully.' });
      await queryClient.invalidateQueries({ queryKey: ['leave'] });
    },
    onError: (error) => {
      // Previously there was no onError, so a rejected request (HTTP 400/500)
      // vanished silently and the form looked like it did nothing. Surface the
      // backend's own message instead.
      setNotice({
        type: 'error',
        text: (error as Error)?.message ?? 'Could not submit your leave request. Please try again.',
      });
    },
  });

  const approveLeaveMutation = useMutation({
    mutationFn: (leaveRequestId: number) => api.leave.approve(leaveRequestId, { approved: true }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['leave'] });
    },
  });

  const rejectLeaveMutation = useMutation({
    mutationFn: ({ leaveRequestId, reason }: { leaveRequestId: number; reason: string }) =>
      api.leave.reject(leaveRequestId, reason),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['leave'] });
    },
  });

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const validationError = validateLeaveForm(form);
    if (validationError) {
      setNotice({ type: 'error', text: validationError });
      return;
    }
    setNotice(null);
    applyLeaveMutation.mutate();
  };

  // Native single-select semantics via a radio group: exactly one leave
  // type can ever be selected, and picking an option replaces the previous
  // choice. Unlike the old checkbox group, a radio cannot be toggled off by
  // re-clicking — which is fine because a leave type is required anyway.
  const selectLeaveType = (value: string) => {
    setNotice(null);
    setForm((prev) => ({ ...prev, leaveType: value }));
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Leave Management" description="Apply for leave, review balances, and manage approval queues." />

      <div className="grid gap-6 xl:grid-cols-[0.85fr_1.15fr]">
        <Card>
          <h3 className="text-lg font-semibold text-slate-900">Apply Leave</h3>
          <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              <span>Leave Type</span>
              <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Leave type">
                {LEAVE_TYPE_OPTIONS.map((option) => {
                  const checked = form.leaveType === option.value;
                  return (
                    <label
                      key={option.value}
                      className={cn(
                        'flex cursor-pointer items-start gap-3 rounded-xl border px-4 py-3 transition',
                        checked
                          ? 'border-brand-400 bg-brand-50 ring-2 ring-brand-100'
                          : 'border-slate-200 bg-white hover:border-brand-300',
                      )}
                    >
                      <input
                        type="radio"
                        name="leaveType"
                        value={option.value}
                        className="mt-0.5 h-4 w-4 rounded-full border-slate-300 text-brand-600 focus:ring-brand-300"
                        checked={checked}
                        onChange={() => selectLeaveType(option.value)}
                      />
                      <span className="flex flex-col">
                        <span className="font-semibold text-slate-800">{option.label}</span>
                        <span className="text-xs font-normal text-slate-500">{option.hint}</span>
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>
            <Input label="From" type="date" value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })} />
            <Input label="To" type="date" value={form.to} onChange={(e) => setForm({ ...form, to: e.target.value })} />
            <Input
              label="Contact During Leave"
              value={form.contactDuringLeave}
              onChange={(e) => setForm({ ...form, contactDuringLeave: e.target.value })}
            />
            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              <span>Reason</span>
              <textarea
                rows={4}
                className="rounded-xl border border-slate-200 px-4 py-3 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
                value={form.reason}
                onChange={(e) => setForm({ ...form, reason: e.target.value })}
              />
            </label>
            <div className="rounded-2xl bg-brand-50 p-4 text-sm text-brand-800">
              Casual &amp; Sick share one balance • Paid is a separate balance • LOP is unpaid (loss of pay).
            </div>
            {notice ? (
              <div
                role="alert"
                className={cn(
                  'rounded-xl border px-4 py-3 text-sm',
                  notice.type === 'success'
                    ? 'border-brand-200 bg-brand-50 text-brand-800'
                    : 'border-rose-200 bg-rose-50 text-rose-700',
                )}
              >
                {notice.text}
              </div>
            ) : null}
            <Button type="submit" disabled={applyLeaveMutation.isPending}>
              {applyLeaveMutation.isPending ? 'Submitting...' : 'Submit Request'}
            </Button>
          </form>
        </Card>

        <LeaveCalendar requests={data} />
      </div>

      <DataTable
        data={data}
        columns={[
          { key: 'employee', header: 'Employee', render: (request) => request.employeeName },
          { key: 'type', header: 'Type', render: (request) => request.leaveType },
          { key: 'period', header: 'Period', render: (request) => `${request.from} to ${request.to}` },
          { key: 'days', header: 'Days', render: (request) => request.days },
          {
            key: 'status',
            header: 'Status',
            render: (request) => (
              <StatusBadge
                label={request.status}
                tone={request.status === 'Approved' ? 'success' : request.status === 'Rejected' ? 'danger' : 'warning'}
              />
            ),
          },
          {
            key: 'approval',
            header: 'Approval',
            render: (request) => (
              canApproveLeave ? (
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    onClick={() => request.backendId && void approveLeaveMutation.mutateAsync(request.backendId)}
                  >
                    Approve
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() => {
                      if (!request.backendId) return;
                      const reason = window.prompt('Enter rejection reason', 'Rejected from leave dashboard');
                      if (reason) {
                        void rejectLeaveMutation.mutateAsync({ leaveRequestId: request.backendId, reason });
                      }
                    }}
                  >
                    Reject
                  </Button>
                </div>
              ) : (
                <span className="text-sm text-slate-500">Approval restricted to Admin, HR, and Team Leaders</span>
              )
            ),
          },
        ]}
      />
    </div>
  );
};
