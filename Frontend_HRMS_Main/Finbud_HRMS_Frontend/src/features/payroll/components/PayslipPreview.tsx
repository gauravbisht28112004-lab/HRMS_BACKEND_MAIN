import { PayrollEntry } from '@/types';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { formatCurrency } from '@/utils/format';

interface PayslipPreviewProps {
  entry: PayrollEntry;
}

export const PayslipPreview = ({ entry }: PayslipPreviewProps) => (
  <Card className="bg-slate-950 text-white">
    <div className="flex items-start justify-between">
      <div>
        <p className="text-sm uppercase tracking-[0.25em] text-slate-400">Finbud Financial</p>
        <h3 className="mt-3 text-2xl font-semibold">Payslip</h3>
        <p className="mt-2 text-sm text-slate-400">{entry.month}</p>
      </div>
      <Button variant="secondary">Download PDF</Button>
    </div>

    <div className="mt-8 grid gap-4 md:grid-cols-3">
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Employee</p>
        <p className="mt-2 text-lg">{entry.employeeName}</p>
      </div>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Department</p>
        <p className="mt-2 text-lg">{entry.department}</p>
      </div>
      <div>
        <p className="text-xs uppercase tracking-wide text-slate-500">Status</p>
        <p className="mt-2 text-lg">{entry.status}</p>
      </div>
    </div>

    <div className="mt-8 grid gap-4 md:grid-cols-3">
      <div className="rounded-2xl bg-white/5 p-4">
        <p className="text-sm text-slate-400">Earnings</p>
        <p className="mt-2 text-2xl font-semibold">{formatCurrency(entry.earnings)}</p>
      </div>
      <div className="rounded-2xl bg-white/5 p-4">
        <p className="text-sm text-slate-400">Deductions</p>
        <p className="mt-2 text-2xl font-semibold">{formatCurrency(entry.deductions)}</p>
      </div>
      <div className="rounded-2xl bg-accent-500/20 p-4">
        <p className="text-sm text-accent-100">Net Salary</p>
        <p className="mt-2 text-2xl font-semibold text-white">{formatCurrency(entry.netSalary)}</p>
      </div>
    </div>
  </Card>
);
