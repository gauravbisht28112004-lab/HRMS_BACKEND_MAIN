import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Target, TrendingUp, Users } from 'lucide-react';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatsCard } from '@/components/common/StatsCard';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import type { Employee, MonthlyTarget } from '@/types';

/**
 * Team Leader monthly commitment page — real-data table + inline target editor.
 *
 * <p>Replaces the old hard-coded form (3 input fields with no backend) with a
 * proper team-wide monthly target manager wired to the V17 monthly_targets
 * table. Each TL can set / update targets for their direct reports; the
 * achieved overlay is computed server-side from APPROVED daily commitments.
 *
 * <p>Design choice (option c): table for live read + inline form for write.
 * The form prefills from the selected employee's existing target so the TL
 * can adjust without retyping.
 */
const NOW = new Date();

const MONTH_OPTIONS = [
  { value: '1', label: 'January' },
  { value: '2', label: 'February' },
  { value: '3', label: 'March' },
  { value: '4', label: 'April' },
  { value: '5', label: 'May' },
  { value: '6', label: 'June' },
  { value: '7', label: 'July' },
  { value: '8', label: 'August' },
  { value: '9', label: 'September' },
  { value: '10', label: 'October' },
  { value: '11', label: 'November' },
  { value: '12', label: 'December' },
];

const yearOptions = (() => {
  const current = NOW.getFullYear();
  const years = [current - 1, current, current + 1];
  return years.map((y) => ({ value: String(y), label: String(y) }));
})();

interface FormState {
  employeeId: string;
  targetDisbursalAmount: string;
  targetLogins: string;
  notes: string;
}

const EMPTY_FORM: FormState = {
  employeeId: '',
  targetDisbursalAmount: '',
  targetLogins: '',
  notes: '',
};

export const TeamLeaderMonthlyCommitmentPage = () => {
  const user = useAuthStore((state) => state.user);
  const managerId = user?.employeeDbId;
  const queryClient = useQueryClient();

  const [year, setYear] = useState(String(NOW.getFullYear()));
  const [month, setMonth] = useState(String(NOW.getMonth() + 1));
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const yearNum = Number(year);
  const monthNum = Number(month);

  // -- Direct reports (drives the employee dropdown) --
  const { data: teamMembers = [] } = useQuery<Employee[]>({
    queryKey: ['my-team', managerId],
    queryFn: () => (managerId ? api.employees.listByManager(managerId) : Promise.resolve([])),
    enabled: Boolean(managerId),
  });

  // -- Monthly targets for the picked period --
  const { data: targets = [], isLoading } = useQuery<MonthlyTarget[]>({
    queryKey: ['tl-monthly-targets', managerId, yearNum, monthNum],
    queryFn: () =>
      managerId
        ? api.commitments.monthlyTargets.listForManager(managerId, yearNum, monthNum)
        : Promise.resolve([]),
    enabled: Boolean(managerId),
  });

  /** Prefill the form when the user picks an employee whose target already exists. */
  useEffect(() => {
    if (!form.employeeId) return;
    const existing = targets.find((t) => String(t.employeeId) === form.employeeId);
    if (existing) {
      setForm((prev) => ({
        ...prev,
        targetDisbursalAmount: String(existing.targetDisbursalAmount ?? ''),
        targetLogins: String(existing.targetLogins ?? ''),
        notes: existing.notes ?? '',
      }));
    } else {
      setForm((prev) => ({
        ...prev,
        targetDisbursalAmount: '',
        targetLogins: '',
        notes: '',
      }));
    }
    // intentionally only react to employee or period changes — not user typing
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.employeeId, yearNum, monthNum, targets.length]);

  const totals = useMemo(() => {
    if (targets.length === 0) {
      return { totalTarget: 0, totalAchieved: 0, avgPercent: 0 };
    }
    const totalTarget = targets.reduce((sum, t) => sum + (t.targetDisbursalAmount ?? 0), 0);
    const totalAchieved = targets.reduce((sum, t) => sum + (t.achievedDisbursalAmount ?? 0), 0);
    const avgPercent = Math.round(
      targets.reduce((sum, t) => sum + (t.achievedPercent ?? 0), 0) / targets.length,
    );
    return { totalTarget, totalAchieved, avgPercent };
  }, [targets]);

  const upsertMutation = useMutation({
    mutationFn: async (payload: {
      employeeId: number;
      targetDisbursalAmount: number;
      targetLogins: number;
      notes?: string;
    }) =>
      api.commitments.monthlyTargets.upsert(payload.employeeId, {
        year: yearNum,
        month: monthNum,
        targetDisbursalAmount: payload.targetDisbursalAmount,
        targetLogins: payload.targetLogins,
        notes: payload.notes,
      }),
    onSuccess: () => {
      setFeedback('Target saved.');
      setErrorMsg(null);
      queryClient.invalidateQueries({ queryKey: ['tl-monthly-targets', managerId, yearNum, monthNum] });
      queryClient.invalidateQueries({ queryKey: ['tl-team-monthly', managerId] });
    },
    onError: (err: unknown) => {
      setErrorMsg(err instanceof Error ? err.message : 'Failed to save target.');
      setFeedback(null);
    },
  });

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.employeeId) {
      setErrorMsg('Pick a team member first.');
      return;
    }
    const employeeIdNum = Number(form.employeeId);
    const target = Number(form.targetDisbursalAmount || 0);
    const logins = Number(form.targetLogins || 0);
    if (target < 0 || logins < 0) {
      setErrorMsg('Targets must be zero or positive.');
      return;
    }
    upsertMutation.mutate({
      employeeId: employeeIdNum,
      targetDisbursalAmount: target,
      targetLogins: logins,
      notes: form.notes?.trim() ? form.notes.trim() : undefined,
    });
  };

  const employeeOptions = useMemo(
    () => [
      { value: '', label: '— Select team member —' },
      ...teamMembers.map((m) => ({
        value: String(m.backendId ?? ''),
        label: `${m.firstName} ${m.lastName}${m.id ? ` (${m.id})` : ''}`,
      })),
    ],
    [teamMembers],
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
        title="Team Leader Monthly Commitment"
        description="Set monthly disbursal targets for your direct reports and track achievement progress."
      />

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="grid gap-3 md:grid-cols-2">
          <Select
            label="Year"
            value={year}
            options={yearOptions}
            onChange={(e) => setYear(e.target.value)}
          />
          <Select
            label="Month"
            value={month}
            options={MONTH_OPTIONS}
            onChange={(e) => setMonth(e.target.value)}
          />
        </div>
      </Card>

      <div className="grid gap-4 md:grid-cols-3">
        <StatsCard
          label="Team Size"
          value={String(teamMembers.length)}
          meta="Active direct reports"
          icon={<Users size={22} />}
        />
        <StatsCard
          label="Total Target"
          value={formatCurrency(totals.totalTarget)}
          meta={`Disbursal target for ${MONTH_OPTIONS[monthNum - 1]?.label ?? ''} ${year}`}
          icon={<Target size={22} />}
        />
        <StatsCard
          label="Avg. Progress"
          value={`${totals.avgPercent}%`}
          meta={`${formatCurrency(totals.totalAchieved)} achieved so far`}
          icon={<TrendingUp size={22} />}
        />
      </div>

      <Card className="border border-slate-200 bg-white shadow-none">
        <h2 className="text-xl font-semibold text-slate-900">Set / Update Target</h2>
        <p className="mt-1 text-sm text-slate-500">
          Pick a team member and save their monthly target. If a target already exists, the form
          prefills with current values — saving updates it in place.
        </p>
        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div className="grid gap-3 md:grid-cols-2">
            <Select
              label="Team Member"
              value={form.employeeId}
              options={employeeOptions}
              onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
            />
            <Input
              label="Target Disbursal (INR)"
              type="number"
              min={0}
              step={1000}
              value={form.targetDisbursalAmount}
              onChange={(e) => setForm({ ...form, targetDisbursalAmount: e.target.value })}
              placeholder="e.g. 1500000"
            />
            <Input
              label="Target Logins"
              type="number"
              min={0}
              value={form.targetLogins}
              onChange={(e) => setForm({ ...form, targetLogins: e.target.value })}
              placeholder="e.g. 40"
            />
            <Input
              label="Notes (optional)"
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              placeholder="Stretch goal, context, etc."
            />
          </div>

          {feedback && (
            <p className="text-sm font-medium text-emerald-600">{feedback}</p>
          )}
          {errorMsg && (
            <p className="text-sm font-medium text-rose-600">{errorMsg}</p>
          )}

          <div className="flex justify-end">
            <Button type="submit" disabled={upsertMutation.isPending}>
              {upsertMutation.isPending ? 'Saving…' : 'Save Target'}
            </Button>
          </div>
        </form>
      </Card>

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-slate-900">Team Targets — {MONTH_OPTIONS[monthNum - 1]?.label} {year}</h2>
          <span className="text-xs text-slate-500">Achieved values come from APPROVED daily commitments.</span>
        </div>
        <div className="mt-5">
          <DataTable
            data={targets}
            emptyTitle={isLoading ? 'Loading…' : 'No targets set yet'}
            emptyDescription={
              isLoading
                ? 'Fetching team monthly targets.'
                : 'Use the form above to set targets for each team member.'
            }
            columns={[
              {
                key: 'employee',
                header: 'Team Member',
                render: (row) => row.employeeName ?? row.employeeCode ?? `#${row.employeeId}`,
              },
              {
                key: 'target',
                header: 'Target ₹',
                render: (row) => formatCurrency(row.targetDisbursalAmount ?? 0),
              },
              {
                key: 'achieved',
                header: 'Achieved ₹',
                render: (row) => formatCurrency(row.achievedDisbursalAmount ?? 0),
              },
              {
                key: 'logins',
                header: 'Target Logins',
                render: (row) => row.targetLogins ?? 0,
              },
              {
                key: 'progress',
                header: 'Progress',
                render: (row) => (
                  <StatusBadge
                    label={`${row.achievedPercent ?? 0}%`}
                    tone={
                      (row.achievedPercent ?? 0) >= 80
                        ? 'success'
                        : (row.achievedPercent ?? 0) >= 50
                        ? 'info'
                        : (row.achievedPercent ?? 0) > 0
                        ? 'warning'
                        : 'neutral'
                    }
                  />
                ),
              },
              {
                key: 'set-by',
                header: 'Set By',
                render: (row) => row.setByName ?? '—',
              },
              {
                key: 'actions',
                header: '',
                render: (row) => (
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setForm({
                        employeeId: String(row.employeeId),
                        targetDisbursalAmount: String(row.targetDisbursalAmount ?? ''),
                        targetLogins: String(row.targetLogins ?? ''),
                        notes: row.notes ?? '',
                      });
                      setFeedback(null);
                      setErrorMsg(null);
                      window.scrollTo({ top: 0, behavior: 'smooth' });
                    }}
                  >
                    Edit
                  </Button>
                ),
              },
            ]}
          />
        </div>
      </Card>
    </div>
  );
};
