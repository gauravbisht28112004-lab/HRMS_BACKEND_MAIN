import { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  emptyTitle?: string;
  emptyDescription?: string;
}

export const DataTable = <T,>({ columns, data, emptyTitle, emptyDescription }: DataTableProps<T>) => (
  <Card className="overflow-hidden p-0">
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-100">
        <thead className="bg-slate-50/80">
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="px-5 py-4 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {data.length > 0 ? (
            data.map((row, index) => (
              <tr key={index} className="hover:bg-slate-50/80">
                {columns.map((column) => (
                  <td key={column.key} className="whitespace-nowrap px-5 py-4 text-sm text-slate-700">
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length} className="px-6 py-14 text-center">
                <p className="text-base font-semibold text-slate-800">{emptyTitle ?? 'No records found'}</p>
                <p className="mt-2 text-sm text-slate-500">{emptyDescription ?? 'Adjust filters or create a new record.'}</p>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  </Card>
);
