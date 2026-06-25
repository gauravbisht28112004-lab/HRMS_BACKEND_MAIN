import { useState } from 'react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';

interface ReportCardProps {
  title: string;
  description: string;
  /**
   * Triggers the export. Should resolve once the download has started and
   * reject with an Error whose message is safe to show the user.
   */
  onExport: () => Promise<void>;
}

export const ReportCard = ({ title, description, onExport }: ReportCardProps) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleExport = async () => {
    setError(null);
    setLoading(true);
    try {
      await onExport();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
      <p className="mt-2 text-sm text-slate-500">{description}</p>
      <div className="mt-5 flex items-center gap-3">
        <Button onClick={handleExport} disabled={loading}>
          {loading ? 'Exporting…' : 'Export'}
        </Button>
      </div>
      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}
    </Card>
  );
};
