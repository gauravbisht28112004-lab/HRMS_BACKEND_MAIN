import { HTMLAttributes, PropsWithChildren } from 'react';
import { cn } from '@/utils/cn';

interface CardProps extends PropsWithChildren, HTMLAttributes<HTMLDivElement> {
  className?: string;
}

export const Card = ({ children, className, ...props }: CardProps) => (
  <div className={cn('rounded-3xl border border-white/60 bg-white/90 p-5 shadow-panel backdrop-blur', className)} {...props}>
    {children}
  </div>
);
