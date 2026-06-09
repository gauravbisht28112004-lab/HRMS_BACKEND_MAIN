import { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';

interface FilterBarProps {
  children: ReactNode;
}

export const FilterBar = ({ children }: FilterBarProps) => (
  <Card className="p-4">
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">{children}</div>
  </Card>
);
