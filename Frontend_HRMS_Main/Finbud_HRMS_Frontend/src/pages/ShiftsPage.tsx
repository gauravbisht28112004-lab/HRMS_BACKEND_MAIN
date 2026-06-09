import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { ShiftForm } from '@/features/shifts/components/ShiftForm';
import { api } from '@/services/api';
import { mapShiftFormToRequest, type ShiftFormValues } from '@/services/requestMappers';
import type { Shift } from '@/types';

const weeklyOffDaysToString = (days: number[] | undefined): string =>
  (days ?? []).map((d) => String(d)).join(',');

const shiftToFormValues = (shift: Shift): ShiftFormValues => ({
  name: shift.name,
  code: shift.code ?? '',
  startTime: shift.startTime,
  endTime: shift.endTime,
  breakDurationMinutes: String(shift.breakDurationMinutes ?? 0),
  gracePeriodMinutes: String(shift.gracePeriodMinutes ?? 0),
  weeklyOffDays: weeklyOffDaysToString(shift.weeklyOffDays),
  overtimeThresholdHours: String(shift.overtimeThresholdHours ?? 0),
});

export const ShiftsPage = () => {
  const queryClient = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ['shifts'], queryFn: api.shifts.list });

  const [editingId, setEditingId] = useState<number | null>(null);

  const editingShift = useMemo(
    () => (editingId != null ? data.find((shift) => shift.backendId === editingId) ?? null : null),
    [data, editingId],
  );

  const createMutation = useMutation({
    mutationFn: (values: ShiftFormValues) => api.shifts.create(mapShiftFormToRequest(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['shifts'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: number; values: ShiftFormValues }) =>
      api.shifts.update(id, mapShiftFormToRequest(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['shifts'] });
      setEditingId(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.shifts.remove(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['shifts'] });
      setEditingId((current) => current); // harmless; keeps state consistent
    },
  });

  const handleSubmit = async (values: ShiftFormValues) => {
    if (editingShift?.backendId != null) {
      await updateMutation.mutateAsync({ id: editingShift.backendId, values });
      return;
    }
    await createMutation.mutateAsync(values);
  };

  const handleDelete = (shift: Shift) => {
    if (shift.backendId == null) return;
    const ok = window.confirm(
      `Delete shift "${shift.name}"? This cannot be undone. Employees currently assigned to this shift must be reassigned first.`,
    );
    if (!ok) return;
    deleteMutation.mutate(shift.backendId);
  };

  const isEditing = editingShift != null;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Shift Management"
        description="Define shift policies, grace periods, overtime rules, and employee assignments."
      />

      <ShiftForm
        onSubmit={handleSubmit}
        isSubmitting={createMutation.isPending || updateMutation.isPending}
        initialValues={editingShift ? shiftToFormValues(editingShift) : null}
        title={isEditing ? `Edit shift: ${editingShift?.name}` : 'Create a new shift'}
        submitLabel={isEditing ? 'Save Changes' : 'Save Shift'}
        onCancel={isEditing ? () => setEditingId(null) : undefined}
      />

      {deleteMutation.isError ? (
        <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {(deleteMutation.error as Error).message || 'Failed to delete shift.'}
        </div>
      ) : null}

      <DataTable
        data={data}
        columns={[
          { key: 'name', header: 'Shift', render: (shift) => shift.name },
          { key: 'hours', header: 'Hours', render: (shift) => `${shift.startTime} - ${shift.endTime}` },
          { key: 'break', header: 'Break', render: (shift) => shift.breakTime },
          { key: 'grace', header: 'Grace Period', render: (shift) => shift.gracePeriod },
          { key: 'weeklyOff', header: 'Weekly Off', render: (shift) => shift.weeklyOff },
          { key: 'overtime', header: 'Overtime Rule', render: (shift) => shift.overtimeRule },
          {
            key: 'actions',
            header: 'Actions',
            render: (shift) => (
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  onClick={() => shift.backendId != null && setEditingId(shift.backendId)}
                  disabled={shift.backendId == null}
                >
                  Edit
                </Button>
                <Button
                  variant="danger"
                  onClick={() => handleDelete(shift)}
                  disabled={
                    shift.backendId == null ||
                    (deleteMutation.isPending && deleteMutation.variables === shift.backendId)
                  }
                >
                  {deleteMutation.isPending && deleteMutation.variables === shift.backendId ? 'Deleting…' : 'Delete'}
                </Button>
              </div>
            ),
          },
        ]}
      />
    </div>
  );
};
