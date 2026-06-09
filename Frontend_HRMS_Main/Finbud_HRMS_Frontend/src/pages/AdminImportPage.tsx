import { ChangeEvent, DragEvent, useMemo, useRef, useState } from 'react';
import { AlertTriangle, CheckCircle2, Eraser, FileSpreadsheet, Loader2, UploadCloud, X } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/common/PageHeader';
import { api, type BackendImportResponse, type ImportEmployeesOptions } from '@/services/api';
import { ApiError } from '@/services/contracts';

/**
 * Admin-only UI to upload the monthly Excel employee master and run the
 * server-side importer without touching Swagger, curl, or the terminal.
 *
 * Flow matches the backend contract in {@code AdminImportController}:
 *   1. Pick the .xlsx file with a drag-and-drop zone or file picker.
 *   2. Toggle `dryRun` / `includeResigned` / `createUsers`.
 *   3. Hit "Validate (dry-run)" to see counts + per-row errors without
 *      committing. The server wraps the whole pass in a detached JPA
 *      session so nothing is written.
 *   4. Hit "Run real import" to persist. Button is gated behind a
 *      confirmation step to avoid accidentally writing prod data.
 *
 * The response panel stays visible across runs so an operator can compare
 * a dry-run outcome to the subsequent real run without re-uploading.
 */
const ACCEPTED_EXTENSIONS = ['.xlsx', '.xls'] as const;
const MAX_FILE_SIZE_MB = 25;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

interface ImportRunSummary {
  kind: 'dry-run' | 'real';
  response: BackendImportResponse;
  executedAt: string;
  fileName: string;
}

const formatNumber = (value: number | undefined) =>
  typeof value === 'number' ? value.toLocaleString() : '0';

const isExcelFile = (file: File) =>
  ACCEPTED_EXTENSIONS.some((ext) => file.name.toLowerCase().endsWith(ext));

export const AdminImportPage = () => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [includeResigned, setIncludeResigned] = useState(true);
  const [createUsers, setCreateUsers] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);
  const [lastRun, setLastRun] = useState<ImportRunSummary | null>(null);
  const [confirmReal, setConfirmReal] = useState(false);

  const canRun = Boolean(file) && !isSubmitting;

  const humanSize = useMemo(() => {
    if (!file) return null;
    const kb = file.size / 1024;
    if (kb < 1024) return `${kb.toFixed(1)} KB`;
    return `${(kb / 1024).toFixed(2)} MB`;
  }, [file]);

  const acceptFile = (candidate: File | null | undefined) => {
    setErrorMessage(null);
    setErrorDetails([]);
    setConfirmReal(false);
    if (!candidate) {
      setFile(null);
      return;
    }
    if (!isExcelFile(candidate)) {
      setErrorMessage('Please pick an .xlsx or .xls file — other formats are not supported.');
      return;
    }
    if (candidate.size > MAX_FILE_SIZE_BYTES) {
      setErrorMessage(`File is larger than ${MAX_FILE_SIZE_MB} MB. Split the workbook or compress unused sheets.`);
      return;
    }
    setFile(candidate);
  };

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    acceptFile(event.target.files?.[0] ?? null);
    // Reset the input so picking the same file twice still fires onChange.
    event.target.value = '';
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setIsDragging(false);
    acceptFile(event.dataTransfer.files?.[0] ?? null);
  };

  const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => setIsDragging(false);

  const runImport = async (dryRun: boolean) => {
    if (!file) return;
    setIsSubmitting(true);
    setErrorMessage(null);
    setErrorDetails([]);

    const options: ImportEmployeesOptions = {
      dryRun,
      includeResigned,
      createUsers,
    };

    try {
      const response = await api.admin.importEmployees(file, options);
      setLastRun({
        kind: dryRun ? 'dry-run' : 'real',
        response,
        executedAt: new Date().toLocaleString(),
        fileName: file.name,
      });
      if (!dryRun) {
        setConfirmReal(false);
      }
    } catch (err) {
      // ApiError is thrown by the axios response interceptor for 4xx/5xx
      // responses with an ApiResponse body — it already contains the
      // backend's message and the root-cause list produced by
      // GlobalExceptionHandler. Fall back to bare Error for timeouts and
      // network failures.
      if (err instanceof ApiError) {
        setErrorMessage(
          err.statusCode === 500
            ? `Server error (HTTP 500): ${err.message}`
            : err.message,
        );
        setErrorDetails(err.errors ?? []);
      } else {
        const raw =
          err instanceof Error ? err.message : 'Something went wrong while importing. Check the backend logs.';
        const timedOut = /timeout of \d+ms exceeded/i.test(raw) || raw.toLowerCase().includes('timeout');
        setErrorMessage(
          timedOut
            ? `${raw} — the server may still be finishing the import. Wait a minute, then reload the Employees page to confirm.`
            : raw,
        );
        setErrorDetails([]);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const clearSelection = () => {
    setFile(null);
    setErrorMessage(null);
    setErrorDetails([]);
    setConfirmReal(false);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Import Employees"
        description="Upload the monthly Noida master workbook and populate the portal without touching Swagger or the terminal."
      />

      <Card className="space-y-6 p-6">
        <div>
          <h2 className="text-base font-semibold text-slate-900">1. Pick the Excel workbook</h2>
          <p className="mt-1 text-sm text-slate-500">
            Accepts the standard Noida master data layout in .xlsx or .xls. Maximum {MAX_FILE_SIZE_MB} MB.
          </p>
        </div>

        <div
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onClick={() => fileInputRef.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              fileInputRef.current?.click();
            }
          }}
          className={`flex cursor-pointer flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed p-8 text-center transition ${
            isDragging
              ? 'border-brand-500 bg-brand-50/50'
              : 'border-slate-200 bg-slate-50/60 hover:border-brand-400 hover:bg-brand-50/30'
          }`}
        >
          <div className="rounded-2xl bg-white p-3 text-brand-700 shadow-sm">
            <UploadCloud size={28} />
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-800">Drop the Excel file here or click to browse</p>
            <p className="mt-1 text-xs text-slate-500">.xlsx or .xls · up to {MAX_FILE_SIZE_MB} MB</p>
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
            className="hidden"
            onChange={handleInputChange}
          />
        </div>

        {file ? (
          <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3">
            <div className="flex items-center gap-3">
              <div className="rounded-xl bg-emerald-50 p-2 text-emerald-600">
                <FileSpreadsheet size={20} />
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-900">{file.name}</p>
                <p className="text-xs text-slate-500">{humanSize}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={clearSelection}
              className="flex items-center gap-1 rounded-xl px-2 py-1 text-xs font-medium text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
              aria-label="Remove selected file"
            >
              <X size={14} />
              Remove
            </button>
          </div>
        ) : null}

        {errorMessage ? (
          <div className="flex items-start gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            <AlertTriangle size={18} className="mt-0.5 flex-shrink-0" />
            <div className="flex-1 space-y-2">
              <p className="font-medium">{errorMessage}</p>
              {errorDetails.length > 0 ? (
                <details open={errorDetails.length <= 3} className="text-xs leading-5 text-rose-800">
                  <summary className="cursor-pointer font-semibold">
                    {errorDetails.length} root-cause detail{errorDetails.length === 1 ? '' : 's'}
                  </summary>
                  <ul className="mt-2 list-disc space-y-1 pl-5">
                    {errorDetails.map((detail, idx) => (
                      <li key={idx} className="whitespace-pre-wrap break-words">
                        {detail}
                      </li>
                    ))}
                  </ul>
                </details>
              ) : null}
            </div>
          </div>
        ) : null}
      </Card>

      <Card className="space-y-5 p-6">
        <div>
          <h2 className="text-base font-semibold text-slate-900">2. Import options</h2>
          <p className="mt-1 text-sm text-slate-500">
            Match these to whatever the backend should do for this sheet. Dry-run is the safest starting point.
          </p>
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          <ToggleOption
            label="Include resigned employees"
            description="Import rows whose status column indicates the employee has resigned/left."
            checked={includeResigned}
            onChange={setIncludeResigned}
          />
          <ToggleOption
            label="Create login accounts"
            description="Provision a User record (with default password) alongside each imported employee."
            checked={createUsers}
            onChange={setCreateUsers}
          />
        </div>
      </Card>

      <Card className="space-y-5 p-6">
        <div>
          <h2 className="text-base font-semibold text-slate-900">3. Run the import</h2>
          <p className="mt-1 text-sm text-slate-500">
            Validate with a dry-run first; the server detaches all JPA changes so nothing is written. If the result
            looks clean, click "Run real import" to persist. A real import of 400+ rows can take up to a minute — keep
            this tab open while it runs.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <Button
            variant="secondary"
            disabled={!canRun}
            onClick={() => void runImport(true)}
            type="button"
          >
            {isSubmitting && lastRun?.kind !== 'real' ? (
              <span className="flex items-center gap-2">
                <Loader2 className="animate-spin" size={16} />
                Validating…
              </span>
            ) : (
              'Validate (dry-run)'
            )}
          </Button>

          {confirmReal ? (
            <div className="flex items-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              <AlertTriangle size={16} />
              <span>This writes to the database. Are you sure?</span>
              <Button
                variant="danger"
                type="button"
                disabled={!canRun}
                onClick={() => void runImport(false)}
              >
                {isSubmitting ? (
                  <span className="flex items-center gap-2">
                    <Loader2 className="animate-spin" size={16} />
                    Importing…
                  </span>
                ) : (
                  'Yes, persist'
                )}
              </Button>
              <Button variant="ghost" type="button" onClick={() => setConfirmReal(false)}>
                Cancel
              </Button>
            </div>
          ) : (
            <Button
              disabled={!canRun}
              onClick={() => setConfirmReal(true)}
              type="button"
            >
              Run real import
            </Button>
          )}
        </div>
      </Card>

      {lastRun ? <ResultPanel summary={lastRun} /> : null}
    </div>
  );
};

interface ToggleOptionProps {
  label: string;
  description: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}

const ToggleOption = ({ label, description, checked, onChange }: ToggleOptionProps) => (
  <label className="flex cursor-pointer items-start gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 transition hover:border-brand-300">
    <input
      type="checkbox"
      checked={checked}
      onChange={(event) => onChange(event.target.checked)}
      className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
    />
    <span className="flex-1">
      <span className="block text-sm font-medium text-slate-900">{label}</span>
      <span className="mt-1 block text-xs leading-5 text-slate-500">{description}</span>
    </span>
  </label>
);

const ResultPanel = ({ summary }: { summary: ImportRunSummary }) => {
  const { response, kind, executedAt, fileName } = summary;
  const isSuccess = (response.failureCount ?? 0) === 0;

  // Backend DTO uses `totalRecords`; tolerate the legacy `totalRows` alias
  // for older server builds.
  const totalValue = response.totalRecords ?? response.totalRows;

  // Failed rows live in `results[]` with status === 'FAILED'. The old
  // `errors[]` alias is kept for backward compat with any older server.
  const failedRows = useMemo(() => {
    if (response.results && response.results.length > 0) {
      return response.results
        .filter((r) => r.status === 'FAILED')
        .map((r) => ({
          rowNumber: r.rowNumber,
          employeeId: r.employeeCode,
          message: r.message ?? 'No message provided.',
        }));
    }
    if (response.errors && response.errors.length > 0) {
      return response.errors.map((e) => ({
        rowNumber: e.rowNumber,
        employeeId: e.employeeId,
        message: e.message,
      }));
    }
    return [];
  }, [response.results, response.errors]);

  const warnings = response.warnings ?? [];

  return (
    <Card className="space-y-5 p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            {isSuccess ? (
              <CheckCircle2 size={20} className="text-emerald-600" />
            ) : (
              <AlertTriangle size={20} className="text-rose-600" />
            )}
            <h2 className="text-base font-semibold text-slate-900">
              {kind === 'dry-run' ? 'Dry-run result' : 'Import result'}
            </h2>
            <span
              className={`rounded-full px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide ${
                kind === 'dry-run'
                  ? 'bg-slate-100 text-slate-700'
                  : 'bg-emerald-100 text-emerald-700'
              }`}
            >
              {kind === 'dry-run' ? 'validation only' : 'persisted'}
            </span>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            {fileName} · {executedAt}
          </p>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <StatTile label="Total rows" value={formatNumber(totalValue)} tone="neutral" />
        <StatTile label="Inserted" value={formatNumber(response.insertedCount)} tone="success" />
        <StatTile label="Updated" value={formatNumber(response.updatedCount)} tone="info" />
        <StatTile
          label="Skipped"
          value={formatNumber(response.skippedCount)}
          tone={response.skippedCount > 0 ? 'warning' : 'neutral'}
          icon={response.skippedCount > 0 ? <Eraser size={14} /> : undefined}
        />
        <StatTile
          label="Failed"
          value={formatNumber(response.failureCount)}
          tone={response.failureCount > 0 ? 'danger' : 'success'}
        />
      </div>

      {failedRows.length > 0 ? (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-slate-900">
            Row-level errors ({failedRows.length})
          </h3>
          <p className="text-xs text-slate-500">
            These rows were skipped and <strong>not</strong> persisted. Fix the underlying data in the
            workbook (e.g. salary structure, username collision, bad date) and re-run the import — the
            rows that already succeeded will simply be treated as updates.
          </p>
          <div className="max-h-80 overflow-y-auto rounded-2xl border border-slate-200">
            <table className="w-full text-left text-sm">
              <thead className="sticky top-0 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-2">Row</th>
                  <th className="px-4 py-2">Employee ID</th>
                  <th className="px-4 py-2">Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {failedRows.map((err, index) => (
                  <tr key={`${err.rowNumber}-${index}`}>
                    <td className="px-4 py-2 font-mono text-xs text-slate-600">{err.rowNumber}</td>
                    <td className="px-4 py-2 text-xs text-slate-700">{err.employeeId ?? '—'}</td>
                    <td className="px-4 py-2 text-xs text-rose-700">{err.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (response.failureCount ?? 0) > 0 ? (
        <p className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          The backend reported {response.failureCount} failed row(s) but did not return per-row
          details. Check the Spring Boot application log for stack traces.
        </p>
      ) : (
        <p className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          No row-level errors reported by the backend.
        </p>
      )}

      {warnings.length > 0 ? (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-slate-900">
            Warnings ({warnings.length})
          </h3>
          <ul className="max-h-60 space-y-1 overflow-y-auto rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-900">
            {warnings.map((msg, idx) => (
              <li key={idx} className="list-disc pl-1">
                {msg}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </Card>
  );
};

type StatTone = 'neutral' | 'success' | 'info' | 'warning' | 'danger';

const toneClasses: Record<StatTone, string> = {
  neutral: 'bg-slate-50 text-slate-700',
  success: 'bg-emerald-50 text-emerald-800',
  info: 'bg-sky-50 text-sky-800',
  warning: 'bg-amber-50 text-amber-800',
  danger: 'bg-rose-50 text-rose-800',
};

const StatTile = ({
  label,
  value,
  tone,
  icon,
}: {
  label: string;
  value: string;
  tone: StatTone;
  icon?: React.ReactNode;
}) => (
  <div className={`rounded-2xl px-4 py-3 ${toneClasses[tone]}`}>
    <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide opacity-70">
      {icon}
      {label}
    </div>
    <p className="mt-1 text-2xl font-semibold">{value}</p>
  </div>
);
