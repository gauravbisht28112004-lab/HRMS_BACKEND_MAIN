import { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';

interface StatsCardProps {
  label: string;
  value: string;
  meta: string;
  icon: ReactNode;
}

export const StatsCard = ({ label, value, meta, icon }: StatsCardProps) => (
  <Card className="overflow-hidden">
    <div className="flex items-start justify-between">
      <div>
        <p className="text-sm text-slate-500">{label}</p>
        <p className="mt-3 text-3xl font-semibold text-slate-900">{value}</p>
        <p className="mt-2 text-sm text-slate-500">{meta}</p>
      </div>
      <div className="rounded-2xl bg-brand-50 p-3 text-brand-700">{icon}</div>
    </div>
  </Card>
);
