import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';

interface ReportCardProps {
  title: string;
  description: string;
}

export const ReportCard = ({ title, description }: ReportCardProps) => (
  <Card>
    <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
    <p className="mt-2 text-sm text-slate-500">{description}</p>
    <div className="mt-5 flex gap-3">
      <Button variant="secondary">Filter</Button>
      <Button>Export</Button>
    </div>
  </Card>
);
