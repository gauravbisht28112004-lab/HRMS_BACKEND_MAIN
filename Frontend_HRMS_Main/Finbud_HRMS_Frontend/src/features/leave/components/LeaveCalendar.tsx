import { Card } from '@/components/ui/Card';
import { LeaveRequest } from '@/types';

interface LeaveCalendarProps {
  requests: LeaveRequest[];
}

export const LeaveCalendar = ({ requests }: LeaveCalendarProps) => (
  <Card>
    <div className="flex items-center justify-between">
      <h3 className="text-lg font-semibold text-slate-900">Leave Calendar</h3>
      <p className="text-sm text-slate-500">Approval timeline</p>
    </div>
    <div className="mt-5 grid gap-3 md:grid-cols-2">
      {requests.map((request) => (
        <div key={request.id} className="rounded-2xl border border-slate-100 bg-slate-50 p-4">
          <p className="font-medium text-slate-900">{request.employeeName}</p>
          <p className="mt-1 text-sm text-slate-500">
            {request.from} to {request.to}
          </p>
          <p className="mt-3 text-sm text-slate-600">{request.reason}</p>
        </div>
      ))}
    </div>
  </Card>
);
