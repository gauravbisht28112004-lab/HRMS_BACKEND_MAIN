import { AuditLog } from '@/types';
import { Card } from '@/components/ui/Card';

interface AuditTimelineProps {
  logs: AuditLog[];
}

export const AuditTimeline = ({ logs }: AuditTimelineProps) => (
  <Card>
    <h3 className="text-lg font-semibold text-slate-900">Activity Timeline</h3>
    <div className="mt-5 space-y-5">
      {logs.map((log) => (
        <div key={log.id} className="flex gap-4">
          <div className="mt-2 h-3 w-3 rounded-full bg-brand-600" />
          <div className="flex-1 border-b border-slate-100 pb-5">
            <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
              <p className="font-medium text-slate-900">
                {log.user} • {log.action}
              </p>
              <p className="text-xs text-slate-500">{log.timestamp}</p>
            </div>
            <p className="mt-1 text-sm text-slate-500">{log.entity}</p>
            <p className="mt-2 text-sm text-slate-600">{log.details}</p>
          </div>
        </div>
      ))}
    </div>
  </Card>
);
