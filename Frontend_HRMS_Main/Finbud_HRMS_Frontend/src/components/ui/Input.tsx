import { InputHTMLAttributes } from 'react';
import { cn } from '@/utils/cn';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
}

export const Input = ({ className, label, ...props }: InputProps) => (
  <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
    {label ? <span>{label}</span> : null}
    <input
      className={cn(
        'rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-400 focus:ring-2 focus:ring-brand-100',
        className,
      )}
      {...props}
    />
  </label>
);
