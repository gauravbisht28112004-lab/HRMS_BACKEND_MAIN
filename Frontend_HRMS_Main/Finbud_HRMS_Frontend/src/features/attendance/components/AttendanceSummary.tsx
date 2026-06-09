import { AlarmClockCheck, CircleAlert, ClockArrowUp, Timer } from 'lucide-react';
import { AttendanceRecord } from '@/types';
import { StatsCard } from '@/components/common/StatsCard';

interface AttendanceSummaryProps {
  records: AttendanceRecord[];
}

export const AttendanceSummary = ({ records }: AttendanceSummaryProps) => {
  const present = records.filter((item) => item.status === 'Present').length;
  const late = records.filter((item) => item.late).length;
  const missing = records.filter((item) => item.missingPunch).length;
  const overtime = records.reduce((sum, item) => sum + item.overtimeHours, 0);

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <StatsCard label="Present Today" value={String(present)} meta="Live attendance sync" icon={<AlarmClockCheck size={22} />} />
      <StatsCard label="Late Arrivals" value={String(late)} meta="Need team leader review" icon={<ClockArrowUp size={22} />} />
      <StatsCard label="Missing Punch" value={String(missing)} meta="Auto-alerts generated" icon={<CircleAlert size={22} />} />
      <StatsCard label="Overtime Hours" value={overtime.toFixed(1)} meta="Eligible for payroll" icon={<Timer size={22} />} />
    </div>
  );
};
