import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import type { DailyCommitment, CommitmentStatus } from '@/types';

/**
 * Real-data Daily Commitment page (Q1 Phase A frontend).
 *
 * <p>Workflow:
 *   1. If no commitment exists for today, employee fills the targets form
 *      and Creates a DRAFT row.
 *   2. While DRAFT, employee can edit targets + fill end-of-day actuals.
 *   3. Employee clicks Submit → status SUBMITTED, locked from edits.
 *   4. TL approves or rejects (separate UI; here we just show the status).
 *   5. If REJECTED, employee can revise + re-submit. If APPROVED, locked.
 */
const TODAY = new Date().toISOString().slice(0, 10);

const statusTone: Record<CommitmentStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

interface FormState {
  targetCalls: string;
  targetOtps: string;
  targetInterestedCustomers: string;
  targetDisbursalAmount: string;
  actualCalls: string;
  actualOtps: string;
  actualInterestedCustomers: string;
  actualDisbursalAmount: string;
  notes: string;
}

const blankForm: FormState = {
  targetCalls: '50',
  targetOtps: '10',
  targetInterestedCustomers: '5',
  targetDisbursalAmount: '50000',
  actualCalls: '',
  actualOtps: '',
  actualInterestedCustomers: '',
  actualDisbursalAmount: '',
  notes: '',
};

const fromCommitment = (c: DailyCommitment): FormState => ({
  targetCalls: String(c.targetCalls),
  targetOtps: String(c.targetOtps),
  targetInterestedCustomers: String(c.targetInterestedCustomers),
  targetDisbursalAmount: String(c.targetDisbursalAmount),
  actualCalls: String(c.actualCalls ?? 0),
  actualOtps: String(c.actualOtps ?? 0),
  actualInterestedCustomers: String(c.actualInterestedCustomers ?? 0),
  actualDisbursalAmount: String(c.actualDisbursalAmount ?? 0),
  notes: c.notes ?? '',
});

export const EmployeeDailyCommitmentPage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const [date, setDate] = useState(TODAY);
  const [form, setForm] = useState<FormState>(blankForm);
  const [error, setError] = useState<string | null>(null);

  const { data: commitment, isLoading } = useQuery<DailyCommitment | null>({
    queryKey: ['my-daily-commitment', date],
    queryFn: () => api.commitments.daily.getMineForDate(date),
    enabled: Boolean(user?.employeeDbId),
  });

  // Sync the form with the loaded commitment whenever the date or row changes.
  useEffect(() => {
    if (commitment) setForm(fromCommitment(commitment));
    else setForm(blankForm);
  }, [commitment]);

  const isLocked = commitment?.status === 'SUBMITTED' || commitment?.status === 'APPROVED';
  const isEditable = !isLocked;
  const canFillActuals = commitment?.status === 'DRAFT' || commitment?.status === 'REJECTED';

  // ---------- Mutations ----------

  const createMutation = useMutation({
    mutationFn: () =>
      api.commitments.daily.create({
        workDate: date,
        targetCalls: Number(form.targetCalls) || 0,
        targetOtps: Number(form.targetOtps) || 0,
        targetInterestedCustomers: Number(form.targetInterestedCustomers) || 0,
        targetDisbursalAmount: Number(form.targetDisbursalAmount) || 0,
        notes: form.notes || undefined,
      }),
    onSuccess: () => {
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['my-daily-commitment', date] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not create commitment.')),
  });

  const updateMutation = useMutation({
    mutationFn: () => {
      if (!commitment) throw new Error('No commitment loaded');
      return api.commitments.daily.update(commitment.id, {
        targetCalls: Number(form.targetCalls) || 0,
        targetOtps: Number(form.targetOtps) || 0,
        targetInterestedCustomers: Number(form.targetInterestedCustomers) || 0,
        targetDisbursalAmount: Number(form.targetDisbursalAmount) || 0,
        actualCalls: Number(form.actualCalls) || 0,
        actualOtps: Number(form.actualOtps) || 0,
        actualInterestedCustomers: Number(form.actualInterestedCustomers) || 0,
        actualDisbursalAmount: Number(form.actualDisbursalAmount) || 0,
        notes: form.notes || undefined,
      });
    },
    onSuccess: () => {
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['my-daily-commitment', date] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not save commitment.')),
  });

  const submitMutation = useMutation({
    mutationFn: () => {
      if (!commitment) throw new Error('No commitment to submit');
      return api.commitments.daily.submit(commitment.id);
    },
    onSuccess: () => {
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['my-daily-commitment', date] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not submit commitment.')),
  });

  const summary = useMemo(() => {
    if (!commitment) return null;
    const pct = (a: number, t: number) => (t > 0 ? Math.min(100, Math.round((a / t) * 100)) : 0);
    return {
      callsPct: pct(commitment.actualCalls ?? 0, commitment.targetCalls ?? 0),
      otpsPct: pct(commitment.actualOtps ?? 0, commitment.targetOtps ?? 0),
      customersPct: pct(commitment.actualInterestedCustomers ?? 0, commitment.targetInterestedCustomers ?? 0),
      disbursalPct: pct(Number(commitment.actualDisbursalAmount ?? 0), Number(commitment.targetDisbursalAmount ?? 0)),
    };
  }, [commitment]);

  // ---------- Render ----------

  if (!user?.employeeDbId) {
    return (
      <Card className="border border-amber-200 bg-amber-50">
        <p className="text-sm font-semibold text-amber-800">Employee profile not linked</p>
        <p className="mt-1 text-sm text-amber-700">
          Your login isn&apos;t linked to an employee record. Ask HR to provision your profile.
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Daily Commitment"
        description="Set your day's targets, fill actuals at end of day, and submit for TL approval."
        action={
          <Input
            label="Date"
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
            max={TODAY}
          />
        }
      />

      {commitment ? (
        <Card className="border border-slate-200 bg-white shadow-none">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">Status</p>
              <h3 className="text-lg font-semibold text-slate-900">{commitment.workDate}</h3>
            </div>
            <StatusBadge label={commitment.status} tone={statusTone[commitment.status]} />
          </div>
          {commitment.status === 'REJECTED' && commitment.rejectionReason ? (
            <div className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
              <p className="font-semibold">Rejected by {commitment.approvedByName ?? 'your TL'}:</p>
              <p>{commitment.rejectionReason}</p>
            </div>
          ) : null}
        </Card>
      ) : null}

      <Card className="border border-slate-200 bg-white shadow-none">
        <h3 className="text-xl font-semibold text-slate-900">Targets</h3>
        <p className="mt-1 text-sm text-slate-500">What you commit to achieve today.</p>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <Input
            label="Target Calls"
            type="number"
            value={form.targetCalls}
            onChange={(event) => setForm({ ...form, targetCalls: event.target.value })}
            disabled={!isEditable}
          />
          <Input
            label="Target OTPs"
            type="number"
            value={form.targetOtps}
            onChange={(event) => setForm({ ...form, targetOtps: event.target.value })}
            disabled={!isEditable}
          />
          <Input
            label="Target Interested Customers"
            type="number"
            value={form.targetInterestedCustomers}
            onChange={(event) => setForm({ ...form, targetInterestedCustomers: event.target.value })}
            disabled={!isEditable}
          />
          <Input
            label="Target Disbursal (₹)"
            type="number"
            value={form.targetDisbursalAmount}
            onChange={(event) => setForm({ ...form, targetDisbursalAmount: event.target.value })}
            disabled={!isEditable}
          />
        </div>
      </Card>

      {commitment ? (
        <Card className="border border-slate-200 bg-white shadow-none">
          <h3 className="text-xl font-semibold text-slate-900">End-of-day Actuals</h3>
          <p className="mt-1 text-sm text-slate-500">Fill these in at end of day, then Submit for TL approval.</p>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Input
              label="Actual Calls"
              type="number"
              value={form.actualCalls}
              onChange={(event) => setForm({ ...form, actualCalls: event.target.value })}
              disabled={!canFillActuals}
            />
            <Input
              label="Actual OTPs"
              type="number"
              value={form.actualOtps}
              onChange={(event) => setForm({ ...form, actualOtps: event.target.value })}
              disabled={!canFillActuals}
            />
            <Input
              label="Actual Interested Customers"
              type="number"
              value={form.actualInterestedCustomers}
              onChange={(event) => setForm({ ...form, actualInterestedCustomers: event.target.value })}
              disabled={!canFillActuals}
            />
            <Input
              label="Actual Disbursal (₹)"
              type="number"
              value={form.actualDisbursalAmount}
              onChange={(event) => setForm({ ...form, actualDisbursalAmount: event.target.value })}
              disabled={!canFillActuals}
            />
          </div>
          {summary ? (
            <div className="mt-5 grid gap-3 md:grid-cols-4 text-sm text-slate-600">
              <Stat label="Calls" pct={summary.callsPct} />
              <Stat label="OTPs" pct={summary.otpsPct} />
              <Stat label="Customers" pct={summary.customersPct} />
              <Stat label="Disbursal" pct={summary.disbursalPct} />
            </div>
          ) : null}
        </Card>
      ) : null}

      <Card className="border border-slate-200 bg-white shadow-none">
        <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
          <span>Notes (optional)</span>
          <textarea
            rows={3}
            disabled={!isEditable}
            value={form.notes}
            onChange={(event) => setForm({ ...form, notes: event.target.value })}
            className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100 disabled:bg-slate-50"
            placeholder="Anything your TL should know…"
          />
        </label>
      </Card>

      {error ? (
        <Card className="border border-rose-200 bg-rose-50">
          <p className="text-sm text-rose-800">{error}</p>
        </Card>
      ) : null}

      <div className="flex flex-wrap gap-3">
        {!commitment ? (
          <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending || isLoading || date !== TODAY}>
            {createMutation.isPending ? 'Creating…' : 'Create Commitment'}
          </Button>
        ) : (
          <>
            <Button onClick={() => updateMutation.mutate()} disabled={!isEditable || updateMutation.isPending}>
              {updateMutation.isPending ? 'Saving…' : 'Save'}
            </Button>
            <Button variant="secondary" onClick={() => submitMutation.mutate()} disabled={!canFillActuals || submitMutation.isPending}>
              {submitMutation.isPending ? 'Submitting…' : 'Submit for TL approval'}
            </Button>
          </>
        )}
        {date !== TODAY ? (
          <p className="text-sm text-slate-500 self-center">
            Viewing past day — read-only. Switch to today to create or edit.
          </p>
        ) : null}
      </div>
    </div>
  );
};

const Stat = ({ label, pct }: { label: string; pct: number }) => (
  <div className="rounded-2xl bg-[#f8f8f4] p-3">
    <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
    <p className="mt-1 text-lg font-semibold text-slate-900">{pct}%</p>
  </div>
);

const extractError = (err: unknown, fallback: string): string => {
  const apiMessage = (err as { response?: { data?: { message?: string } } }).response?.data?.message;
  if (apiMessage) return apiMessage;
  if (err instanceof Error && err.message) return err.message;
  return fallback;
};
