import { useMemo, useState } from 'react';
import { IndianRupee, Layers, Target, TrendingUp } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
import { AnnouncementBoard } from './AnnouncementBoard';
import type { HierarchyDashboard as HierarchyDashboardData, UserRole } from '@/types';

/**
 * The unified target-flow dashboard for every supervisor level:
 *
 *   Admin -> Manager -> Team Leader -> ATL -> Employee
 *
 * Shows the owner's target (handed down by the level above), their whole
 * team's disbursal to date, and a breakdown of their DIRECT reports only —
 * each with the target the owner assigned them and that report's own
 * whole-team disbursal. Targets are assigned inline, one hop down the chain.
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

const achievementTone = (percent: number): 'success' | 'info' | 'warning' =>
  percent >= 80 ? 'success' : percent >= 50 ? 'info' : 'warning';

/** Announcement audiences only cover the legacy roles; map new ones safely. */
const announcementRole = (role: UserRole | undefined): 'Admin' | 'HR' | 'Team Leader' | 'ATL' | 'Employee' => {
  if (role === 'Admin' || role === 'HR' || role === 'ATL' || role === 'Employee') return role;
  return 'Team Leader';
};

export const HierarchyDashboard = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();

  const [year, setYear] = useState(NOW.getFullYear());
  const [month, setMonth] = useState(NOW.getMonth() + 1);
  const [edits, setEdits] = useState<Record<number, string>>({});
  const [message, setMessage] = useState<{ tone: 'ok' | 'err'; text: string } | null>(null);

  const { data, isLoading, isError } = useQuery<HierarchyDashboardData>({
    queryKey: ['hierarchy-dashboard', year, month],
    queryFn: () => api.hierarchy.myDashboard(year, month),
  });

  const assign = useMutation({
    mutationFn: async ({ reportId, amount }: { reportId: number; amount: number }) => {
      // Preserve any existing logins / notes so we only move the disbursal
      // amount — logins stay owned by the ATL "assign targets" page.
      let targetLogins = 0;
      let notes: string | undefined;
      try {
        const existing = await api.commitments.monthlyTargets.getForEmployee(reportId, year, month);
        targetLogins = existing.targetLogins ?? 0;
        notes = existing.notes ?? undefined;
      } catch {
        /* no existing target — defaults stand */
      }
      return api.commitments.monthlyTargets.upsert(reportId, {
        year,
        month,
        targetDisbursalAmount: amount,
        targetLogins,
        notes,
      });
    },
    onSuccess: (_res, variables) => {
      queryClient.invalidateQueries({ queryKey: ['hierarchy-dashboard'] });
      setEdits((prev) => {
        const next = { ...prev };
        delete next[variables.reportId];
        return next;
      });
      setMessage({ tone: 'ok', text: 'Target saved.' });
    },
    onError: () =>
      setMessage({ tone: 'err', text: 'Could not save. You can only assign targets to your own direct reports.' }),
  });

  const reports = data?.reports ?? [];
  const reportsLabel = data?.reportsRoleLabel || 'Report';
  const canAssign = reports.length > 0;

  const allocationNote = useMemo(() => {
    if (!data || data.myTargetDisbursalAmount <= 0) return null;
    const remaining = data.unallocatedRemaining;
    if (remaining === 0) {
      return { tone: 'ok' as const, text: 'Fully assigned — allocations match your target exactly.' };
    }
    if (remaining > 0) {
      return {
        tone: 'warn' as const,
        text: `You have ${formatCurrency(remaining)} of your target still to assign across your ${reportsLabel}s.`,
      };
    }
    return {
      tone: 'warn' as const,
      text: `You've assigned ${formatCurrency(-remaining)} more than your own target (over-allocated).`,
    };
  }, [data, reportsLabel]);

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${data?.roleLabel ?? 'My'} Dashboard`}
        description="Your monthly target and your whole team's disbursal to date. Assign targets to your direct reports below."
      />

      <div className="flex flex-wrap items-end gap-3">
        <Select
          label="Month"
          options={MONTH_OPTIONS}
          value={String(month)}
          onChange={(e) => setMonth(Number(e.target.value))}
        />
        <Select
          label="Year"
          options={YEAR_OPTIONS}
          value={String(year)}
          onChange={(e) => setYear(Number(e.target.value))}
        />
      </div>

      {isLoading ? (
        <Card className="border border-slate-200 bg-white shadow-none">
          <p className="text-sm text-slate-500">Loading your dashboard…</p>
        </Card>
      ) : isError || !data ? (
        <Card className="border border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-800">Couldn&apos;t load your dashboard</p>
          <p className="mt-1 text-sm text-amber-700">
            Your login may not be linked to an employee record yet. Ask HR to provision your profile.
          </p>
        </Card>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <StatsCard
              label="My Monthly Target"
              value={formatCurrency(data.myTargetDisbursalAmount)}
              meta={data.roleLabel === 'Admin' ? 'Org target (if set)' : 'Assigned by the level above'}
              icon={<Target size={22} />}
            />
            <StatsCard
              label="Team Disbursed (to date)"
              value={formatCurrency(data.teamDisbursedToDate)}
              meta="Approved disbursal, whole team"
              icon={<IndianRupee size={22} />}
            />
            <StatsCard
              label="Team Achievement"
              value={`${data.teamAchievedPercent}%`}
              meta="Disbursed vs my target"
              icon={<TrendingUp size={22} />}
            />
            <StatsCard
              label={`Assigned to ${reportsLabel}s`}
              value={formatCurrency(data.allocatedToReports)}
              meta={`${reports.length} direct ${reportsLabel}${reports.length === 1 ? '' : 's'}`}
              icon={<Layers size={22} />}
            />
          </div>

          {allocationNote ? (
            <div
              className={`rounded-2xl border p-4 text-sm ${
                allocationNote.tone === 'ok'
                  ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
                  : 'border-amber-200 bg-amber-50 text-amber-800'
              }`}
            >
              {allocationNote.text}
            </div>
          ) : null}

          {message ? (
            <div
              className={`rounded-xl border p-3 text-sm ${
                message.tone === 'ok'
                  ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
                  : 'border-rose-200 bg-rose-50 text-rose-800'
              }`}
            >
              {message.text}
            </div>
          ) : null}

          <Card className="border border-slate-200 bg-white shadow-none">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold text-slate-900">My {reportsLabel}s</h2>
              <span className="text-xs text-slate-500">
                Each figure is that {reportsLabel}&apos;s whole team — no overlap between levels.
              </span>
            </div>

            {!canAssign ? (
              <p className="mt-4 text-sm text-slate-500">
                You have no direct reports for this view. Your own target is {formatCurrency(data.myTargetDisbursalAmount)}{' '}
                and your disbursal to date is {formatCurrency(data.teamDisbursedToDate)}.
              </p>
            ) : (
              <div className="mt-5 overflow-x-auto">
                <table className="w-full border-collapse text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-500">
                      <th className="py-3 pr-4">{reportsLabel}</th>
                      <th className="py-3 pr-4">Assigned Target (₹)</th>
                      <th className="py-3 pr-4">Team Disbursed</th>
                      <th className="py-3 pr-4">Achieved</th>
                      <th className="py-3 pr-0" />
                    </tr>
                  </thead>
                  <tbody>
                    {reports.map((row) => {
                      const editValue = edits[row.employeeId];
                      const inputValue =
                        editValue !== undefined ? editValue : String(row.assignedTargetDisbursalAmount ?? 0);
                      const dirty =
                        editValue !== undefined && Number(editValue) !== (row.assignedTargetDisbursalAmount ?? 0);
                      const saving =
                        assign.isPending && assign.variables?.reportId === row.employeeId;
                      return (
                        <tr key={row.employeeId} className="border-b border-slate-100">
                          <td className="py-3 pr-4">
                            <p className="font-medium text-slate-900">{row.employeeName}</p>
                            <p className="text-xs text-slate-500">{row.employeeCode}</p>
                          </td>
                          <td className="py-3 pr-4">
                            <Input
                              type="number"
                              min={0}
                              step={1000}
                              value={inputValue}
                              onChange={(e) =>
                                setEdits((prev) => ({ ...prev, [row.employeeId]: e.target.value }))
                              }
                              className="w-40"
                            />
                          </td>
                          <td className="py-3 pr-4 text-slate-700">
                            {formatCurrency(row.teamDisbursedToDate)}
                          </td>
                          <td className="py-3 pr-4">
                            <StatusBadge
                              label={`${row.achievedPercent}%`}
                              tone={achievementTone(row.achievedPercent)}
                            />
                          </td>
                          <td className="py-3 pr-0 text-right">
                            <Button
                              variant="secondary"
                              disabled={!dirty || saving}
                              onClick={() =>
                                assign.mutate({ reportId: row.employeeId, amount: Number(inputValue) || 0 })
                              }
                            >
                              {saving ? 'Saving…' : 'Save'}
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <AnnouncementBoard role={announcementRole(user?.role)} />
        </>
      )}
    </div>
  );
};
