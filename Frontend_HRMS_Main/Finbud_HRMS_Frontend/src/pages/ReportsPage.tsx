import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { FilterBar } from '@/components/common/FilterBar';
import { PageHeader } from '@/components/common/PageHeader';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { ReportCard } from '@/features/reports/components/ReportCard';
import { api } from '@/services/api';

/** YYYY-MM-DD for today. The dashboard reports from go-live (today) onward only. */
const today = () => new Date().toISOString().slice(0, 10);

/** Turn a downloaded Blob into a browser download. */
const triggerDownload = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};

export const ReportsPage = () => {
  // Reports run from today (go-live) onward — there is no valid pre-go-live data.
  const minDate = today();
  const [startDate, setStartDate] = useState(today);
  const [endDate, setEndDate] = useState(today);
  const [department, setDepartment] = useState('all');

  const { data: departments = [] } = useQuery({
    queryKey: ['departments-options'],
    queryFn: api.departments.list,
  });

  const departmentOptions = useMemo(
    () => [
      { label: 'All Departments', value: 'all' },
      ...departments.map((d) => ({ label: d.name, value: String(d.backendId ?? '') })),
    ],
    [departments],
  );

  const departmentId =
    department === 'all' || department === '' ? undefined : Number(department);

  /**
   * Shared export runner: validate the date window, fetch the xlsx for the
   * current filters, and start the download. Throws a user-friendly Error so
   * the calling ReportCard can surface it inline.
   */
  const download = async (
    fetcher: (start: string, end: string, deptId?: number) => Promise<Blob>,
    name: string,
  ) => {
    if (!startDate || !endDate) {
      throw new Error('Please choose both a start and end date.');
    }
    if (startDate > endDate) {
      throw new Error('Start date must be on or before the end date.');
    }
    if (startDate < minDate) {
      throw new Error('Reports are available from today onward only.');
    }
    const blob = await fetcher(startDate, endDate, departmentId);
    triggerDownload(blob, `${name}-${startDate}-to-${endDate}.xlsx`);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Reports Dashboard"
        description="Generate attendance, leave, payroll, and overtime reports with export-ready controls."
      />
      <FilterBar>
        <Input
          label="Start Date"
          type="date"
          min={minDate}
          value={startDate}
          onChange={(event) => setStartDate(event.target.value)}
        />
        <Input
          label="End Date"
          type="date"
          min={startDate}
          value={endDate}
          onChange={(event) => setEndDate(event.target.value)}
        />
        <Select
          label="Department"
          value={department}
          onChange={(event) => setDepartment(event.target.value)}
          options={departmentOptions}
        />
      </FilterBar>

      <div className="grid gap-4 md:grid-cols-2">
        <ReportCard
          title="Attendance Report"
          description="Daily summaries, late arrivals, absences, and missing punches."
          onExport={() => download(api.reports.attendanceXlsx, 'attendance')}
        />
        <ReportCard
          title="Leave Report"
          description="Balance utilization, approval rates, and team calendar patterns."
          onExport={() => download(api.reports.leaveXlsx, 'leave')}
        />
        <ReportCard
          title="Payroll Report"
          description="Monthly payroll totals, deductions, and cost-center visibility."
          onExport={() => download(api.reports.payrollXlsx, 'payroll')}
        />
        <ReportCard
          title="Overtime Report"
          description="Overtime trend monitoring with shift and department filters."
          onExport={() => download(api.reports.overtimeXlsx, 'overtime')}
        />
      </div>
    </div>
  );
};
