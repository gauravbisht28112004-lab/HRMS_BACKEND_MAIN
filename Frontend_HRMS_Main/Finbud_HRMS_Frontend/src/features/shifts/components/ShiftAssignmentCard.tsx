import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { StatusBadge } from '@/components/common/StatusBadge';
import { api } from '@/services/api';
import type { ShiftAssignment } from '@/types';

interface ShiftAssignmentCardProps {
  employeeBackendId: number | null | undefined;
}

interface AssignmentFormValues {
  shiftTypeId: string;
  effectiveFrom: string;
  effectiveTo: string;
}

const todayIso = () => new Date().toISOString().slice(0, 10);

const emptyForm = (): AssignmentFormValues => ({
  shiftTypeId: '',
  effectiveFrom: todayIso(),
  effectiveTo: '',
});

const toPayload = (form: AssignmentFormValues) => ({
  shiftTypeId: Number(form.shiftTypeId),
  effectiveFrom: form.effectiveFrom,
  effectiveTo: form.effectiveTo ? form.effectiveTo : null,
});

const formatDate = (iso: string | null | undefined): string => {
  if (!iso) return '—';
  // Preserve the ISO yyyy-mm-dd as given; we avoid timezone games here.
  return iso;
};

/**
 * Shift assignment card shown on the Employee Profile page for Admin/HR.
 *
 * <p>Surfaces the current shift, the full assignment history (newest first),
 * and a create/edit form. On create the backend auto-closes any prior
 * open-ended assignment by setting its effective_to to (newStart - 1 day),
 * and refreshes Employee.shiftType to point at the new current shift.</p>
 */
export const ShiftAssignmentCard = ({ employeeBackendId }: ShiftAssignmentCardProps) => {
  const queryClient = useQueryClient();
  const enabled = typeof employeeBackendId === 'number';

  const { data: shifts = [] } = useQuery({
    queryKey: ['shifts'],
    queryFn: api.shifts.list,
  });

  const { data: assignments = [], isLoading, isError, error } = useQuery({
    queryKey: ['shift-assignments', employeeBackendId],
    queryFn: () => api.shifts.assignments.listForEmployee(employeeBackendId as number),
    enabled,
  });

  const currentAssignment = useMemo<ShiftAssignment | null>(
    () => assignments.find((row) => row.current) ?? null,
    [assignments],
  );

  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<AssignmentFormValues>(() => emptyForm());
  const [showForm, setShowForm] = useState<boolean>(false);

  // When user picks a row to edit, hydrate the form with its values.
  useEffect(() => {
    if (editingId == null) {
      return;
    }
    const row = assignments.find((a) => a.id === editingId);
    if (!row) {
      return;
    }
    setForm({
      shiftTypeId: String(row.shiftTypeId),
      effectiveFrom: row.effectiveFrom,
      effectiveTo: row.effectiveTo ?? '',
    });
    setShowForm(true);
  }, [editingId, assignments]);

  const invalidate = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['shift-assignments', employeeBackendId] }),
      queryClient.invalidateQueries({ queryKey: ['employee'] }),
      queryClient.invalidateQueries({ queryKey: ['employees'] }),
    ]);
  };

  const createMutation = useMutation({
    mutationFn: (values: AssignmentFormValues) =>
      api.shifts.assignments.create(employeeBackendId as number, toPayload(values)),
    onSuccess: async () => {
      await invalidate();
      setForm(emptyForm());
      setShowForm(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: AssignmentFormValues }) =>
      api.shifts.assignments.update(id, toPayload(values)),
    onSuccess: async () => {
      await invalidate();
      setEditingId(null);
      setForm(emptyForm());
      setShowForm(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.shifts.assignments.remove(id),
    onSuccess: async () => {
      await invalidate();
    },
  });

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!enabled) return;
    if (!form.shiftTypeId || !form.effectiveFrom) return;

    if (editingId != null) {
      await updateMutation.mutateAsync({ id: editingId, values: form });
    } else {
      await createMutation.mutateAsync(form);
    }
  };

  const handleEdit = (row: ShiftAssignment) => {
    setEditingId(row.id);
  };

  const handleDelete = (row: ShiftAssignment) => {
    const ok = window.confirm(
      `Delete this shift assignment (${row.shiftTypeName ?? row.shiftTypeCode ?? '?'} from ${row.effectiveFrom})? This cannot be undone.`,
    );
    if (!ok) return;
    deleteMutation.mutate(row.id);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setForm(emptyForm());
    setShowForm(false);
  };

  if (!enabled) {
    return null;
  }

  const submitBusy = createMutation.isPending || updateMutation.isPending;
  const mutationError =
    (createMutation.error as Error | null)?.message ??
    (updateMutation.error as Error | null)?.message ??
    (deleteMutation.error as Error | null)?.message ??
    null;

  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">Shift Assignment</h3>
          <p className="mt-1 text-sm text-slate-500">
            Authoritative timeline of shift assignments. Creating a new one auto-closes the previous open assignment
            and updates the employee’s current shift.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {currentAssignment ? (
            <StatusBadge label="Current" tone="success" />
          ) : (
            <StatusBadge label="No active shift" tone="warning" />
          )}
        </div>
      </div>

      <div className="mt-4 grid gap-3 rounded-xl border border-slate-200 bg-slate-50/60 p-4 text-sm text-slate-700 md:grid-cols-3">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">Current shift</p>
          <p className="mt-1 font-semibold text-slate-900">
            {currentAssignment
              ? `${currentAssignment.shiftTypeName ?? '—'}${currentAssignment.shiftTypeCode ? ` (${currentAssignment.shiftTypeCode})` : ''}`
              : 'Not assigned'}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">Effective from</p>
          <p className="mt-1 font-semibold text-slate-900">{formatDate(currentAssignment?.effectiveFrom)}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">Effective to</p>
          <p className="mt-1 font-semibold text-slate-900">
            {currentAssignment?.effectiveTo ? formatDate(currentAssignment.effectiveTo) : 'Open-ended'}
          </p>
        </div>
      </div>

      <div className="mt-4 flex items-center justify-end gap-2">
        {showForm || editingId != null ? null : (
          <Button
            onClick={() => {
              setEditingId(null);
              setForm(emptyForm());
              setShowForm(true);
            }}
          >
            Assign New Shift
          </Button>
        )}
      </div>

      {(showForm || editingId != null) && (
        <form
          className="mt-4 grid gap-4 rounded-xl border border-slate-200 bg-white p-4 md:grid-cols-4"
          onSubmit={handleSubmit}
        >
          <label className="flex flex-col gap-1 text-sm font-medium text-slate-700 md:col-span-2">
            <span>Shift</span>
            <select
              className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
              value={form.shiftTypeId}
              onChange={(event) => setForm({ ...form, shiftTypeId: event.target.value })}
              required
            >
              <option value="" disabled>
                Select a shift
              </option>
              {shifts.map((shift) => (
                <option key={shift.backendId ?? shift.id} value={shift.backendId ?? ''}>
                  {shift.name}
                  {shift.code ? ` (${shift.code})` : ''} — {shift.startTime}–{shift.endTime}
                </option>
              ))}
            </select>
          </label>
          <Input
            label="Effective From"
            type="date"
            value={form.effectiveFrom}
            onChange={(event) => setForm({ ...form, effectiveFrom: event.target.value })}
            required
          />
          <Input
            label="Effective To (optional)"
            type="date"
            value={form.effectiveTo}
            onChange={(event) => setForm({ ...form, effectiveTo: event.target.value })}
            placeholder="Open-ended"
          />
          <div className="md:col-span-4 flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs text-slate-500">
              Leaving <span className="font-semibold">Effective To</span> blank keeps the assignment open-ended.
              Creating a new assignment auto-closes the previous one.
            </p>
            <div className="flex items-center gap-2">
              <Button type="button" variant="secondary" onClick={handleCancelEdit} disabled={submitBusy}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitBusy || !form.shiftTypeId || !form.effectiveFrom}>
                {submitBusy ? 'Saving…' : editingId != null ? 'Save Changes' : 'Create Assignment'}
              </Button>
            </div>
          </div>
        </form>
      )}

      {mutationError ? (
        <div className="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {mutationError}
        </div>
      ) : null}

      <div className="mt-6 overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead>
            <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <th className="py-2 pr-4">Shift</th>
              <th className="py-2 pr-4">From</th>
              <th className="py-2 pr-4">To</th>
              <th className="py-2 pr-4">Status</th>
              <th className="py-2 pr-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <tr>
                <td className="py-3 text-slate-500" colSpan={5}>
                  Loading assignments…
                </td>
              </tr>
            ) : isError ? (
              <tr>
                <td className="py-3 text-rose-600" colSpan={5}>
                  {(error as Error).message || 'Failed to load shift assignments.'}
                </td>
              </tr>
            ) : assignments.length === 0 ? (
              <tr>
                <td className="py-3 text-slate-500" colSpan={5}>
                  No shift assignments yet.
                </td>
              </tr>
            ) : (
              assignments.map((row) => (
                <tr key={row.id} className={editingId === row.id ? 'bg-brand-50/60' : undefined}>
                  <td className="py-3 pr-4 font-medium text-slate-900">
                    {row.shiftTypeName ?? '—'}
                    {row.shiftTypeCode ? <span className="ml-2 text-xs text-slate-500">({row.shiftTypeCode})</span> : null}
                  </td>
                  <td className="py-3 pr-4 text-slate-700">{formatDate(row.effectiveFrom)}</td>
                  <td className="py-3 pr-4 text-slate-700">{row.effectiveTo ? formatDate(row.effectiveTo) : 'Open-ended'}</td>
                  <td className="py-3 pr-4">
                    {row.current ? (
                      <StatusBadge label="Current" tone="success" />
                    ) : row.effectiveTo && new Date(row.effectiveTo) < new Date() ? (
                      <StatusBadge label="Ended" tone="danger" />
                    ) : (
                      <StatusBadge label="Scheduled" tone="warning" />
                    )}
                  </td>
                  <td className="py-3 pr-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button variant="secondary" onClick={() => handleEdit(row)}>
                        Edit
                      </Button>
                      <Button
                        variant="danger"
                        onClick={() => handleDelete(row)}
                        disabled={deleteMutation.isPending && deleteMutation.variables === row.id}
                      >
                        {deleteMutation.isPending && deleteMutation.variables === row.id ? 'Deleting…' : 'Delete'}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Card>
  );
};
