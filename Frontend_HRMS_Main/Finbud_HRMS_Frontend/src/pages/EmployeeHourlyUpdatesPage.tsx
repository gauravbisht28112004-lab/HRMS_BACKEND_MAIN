import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import type { HourlyUpdate } from '@/types';

/**
 * Real-data Hourly Updates page (Q1 Phase B frontend).
 *
 * <p>Pure activity log — no approval. The same {@code (date, hourSlot)}
 * pair upserts in place, so re-submitting an hour overwrites instead of
 * creating duplicates. Backend enforces this with a UNIQUE constraint.
 */
const TODAY = new Date().toISOString().slice(0, 10);

interface DraftState {
  hourSlot: string;
  callsDone: string;
  otpsAchieved: string;
  interestedCustomers: string;
  notes: string;
}

const blankDraft: DraftState = {
  hourSlot: '',
  callsDone: '',
  otpsAchieved: '',
  interestedCustomers: '',
  notes: '',
};

export const EmployeeHourlyUpdatesPage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const [date, setDate] = useState(TODAY);
  const [draft, setDraft] = useState<DraftState>(blankDraft);
  const [error, setError] = useState<string | null>(null);

  const { data: rows = [], isLoading } = useQuery<HourlyUpdate[]>({
    queryKey: ['my-hourly-updates', date],
    queryFn: () => api.commitments.hourly.listMineForDate(date),
    enabled: Boolean(user?.employeeDbId),
  });

  const upsertMutation = useMutation({
    mutationFn: () => {
      if (!draft.hourSlot.trim()) {
        throw new Error('Hour slot is required (e.g. "10:00-11:00").');
      }
      return api.commitments.hourly.upsert({
        workDate: date,
        hourSlot: draft.hourSlot.trim(),
        callsDone: Number(draft.callsDone) || 0,
        otpsAchieved: Number(draft.otpsAchieved) || 0,
        interestedCustomers: Number(draft.interestedCustomers) || 0,
        notes: draft.notes || undefined,
      });
    },
    onSuccess: () => {
      setDraft(blankDraft);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['my-hourly-updates', date] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not save hourly update.')),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.commitments.hourly.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['my-hourly-updates', date] }),
  });

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
        title="Hourly Updates"
        description="Log per-hour calls, OTPs, and interested customers. No approval needed — these feed the daily / weekly reports."
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

      <Card className="border border-slate-200 bg-white shadow-none">
        <h3 className="text-lg font-semibold text-slate-900">Submit / Update an Hour</h3>
        <p className="mt-1 text-sm text-slate-500">
          Re-submitting the same hour overwrites the existing entry — no duplicates.
        </p>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <Input
            label="Hour Slot"
            placeholder="10:00-11:00"
            value={draft.hourSlot}
            onChange={(event) => setDraft({ ...draft, hourSlot: event.target.value })}
          />
          <Input
            label="Calls Done"
            type="number"
            value={draft.callsDone}
            onChange={(event) => setDraft({ ...draft, callsDone: event.target.value })}
          />
          <Input
            label="OTPs Achieved"
            type="number"
            value={draft.otpsAchieved}
            onChange={(event) => setDraft({ ...draft, otpsAchieved: event.target.value })}
          />
          <Input
            label="Interested Customers"
            type="number"
            value={draft.interestedCustomers}
            onChange={(event) => setDraft({ ...draft, interestedCustomers: event.target.value })}
          />
          <div className="md:col-span-2">
            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              <span>Notes (optional)</span>
              <textarea
                rows={2}
                value={draft.notes}
                onChange={(event) => setDraft({ ...draft, notes: event.target.value })}
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
              />
            </label>
          </div>
        </div>
        {error ? (
          <p className="mt-3 text-sm text-rose-600">{error}</p>
        ) : null}
        <div className="mt-4 flex justify-end">
          <Button onClick={() => upsertMutation.mutate()} disabled={upsertMutation.isPending}>
            {upsertMutation.isPending ? 'Saving…' : 'Save Hour Entry'}
          </Button>
        </div>
      </Card>

      <DataTable
        data={rows}
        emptyTitle={isLoading ? 'Loading…' : 'No entries for this date'}
        emptyDescription={isLoading ? 'Fetching the day.' : 'Submit your first hour entry from the form above.'}
        columns={[
          { key: 'hourSlot', header: 'Hour', render: (row) => row.hourSlot },
          { key: 'calls', header: 'Calls', render: (row) => row.callsDone },
          { key: 'otps', header: 'OTPs', render: (row) => row.otpsAchieved },
          { key: 'customers', header: 'Customers', render: (row) => row.interestedCustomers },
          {
            key: 'notes',
            header: 'Notes',
            render: (row) =>
              row.notes ? (
                <span className="text-xs text-slate-600">{row.notes}</span>
              ) : (
                <span className="text-xs text-slate-400">—</span>
              ),
          },
          {
            key: 'actions',
            header: '',
            render: (row) => (
              <button
                onClick={() => deleteMutation.mutate(row.id)}
                disabled={deleteMutation.isPending}
                title="Delete this hour entry"
                className="rounded-lg border border-slate-200 bg-white p-1.5 text-slate-500 hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50"
              >
                <Trash2 size={14} />
              </button>
            ),
          },
        ]}
      />
    </div>
  );
};

const extractError = (err: unknown, fallback: string): string => {
  const apiMessage = (err as { response?: { data?: { message?: string } } }).response?.data?.message;
  if (apiMessage) return apiMessage;
  if (err instanceof Error && err.message) return err.message;
  return fallback;
};
