import { useMemo, useState } from 'react';
import { Trophy } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common/PageHeader';
import { Card } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { api } from '@/services/api';
import { formatCurrency, getInitials } from '@/utils/format';
import type { LeaderboardEntry } from '@/types';

/**
 * Real-data sales-disbursal leaderboard (Q3). Aggregates SUM of APPROVED
 * daily commitments per employee over the chosen month, ranked DESC.
 */
const NOW = new Date();
const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export const LeaderboardPage = () => {
  const [year, setYear] = useState<number>(NOW.getFullYear());
  const [month, setMonth] = useState<number>(NOW.getMonth() + 1);

  const { data: entries = [], isLoading } = useQuery<LeaderboardEntry[]>({
    queryKey: ['leaderboard-disbursal', year, month],
    queryFn: () => api.leaderboard.monthlyDisbursal({ year, month }),
  });

  const yearOptions = useMemo(() => {
    const base = NOW.getFullYear();
    return [base - 1, base, base + 1].map((y) => ({ label: String(y), value: String(y) }));
  }, []);

  const monthOptions = MONTH_NAMES.map((name, idx) => ({ label: name, value: String(idx + 1) }));

  return (
    <div className="space-y-6">
      <PageHeader
        title="Disbursal Leaderboard"
        description="Ranked by total APPROVED daily disbursal for the chosen month. Pending or rejected commitments don't count."
      />

      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Month"
            value={String(month)}
            onChange={(event) => setMonth(Number(event.target.value))}
            options={monthOptions}
          />
          <Select
            label="Year"
            value={String(year)}
            onChange={(event) => setYear(Number(event.target.value))}
            options={yearOptions}
          />
        </div>
      </Card>

      <Card className="border border-slate-200 bg-white shadow-none">
        {isLoading ? (
          <p className="text-sm text-slate-500">Loading leaderboard…</p>
        ) : entries.length === 0 ? (
          <div className="rounded-2xl bg-amber-50 p-4 text-sm text-amber-800">
            No approved disbursal yet for {MONTH_NAMES[month - 1]} {year}. The leaderboard fills up as TLs
            approve their team&apos;s daily commitments.
          </div>
        ) : (
          <div className="space-y-4">
            {entries.map((entry) => (
              <div
                key={entry.employeeId}
                className="flex flex-col gap-4 rounded-2xl border border-slate-100 bg-slate-50 p-4 md:flex-row md:items-center md:justify-between"
              >
                <div className="flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-900 font-semibold text-white">
                    {getInitials(entry.employeeName ?? '?')}
                  </div>
                  <div>
                    <p className="font-semibold text-slate-900">
                      #{entry.rank} {entry.employeeName ?? entry.employeeCode}
                    </p>
                    <p className="text-sm text-slate-500">
                      {entry.department ? entry.department : 'Department —'}
                      {entry.employeeCode ? ` · ${entry.employeeCode}` : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3 rounded-2xl bg-white px-4 py-3">
                  <div className="rounded-xl bg-brand-50 p-2 text-brand-700">
                    <Trophy size={18} />
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Approved Disbursal</p>
                    <p className="text-lg font-semibold text-slate-900">
                      {formatCurrency(entry.totalDisbursalAmount)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};
