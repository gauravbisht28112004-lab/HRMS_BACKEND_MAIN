import { useQuery } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { api, type BackendSalaryStructureResponse } from '@/services/api';
import { ApiError } from '@/services/contracts';
import { formatCurrency } from '@/utils/format';

interface SalarySnapshotCardProps {
  /** Backend numeric id of the employee (salary API is keyed by this). */
  employeeBackendId: number | null | undefined;
}

const Row = ({ label, value }: { label: string; value: string }) => (
  <div className="flex items-center justify-between border-b border-slate-100 py-2 last:border-b-0">
    <span className="text-sm text-slate-500">{label}</span>
    <span className="text-sm font-semibold text-slate-900">{value}</span>
  </div>
);

const formatStructureType = (t: string | null | undefined) => {
  if (!t) return '—';
  switch (t) {
    case 'CONTRACT':
      return 'Contract';
    case 'MANAGEMENT':
      return 'Management';
    case 'HIGHLY_SKILLED':
      return 'Highly Skilled';
    default:
      return t;
  }
};

const formatOrDash = (value: number | null | undefined) =>
  value != null ? formatCurrency(value) : '—';

/**
 * Read-only salary snapshot for the Employee Profile page. Only Admin/HR
 * should render this (the parent is responsible for gating by role).
 * Shows monthly CTC, NTH, statutory deductions, incentives. For editing,
 * use the full Salary tab in the Employees management flow.
 */
export const SalarySnapshotCard = ({ employeeBackendId }: SalarySnapshotCardProps) => {
  const { data, isLoading, isError, error } = useQuery<BackendSalaryStructureResponse | null>({
    queryKey: ['salary-structure', employeeBackendId],
    enabled: employeeBackendId != null,
    retry: false,
    queryFn: async () => {
      if (employeeBackendId == null) return null;
      try {
        return await api.salary.get(employeeBackendId);
      } catch (err) {
        if (err instanceof ApiError && (err.statusCode === 404 || /not found/i.test(err.message))) {
          return null;
        }
        throw err;
      }
    },
  });

  return (
    <Card>
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold uppercase tracking-wider text-brand-700">Salary Structure</h3>
          <p className="mt-1 text-xs text-slate-500">
            Current CTC breakdown used for payroll. Edit via Employee Management → Salary tab.
          </p>
        </div>
      </div>

      {isLoading ? (
        <div className="mt-4 text-sm text-slate-500">Loading salary structure…</div>
      ) : isError ? (
        <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          Couldn't load salary structure.
          {error instanceof Error ? ` ${error.message}` : null}
        </div>
      ) : !data ? (
        <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-600">
          No salary structure on file yet. Open this employee in Employee Management to configure CTC, NTH,
          PF, ESI, TDS, and LWF.
        </div>
      ) : (
        <div className="mt-4 grid gap-6 md:grid-cols-2">
          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500">Headline</h4>
            <div className="mt-2">
              <Row label="Structure Type" value={formatStructureType(data.structureType)} />
              <Row label="Monthly Gross CTC" value={formatOrDash(data.monthlyGrossCtc)} />
              <Row label="Net Take Home (NTH)" value={formatOrDash(data.nth)} />
              <Row label="Annual CTC" value={formatOrDash(data.annualCtc ?? data.monthlyCtc)} />
              <Row label="Incentives" value={formatOrDash(data.incentives)} />
              <Row
                label="Effective From"
                value={data.effectiveFrom ? data.effectiveFrom.slice(0, 10) : '—'}
              />
            </div>
          </div>

          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500">Statutory</h4>
            <div className="mt-2">
              <Row label="Employer PF" value={formatOrDash(data.employerPf)} />
              <Row label="Employee PF" value={formatOrDash(data.employeePf)} />
              <Row label="Employer ESI" value={formatOrDash(data.employerEsi)} />
              <Row label="Employee ESI" value={formatOrDash(data.employeeEsi)} />
              <Row label="LWF" value={formatOrDash(data.lwfAmount)} />
              <Row label="TDS" value={formatOrDash(data.tdsAmount)} />
              <Row label="Other Deductions" value={formatOrDash(data.otherDeductions)} />
            </div>
          </div>
        </div>
      )}
    </Card>
  );
};
