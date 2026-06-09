import { FormEvent, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { api } from '@/services/api';
import type { Employee, PayrollEntry } from '@/types';

interface GeneratePayrollModalProps {
  open: boolean;
  employees: Employee[];
  /** Pre-selected employee (e.g. when clicking "Generate" from a row). */
  initialEmployeeBackendId?: number | null;
  /** Month index 1..12. */
  initialMonth?: number;
  initialYear?: number;
  onClose: () => void;
  onGenerated: (entry: PayrollEntry) => void;
}

const MONTH_OPTIONS = [
  { label: 'January', value: '1' },
  { label: 'February', value: '2' },
  { label: 'March', value: '3' },
  { label: 'April', value: '4' },
  { label: 'May', value: '5' },
  { label: 'June', value: '6' },
  { label: 'July', value: '7' },
  { label: 'August', value: '8' },
  { label: 'September', value: '9' },
  { label: 'October', value: '10' },
  { label: 'November', value: '11' },
  { label: 'December', value: '12' },
];

const yearOptions = () => {
  const now = new Date().getFullYear();
  const years: { label: string; value: string }[] = [];
  for (let y = now + 1; y >= now - 3; y -= 1) {
    years.push({ label: String(y), value: String(y) });
  }
  return years;
};

export const GeneratePayrollModal = ({
  open,
  employees,
  initialEmployeeBackendId,
  initialMonth,
  initialYear,
  onClose,
  onGenerated,
}: GeneratePayrollModalProps) => {
  const [employeeId, setEmployeeId] = useState<string>('');
  const [month, setMonth] = useState<string>(String(initialMonth ?? new Date().getMonth() + 1));
  const [year, setYear] = useState<string>(String(initialYear ?? new Date().getFullYear()));
  const [lopDays, setLopDays] = useState<string>('');
  const [incentivesOverride, setIncentivesOverride] = useState<string>('');
  const [adjustments, setAdjustments] = useState<string>('');
  const [adjustmentReason, setAdjustmentReason] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Reset state whenever the modal is (re)opened.
  useEffect(() => {
    if (!open) return;
    setEmployeeId(initialEmployeeBackendId ? String(initialEmployeeBackendId) : '');
    setMonth(String(initialMonth ?? new Date().getMonth() + 1));
    setYear(String(initialYear ?? new Date().getFullYear()));
    setLopDays('');
    setIncentivesOverride('');
    setAdjustments('');
    setAdjustmentReason('');
    setError(null);
    setIsSubmitting(false);
  }, [open, initialEmployeeBackendId, initialMonth, initialYear]);

  if (!open) return null;

  const employeeOptions = [
    { label: 'Select employee…', value: '' },
    ...employees
      .filter((e) => e.backendId != null)
      .map((e) => ({
        label: `${e.firstName} ${e.lastName} — ${e.id}`,
        value: String(e.backendId),
      })),
  ];

  const parseOrUndef = (value: string): number | undefined => {
    if (value.trim() === '') return undefined;
    const num = Number(value);
    return Number.isFinite(num) ? num : undefined;
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    const empId = Number(employeeId);
    const monthNum = Number(month);
    const yearNum = Number(year);
    if (!empId || !monthNum || !yearNum) {
      setError('Please pick an employee, month, and year.');
      return;
    }
    setIsSubmitting(true);
    try {
      const result = await api.payroll.generate({
        employeeId: empId,
        month: monthNum,
        year: yearNum,
        lopDays: parseOrUndef(lopDays),
        incentivesOverride: parseOrUndef(incentivesOverride),
        adjustments: parseOrUndef(adjustments),
        adjustmentReason: adjustmentReason.trim() || undefined,
      });
      onGenerated(result);
      onClose();
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to generate payroll.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm">
      <Card className="w-full max-w-2xl">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-brand-600">Payroll</p>
            <h3 className="mt-1 text-xl font-semibold text-slate-900">Generate Payroll</h3>
            <p className="mt-1 text-sm text-slate-500">
              Pick the employee + month and optionally override LOP days, incentives, or apply an ad-hoc
              adjustment. The system will read the active salary structure and attendance to compute the rest.
            </p>
          </div>
        </div>

        {error ? (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {error}
          </div>
        ) : null}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <Select
              label="Employee *"
              value={employeeId}
              onChange={(event) => setEmployeeId(event.target.value)}
              options={employeeOptions}
              required
            />
            <div className="grid gap-4 md:grid-cols-2">
              <Select
                label="Month *"
                value={month}
                onChange={(event) => setMonth(event.target.value)}
                options={MONTH_OPTIONS}
                required
              />
              <Select
                label="Year *"
                value={year}
                onChange={(event) => setYear(event.target.value)}
                options={yearOptions()}
                required
              />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <Input
              label="LOP Days (override)"
              type="number"
              min={0}
              step="0.5"
              value={lopDays}
              onChange={(event) => setLopDays(event.target.value)}
              placeholder="Blank = use attendance"
            />
            <Input
              label="Incentives (override)"
              type="number"
              min={0}
              step="0.01"
              value={incentivesOverride}
              onChange={(event) => setIncentivesOverride(event.target.value)}
              placeholder="Blank = use structure"
            />
            <Input
              label="Adjustment (+/-)"
              type="number"
              step="0.01"
              value={adjustments}
              onChange={(event) => setAdjustments(event.target.value)}
              placeholder="e.g. -500 or 2000"
            />
          </div>

          <Input
            label="Adjustment Reason"
            value={adjustmentReason}
            onChange={(event) => setAdjustmentReason(event.target.value)}
            placeholder="Shown on the payslip when adjustment is non-zero"
          />

          <div className="flex items-center justify-end gap-3 border-t border-slate-100 pt-4">
            <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Generating…' : 'Generate Payroll'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};
