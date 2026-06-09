import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { PageHeader } from '@/components/common/PageHeader';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import type { MonthlyTarget } from '@/types';

/**
 * Real-data Monthly Targets page (Q1 Phase C frontend, Employee view).
 *
 * <p>Targets are set by the TL (or HR/Admin) — employees only read here.
 * Achieved disbursal is computed server-side as the SUM of APPROVED daily
 * commitments for the chosen period. Progress percent is achieved/target
 * capped at 100.
 */
const NOW = new Date();
const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export const EmployeeMonthlyTargetsPage = () => {
  const user = useAuthStore((state) => state.user);
  const [year, setYear] = useState<number>(NOW.getFullYear());
  const [month, setMonth] = useState<number>(NOW.getMonth() + 1);

  const { data: target, isLoading } = useQuery<MonthlyTarget>({
    queryKey: ['my-monthly-target', year, month],
    queryFn: () => api.commitments.monthlyTargets.getMine(year, month),
    enabled: Boolean(user?.employeeDbId),
  });

  const yearOptions = useMemo(() => {
    const base = NOW.getFullYear();
    return [base - 1, base, base + 1].map((y) => ({ label: String(y), value: String(y) }));
  }, []);

  const monthOptions = MONTH_NAMES.map((name, idx) => ({ label: name, value: String(idx + 1) }));

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

  const targetSet = (target?.targetDisbursalAmount ?? 0) > 0 || (target?.targetLogins ?? 0) > 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Monthly Targets"
        description="Sales targets your TL has set for the month. Achieved figures are computed from your APPROVED daily commitments."
      />

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="grid gap-4 md:grid-cols-2">
          <Select label="Month" value={String(month)} onChange={(event) => setMonth(Number(event.target.value))} options={monthOptions} />
          <Select label="Year" value={String(year)} onChange={(event) => setYear(Number(event.target.value))} options={yearOptions} />
        </div>
      </Card>

      {!targetSet ? (
        <Card className="border border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-800">No target set yet</p>
          <p className="mt-1 text-sm text-amber-700">
            Your TL hasn&apos;t set a target for {MONTH_NAMES[month - 1]} {year}. Ping them to publish one.
          </p>
        </Card>
      ) : null}

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="rounded-2xl bg-[#f8f7f2] p-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xl font-semibold text-slate-900">
                {MONTH_NAMES[month - 1]} {year}
              </p>
              <p className="text-sm text-slate-500">
                {target?.setByName ? `Target set by ${target.setByName}` : 'Awaiting target from TL'}
              </p>
            </div>
            {isLoading ? <span className="text-sm text-slate-400">Loading…</span> : null}
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Card className="border border-slate-200 bg-white shadow-none">
              <p className="text-sm text-slate-500">Target Disbursal</p>
              <p className="mt-2 text-3xl font-semibold text-slate-900">
                {formatCurrency(target?.targetDisbursalAmount ?? 0)}
              </p>
              <p className="mt-2 text-sm text-slate-500">Monthly disbursal goal</p>
            </Card>
            <Card className="border border-slate-200 bg-white shadow-none">
              <p className="text-sm text-slate-500">Achieved So Far</p>
              <p className="mt-2 text-3xl font-semibold text-slate-900">
                {formatCurrency(target?.achievedDisbursalAmount ?? 0)}
              </p>
              <p className="mt-2 text-sm text-slate-500">
                Sum of APPROVED daily commitments
              </p>
            </Card>
            <Card className="border border-slate-200 bg-white shadow-none">
              <p className="text-sm text-slate-500">Target Logins</p>
              <p className="mt-2 text-3xl font-semibold text-slate-900">
                {target?.targetLogins ?? 0}
              </p>
              <p className="mt-2 text-sm text-slate-500">Total login target</p>
            </Card>
            <Card className="border border-brand-200 bg-brand-50 shadow-none">
              <p className="text-sm text-brand-800">Progress</p>
              <p className="mt-2 text-3xl font-semibold text-brand-900">
                {target?.achievedPercent ?? 0}%
              </p>
              <p className="mt-2 text-sm text-brand-800">Achieved vs target</p>
            </Card>
          </div>

          {target?.notes ? (
            <p className="mt-5 rounded-2xl bg-white p-4 text-sm text-slate-600">
              <span className="font-semibold text-slate-700">Note from TL:</span> {target.notes}
            </p>
          ) : null}
        </div>
      </Card>

      <Card className="border border-slate-200 bg-white shadow-none">
        <h3 className="text-xl font-semibold text-slate-900">Tips to hit your target</h3>
        <ul className="mt-4 list-disc space-y-2 pl-5 text-sm text-slate-600">
          <li>Set your daily commitment every morning — break the monthly goal into daily wins.</li>
          <li>Log hourly updates so you can see the day&apos;s pace and course-correct.</li>
          <li>Submit each day&apos;s commitment for TL approval — only APPROVED rows count toward the achieved total.</li>
          <li>Watch the leaderboard for friendly competition with your peers.</li>
        </ul>
      </Card>
    </div>
  );
};
