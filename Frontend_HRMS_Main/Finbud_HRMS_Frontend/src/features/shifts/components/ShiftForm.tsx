import { FormEvent, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import type { ShiftFormValues } from '@/services/requestMappers';

interface ShiftFormProps {
  onSubmit: (values: ShiftFormValues) => Promise<unknown> | void;
  isSubmitting?: boolean;
  initialValues?: ShiftFormValues | null;
  submitLabel?: string;
  onCancel?: () => void;
  title?: string;
}

const defaultValues: ShiftFormValues = {
  name: '',
  code: '',
  startTime: '',
  endTime: '',
  breakDurationMinutes: '60',
  gracePeriodMinutes: '10',
  weeklyOffDays: '0',
  overtimeThresholdHours: '8',
};

export const ShiftForm = ({
  onSubmit,
  isSubmitting = false,
  initialValues = null,
  submitLabel,
  onCancel,
  title,
}: ShiftFormProps) => {
  const [form, setForm] = useState<ShiftFormValues>(initialValues ?? defaultValues);

  // Keep the form in sync when the parent swaps which row is being edited.
  useEffect(() => {
    setForm(initialValues ?? defaultValues);
  }, [initialValues]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await onSubmit(form);
    // Only reset after a fresh-create; when editing we let the parent
    // decide (usually by clearing the selected row on success).
    if (!initialValues) {
      setForm(defaultValues);
    }
  };

  const computedSubmitLabel = submitLabel ?? (initialValues ? 'Save Changes' : 'Save Shift');
  const pendingLabel = initialValues ? 'Saving…' : 'Saving…';

  return (
    <Card>
      {title ? <h3 className="mb-4 text-base font-semibold text-slate-800">{title}</h3> : null}
      <form className="grid gap-4 md:grid-cols-2 xl:grid-cols-4" onSubmit={handleSubmit}>
        <Input label="Shift Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <Input label="Shift Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
        <Input label="Start Time" type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
        <Input label="End Time" type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
        <Input
          label="Break Duration (mins)"
          type="number"
          value={form.breakDurationMinutes}
          onChange={(e) => setForm({ ...form, breakDurationMinutes: e.target.value })}
        />
        <Input
          label="Grace Period (mins)"
          type="number"
          value={form.gracePeriodMinutes}
          onChange={(e) => setForm({ ...form, gracePeriodMinutes: e.target.value })}
        />
        <Input
          label="Weekly Off Days"
          value={form.weeklyOffDays}
          onChange={(e) => setForm({ ...form, weeklyOffDays: e.target.value })}
          placeholder="0 for Sunday, 6 for Saturday"
        />
        <Input
          label="Overtime Threshold"
          type="number"
          step="0.5"
          value={form.overtimeThresholdHours}
          onChange={(e) => setForm({ ...form, overtimeThresholdHours: e.target.value })}
        />
        <div className="md:col-span-2 xl:col-span-4 flex flex-wrap items-center justify-end gap-2">
          {onCancel ? (
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Cancel
            </Button>
          ) : null}
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? pendingLabel : computedSubmitLabel}
          </Button>
        </div>
      </form>
    </Card>
  );
};
