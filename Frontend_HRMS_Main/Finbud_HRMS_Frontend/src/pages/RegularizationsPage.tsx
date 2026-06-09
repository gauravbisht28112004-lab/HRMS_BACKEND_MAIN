import { FormEvent, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import type { Regularization, RegularizationStatus } from '@/types';

/**
 * Regularizations are the *only* way an employee can correct a bad punch
 * after the fact — there are no direct "edit my row" buttons anywhere on the
 * portal. They submit a requested punch-in/out + reason, and a TL / HR /
 * Admin either APPROVES (which rewrites the underlying attendance row) or
 * REJECTS (with a note). This page is the one-stop shop for both sides of
 * that loop.
 */

const statusTone = (status: RegularizationStatus) => {
  switch (status) {
    case 'APPROVED':
      return 'success' as const;
    case 'REJECTED':
      return 'danger' as const;
    default:
      return 'warning' as const;
  }
};

const fmtDateTime = (value: string | null | undefined) =>
  value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—';

/**
 * A requested punch time is submitted as `YYYY-MM-DDTHH:mm` (browser
 * datetime-local value). The backend accepts ISO-8601 LocalDateTime —
 * which is exactly what the input gives us, so we can pass it through.
 */
const emptyForm = {
  attendanceDate: new Date().toISOString().slice(0, 10),
  requestedPunchIn: '',
  requestedPunchOut: '',
  reason: '',
};

export const RegularizationsPage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const canReview = user?.role === 'Admin' || user?.role === 'HR' || user?.role === 'Team Leader';

  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [reviewDialog, setReviewDialog] = useState<
    { id: number; action: 'approve' | 'reject'; notes: string } | null
  >(null);

  // ---- Employee-side: my submissions --------------------------------
  const { data: myRequests = [], isFetching: loadingMine } = useQuery<Regularization[]>({
    queryKey: ['regularizations-mine'],
    queryFn: () => api.regularizations.listMine(),
    enabled: Boolean(user),
  });

  const { data: pending = [], isFetching: loadingPending } = useQuery<Regularization[]>({
    queryKey: ['regularizations-pending'],
    queryFn: () => api.regularizations.listPending(),
    enabled: canReview,
    refetchInterval: 60_000,
  });

  const submitMutation = useMutation({
    mutationFn: () => {
      if (!form.attendanceDate) {
        throw new Error('Attendance date is required.');
      }
      if (!form.reason.trim()) {
        throw new Error('Please describe why you are filing this regularization.');
      }
      if (!form.requestedPunchIn && !form.requestedPunchOut) {
        throw new Error('Provide at least one corrected punch time.');
      }
      return api.regularizations.submit({
        attendanceDate: form.attendanceDate,
        requestedPunchIn: form.requestedPunchIn || undefined,
        requestedPunchOut: form.requestedPunchOut || undefined,
        reason: form.reason.trim(),
      });
    },
    onSuccess: async () => {
      setForm(emptyForm);
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['regularizations-mine'] });
      await queryClient.invalidateQueries({ queryKey: ['regularizations-pending'] });
    },
    onError: (err: unknown) => {
      const serverMessage = (err as { response?: { data?: { message?: string } } }).response?.data?.message;
      setFormError(serverMessage ?? (err instanceof Error ? err.message : 'Could not submit request.'));
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => api.regularizations.cancelOwn(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['regularizations-mine'] }),
  });

  const reviewMutation = useMutation({
    mutationFn: ({ id, approve, reviewNotes }: { id: number; approve: boolean; reviewNotes?: string }) =>
      api.regularizations.review(id, { approve, reviewNotes }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['regularizations-pending'] });
      await queryClient.invalidateQueries({ queryKey: ['regularizations-mine'] });
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
    },
  });

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    try {
      await submitMutation.mutateAsync();
    } catch {
      // Error state captured in onError.
    }
  };

  const pendingCount = useMemo(() => pending.length, [pending]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Regularizations"
        description={
          canReview
            ? 'Review punch corrections submitted by your team and approve or reject with a clear note.'
            : 'Correct a missed punch. Once approved by your team leader or HR, your attendance row is updated automatically.'
        }
      />

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <Card>
          <h3 className="text-lg font-semibold text-slate-900">File a regularization</h3>
          <p className="mt-1 text-sm text-slate-500">
            Use this form when you forgot to punch in/out or your punch was rejected. HR reviews every request.
          </p>
          <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
            <Input
              label="Attendance Date"
              type="date"
              value={form.attendanceDate}
              onChange={(event) => setForm({ ...form, attendanceDate: event.target.value })}
              max={new Date().toISOString().slice(0, 10)}
            />
            <div className="grid gap-4 md:grid-cols-2">
              <Input
                label="Requested Punch In"
                type="datetime-local"
                value={form.requestedPunchIn}
                onChange={(event) => setForm({ ...form, requestedPunchIn: event.target.value })}
              />
              <Input
                label="Requested Punch Out"
                type="datetime-local"
                value={form.requestedPunchOut}
                onChange={(event) => setForm({ ...form, requestedPunchOut: event.target.value })}
              />
            </div>
            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              <span>Reason</span>
              <textarea
                rows={4}
                className="rounded-xl border border-slate-200 px-4 py-3 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
                placeholder="Forgot to punch out after client meeting at 7pm. Left office at 8:15pm."
                value={form.reason}
                onChange={(event) => setForm({ ...form, reason: event.target.value })}
                required
              />
            </label>
            {formError ? (
              <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
                {formError}
              </div>
            ) : null}
            <Button type="submit" disabled={submitMutation.isPending}>
              {submitMutation.isPending ? 'Submitting…' : 'Submit Request'}
            </Button>
          </form>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">My requests</h3>
              <p className="text-sm text-slate-500">Most recent regularizations you filed.</p>
            </div>
            <StatusBadge
              label={`${myRequests.filter((r) => r.status === 'PENDING').length} pending`}
              tone="neutral"
            />
          </div>
          <div className="mt-4">
            <DataTable
              data={myRequests}
              emptyTitle={loadingMine ? 'Loading…' : 'No requests yet'}
              emptyDescription={loadingMine ? 'Fetching your history.' : 'File one when you miss a punch.'}
              columns={[
                { key: 'date', header: 'Date', render: (record) => record.attendanceDate },
                {
                  key: 'punchIn',
                  header: 'Punch In',
                  render: (record) => fmtDateTime(record.requestedPunchIn),
                },
                {
                  key: 'punchOut',
                  header: 'Punch Out',
                  render: (record) => fmtDateTime(record.requestedPunchOut),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (record) => <StatusBadge label={record.status} tone={statusTone(record.status)} />,
                },
                {
                  key: 'notes',
                  header: 'Reviewer notes',
                  render: (record) =>
                    record.reviewNotes ? (
                      <span className="text-xs text-slate-600">{record.reviewNotes}</span>
                    ) : (
                      <span className="text-xs text-slate-400">—</span>
                    ),
                },
                {
                  key: 'actions',
                  header: '',
                  render: (record) =>
                    record.status === 'PENDING' ? (
                      <Button
                        variant="ghost"
                        onClick={() => cancelMutation.mutate(record.id)}
                        disabled={cancelMutation.isPending}
                      >
                        Cancel
                      </Button>
                    ) : null,
                },
              ]}
            />
          </div>
        </Card>
      </div>

      {canReview && (
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">Approval queue</p>
              <h3 className="text-lg font-semibold text-slate-900">Pending regularizations</h3>
              <p className="text-sm text-slate-500">
                Approving rewrites the attendance row for that date. Rejecting keeps the row unchanged.
              </p>
            </div>
            <StatusBadge
              label={`${pendingCount} pending`}
              tone={pendingCount ? 'warning' : 'neutral'}
            />
          </div>
          <div className="mt-4">
            <DataTable
              data={pending}
              emptyTitle={loadingPending ? 'Loading…' : 'Queue is clear'}
              emptyDescription={
                loadingPending ? 'Fetching the latest requests.' : 'Nothing waiting on your review right now.'
              }
              columns={[
                { key: 'employee', header: 'Employee', render: (record) => record.employeeName },
                { key: 'date', header: 'Date', render: (record) => record.attendanceDate },
                {
                  key: 'punchIn',
                  header: 'Requested In',
                  render: (record) => fmtDateTime(record.requestedPunchIn),
                },
                {
                  key: 'punchOut',
                  header: 'Requested Out',
                  render: (record) => fmtDateTime(record.requestedPunchOut),
                },
                {
                  key: 'reason',
                  header: 'Reason',
                  render: (record) => (
                    <span className="line-clamp-2 block max-w-xs text-xs text-slate-600">{record.reason}</span>
                  ),
                },
                {
                  key: 'actions',
                  header: 'Actions',
                  render: (record) => (
                    <div className="flex items-center gap-2">
                      <Button
                        onClick={() => setReviewDialog({ id: record.id, action: 'approve', notes: '' })}
                        disabled={reviewMutation.isPending}
                      >
                        Approve
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={() => setReviewDialog({ id: record.id, action: 'reject', notes: '' })}
                        disabled={reviewMutation.isPending}
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

      {reviewDialog ? (
        <ReviewDialog
          action={reviewDialog.action}
          notes={reviewDialog.notes}
          onChange={(notes) => setReviewDialog({ ...reviewDialog, notes })}
          onCancel={() => setReviewDialog(null)}
          onConfirm={async () => {
            if (reviewDialog.action === 'reject' && !reviewDialog.notes.trim()) return;
            await reviewMutation.mutateAsync({
              id: reviewDialog.id,
              approve: reviewDialog.action === 'approve',
              reviewNotes: reviewDialog.notes.trim() || undefined,
            });
            setReviewDialog(null);
          }}
          submitting={reviewMutation.isPending}
        />
      ) : null}
    </div>
  );
};

interface ReviewDialogProps {
  action: 'approve' | 'reject';
  notes: string;
  onChange: (notes: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
  submitting: boolean;
}

const ReviewDialog = ({ action, notes, onChange, onCancel, onConfirm, submitting }: ReviewDialogProps) => {
  const approving = action === 'approve';
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
      <Card className="w-full max-w-md">
        <h3 className="text-lg font-semibold text-slate-900">
          {approving ? 'Approve regularization' : 'Reject regularization'}
        </h3>
        <p className="mt-2 text-sm text-slate-500">
          {approving
            ? 'The attendance row for this date will be rewritten with the requested punch times.'
            : 'Tell the employee why you are rejecting. This note is visible to them.'}
        </p>
        <label className="mt-4 flex flex-col gap-2 text-sm font-medium text-slate-700">
          <span>{approving ? 'Review notes (optional)' : 'Rejection reason'}</span>
          <textarea
            className="h-28 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
            placeholder={approving ? 'Verified with TL. Applying.' : 'Needs proof of travel or client meeting.'}
            value={notes}
            onChange={(event) => onChange(event.target.value)}
          />
        </label>
        <div className="mt-4 flex justify-end gap-3">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            variant={approving ? 'primary' : 'danger'}
            onClick={onConfirm}
            disabled={submitting || (!approving && !notes.trim())}
          >
            {submitting ? 'Saving…' : approving ? 'Approve' : 'Reject'}
          </Button>
        </div>
      </Card>
    </div>
  );
};
