import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import {
  api,
  type BackendSalaryStructureRequest,
  type BackendSalaryStructureResponse,
  type SalaryStructureType,
} from '@/services/api';
import { ApiError } from '@/services/contracts';
import { formatCurrency } from '@/utils/format';

interface SalaryTabProps {
  /**
   * Backend primary key of the employee whose salary structure we're
   * viewing/editing. `null` means the parent hasn't saved the employee
   * yet — we show a "save employee first" prompt in that case because
   * the salary API is keyed by employee id.
   */
  employeeBackendId: number | null;
  /**
   * Only Admin/HR should land here. Set by the parent form based on
   * `useAuthStore().user.role`.
   */
  canEdit: boolean;
}

type FormState = {
  structureType: SalaryStructureType;
  monthlyGrossCtc: string;
  nth: string;
  tdsAmount: string;
  tdsRatePercent: string;
  employerPf: string;
  employeePf: string;
  employerEsi: string;
  employeeEsi: string;
  lwfAmount: string;
  incentives: string;
  otherDeductions: string;
  numOfMonths: string;
  annualCtc: string;
  effectiveFrom: string;
  effectiveTo: string;
};

const STRUCTURE_TYPE_OPTIONS: { label: string; value: SalaryStructureType }[] = [
  { label: 'Contract', value: 'CONTRACT' },
  { label: 'Management', value: 'MANAGEMENT' },
  { label: 'Highly Skilled', value: 'HIGHLY_SKILLED' },
];

const todayIso = () => new Date().toISOString().slice(0, 10);

const emptyForm = (): FormState => ({
  structureType: 'CONTRACT',
  monthlyGrossCtc: '',
  nth: '',
  tdsAmount: '',
  tdsRatePercent: '',
  employerPf: '',
  employeePf: '',
  employerEsi: '',
  employeeEsi: '',
  lwfAmount: '',
  incentives: '',
  otherDeductions: '',
  numOfMonths: '12',
  annualCtc: '',
  effectiveFrom: todayIso(),
  effectiveTo: '',
});

const formFromResponse = (res: BackendSalaryStructureResponse): FormState => ({
  structureType: (res.structureType ?? 'CONTRACT') as SalaryStructureType,
  monthlyGrossCtc: res.monthlyGrossCtc != null ? String(res.monthlyGrossCtc) : '',
  nth: res.nth != null ? String(res.nth) : '',
  tdsAmount: res.tdsAmount != null ? String(res.tdsAmount) : '',
  tdsRatePercent: res.tdsRatePercent != null ? String(res.tdsRatePercent) : '',
  employerPf: res.employerPf != null ? String(res.employerPf) : '',
  employeePf: res.employeePf != null ? String(res.employeePf) : '',
  employerEsi: res.employerEsi != null ? String(res.employerEsi) : '',
  employeeEsi: res.employeeEsi != null ? String(res.employeeEsi) : '',
  lwfAmount: res.lwfAmount != null ? String(res.lwfAmount) : '',
  incentives: res.incentives != null ? String(res.incentives) : '',
  otherDeductions: res.otherDeductions != null ? String(res.otherDeductions) : '',
  numOfMonths: res.numOfMonths != null ? String(res.numOfMonths) : '12',
  annualCtc: res.annualCtc != null ? String(res.annualCtc) : '',
  effectiveFrom: res.effectiveFrom ? res.effectiveFrom.slice(0, 10) : todayIso(),
  effectiveTo: res.effectiveTo ? res.effectiveTo.slice(0, 10) : '',
});

const parseNumOrNull = (value: string): number | null => {
  if (value == null || value.trim() === '') return null;
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
};

const parseNumOrZero = (value: string): number => {
  const parsed = parseNumOrNull(value);
  return parsed ?? 0;
};

const formToRequest = (form: FormState): BackendSalaryStructureRequest => ({
  structureType: form.structureType,
  monthlyGrossCtc: parseNumOrZero(form.monthlyGrossCtc),
  nth: parseNumOrZero(form.nth),
  tdsAmount: parseNumOrNull(form.tdsAmount),
  tdsRatePercent: parseNumOrNull(form.tdsRatePercent),
  employerPf: parseNumOrNull(form.employerPf),
  employeePf: parseNumOrNull(form.employeePf),
  employerEsi: parseNumOrNull(form.employerEsi),
  employeeEsi: parseNumOrNull(form.employeeEsi),
  lwfAmount: parseNumOrNull(form.lwfAmount),
  incentives: parseNumOrNull(form.incentives),
  otherDeductions: parseNumOrNull(form.otherDeductions),
  numOfMonths: parseNumOrNull(form.numOfMonths),
  annualCtc: parseNumOrNull(form.annualCtc),
  effectiveFrom: form.effectiveFrom,
  effectiveTo: form.effectiveTo ? form.effectiveTo : null,
  isActive: true,
});

const SummaryStat = ({ label, value }: { label: string; value: string }) => (
  <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
    <p className="text-xs uppercase tracking-wider text-slate-500">{label}</p>
    <p className="mt-1 text-lg font-semibold text-slate-900">{value}</p>
  </div>
);

export const SalaryTab = ({ employeeBackendId, canEdit }: SalaryTabProps) => {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<FormState>(emptyForm());
  const [bannerError, setBannerError] = useState<string | null>(null);
  const [bannerSuccess, setBannerSuccess] = useState<string | null>(null);

  const salaryQuery = useQuery<BackendSalaryStructureResponse | null>({
    queryKey: ['salary-structure', employeeBackendId],
    enabled: employeeBackendId != null,
    retry: false,
    queryFn: async () => {
      if (employeeBackendId == null) return null;
      try {
        return await api.salary.get(employeeBackendId);
      } catch (error) {
        if (error instanceof ApiError && (error.statusCode === 404 || /not found/i.test(error.message))) {
          // No structure yet — treat as empty so HR can create one.
          return null;
        }
        throw error;
      }
    },
  });

  useEffect(() => {
    if (salaryQuery.isSuccess) {
      setForm(salaryQuery.data ? formFromResponse(salaryQuery.data) : emptyForm());
    }
  }, [salaryQuery.isSuccess, salaryQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (payload: BackendSalaryStructureRequest) => {
      if (employeeBackendId == null) {
        throw new Error('Employee must be saved before a salary structure can be defined.');
      }
      if (salaryQuery.data?.id) {
        return api.salary.update(salaryQuery.data.id, payload);
      }
      return api.salary.create(employeeBackendId, payload);
    },
    onSuccess: async (response) => {
      setBannerError(null);
      setBannerSuccess('Salary structure saved successfully.');
      setForm(formFromResponse(response));
      await queryClient.invalidateQueries({ queryKey: ['salary-structure', employeeBackendId] });
    },
    onError: (error: unknown) => {
      setBannerSuccess(null);
      if (error instanceof ApiError) {
        setBannerError(error.errors?.length ? `${error.message} ${error.errors.join(' ')}` : error.message);
      } else if (error instanceof Error) {
        setBannerError(error.message);
      } else {
        setBannerError('Unable to save the salary structure. Please try again.');
      }
    },
  });

  const updateField =
    <K extends keyof FormState>(key: K) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((prev) => ({ ...prev, [key]: event.target.value } as FormState));
    };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBannerError(null);
    setBannerSuccess(null);
    await saveMutation.mutateAsync(formToRequest(form));
  };

  // Quick derived summary so HR can sanity-check the structure before saving.
  const monthlyCtc = useMemo(() => parseNumOrZero(form.monthlyGrossCtc), [form.monthlyGrossCtc]);
  const nth = useMemo(() => parseNumOrZero(form.nth), [form.nth]);
  const annualCtc = useMemo(() => {
    const explicit = parseNumOrNull(form.annualCtc);
    if (explicit != null) return explicit;
    const months = parseNumOrNull(form.numOfMonths) ?? 12;
    return monthlyCtc * months;
  }, [form.annualCtc, form.numOfMonths, monthlyCtc]);

  if (employeeBackendId == null) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-sm text-slate-600">
        Save the employee first — once the record exists you can define the salary structure here
        (Monthly CTC, NTH, PF, ESI, TDS, LWF, incentives). This is also populated automatically from
        the Noida Master Employee import.
      </div>
    );
  }

  if (!canEdit) {
    return (
      <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
        You don't have permission to view salary details. Ask an Admin or HR to open this record.
      </div>
    );
  }

  if (salaryQuery.isLoading) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
        Loading salary structure…
      </div>
    );
  }

  if (salaryQuery.isError) {
    return (
      <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
        Couldn't load the salary structure.
        {salaryQuery.error instanceof Error ? ` ${salaryQuery.error.message}` : null}
      </div>
    );
  }

  const hasExistingStructure = Boolean(salaryQuery.data?.id);

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="flex flex-col gap-1">
        <h4 className="text-sm font-semibold uppercase tracking-wider text-brand-700">Salary Structure (CTC)</h4>
        <p className="text-xs text-slate-500">
          {hasExistingStructure
            ? 'Editing the active structure will overwrite the existing record. Use this screen when a salary revision happens.'
            : 'No salary structure on file yet. Enter the CTC breakdown below — this is what payroll uses to compute monthly earnings and deductions.'}
        </p>
      </div>

      {bannerError ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {bannerError}
        </div>
      ) : null}
      {bannerSuccess ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {bannerSuccess}
        </div>
      ) : null}

      <div className="grid gap-3 md:grid-cols-3">
        <SummaryStat label="Monthly Gross CTC" value={formatCurrency(monthlyCtc)} />
        <SummaryStat label="Net Take Home (NTH)" value={formatCurrency(nth)} />
        <SummaryStat label="Annual CTC" value={formatCurrency(annualCtc)} />
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <Select
          label="Structure Type *"
          value={form.structureType}
          onChange={updateField('structureType')}
          options={STRUCTURE_TYPE_OPTIONS}
        />
        <Input
          label="Monthly Gross CTC *"
          type="number"
          min={0}
          step="0.01"
          value={form.monthlyGrossCtc}
          onChange={updateField('monthlyGrossCtc')}
          required
        />
        <Input
          label="Net Take Home (NTH) *"
          type="number"
          min={0}
          step="0.01"
          value={form.nth}
          onChange={updateField('nth')}
          required
        />
        <Input
          label="Annual CTC (override)"
          type="number"
          min={0}
          step="0.01"
          value={form.annualCtc}
          onChange={updateField('annualCtc')}
          placeholder="Leave blank to auto-calculate"
        />
        <Input
          label="Number of Months"
          type="number"
          min={1}
          max={12}
          value={form.numOfMonths}
          onChange={updateField('numOfMonths')}
        />
      </div>

      <div>
        <h5 className="text-xs font-semibold uppercase tracking-wider text-slate-500">Statutory Deductions</h5>
        <div className="mt-3 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <Input
            label="Employer PF"
            type="number"
            min={0}
            step="0.01"
            value={form.employerPf}
            onChange={updateField('employerPf')}
          />
          <Input
            label="Employee PF"
            type="number"
            min={0}
            step="0.01"
            value={form.employeePf}
            onChange={updateField('employeePf')}
          />
          <Input
            label="Employer ESI"
            type="number"
            min={0}
            step="0.01"
            value={form.employerEsi}
            onChange={updateField('employerEsi')}
          />
          <Input
            label="Employee ESI"
            type="number"
            min={0}
            step="0.01"
            value={form.employeeEsi}
            onChange={updateField('employeeEsi')}
          />
          <Input
            label="LWF Amount"
            type="number"
            min={0}
            step="0.01"
            value={form.lwfAmount}
            onChange={updateField('lwfAmount')}
          />
          <Input
            label="TDS Amount"
            type="number"
            min={0}
            step="0.01"
            value={form.tdsAmount}
            onChange={updateField('tdsAmount')}
          />
          <Input
            label="TDS Rate (%)"
            type="number"
            min={0}
            step="0.01"
            value={form.tdsRatePercent}
            onChange={updateField('tdsRatePercent')}
          />
        </div>
      </div>

      <div>
        <h5 className="text-xs font-semibold uppercase tracking-wider text-slate-500">Earnings & Adjustments</h5>
        <div className="mt-3 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <Input
            label="Standing Incentives"
            type="number"
            min={0}
            step="0.01"
            value={form.incentives}
            onChange={updateField('incentives')}
            placeholder="Monthly incentive (can be overridden per run)"
          />
          <Input
            label="Other Deductions"
            type="number"
            min={0}
            step="0.01"
            value={form.otherDeductions}
            onChange={updateField('otherDeductions')}
          />
        </div>
      </div>

      <div>
        <h5 className="text-xs font-semibold uppercase tracking-wider text-slate-500">Validity</h5>
        <div className="mt-3 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <Input
            label="Effective From *"
            type="date"
            value={form.effectiveFrom}
            onChange={updateField('effectiveFrom')}
            required
          />
          <Input
            label="Effective To"
            type="date"
            value={form.effectiveTo}
            onChange={updateField('effectiveTo')}
            placeholder="Leave blank if open-ended"
          />
        </div>
      </div>

      <div className="flex items-center justify-between border-t border-slate-100 pt-4">
        <p className="text-xs text-slate-500">
          Payroll will pull Monthly Gross CTC, NTH, PF, ESI, LWF, TDS, and Incentives from this record when
          generating payslips. Manual LOP days and one-off adjustments are applied at generate-time.
        </p>
        <Button type="submit" disabled={saveMutation.isPending}>
          {saveMutation.isPending
            ? 'Saving…'
            : hasExistingStructure
              ? 'Save changes'
              : 'Create salary structure'}
        </Button>
      </div>
    </form>
  );
};
