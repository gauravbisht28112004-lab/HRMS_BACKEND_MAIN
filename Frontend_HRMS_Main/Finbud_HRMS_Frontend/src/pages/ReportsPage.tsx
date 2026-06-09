import { FilterBar } from '@/components/common/FilterBar';
import { PageHeader } from '@/components/common/PageHeader';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { ReportCard } from '@/features/reports/components/ReportCard';

export const ReportsPage = () => (
  <div className="space-y-6">
    <PageHeader title="Reports Dashboard" description="Generate attendance, leave, payroll, and overtime reports with export-ready controls." />
    <FilterBar>
      <Input label="Date Range" type="date" />
      <Select
        label="Department"
        options={[
          { label: 'All Departments', value: 'All' },
          { label: 'Finance', value: 'Finance' },
          { label: 'Engineering', value: 'Engineering' },
          { label: 'HR', value: 'HR' },
        ]}
      />
      <Select
        label="Report Type"
        options={[
          { label: 'Attendance', value: 'Attendance' },
          { label: 'Leave', value: 'Leave' },
          { label: 'Payroll', value: 'Payroll' },
          { label: 'Overtime', value: 'Overtime' },
        ]}
      />
    </FilterBar>

    <div className="grid gap-4 md:grid-cols-2">
      <ReportCard title="Attendance Report" description="Daily summaries, late arrivals, absences, and missing punches." />
      <ReportCard title="Leave Report" description="Balance utilization, approval rates, and team calendar patterns." />
      <ReportCard title="Payroll Report" description="Monthly payroll totals, deductions, and cost-center visibility." />
      <ReportCard title="Overtime Report" description="Overtime trend monitoring with shift and department filters." />
    </div>
  </div>
);
