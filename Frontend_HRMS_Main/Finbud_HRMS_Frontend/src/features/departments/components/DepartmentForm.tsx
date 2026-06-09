import { FormEvent, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import type { DepartmentFormValues } from '@/services/requestMappers';

interface DepartmentFormProps {
  onSubmit: (values: DepartmentFormValues) => Promise<unknown> | void;
  isSubmitting?: boolean;
  managerOptions: { label: string; value: string }[];
}

const defaultValues: DepartmentFormValues = {
  name: '',
  code: '',
  managerId: '',
};

export const DepartmentForm = ({ onSubmit, isSubmitting = false, managerOptions }: DepartmentFormProps) => {
  const [form, setForm] = useState<DepartmentFormValues>(defaultValues);

  useEffect(() => {
    setForm(defaultValues);
  }, [managerOptions]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await onSubmit(form);
    setForm(defaultValues);
  };

  return (
    <Card>
      <form onSubmit={handleSubmit} className="grid gap-4 md:grid-cols-3">
        <Input label="Department Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
        <Input label="Department Code" value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} />
        <Select
          label="Assign Team Leader"
          value={form.managerId}
          onChange={(event) => setForm({ ...form, managerId: event.target.value })}
          options={managerOptions}
        />
        <div className="md:col-span-2 flex justify-end">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Create Department'}
          </Button>
        </div>
      </form>
    </Card>
  );
};
