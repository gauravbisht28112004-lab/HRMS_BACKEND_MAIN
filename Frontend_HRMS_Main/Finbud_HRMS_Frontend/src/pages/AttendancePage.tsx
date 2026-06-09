import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { DataTable } from '@/components/common/DataTable';
import { FilterBar } from '@/components/common/FilterBar';
import { PageHeader } from '@/components/common/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { AttendanceSummary } from '@/features/attendance/components/AttendanceSummary';
import { MarkAttendanceCard } from '@/features/attendance/components/MarkAttendanceCard';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import type { AttendanceRecord, Employee } from '@/types';

/**
 * Tone mapping for the approval status badge. Undefined (= legacy row
 * that predates the portal flow) renders as neutral so nothing looks
 * broken for historical data.
 */
const approvalTone = (status: AttendanceRecord['approvalStatus']) => {
  switch (status) {
    case 'APPROVED':
      return 'success' as const;
    case 'REJECTED':
      return 'danger' as const;
    case 'PENDING':
      return 'warning' as const;
    default:
      return 'neutral' as const;
  }
};

export const AttendancePage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [employee, setEmployee] = useState('All');
  const [rejectDialog, setRejectDialog] = useState<{ id: number; reason: string } | null>(null);

  const canReviewAttendance = user?.role === 'Admin' || user?.role === 'HR' || user?.role === 'Team Leader';

  const { data: accessibleEmployees = [] } = useQuery<Employee[]>({
    queryKey: ['attendance-employees', user?.role, user?.employeeDbId, user?.employeeId],
    queryFn: async () => {
      if (!user) return [];
      if (user.role === 'Employee' && user.employeeId) {
        const employeeProfile = await api.employees.getByEmployeeId(user.employeeId);
        return employeeProfile ? [employeeProfile] : [];
      }
      if (user.role === 'Team Leader' && user.employeeDbId) {
        return api.employees.listByManager(user.employeeDbId);
      }
      return api.employees.list();
    },
    enabled: Boolean(user),
  });

  const employeeIds = accessibleEmployees.flatMap((item) => (item.backendId ? [item.backendId] : []));

  const { data = [] } = useQuery<AttendanceRecord[]>({
    queryKey: ['attendance', employeeIds.join(',')],
    queryFn: () => api.attendance.listByEmployeeIds(employeeIds),
    enabled: employeeIds.length > 0,
  });

  const { data: pendingApprovals = [], isFetching: loadingApprovals } = useQuery<AttendanceRecord[]>({
    queryKey: ['attendance-pending-approvals', user?.role, user?.employeeDbId],
    queryFn: () => api.attendance.pendingApprovals(),
    enabled: canReviewAttendance,
    refetchInterval: 60_000,
  });

  const reviewMutation = useMutation({
    mutationFn: ({ id, approve, rejectionReason }: { id: number; approve: boolean; rejectionReason?: string }) =>
      api.attendance.review(id, { approve, rejectionReason }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['attendance-pending-approvals'] });
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
      await queryClient.invalidateQueries({ queryKey: ['attendance-today'] });
    },
  });

  const accessibleNames = useMemo(
    () => accessibleEmployees.map((item) => `${item.firstName} ${item.lastName}`),
    [accessibleEmployees],
  );

  const employeeOptions = useMemo(
    () => [{ label: 'All Employees', value: 'All' }, ...accessibleNames.map((name) => ({ label: name, value: name }))],
    [accessibleNames],
  );

  const filtered = useMemo(
    () =>
      data.filter(
        (record) =>
          record.date === date &&
          accessibleNames.includes(record.employeeName) &&
          (employee === 'All' || record.employeeName === employee),
      ),
    [accessibleNames, data, date, employee],
  );

  const isEmployee = user?.role === 'Employee';

  if (isEmployee) {
    const employeeAttendance = data
      .slice()
      .sort((a, b) => (a.date < b.date ? 1 : -1));

    return (
      <div className="space-y-6">
        <PageHeader
          title="My Attendance"
          description="Mark in and out, check your approval status, and review past days."
          action={
            <Link
              to="/regularizations"
              className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              File regularization
            </Link>
          }
        />

        <MarkAttendanceCard />

        <DataTable
          data={employeeAttendance}
          emptyTitle="No attendance yet"
          emptyDescription="Once you start marking in, your history will appear here."
          columns={[
            { key: 'date', header: 'Date', render: (record) => record.date },
            { key: 'punchIn', header: 'Punch In', render: (record) => record.punchIn },
            { key: 'punchOut', header: 'Punch Out', render: (record) => record.punchOut },
            { key: 'hours', header: 'Working Hours', render: (record) => record.workingHours },
            {
              key: 'approval',
              header: 'Approval',
              render: (record) =>
                record.approvalStatus ? (
                  <StatusBadge label={record.approvalStatus} tone={approvalTone(record.approvalStatus)} />
                ) : (
                  <StatusBadge label={record.status} tone="neutral" />
                ),
            },
            {
              key: 'flags',
              header: 'Flags',
              render: (record) => (
                <div className="flex flex-wrap gap-2">
                  {record.late ? <StatusBadge label="Late" tone="warning" /> : null}
                  {record.earlyExit ? <StatusBadge label="Early Exit" tone="danger" /> : null}
                  {record.missingPunch ? <StatusBadge label="Missing Punch" tone="danger" /> : null}
                  {record.isAutoAbsent ? <StatusBadge label="Auto-Absent" tone="danger" /> : null}
                  {record.overtimeHours > 0 ? <StatusBadge label={`OT ${record.overtimeHours}h`} tone="info" /> : null}
                </div>
              ),
            },
          ]}
        />
      </div>
    );
  }

  // TL / HR / Admin view
  return (
    <div className="space-y-6">
      <PageHeader
        title="Attendance Operations"
        description="Approve portal punches, monitor flags, and keep the attendance board clean."
        action={
          <div className="flex gap-2">
            <Link
              to="/regularizations"
              className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Regularizations
            </Link>
            {(user?.role === 'Admin' || user?.role === 'HR') && (
              <>
                <Link
                  to="/office-locations"
                  className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Office locations
                </Link>
                <Link
                  to="/holidays"
                  className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Public holidays
                </Link>
              </>
            )}
          </div>
        }
      />

      <MarkAttendanceCard />

      {canReviewAttendance && (
        <Card className="border border-slate-200 bg-white">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">Approval queue</p>
              <h2 className="text-lg font-semibold text-slate-900">Pending portal punches</h2>
              <p className="text-sm text-slate-500">
                Review every punch your reports submit. Rejecting requires a reason so the employee can file a regularization.
              </p>
            </div>
            <StatusBadge label={`${pendingApprovals.length} pending`} tone={pendingApprovals.length ? 'warning' : 'neutral'} />
          </div>

          <div className="mt-4">
            <DataTable
              data={pendingApprovals}
              emptyTitle={loadingApprovals ? 'Loading…' : 'Nothing to approve'}
              emptyDescription={loadingApprovals ? 'Fetching the latest queue.' : 'You are fully up to date.'}
              columns={[
                { key: 'employee', header: 'Employee', render: (record) => record.employeeName },
                { key: 'date', header: 'Date', render: (record) => record.date },
                { key: 'in', header: 'Punch In', render: (record) => record.punchIn },
                { key: 'out', header: 'Punch Out', render: (record) => record.punchOut },
                {
                  key: 'flags',
                  header: 'Flags',
                  render: (record) => (
                    <div className="flex flex-wrap gap-2">
                      {record.late ? <StatusBadge label="Late" tone="warning" /> : null}
                      {record.missingPunch ? <StatusBadge label="Missing Punch" tone="danger" /> : null}
                      {record.isAutoAbsent ? <StatusBadge label="Auto-Absent" tone="danger" /> : null}
                      {record.overtimeHours > 0 ? <StatusBadge label={`OT ${record.overtimeHours}h`} tone="info" /> : null}
                    </div>
                  ),
                },
                {
                  key: 'actions',
                  header: 'Actions',
                  render: (record) => (
                    <div className="flex items-center gap-2">
                      <Button
                        onClick={() =>
                          record.backendId &&
                          reviewMutation.mutate({ id: record.backendId, approve: true })
                        }
                        disabled={reviewMutation.isPending}
                      >
                        Approve
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={() => record.backendId && setRejectDialog({ id: record.backendId, reason: '' })}
                      >
                        Reject
                      </Button>
                    </div>
                  ),
                },
              ]}
            />
          </div>
        </Card>
      )}

      <AttendanceSummary records={filtered} />

      <FilterBar>
        <Input label="Attendance Date" type="date" value={date} onChange={(event) => setDate(event.target.value)} />
        <Select
          label="Employee"
          value={employee}
          onChange={(event) => setEmployee(event.target.value)}
          options={employeeOptions}
        />
        <div className="flex items-end text-sm text-slate-500">Click Reject to capture a reason before closing the row.</div>
      </FilterBar>

      <DataTable
        data={filtered}
        columns={[
          { key: 'employee', header: 'Employee', render: (record) => record.employeeName },
          { key: 'department', header: 'Department', render: (record) => record.department },
          { key: 'punchIn', header: 'Punch In', render: (record) => record.punchIn },
          { key: 'punchOut', header: 'Punch Out', render: (record) => record.punchOut },
          { key: 'hours', header: 'Working Hours', render: (record) => record.workingHours },
          {
            key: 'indicators',
            header: 'Indicators',
            render: (record) => (
              <div className="flex flex-wrap gap-2">
                {record.late ? <StatusBadge label="Late" tone="warning" /> : null}
                {record.earlyExit ? <StatusBadge label="Early Exit" tone="danger" /> : null}
                {record.missingPunch ? <StatusBadge label="Missing Punch" tone="danger" /> : null}
                {record.overtimeHours > 0 ? <StatusBadge label={`OT ${record.overtimeHours}h`} tone="info" /> : null}
              </div>
            ),
          },
          {
            key: 'approval',
            header: 'Approval',
            render: (record) =>
              record.approvalStatus ? (
                <StatusBadge label={record.approvalStatus} tone={approvalTone(record.approvalStatus)} />
              ) : (
                <StatusBadge label="—" tone="neutral" />
              ),
          },
          {
            key: 'status',
            header: 'Status',
            render: (record) => (
              <StatusBadge
                label={record.status}
                tone={record.status === 'Present' ? 'success' : record.status === 'Absent' ? 'danger' : 'info'}
              />
            ),
          },
        ]}
      />

      {rejectDialog ? (
        <RejectionDialog
          reason={rejectDialog.reason}
          onChange={(reason) => setRejectDialog({ id: rejectDialog.id, reason })}
          onCancel={() => setRejectDialog(null)}
          onConfirm={async () => {
            if (!rejectDialog.reason.trim()) return;
            await reviewMutation.mutateAsync({
              id: rejectDialog.id,
              approve: false,
              rejectionReason: rejectDialog.reason.trim(),
            });
            setRejectDialog(null);
          }}
          submitting={reviewMutation.isPending}
        />
      ) : null}
    </div>
  );
};

interface RejectionDialogProps {
  reason: string;
  onChange: (reason: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
  submitting: boolean;
}

const RejectionDialog = ({ reason, onChange, onCancel, onConfirm, submitting }: RejectionDialogProps) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
    <Card className="w-full max-w-md">
      <h3 className="text-lg font-semibold text-slate-900">Reject portal punch</h3>
      <p className="mt-2 text-sm text-slate-500">
        Tell the employee why you are rejecting the row. They will see this reason when they file a regularization.
      </p>
      <textarea
        className="mt-4 h-28 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
        placeholder="Outside the geofence, needs proof of travel…"
        value={reason}
        onChange={(event) => onChange(event.target.value)}
      />
      <div className="mt-4 flex justify-end gap-3">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button variant="danger" onClick={onConfirm} disabled={submitting || !reason.trim()}>
          {submitting ? 'Rejecting…' : 'Reject'}
        </Button>
      </div>
    </Card>
  </div>
);
