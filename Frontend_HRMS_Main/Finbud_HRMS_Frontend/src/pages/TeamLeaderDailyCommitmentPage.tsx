import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import type { CommitmentStatus, DailyCommitment } from '@/types';

/**
 * Team Leader's daily commitment console (Q1 Phase A team-side).
 *
 * <p>Shows two sections:
 *   1. <b>Pending Approvals</b> — every direct report's SUBMITTED row,
 *      with Approve / Reject buttons (rejection requires a reason, the
 *      backend enforces this).
 *   2. <b>Team Snapshot for Date</b> — every direct report's row for the
 *      chosen date (any status), so the TL can see the day at a glance.
 *
 * <p>Also exposes a Download Excel button that hits
 * {@code /api/reports/commitment/team/{managerId}.xlsx} for a date range.
 */
const TODAY = new Date().toISOString().slice(0, 10);

const statusTone: Record<CommitmentStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

export const TeamLeaderDailyCommitmentPage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const managerId = user?.employeeDbId;

  const [date, setDate] = useState(TODAY);
  const [reportStart, setReportStart] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 7);
    return d.toISOString().slice(0, 10);
  });
  const [reportEnd, setReportEnd] = useState(TODAY);
  const [rejectDialog, setRejectDialog] = useState<{ id: number; reason: string } | null>(null);

  // ---- Queries ----

  const { data: pending = [], isFetching: pendingLoading } = useQuery<DailyCommitment[]>({
    queryKey: ['tl-pending-commitments', managerId],
    queryFn: () => (managerId ? api.commitments.daily.listPendingForManager(managerId) : Promise.resolve([])),
    enabled: Boolean(managerId),
    refetchInterval: 60_000,
  });

  const { data: teamForDate = [] } = useQuery<DailyCommitment[]>({
    queryKey: ['tl-team-for-date', managerId, date],
    queryFn: () => (managerId ? api.commitments.daily.listTeamForDate(managerId, date) : Promise.resolve([])),
    enabled: Boolean(managerId),
  });

  // ---- Mutations ----

  const reviewMutation = useMutation({
    mutationFn: ({ id, approve, rejectionReason }: { id: number; approve: boolean; rejectionReason?: string }) =>
      api.commitments.daily.review(id, { approve, rejectionReason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tl-pending-commitments', managerId] });
      queryClient.invalidateQueries({ queryKey: ['tl-team-for-date', managerId] });
    },
  });

  const downloadReport = async () => {
    if (!managerId) return;
    try {
      const blob = await api.commitmentReports.teamXlsx(managerId, reportStart, reportEnd);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `team-commitments-${reportStart}-to-${reportEnd}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Team report download failed', err);
    }
  };

  const teamCount = useMemo(
    () => new Set(teamForDate.map((c) => c.employeeId)).size,
    [teamForDate],
  );

  if (!managerId) {
    return (
      <Card className="border border-amber-200 bg-amber-50">
        <p className="text-sm font-semibold text-amber-800">Team Leader profile not linked</p>
        <p className="mt-1 text-sm text-amber-700">
          Your login isn&apos;t linked to an employee record. Ask HR to provision your profile.
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Team Daily Commitments"
        description="Approve your team's submitted commitments and pull a snapshot for any date. Download Excel reports any time."
      />

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">Approval queue</p>
            <h2 className="text-lg font-semibold text-slate-900">Pending submissions</h2>
            <p className="text-sm text-slate-500">
              Approving rolls the row's actuals into the leaderboard. Rejecting requires a reason.
            </p>
          </div>
          <StatusBadge label={`${pending.length} pending`} tone={pending.length ? 'warning' : 'neutral'} />
        </div>

        <div className="mt-4">
          <DataTable
            data={pending}
            emptyTitle={pendingLoading ? 'Loading…' : 'Nothing pending'}
            emptyDescription={pendingLoading ? 'Fetching the queue.' : 'Your team has no submitted commitments waiting on you.'}
            columns={[
              { key: 'employee', header: 'Employee', render: (row) => row.employeeName ?? row.employeeCode },
              { key: 'date', header: 'Date', render: (row) => row.workDate },
              {
                key: 'targets',
                header: 'Targets (calls / OTPs / cust / ₹)',
                render: (row) =>
                  `${row.targetCalls} / ${row.targetOtps} / ${row.targetInterestedCustomers} / ${formatCurrency(row.targetDisbursalAmount)}`,
              },
              {
                key: 'actuals',
                header: 'Actuals (calls / OTPs / cust / ₹)',
                render: (row) =>
                  `${row.actualCalls} / ${row.actualOtps} / ${row.actualInterestedCustomers} / ${formatCurrency(row.actualDisbursalAmount)}`,
              },
              {
                key: 'actions',
                header: 'Actions',
                render: (row) => (
                  <div className="flex items-center gap-2">
                    <Button
                      onClick={() => reviewMutation.mutate({ id: row.id, approve: true })}
                      disabled={reviewMutation.isPending}
                    >
                      Approve
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => setRejectDialog({ id: row.id, reason: '' })}
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

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Team snapshot</h2>
            <p className="text-sm text-slate-500">
              Every direct report's commitment for {date} ({teamCount} employees).
            </p>
          </div>
          <Input label="Date" type="date" value={date} onChange={(event) => setDate(event.target.value)} max={TODAY} />
        </div>

        <div className="mt-4">
          <DataTable
            data={teamForDate}
            emptyTitle="No commitments for this date"
            emptyDescription="Your team hasn't created commitments for this day."
            columns={[
              { key: 'employee', header: 'Employee', render: (row) => row.employeeName ?? row.employeeCode },
              { key: 'targetDisbursal', header: 'Target ₹', render: (row) => formatCurrency(row.targetDisbursalAmount) },
              { key: 'actualDisbursal', header: 'Actual ₹', render: (row) => formatCurrency(row.actualDisbursalAmount) },
              { key: 'targetCalls', header: 'Target Calls', render: (row) => row.targetCalls },
              { key: 'actualCalls', header: 'Actual Calls', render: (row) => row.actualCalls },
              {
                key: 'status',
                header: 'Status',
                render: (row) => <StatusBadge label={row.status} tone={statusTone[row.status]} />,
              },
            ]}
          />
        </div>
      </Card>

      <Card className="border border-slate-200 bg-white shadow-none">
        <h2 className="text-lg font-semibold text-slate-900">Download Excel report</h2>
        <p className="text-sm text-slate-500">Daily commitment breakdown for your team in a date window.</p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Input label="Start date" type="date" value={reportStart} onChange={(event) => setReportStart(event.target.value)} />
          <Input label="End date" type="date" value={reportEnd} onChange={(event) => setReportEnd(event.target.value)} />
          <div className="flex items-end">
            <Button onClick={downloadReport}>Download .xlsx</Button>
          </div>
        </div>
      </Card>

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
      <h3 className="text-lg font-semibold text-slate-900">Reject commitment</h3>
      <p className="mt-2 text-sm text-slate-500">
        Tell the employee what to fix. They&apos;ll see this when they revise and re-submit.
      </p>
      <textarea
        className="mt-4 h-28 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
        placeholder="Targets too low — please align to monthly goal."
        value={reason}
        onChange={(event) => onChange(event.target.value)}
      />
      <div className="mt-4 flex justify-end gap-3">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button variant="danger" onClick={onConfirm} disabled={submitting || !reason.trim()}>
          {submitting ? 'Rejecting…' : 'Reject'}
        </Button>
      </div>
    </Card>
  </div>
);
