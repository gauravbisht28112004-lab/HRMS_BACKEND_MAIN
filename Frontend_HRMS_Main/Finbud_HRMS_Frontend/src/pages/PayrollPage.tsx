import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileText, PlayCircle } from 'lucide-react';
import { DataTable } from '@/components/common/DataTable';
import { FilterBar } from '@/components/common/FilterBar';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Card } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { GeneratePayrollModal } from '@/features/payroll/components/GeneratePayrollModal';
import { api } from '@/services/api';
import { ApiError } from '@/services/contracts';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency } from '@/utils/format';
import type { Employee, PayrollEntry } from '@/types';

const MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

const monthOptions = () =>
  MONTH_NAMES.map((name, index) => ({ label: name, value: String(index + 1) }));

const yearOptions = () => {
  const now = new Date().getFullYear();
  const years: { label: string; value: string }[] = [];
  for (let y = now + 1; y >= now - 3; y -= 1) {
    years.push({ label: String(y), value: String(y) });
  }
  return years;
};

const statusTone = (status: PayrollEntry['backendStatus']): 'success' | 'warning' | 'danger' => {
  if (status === 'PAID') return 'success';
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  return 'warning';
};

const statusLabel = (status: PayrollEntry['backendStatus']) => {
  switch (status) {
    case 'PAID':
      return 'Paid';
    case 'APPROVED':
      return 'Approved';
    case 'REJECTED':
      return 'Rejected';
    case 'DRAFT':
      return 'Draft';
    case 'PENDING':
    default:
      return 'Pending';
  }
};

const SummaryTile = ({ label, value }: { label: string; value: string }) => (
  <Card>
    <p className="text-sm text-slate-500">{label}</p>
    <p className="mt-2 text-2xl font-semibold text-slate-900">{value}</p>
  </Card>
);

const errorMessage = (error: unknown): string => {
  if (error instanceof ApiError) {
    return error.errors?.length ? `${error.message} ${error.errors.join(' ')}` : error.message;
  }
  if (error instanceof Error) return error.message;
  return 'Unexpected error.';
};

export const PayrollPage = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const today = new Date();

  const canEditPayroll = user?.role === 'HR' || user?.role === 'Admin';
  const [month, setMonth] = useState<string>(String(today.getMonth() + 1));
  const [year, setYear] = useState<string>(String(today.getFullYear()));
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'All' | PayrollEntry['backendStatus']>('All');
  const [generateModalOpen, setGenerateModalOpen] = useState(false);
  const [preselectedEmployeeId, setPreselectedEmployeeId] = useState<number | null>(null);
  const [banner, setBanner] = useState<{ tone: 'success' | 'error'; message: string } | null>(null);

  const monthNum = Number(month);
  const yearNum = Number(year);

  // Employees list — needed so we can offer a "Generate for…" picker even when
  // no payroll rows exist yet for the selected month.
  const employeesQuery = useQuery<Employee[]>({
    queryKey: ['employees'],
    queryFn: api.employees.list,
    enabled: Boolean(user),
  });

  const payrollQuery = useQuery<PayrollEntry[]>({
    queryKey: ['payroll', monthNum, yearNum],
    queryFn: () => api.payroll.listByMonth(monthNum, yearNum),
    enabled: canEditPayroll,
  });

  const summaryQuery = useQuery({
    queryKey: ['payroll-summary', monthNum, yearNum],
    queryFn: () => api.payroll.summary(monthNum, yearNum),
    enabled: canEditPayroll,
  });

  const approveMutation = useMutation({
    mutationFn: (payrollId: number) => api.payroll.approve(payrollId),
    onSuccess: async () => {
      setBanner({ tone: 'success', message: 'Payroll approved.' });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['payroll', monthNum, yearNum] }),
        queryClient.invalidateQueries({ queryKey: ['payroll-summary', monthNum, yearNum] }),
      ]);
    },
    onError: (error) => setBanner({ tone: 'error', message: errorMessage(error) }),
  });

  const markPaidMutation = useMutation({
    mutationFn: (payrollId: number) => api.payroll.markPaid(payrollId),
    onSuccess: async () => {
      setBanner({ tone: 'success', message: 'Payroll marked as paid.' });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['payroll', monthNum, yearNum] }),
        queryClient.invalidateQueries({ queryKey: ['payroll-summary', monthNum, yearNum] }),
      ]);
    },
    onError: (error) => setBanner({ tone: 'error', message: errorMessage(error) }),
  });

  const downloadPayslipMutation = useMutation({
    mutationFn: async (entry: PayrollEntry) => {
      if (!entry.backendId) throw new Error('Missing payroll id.');
      const blob = await api.payroll.payslipPdf(entry.backendId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `payslip-${entry.employeeName.replace(/\s+/g, '_')}-${entry.month}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    },
    onError: (error) => setBanner({ tone: 'error', message: errorMessage(error) }),
  });

  const rows = payrollQuery.data ?? [];
  const filtered = useMemo(
    () =>
      rows.filter((entry) => {
        const matchesSearch = entry.employeeName.toLowerCase().includes(search.toLowerCase());
        const matchesStatus = statusFilter === 'All' || entry.backendStatus === statusFilter;
        return matchesSearch && matchesStatus;
      }),
    [rows, search, statusFilter],
  );

  const summary = summaryQuery.data;

  const openGenerateFor = (employeeId: number | null) => {
    setPreselectedEmployeeId(employeeId);
    setGenerateModalOpen(true);
  };

  if (!canEditPayroll) {
    return (
      <div className="space-y-4">
        <PageHeader
          title="Payroll Operations"
          description="Payroll is restricted to HR and Admin users."
        />
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          You don't have permission to view or manage payroll. Please check with HR.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Payroll Operations"
        description="Generate, approve, mark-paid, and download Indian payslips straight from the imported CTC structure."
        action={
          <Button onClick={() => openGenerateFor(null)}>
            <PlayCircle size={16} className="mr-2" />
            Generate Payroll
          </Button>
        }
      />

      <FilterBar>
        <Select
          label="Month"
          value={month}
          onChange={(event) => setMonth(event.target.value)}
          options={monthOptions()}
        />
        <Select
          label="Year"
          value={year}
          onChange={(event) => setYear(event.target.value)}
          options={yearOptions()}
        />
        <Select
          label="Status"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as typeof statusFilter)}
          options={[
            { label: 'All', value: 'All' },
            { label: 'Draft', value: 'DRAFT' },
            { label: 'Pending', value: 'PENDING' },
            { label: 'Approved', value: 'APPROVED' },
            { label: 'Paid', value: 'PAID' },
            { label: 'Rejected', value: 'REJECTED' },
          ]}
        />
        <Input
          placeholder="Search employee"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </FilterBar>

      {banner ? (
        <div
          className={
            banner.tone === 'success'
              ? 'rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700'
              : 'rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700'
          }
        >
          <div className="flex items-center justify-between gap-2">
            <span>{banner.message}</span>
            <button
              onClick={() => setBanner(null)}
              className="text-xs underline decoration-dotted"
              type="button"
            >
              Dismiss
            </button>
          </div>
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-4">
        <SummaryTile label="Runs this month" value={String(summary?.totalEmployees ?? rows.length)} />
        <SummaryTile label="Gross Earnings" value={formatCurrency(summary?.totalGrossEarnings ?? 0)} />
        <SummaryTile
          label="Total Deductions"
          value={formatCurrency(summary?.totalDeductions ?? 0)}
        />
        <SummaryTile label="Net Pay (Paid)" value={formatCurrency(summary?.totalNetPay ?? 0)} />
      </div>

      {payrollQuery.isLoading ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Loading payroll for {MONTH_NAMES[monthNum - 1]} {yearNum}…
        </div>
      ) : payrollQuery.isError ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
          Couldn't load payroll. {errorMessage(payrollQuery.error)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-sm text-slate-600">
          No payroll rows for {MONTH_NAMES[monthNum - 1]} {yearNum}. Click "Generate Payroll" to create the
          first run for an employee — the system reads the active salary structure and attendance to compute
          gross, deductions, LOP, and net pay.
        </div>
      ) : (
        <DataTable
          data={filtered}
          columns={[
            {
              key: 'employee',
              header: 'Employee',
              render: (entry) => (
                <div className="font-medium text-slate-900">{entry.employeeName}</div>
              ),
            },
            { key: 'department', header: 'Department', render: (entry) => entry.department },
            {
              key: 'earnings',
              header: 'Earnings',
              render: (entry) => formatCurrency(entry.earnings),
            },
            {
              key: 'deductions',
              header: 'Deductions',
              render: (entry) => formatCurrency(entry.deductions),
            },
            {
              key: 'net',
              header: 'Net Pay',
              render: (entry) => (
                <span className="font-semibold text-slate-900">
                  {formatCurrency(entry.netSalary)}
                </span>
              ),
            },
            {
              key: 'status',
              header: 'Status',
              render: (entry) => (
                <StatusBadge
                  label={statusLabel(entry.backendStatus)}
                  tone={statusTone(entry.backendStatus)}
                />
              ),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (entry) => {
                const pending =
                  approveMutation.isPending &&
                  approveMutation.variables === entry.backendId;
                const paying =
                  markPaidMutation.isPending &&
                  markPaidMutation.variables === entry.backendId;
                const downloading =
                  downloadPayslipMutation.isPending &&
                  downloadPayslipMutation.variables?.backendId === entry.backendId;

                return (
                  <div className="flex flex-wrap gap-2">
                    {entry.backendStatus === 'PENDING' || entry.backendStatus === 'DRAFT' ? (
                      <Button
                        variant="secondary"
                        onClick={() =>
                          entry.backendId && approveMutation.mutate(entry.backendId)
                        }
                        disabled={pending || !entry.backendId}
                      >
                        {pending ? 'Approving…' : 'Approve'}
                      </Button>
                    ) : null}
                    {entry.backendStatus === 'APPROVED' ? (
                      <Button
                        onClick={() =>
                          entry.backendId && markPaidMutation.mutate(entry.backendId)
                        }
                        disabled={paying || !entry.backendId}
                      >
                        {paying ? 'Marking…' : 'Mark Paid'}
                      </Button>
                    ) : null}
                    <Button
                      variant="ghost"
                      onClick={() => downloadPayslipMutation.mutate(entry)}
                      disabled={downloading || !entry.backendId}
                    >
                      {downloading ? (
                        <>
                          <FileText size={14} className="mr-1" /> Preparing…
                        </>
                      ) : (
                        <>
                          <Download size={14} className="mr-1" /> Payslip
                        </>
                      )}
                    </Button>
                    {entry.employeeId ? (
                      <Button
                        variant="ghost"
                        onClick={() => openGenerateFor(entry.employeeId ?? null)}
                      >
                        Re-run
                      </Button>
                    ) : null}
                  </div>
                );
              },
            },
          ]}
        />
      )}

      <GeneratePayrollModal
        open={generateModalOpen}
        employees={employeesQuery.data ?? []}
        initialEmployeeBackendId={preselectedEmployeeId}
        initialMonth={monthNum}
        initialYear={yearNum}
        onClose={() => {
          setGenerateModalOpen(false);
          setPreselectedEmployeeId(null);
        }}
        onGenerated={async () => {
          setBanner({ tone: 'success', message: 'Payroll generated successfully.' });
          await Promise.all([
            queryClient.invalidateQueries({ queryKey: ['payroll', monthNum, yearNum] }),
            queryClient.invalidateQueries({ queryKey: ['payroll-summary', monthNum, yearNum] }),
          ]);
        }}
      />
    </div>
  );
};
