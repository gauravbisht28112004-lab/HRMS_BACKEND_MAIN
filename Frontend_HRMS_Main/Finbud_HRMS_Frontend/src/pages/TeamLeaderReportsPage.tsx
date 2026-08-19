import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, FileSpreadsheet, IndianRupee, Phone, Users } from 'lucide-react';
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
import type { CommitmentStatus, DailyCommitment, Employee } from '@/types';

/**
 * Team Leader Reports — real-data table over the team's daily commitments
 * with filter chips + Excel download for offline analysis.
 *
 * <p>Replaces the prior mock-data fallback that filtered local
 * {@code performanceMetrics} fixtures by team-leader name. Now sources its
 * data from the V15 daily_commitments backend tables via:
 *   - {@code api.commitments.daily.listTeamForDate} (live snapshot)
 *   - {@code api.commitmentReports.teamXlsx} (range export)
 *
 * <p>Design choice (option c): in-screen table for live read + Download
 * button for Excel range export. The TL picks a single date for the table
 * and a date range for the export.
 */
/**
 * Local-calendar ISO date. `toISOString()` converts to UTC first, which in
 * IST (UTC+5:30) rolls the date back a day for anything before 05:30 — that
 * made the export default to "yesterday" and silently drop today's rows.
 */
const toLocalIso = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

const NOW = new Date();
const TODAY_ISO = toLocalIso(NOW);
const startOfMonth = toLocalIso(new Date(NOW.getFullYear(), NOW.getMonth(), 1));

const STATUS_FILTER_OPTIONS: { value: CommitmentStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const commitmentTone: Record<CommitmentStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
};

export const TeamLeaderReportsPage = () => {
  const user = useAuthStore((state) => state.user);
  const managerId = user?.employeeDbId;

  const [snapshotDate, setSnapshotDate] = useState(TODAY_ISO);
  const [statusFilter, setStatusFilter] = useState<CommitmentStatus | 'ALL'>('ALL');
  const [employeeFilter, setEmployeeFilter] = useState('');
  const [exportStart, setExportStart] = useState(startOfMonth);
  const [exportEnd, setExportEnd] = useState(TODAY_ISO);
  const [exportError, setExportError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  // -- Direct reports (drives the employee filter) --
  const { data: teamMembers = [] } = useQuery<Employee[]>({
    queryKey: ['my-team', managerId],
    queryFn: () => (managerId ? api.employees.listByManager(managerId) : Promise.resolve([])),
    enabled: Boolean(managerId),
  });

  // -- Daily-commitment snapshot for the picked date --
  const { data: rows = [], isLoading } = useQuery<DailyCommitment[]>({
    queryKey: ['tl-reports-snapshot', managerId, snapshotDate],
    queryFn: () =>
      managerId
        ? api.commitments.daily.listTeamForDate(managerId, snapshotDate)
        : Promise.resolve([]),
    enabled: Boolean(managerId),
  });

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      if (statusFilter !== 'ALL' && r.status !== statusFilter) return false;
      if (employeeFilter && String(r.employeeId) !== employeeFilter) return false;
      return true;
    });
  }, [rows, statusFilter, employeeFilter]);

  const totals = useMemo(() => {
    const totalTargetCalls = filtered.reduce((s, r) => s + (r.targetCalls ?? 0), 0);
    const totalActualCalls = filtered.reduce((s, r) => s + (r.actualCalls ?? 0), 0);
    const totalTargetDisbursal = filtered.reduce((s, r) => s + (r.targetDisbursalAmount ?? 0), 0);
    const totalActualDisbursal = filtered.reduce((s, r) => s + (r.actualDisbursalAmount ?? 0), 0);
    return { totalTargetCalls, totalActualCalls, totalTargetDisbursal, totalActualDisbursal };
  }, [filtered]);

  /**
   * Options come from the fetched rows, not from `teamMembers`.
   *
   * The snapshot now spans the whole reporting subtree (employees under this
   * user's TLs and ATLs), while `listByManager` only returns direct reports.
   * Building the dropdown from direct reports would list supervisors who never
   * file commitments and omit every employee who actually appears in the table
   * — selecting any option would return zero rows. Deriving from the rows keeps
   * the filter and the data provably in sync.
   */
  const employeeOptions = useMemo(() => {
    const seen = new Map<string, string>();
    rows.forEach((r) => {
      const value = String(r.employeeId ?? '');
      if (!value || seen.has(value)) return;
      const code = r.employeeCode ? ` (${r.employeeCode})` : '';
      seen.set(value, `${r.employeeName ?? `#${r.employeeId}`}${code}`);
    });
    return [
      { value: '', label: 'All team members' },
      ...[...seen.entries()]
        .map(([value, label]) => ({ value, label }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    ];
  }, [rows]);

  /** Stream the team's daily-commitment Excel export and trigger a browser download. */
  const downloadTeamReport = async () => {
    if (!managerId) return;
    if (!exportStart || !exportEnd) {
      setExportError('Pick a start and end date for the export.');
      return;
    }
    if (exportStart > exportEnd) {
      setExportError('Start date must be before end date.');
      return;
    }
    try {
      setExporting(true);
      setExportError(null);
      const blob = await api.commitmentReports.teamXlsx(managerId, exportStart, exportEnd);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `team-report-${exportStart}-to-${exportEnd}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setExportError(err instanceof Error ? err.message : 'Failed to download report.');
    } finally {
      setExporting(false);
    }
  };

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
        title="Team Reports"
        description="Live commitment snapshot for your team plus Excel export for any date range."
      />

      {/* Filters card */}
      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="grid gap-3 md:grid-cols-3">
          <Input
            label="Snapshot Date"
            type="date"
            value={snapshotDate}
            max={TODAY_ISO}
            onChange={(e) => setSnapshotDate(e.target.value)}
          />
          <Select
            label="Team Member"
            value={employeeFilter}
            options={employeeOptions}
            onChange={(e) => setEmployeeFilter(e.target.value)}
          />
          <Select
            label="Status"
            value={statusFilter}
            options={STATUS_FILTER_OPTIONS.map((opt) => ({ value: opt.value, label: opt.label }))}
            onChange={(e) => setStatusFilter(e.target.value as CommitmentStatus | 'ALL')}
          />
        </div>
      </Card>

      <div className="grid gap-4 md:grid-cols-4">
        {/* Direct reports only — deliberately not the subtree size. For a
            Manager these are the Team Leaders, whose own teams roll up into
            the table below. Labelled precisely so the two numbers aren't
            mistaken for each other. */}
        <StatsCard
          label="Direct Reports"
          value={String(teamMembers.length)}
          meta="Supervisors/employees reporting to you"
          icon={<Users size={22} />}
        />
        <StatsCard
          label="Calls — Actual / Target"
          value={`${totals.totalActualCalls} / ${totals.totalTargetCalls}`}
          meta={`On ${snapshotDate}`}
          icon={<Phone size={22} />}
        />
        <StatsCard
          label="Disbursal — Actual"
          value={formatCurrency(totals.totalActualDisbursal)}
          meta={`Target: ${formatCurrency(totals.totalTargetDisbursal)}`}
          icon={<IndianRupee size={22} />}
        />
        <StatsCard
          label="Rows in View"
          value={String(filtered.length)}
          meta={`${rows.length} fetched, ${filtered.length} after filters`}
          icon={<FileSpreadsheet size={22} />}
        />
      </div>

      {/* Daily snapshot table */}
      <Card className="border border-slate-200 bg-white shadow-none">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-slate-900">Daily Snapshot — {snapshotDate}</h2>
        </div>
        <div className="mt-5">
          <DataTable
            data={filtered}
            emptyTitle={isLoading ? 'Loading…' : 'No commitments match'}
            emptyDescription={
              isLoading
                ? 'Fetching the team snapshot.'
                : 'Try a different date or clear the filters above.'
            }
            columns={[
              {
                key: 'employee',
                header: 'Team Member',
                render: (row) => row.employeeName ?? row.employeeCode ?? `#${row.employeeId}`,
              },
              {
                key: 'targetCalls',
                header: 'Target Calls',
                render: (row) => row.targetCalls ?? 0,
              },
              {
                key: 'actualCalls',
                header: 'Actual Calls',
                render: (row) => row.actualCalls ?? 0,
              },
              {
                key: 'targetDisbursal',
                header: 'Target ₹',
                render: (row) => formatCurrency(row.targetDisbursalAmount ?? 0),
              },
              {
                key: 'actualDisbursal',
                header: 'Actual ₹',
                render: (row) => formatCurrency(row.actualDisbursalAmount ?? 0),
              },
              {
                key: 'status',
                header: 'Status',
                render: (row) => (
                  <StatusBadge label={row.status} tone={commitmentTone[row.status]} />
                ),
              },
              {
                key: 'approvedBy',
                header: 'Approved By',
                render: (row) => row.approvedByName ?? '—',
              },
            ]}
          />
        </div>
      </Card>

      {/* Export card */}
      <Card className="border border-slate-200 bg-white shadow-none">
        <h2 className="text-xl font-semibold text-slate-900">Excel Export</h2>
        <p className="mt-1 text-sm text-slate-500">
          Download a full commitment report for your team between two dates. Includes targets,
          actuals, status, approver, and notes for every entry.
        </p>
        <div className="mt-5 grid gap-3 md:grid-cols-3">
          <Input
            label="From"
            type="date"
            value={exportStart}
            max={exportEnd || TODAY_ISO}
            onChange={(e) => setExportStart(e.target.value)}
          />
          <Input
            label="To"
            type="date"
            value={exportEnd}
            min={exportStart}
            max={TODAY_ISO}
            onChange={(e) => setExportEnd(e.target.value)}
          />
          <div className="flex items-end">
            <Button
              type="button"
              onClick={downloadTeamReport}
              disabled={exporting}
              className="w-full"
            >
              <span className="inline-flex items-center gap-2">
                <Download size={16} />
                {exporting ? 'Preparing…' : 'Download Excel'}
              </span>
            </Button>
          </div>
        </div>
        {exportError && <p className="mt-3 text-sm font-medium text-rose-600">{exportError}</p>}
      </Card>
    </div>
  );
};
