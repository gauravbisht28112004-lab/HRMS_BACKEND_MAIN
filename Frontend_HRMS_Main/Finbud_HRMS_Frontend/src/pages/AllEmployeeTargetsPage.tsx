import { useMemo, useState } from 'react';
import { IndianRupee, Target, Users } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common/PageHeader';
import { StatsCard } from '@/components/common/StatsCard';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { api } from '@/services/api';
import { formatCurrency } from '@/utils/format';
import type { HierarchyReportRow } from '@/types';

/**
 * Admin/HR "All Employees" target overview — a flat list of every active
 * employee with their monthly target and their OWN approved disbursal for the
 * period. This is the "all employees" option, distinct from the tiered
 * "Managers under me" view on the dashboard.
 */
const NOW = new Date();

const MONTH_OPTIONS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
].map((label, index) => ({ label, value: String(index + 1) }));

const YEAR_OPTIONS = (() => {
  const current = NOW.getFullYear();
  return [current - 1, current, current + 1].map((y) => ({ label: String(y), value: String(y) }));
})();

const tone = (percent: number): 'success' | 'info' | 'warning' =>
  percent >= 80 ? 'success' : percent >= 50 ? 'info' : 'warning';

export const AllEmployeeTargetsPage = () => {
  const [year, setYear] = useState(NOW.getFullYear());
  const [month, setMonth] = useState(NOW.getMonth() + 1);
  const [search, setSearch] = useState('');

  const { data: rows = [], isLoading } = useQuery<HierarchyReportRow[]>({
    queryKey: ['all-employee-targets', year, month],
    queryFn: () => api.hierarchy.allEmployees(year, month),
  });

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(
      (r) =>
        r.employeeName.toLowerCase().includes(q) || r.employeeCode.toLowerCase().includes(q),
    );
  }, [rows, search]);

  const totals = useMemo(
    () =>
      rows.reduce(
        (acc, r) => ({
          target: acc.target + (r.assignedTargetDisbursalAmount ?? 0),
          disbursed: acc.disbursed + (r.teamDisbursedToDate ?? 0),
        }),
        { target: 0, disbursed: 0 },
      ),
    [rows],
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="All Employees — Targets & Disbursal"
        description="Every active employee's monthly target and their own approved disbursal for the period."
      />

      <div className="flex flex-wrap items-end gap-3">
        <Select label="Month" options={MONTH_OPTIONS} value={String(month)} onChange={(e) => setMonth(Number(e.target.value))} />
        <Select label="Year" options={YEAR_OPTIONS} value={String(year)} onChange={(e) => setYear(Number(e.target.value))} />
        <Input label="Search" placeholder="Name or code" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <StatsCard label="Employees" value={String(rows.length)} meta="Active" icon={<Users size={22} />} />
        <StatsCard label="Total Target" value={formatCurrency(totals.target)} meta="Assigned this period" icon={<Target size={22} />} />
        <StatsCard label="Total Disbursed" value={formatCurrency(totals.disbursed)} meta="Approved to date" icon={<IndianRupee size={22} />} />
      </div>

      <Card className="border border-slate-200 bg-white shadow-none">
        {isLoading ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-3 pr-4">Employee</th>
                  <th className="py-3 pr-4">Target (₹)</th>
                  <th className="py-3 pr-4">Disbursed (₹)</th>
                  <th className="py-3 pr-4">Achieved</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr>
                    <td className="py-4 text-sm text-slate-500" colSpan={4}>
                      No employees match your search.
                    </td>
                  </tr>
                ) : (
                  filtered.map((r) => (
                    <tr key={r.employeeId} className="border-b border-slate-100">
                      <td className="py-3 pr-4">
                        <p className="font-medium text-slate-900">{r.employeeName}</p>
                        <p className="text-xs text-slate-500">{r.employeeCode}</p>
                      </td>
                      <td className="py-3 pr-4 text-slate-700">{formatCurrency(r.assignedTargetDisbursalAmount)}</td>
                      <td className="py-3 pr-4 text-slate-700">{formatCurrency(r.teamDisbursedToDate)}</td>
                      <td className="py-3 pr-4">
                        <StatusBadge label={`${r.achievedPercent}%`} tone={tone(r.achievedPercent)} />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};
