import { useState } from 'react';
import { AuditTimeline } from '@/features/audit/components/AuditTimeline';
import { FilterBar } from '@/components/common/FilterBar';
import { PageHeader } from '@/components/common/PageHeader';
import { Input } from '@/components/ui/Input';
import { auditLogs } from '@/constants/mockData';

export const AuditLogsPage = () => {
  const [query, setQuery] = useState('');

  const filtered = auditLogs.filter(
    (log) =>
      log.user.toLowerCase().includes(query.toLowerCase()) ||
      log.action.toLowerCase().includes(query.toLowerCase()) ||
      log.timestamp.toLowerCase().includes(query.toLowerCase()),
  );

  return (
    <div className="space-y-6">
      <PageHeader title="Audit Logs" description="Trace activity by user, action, and time with an investigation-friendly timeline." />
      <FilterBar>
        <Input label="Filter by user / action / date" value={query} onChange={(event) => setQuery(event.target.value)} />
      </FilterBar>
      <AuditTimeline logs={filtered} />
    </div>
  );
};
