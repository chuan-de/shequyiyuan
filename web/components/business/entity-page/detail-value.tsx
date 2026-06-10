import type { ReactNode } from 'react';

const PHOTO_URL_RE = /\/api\/v1\/photos\/[^/]+\/content/;
const DATETIME_KEYS = new Set([
  'createdAt', 'updatedAt', 'recordDate', 'recordedAt', 'visitDate',
  'lastLoginAt', 'aiConsentAt', 'consentedAt',
]);

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * Pretty-print a detail-view value.
 * - photo URLs become thumbnails
 * - datetime-looking keys get formatted
 * - arrays of prescription-like objects render as a list
 * - arrays of attachment-like objects render as image/PDF chips
 * - generic objects fall back to indented JSON
 */
export function renderDetailValue(key: string, value: unknown): ReactNode {
  if (value === null || value === undefined || value === '') return <span className="text-slate-400">—</span>;

  // Key-aware mappings for enum-ish fields shared across modules.
  if (key === 'sexTypes' && (value === 1 || value === '1')) return <span>男</span>;
  if (key === 'sexTypes' && (value === 2 || value === '2')) return <span>女</span>;
  if (key === 'enabled' || key === 'status') {
    if (value === true || value === 'ENABLED' || value === 'ACTIVE' || value === 'COMPLETED') {
      return <span className="badge badge-green">启用</span>;
    }
    if (value === false || value === 'DISABLED' || value === 'INACTIVE' || value === 'SUSPENDED' || value === 'ARCHIVED' || value === 'CANCELLED') {
      return <span className="badge badge-red">禁用</span>;
    }
  }

  if (typeof value === 'string') {
    if (PHOTO_URL_RE.test(value)) {
      return <img src={value} alt="" className="h-20 w-20 rounded-lg border border-slate-200 object-cover" />;
    }
    if (DATETIME_KEYS.has(key) && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
      return <span>{formatDateTime(value)}</span>;
    }
    return <span className="whitespace-pre-wrap break-words">{value}</span>;
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return <span>{String(value)}</span>;
  }

  if (Array.isArray(value)) {
    if (value.length === 0) return <span className="text-slate-400">—</span>;

    // Prescription-like: [{ medicationId|medicationName, qty|quantity, ... }]
    if (value.every((v) => typeof v === 'object' && v && ('medicationName' in v || 'medicationId' in v))) {
      return (
        <ul className="space-y-1">
          {value.map((item: Record<string, unknown>, idx) => (
            <li key={idx} className="rounded border border-slate-200 bg-slate-50 px-2 py-1">
              <span className="font-medium">{String(item.medicationName ?? item.medicationId ?? '—')}</span>
              <span className="ml-2 text-slate-500">× {String(item.qty ?? item.quantity ?? 1)}</span>
            </li>
          ))}
        </ul>
      );
    }

    // Attachment-like: [{ url, filename, contentType }]
    if (value.every((v) => typeof v === 'object' && v && 'url' in v)) {
      return (
        <div className="flex flex-wrap gap-2">
          {value.map((item: Record<string, unknown>, idx) => {
            const url = String(item.url ?? '');
            const ct = String(item.contentType ?? '');
            const isPdf = ct.includes('pdf') || url.toLowerCase().endsWith('.pdf');
            return isPdf ? (
              <a key={idx} href={url} target="_blank" rel="noreferrer"
                 className="inline-flex h-20 w-20 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-xs font-medium text-rose-600">
                PDF
              </a>
            ) : (
              <a key={idx} href={url} target="_blank" rel="noreferrer">
                <img src={url} alt={String(item.filename ?? '')}
                     className="h-20 w-20 rounded-lg border border-slate-200 object-cover" />
              </a>
            );
          })}
        </div>
      );
    }

    // Generic array of primitives or objects.
    return (
      <ul className="list-inside list-disc space-y-0.5">
        {value.map((v, idx) => (
          <li key={idx}>{typeof v === 'object' ? JSON.stringify(v) : String(v)}</li>
        ))}
      </ul>
    );
  }

  if (typeof value === 'object') {
    return (
      <pre className="overflow-auto rounded bg-slate-50 p-2 text-xs">{JSON.stringify(value, null, 2)}</pre>
    );
  }

  return <span>{String(value)}</span>;
}

export function StatusBadge({ value }: { value: unknown }) {
  if (value === true || value === 'ACTIVE' || value === 'ENABLED' || value === 'COMPLETED') {
    return <span className="badge badge-green">{String(value)}</span>;
  }
  if (value === false || value === 'INACTIVE' || value === 'DISABLED' || value === 'CANCELLED' || value === 'SUSPENDED' || value === 'ARCHIVED') {
    return <span className="badge badge-red">{String(value)}</span>;
  }
  return <span className="badge badge-gray">{String(value ?? '-')}</span>;
}
