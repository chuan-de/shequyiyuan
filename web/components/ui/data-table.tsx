'use client';

import { ReactNode } from 'react';

export type TableColumn<T> = {
  key: string;
  title: string;
  sortable?: boolean;
  render: (row: T) => ReactNode;
};

export type SortState = { field: string; direction: 'asc' | 'desc' } | null;

export function DataTable<T>({
  columns,
  rows,
  sort,
  onSort,
}: {
  columns: TableColumn<T>[];
  rows: T[];
  sort: SortState;
  onSort: (field: string) => void;
}) {
  return (
    <table className="w-full min-w-[560px] table-auto border-collapse text-sm">
      <thead>
        <tr className="border-b border-slate-200 text-left text-slate-600">
          {columns.map((column) => (
            <th key={column.key} className="px-2 py-2">
              <button
                type="button"
                className="inline-flex items-center gap-1 disabled:opacity-100"
                disabled={!column.sortable}
                onClick={() => column.sortable && onSort(column.key)}
              >
                {column.title}
                {sort?.field === column.key ? (sort.direction === 'asc' ? '↑' : '↓') : ''}
              </button>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, idx) => (
          <tr key={idx} className="border-b border-slate-100">
            {columns.map((column) => (
              <td key={column.key} className="px-2 py-2">{column.render(row)}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export function TablePagination({ page, size, total, onChange }: { page: number; size: number; total: number; onChange: (page: number) => void }) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  return (
    <div className="flex items-center justify-end gap-2 text-sm">
      <span className="hint">Page {page} / {totalPages} · Total {total}</span>
      <button type="button" className="btn btn-secondary" disabled={page <= 1} onClick={() => onChange(page - 1)}>Prev</button>
      <button type="button" className="btn btn-secondary" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}
