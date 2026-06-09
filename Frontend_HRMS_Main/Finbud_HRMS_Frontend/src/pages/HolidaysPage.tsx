import { FormEvent, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DataTable } from '@/components/common/DataTable';
import { FilterBar } from '@/components/common/FilterBar';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import type { PublicHolidayPayload } from '@/services/api';
import { api } from '@/services/api';
import type { PublicHoliday } from '@/types';

/**
 * HR / Admin screen for maintaining the public holiday calendar that
 * drives the nightly auto-absent job — days in this table are stamped as
 * HOLIDAY (or OPTIONAL) instead of ABSENT. The page supports per-year
 * filtering so HR can set up next year without losing the current one.
 */

interface FormState {
  id?: number;
  holidayDate: string;
  name: string;
  description: string;
  isOptional: boolean;
}

const blankForm: FormState = {
  holidayDate: '',
  name: '',
  description: '',
  isOptional: false,
};

const toPayload = (form: FormState): PublicHolidayPayload => ({
  holidayDate: form.holidayDate,
  name: form.name.trim(),
  description: form.description.trim() || undefined,
  isOptional: form.isOptional,
});

const fromHoliday = (holiday: PublicHoliday): FormState => ({
  id: holiday.id,
  holidayDate: holiday.holidayDate,
  name: holiday.name,
  description: holiday.description ?? '',
  isOptional: holiday.isOptional,
});

const formatDate = (value: string) => {
  // Use IST consistently so the weekday we display matches the day HR
  // picked from the date input (browsers sometimes off-by-one date fields
  // when the runtime timezone differs from IST).
  try {
    return new Date(`${value}T00:00:00+05:30`).toLocaleDateString('en-IN', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return value;
  }
};

export const HolidaysPage = () => {
  const queryClient = useQueryClient();
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState<number>(currentYear);
  const [form, setForm] = useState<FormState>(blankForm);
  const [error, setError] = useState<string | null>(null);

  const yearOptions = useMemo(
    () =>
      [currentYear - 1, currentYear, currentYear + 1, currentYear + 2].map((y) => ({
        label: String(y),
        value: String(y),
      })),
    [currentYear],
  );

  const { data: holidays = [], isFetching } = useQuery<PublicHoliday[]>({
    queryKey: ['public-holidays', year],
    queryFn: () => api.publicHolidays.listByYear(year),
  });

  const sorted = useMemo(
    () => holidays.slice().sort((a, b) => (a.holidayDate < b.holidayDate ? -1 : 1)),
    [holidays],
  );

  const createMutation = useMutation({
    mutationFn: () => api.publicHolidays.create(toPayload(form)),
    onSuccess: () => {
      setForm(blankForm);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['public-holidays'] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not save holiday.')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id }: { id: number }) => api.publicHolidays.update(id, toPayload(form)),
    onSuccess: () => {
      setForm(blankForm);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['public-holidays'] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not update holiday.')),
  });

  const removeMutation = useMutation({
    mutationFn: (id: number) => api.publicHolidays.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['public-holidays'] }),
  });

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!form.name.trim()) {
      setError('Name is required.');
      return;
    }
    if (!form.holidayDate) {
      setError('Date is required.');
      return;
    }
    if (form.id) {
      updateMutation.mutate({ id: form.id });
    } else {
      createMutation.mutate();
    }
  };

  const busy = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Public holidays"
        description="Official and optional holidays used by the nightly auto-absent job. Optional holidays show up as a suggestion to employees, not an enforced off day."
      />

      <FilterBar>
        <Select
          label="Year"
          value={String(year)}
          onChange={(event) => setYear(Number(event.target.value))}
          options={yearOptions}
        />
        <div className="flex items-end text-sm text-slate-500">
          Showing {sorted.length} holiday{sorted.length === 1 ? '' : 's'} for {year}.
        </div>
      </FilterBar>

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <Card>
          <h3 className="text-lg font-semibold text-slate-900">{form.id ? 'Edit holiday' : 'Add holiday'}</h3>
          <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
            <Input
              label="Date"
              type="date"
              value={form.holidayDate}
              onChange={(event) => setForm({ ...form, holidayDate: event.target.value })}
              required
            />
            <Input
              label="Name"
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              placeholder="Republic Day"
              required
            />
            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              <span>Description</span>
              <textarea
                rows={3}
                className="rounded-xl border border-slate-200 px-4 py-3 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
                value={form.description}
                onChange={(event) => setForm({ ...form, description: event.target.value })}
                placeholder="Gazetted holiday per Ministry of Labour notification."
              />
            </label>
            <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
              <input
                type="checkbox"
                checked={form.isOptional}
                onChange={(event) => setForm({ ...form, isOptional: event.target.checked })}
              />
              Optional / restricted holiday
            </label>

            {error ? (
              <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
                {error}
              </div>
            ) : null}

            <div className="flex items-center gap-3">
              <Button type="submit" disabled={busy}>
                {busy ? 'Saving…' : form.id ? 'Update holiday' : 'Create holiday'}
              </Button>
              {form.id ? (
                <Button type="button" variant="ghost" onClick={() => setForm(blankForm)}>
                  Cancel edit
                </Button>
              ) : null}
            </div>
          </form>
        </Card>

        <Card className="p-0 overflow-hidden">
          <div className="flex items-center justify-between p-5">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">{year} calendar</h3>
              <p className="text-sm text-slate-500">Sorted by date — earliest first.</p>
            </div>
            <StatusBadge
              label={`${sorted.filter((h) => !h.isOptional).length} mandatory`}
              tone="neutral"
            />
          </div>
          <DataTable
            data={sorted}
            emptyTitle={isFetching ? 'Loading…' : `No holidays configured for ${year}`}
            emptyDescription={
              isFetching
                ? 'Fetching holidays for the selected year.'
                : 'Add the first holiday with the form on the left.'
            }
            columns={[
              {
                key: 'date',
                header: 'Date',
                render: (record) => (
                  <span className="font-medium text-slate-900">{formatDate(record.holidayDate)}</span>
                ),
              },
              { key: 'name', header: 'Name', render: (record) => record.name },
              {
                key: 'description',
                header: 'Description',
                render: (record) =>
                  record.description ? (
                    <span className="text-xs text-slate-600">{record.description}</span>
                  ) : (
                    <span className="text-xs text-slate-400">—</span>
                  ),
              },
              {
                key: 'type',
                header: 'Type',
                render: (record) => (
                  <StatusBadge
                    label={record.isOptional ? 'Optional' : 'Mandatory'}
                    tone={record.isOptional ? 'info' : 'success'}
                  />
                ),
              },
              {
                key: 'actions',
                header: '',
                render: (record) => (
                  <div className="flex items-center gap-2">
                    <Button variant="secondary" onClick={() => setForm(fromHoliday(record))}>
                      Edit
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={() => {
                        if (window.confirm(`Delete ${record.name}?`)) {
                          removeMutation.mutate(record.id);
                        }
                      }}
                    >
                      Delete
                    </Button>
                  </div>
                ),
              },
            ]}
          />
        </Card>
      </div>
    </div>
  );
};

const extractError = (err: unknown, fallback: string): string => {
  const payload = (err as { response?: { data?: { message?: string } } } | undefined)?.response?.data;
  if (payload?.message) return payload.message;
  if (err instanceof Error && err.message) return err.message;
  return fallback;
};
