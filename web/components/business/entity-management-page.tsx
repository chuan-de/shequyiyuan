'use client';
import { ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AppShell } from '@/components/layout/app-shell';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { PhotoUploader } from '@/components/ui/file-upload';
import {
  changeEntityStatus, createEntity, currentUser, deleteEntity, EntityRecord,
  getEntity, listEntities, updateEntity,
} from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { useDictionaries } from '@/lib/dictionaries';
import { useRouter } from 'next/navigation';

/** Extra wiring handed to {@link EntityFormField.customRender}. */
export type CustomFieldContext = {
  /** Read another field's current value from the surrounding form. */
  getFormValue: (key: string) => string;
  /** Patch any field in the surrounding form (used by the AI suggestion panel). */
  setFormValue: (key: string, value: string) => void;
  /** Batch patch — applies multiple fields atomically in one re-render. */
  patchForm: (patch: Record<string, string>) => void;
  /** Current user's permissions, so the custom renderer can gate UI elements. */
  permissions: string[];
};

export type EntityFormField = {
  key: string;
  label: string;
  required?: boolean;
  placeholder?: string;
  defaultValue?: string;
  type?: 'text' | 'password' | 'textarea' | 'number' | 'select' | 'dict-select' | 'photo' | 'datetime' | 'custom';
  options?: { value: string; label: string }[];
  /** type 为 'dict-select' 时必填：选项实时来自数据字典（仅启用项）。 */
  dictCode?: string;
  customRender?: (value: string, onChange: (next: string) => void, mode: 'create' | 'edit', ctx: CustomFieldContext) => React.ReactNode;
};

function isoToLocalInput(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const tz = d.getTimezoneOffset() * 60000;
  return new Date(d.getTime() - tz).toISOString().slice(0, 16);
}
function nowLocalInput(): string {
  return isoToLocalInput(new Date().toISOString());
}

export type EntityColumn = {
  key: string;
  title: string;
  type?: 'text' | 'photo' | 'currency' | 'badge';
  /** 设置后列值按数据字典翻译显示（码值 → 中文标签），详情弹窗同样生效。 */
  dictCode?: string;
  render?: (row: EntityRecord) => React.ReactNode;
};

export type PromptOptions = {
  title: string;
  label?: string;
  placeholder?: string;
  type?: 'text' | 'password' | 'number';
  initialValue?: string;
  confirmText?: string;
  validate?: (value: string) => string | null;
};

export type ActionHelpers = {
  token: string;
  reload: () => void;
  showToast: (msg: string, type?: 'success' | 'error') => void;
  prompt: (opts: PromptOptions) => Promise<string | null>;
};

export type EntityPageConfig = {
  title: string;
  route: string;
  permissionPrefix: string;
  columns: EntityColumn[];
  formFields: EntityFormField[];
  statusField?: 'enabled' | 'status';
  createPayload: (form: Record<string, string>) => Record<string, unknown>;
  updatePayload: (form: Record<string, string>, row: EntityRecord) => Record<string, unknown>;
  searchFields?: { key: string; label: string; type?: 'text' | 'select' | 'dict-select'; options?: { value: string; label: string }[]; dictCode?: string }[];
  rowActions?: {
    key: string;
    label: string;
    permission?: string;
    variant?: 'primary' | 'danger';
    onClick: (row: EntityRecord, helpers: ActionHelpers) => void;
  }[];
  labelMap?: Record<string, string>;
};

function initForm(fields: EntityFormField[], row?: EntityRecord | null): Record<string, string> {
  return fields.reduce<Record<string, string>>((acc, f) => {
    const v = row?.[f.key];
    if (f.type === 'datetime') {
      if (typeof v === 'string' && v) acc[f.key] = isoToLocalInput(v);
      else if (!row) acc[f.key] = nowLocalInput();
      else acc[f.key] = '';
    } else if (f.type === 'custom') {
      if (v !== undefined && v !== null && typeof v !== 'string') {
        acc[f.key] = JSON.stringify(v);
      } else if (typeof v === 'string') {
        acc[f.key] = v;
      } else {
        acc[f.key] = f.defaultValue ?? '';
      }
    } else {
      acc[f.key] = typeof v === 'string' ? v : (typeof v === 'number' ? String(v) : f.defaultValue ?? '');
    }
    return acc;
  }, {});
}

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
function renderDetailValue(key: string, value: unknown): ReactNode {
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

function StatusBadge({ value }: { value: unknown }) {
  if (value === true || value === 'ACTIVE' || value === 'ENABLED' || value === 'COMPLETED') {
    return <span className="badge badge-green">{String(value)}</span>;
  }
  if (value === false || value === 'INACTIVE' || value === 'DISABLED' || value === 'CANCELLED' || value === 'SUSPENDED' || value === 'ARCHIVED') {
    return <span className="badge badge-red">{String(value)}</span>;
  }
  return <span className="badge badge-gray">{String(value ?? '-')}</span>;
}

function Modal({ title, onClose, children, footer }: {
  title: string; onClose: () => void; children: React.ReactNode; footer: React.ReactNode;
}) {
  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-box">
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h2 className="text-base font-semibold text-slate-900">{title}</h2>
          <button onClick={onClose} className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
        <div className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4">{footer}</div>
      </div>
    </div>
  );
}

export function EntityManagementPage({ config }: { config: EntityPageConfig }) {
  const router = useRouter();
  const [token, setToken] = useState('');
  const [rows, setRows] = useState<EntityRecord[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [canRead, setCanRead] = useState(false);
  const [canWrite, setCanWrite] = useState(false);
  const [canDelete, setCanDelete] = useState(false);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number | string>>(new Set());
  const [confirmDelete, setConfirmDelete] = useState<{ ids: (number | string)[]; label: string } | null>(null);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [searchParams, setSearchParams] = useState<Record<string, string>>({});
  const searchDebounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState<Record<string, string>>(() => initForm(config.formFields));
  const [editing, setEditing] = useState<EntityRecord | null>(null);
  const [editForm, setEditForm] = useState<Record<string, string>>(() => initForm(config.formFields));
  const [detail, setDetail] = useState<EntityRecord | null>(null);

  type PromptState = PromptOptions & { resolve: (v: string | null) => void };
  const [promptState, setPromptState] = useState<PromptState | null>(null);
  const [promptValue, setPromptValue] = useState('');
  const [promptError, setPromptError] = useState('');

  const openPrompt = useCallback((opts: PromptOptions): Promise<string | null> => {
    return new Promise<string | null>(resolve => {
      setPromptValue(opts.initialValue ?? '');
      setPromptError('');
      setPromptState({ ...opts, resolve });
    });
  }, []);

  function closePrompt(value: string | null) {
    if (!promptState) return;
    promptState.resolve(value);
    setPromptState(null);
    setPromptValue('');
    setPromptError('');
  }

  function confirmPrompt() {
    if (!promptState) return;
    const err = promptState.validate ? promptState.validate(promptValue) : null;
    if (err) { setPromptError(err); return; }
    closePrompt(promptValue);
  }

  const statusKey = config.statusField ?? 'enabled';
  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / size)), [total, size]);

  // 数据字典联动：收集本页所有引用到的 dictCode，一次性加载（带模块级缓存）。
  const dictCodes = useMemo(() => {
    const codes = new Set<string>();
    config.formFields.forEach(f => { if (f.type === 'dict-select' && f.dictCode) codes.add(f.dictCode); });
    config.columns.forEach(c => { if (c.dictCode) codes.add(c.dictCode); });
    config.searchFields?.forEach(s => { if (s.type === 'dict-select' && s.dictCode) codes.add(s.dictCode); });
    return Array.from(codes);
  }, [config]);
  const dicts = useDictionaries(token, dictCodes);
  const dictOptions = useCallback((dictCode?: string) =>
    (dictCode ? dicts.items[dictCode] ?? [] : []).map(i => ({ value: i.value, label: i.name })),
  [dicts.items]);
  // 详情弹窗的字段 → dictCode 映射（columns 优先，formFields 兜底）。
  const detailDictMap = useMemo(() => {
    const map: Record<string, string> = {};
    config.formFields.forEach(f => { if (f.type === 'dict-select' && f.dictCode) map[f.key] = f.dictCode; });
    config.columns.forEach(c => { if (c.dictCode) map[c.key] = c.dictCode; });
    return map;
  }, [config]);

  function showToast(msg: string, type: 'success' | 'error' = 'success') {
    setToast({ msg, type });
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 3000);
  }

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const r = await listEntities(token, config.route, { keyword: q, page, size, ...searchParams });
      setRows(r.records);
      setTotal(r.total);
    } catch (e) {
      showToast((e as Error).message, 'error');
    } finally {
      setLoading(false);
    }
  }, [token, config.route, q, page, size, searchParams]);

  useEffect(() => {
    const t = localStorage.getItem('access_token');
    if (!t) { router.replace('/login'); return; }
    setToken(t);
    currentUser(t)
      .then(u => {
        setCanRead(hasPermission(u, `${config.permissionPrefix}:read`));
        setCanWrite(hasPermission(u, `${config.permissionPrefix}:write`));
        setCanDelete(hasPermission(u, `${config.permissionPrefix}:delete`));
        setPermissions(u.permissions ?? []);
      })
      .catch(() => { localStorage.removeItem('access_token'); localStorage.removeItem('token_type'); router.replace('/login'); });
  }, [router, config.permissionPrefix]);

  useEffect(() => { if (token && canRead) void load(); }, [token, canRead, load]);
  useEffect(() => { setCreateForm(initForm(config.formFields)); }, [config.formFields]);
  useEffect(() => { setSelectedIds(new Set()); }, [rows]);

  function toggleRow(id: number | string) {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  function toggleAll() {
    if (selectedIds.size === rows.length && rows.length > 0) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(rows.map(r => r.id as number | string)));
    }
  }

  async function runDelete(ids: (number | string)[]) {
    const results = await Promise.allSettled(ids.map(id => deleteEntity(token, config.route, id)));
    const failures = results.filter(r => r.status === 'rejected') as PromiseRejectedResult[];
    if (failures.length === 0) {
      showToast(ids.length === 1 ? '删除成功' : `已删除 ${ids.length} 条`);
    } else if (failures.length === ids.length) {
      showToast(`删除失败：${failures[0].reason?.message ?? '请重试'}`, 'error');
    } else {
      showToast(`部分删除失败：成功 ${ids.length - failures.length} / 共 ${ids.length}`, 'error');
    }
    setConfirmDelete(null);
    void load();
  }

  function handleSearchChange(key: string, value: string) {
    if (searchDebounceTimer.current) clearTimeout(searchDebounceTimer.current);
    searchDebounceTimer.current = setTimeout(() => {
      setSearchParams(prev => {
        const next = { ...prev };
        if (value) { next[key] = value; } else { delete next[key]; }
        return next;
      });
      setPage(1);
    }, 300);
  }

  async function handleCreate() {
    try {
      await createEntity(token, config.route, config.createPayload(createForm));
      setShowCreate(false);
      setCreateForm(initForm(config.formFields));
      showToast('创建成功');
      void load();
    } catch (e) {
      showToast((e as Error).message, 'error');
    }
  }

  async function handleUpdate() {
    if (!editing) return;
    try {
      await updateEntity(token, config.route, editing.id, config.updatePayload(editForm, editing));
      setEditing(null);
      showToast('保存成功');
      void load();
    } catch (e) {
      showToast((e as Error).message, 'error');
    }
  }

  async function handleStatusChange(row: EntityRecord) {
    if (!config.statusField) return;
    const current = row[statusKey];
    const isActive = current === true || current === 'ACTIVE' || current === 'ENABLED' || current === 'COMPLETED';
    try {
      await changeEntityStatus(token, config.route, row.id, !isActive);
      showToast('状态已更新');
      void load();
    } catch (e) {
      showToast((e as Error).message, 'error');
    }
  }

  async function handleShowDetail(row: EntityRecord) {
    try {
      const d = await getEntity(token, config.route, row.id);
      setDetail(d);
    } catch (e) {
      showToast((e as Error).message, 'error');
    }
  }

  if (!canRead && token) {
    return (
      <AppShell title={config.title}>
        <div className="rounded-2xl bg-white p-8 shadow-soft text-center">
          <p className="text-rose-500 font-medium">权限不足：缺少 {config.permissionPrefix}:read</p>
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title={config.title}>
      {/* Toast */}
      {toast && (
        <div className={`fixed right-6 top-6 z-50 flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-medium shadow-lg transition ${toast.type === 'error' ? 'bg-rose-50 text-rose-700 ring-1 ring-rose-200' : 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'}`}>
          <span>{toast.type === 'error' ? '✕' : '✓'}</span>
          <span>{toast.msg}</span>
        </div>
      )}

      {/* Search bar (when searchFields configured) */}
      {config.searchFields && config.searchFields.length > 0 && (
        <div className="mb-3 flex flex-wrap items-center gap-3">
          {config.searchFields.map(sf => (
            sf.type === 'select' || sf.type === 'dict-select' ? (
              <Select
                key={sf.key}
                value={searchParams[sf.key] ?? ''}
                onChange={(v) => handleSearchChange(sf.key, v)}
                options={sf.type === 'dict-select' ? dictOptions(sf.dictCode) : sf.options ?? []}
                placeholder={`${sf.label}（全部）`}
                allowEmpty
                emptyLabel={`${sf.label}（全部）`}
                className="max-w-[160px]"
              />
            ) : (
              <Input
                key={sf.key}
                placeholder={`按${sf.label}搜索`}
                className="max-w-[160px]"
                onChange={e => handleSearchChange(sf.key, e.target.value)}
              />
            )
          ))}
        </div>
      )}

      {/* Toolbar */}
      <div className="mb-4 flex items-center gap-3">
        <Input
          value={q}
          onChange={e => { setQ(e.target.value); setPage(1); }}
          placeholder="搜索关键词…"
          className="max-w-xs"
        />
        {canWrite && (
          <Button onClick={() => { setCreateForm(initForm(config.formFields)); setShowCreate(true); }}>
            + 新建
          </Button>
        )}
        {canDelete && selectedIds.size > 0 && (
          <Button
            variant="secondary"
            onClick={() => setConfirmDelete({ ids: Array.from(selectedIds), label: `这 ${selectedIds.size} 条` })}
            className="bg-rose-50 text-rose-600 ring-1 ring-rose-200 hover:bg-rose-100"
          >
            批量删除 ({selectedIds.size})
          </Button>
        )}
        <span className="ml-auto hint">共 {total} 条</span>
      </div>

      {/* Table */}
      <div className="table-wrap">
        <table className="w-full border-collapse">
          <thead className="border-b border-slate-200 bg-slate-50">
            <tr>
              {canDelete && (
                <th className="th w-10">
                  <input
                    type="checkbox"
                    aria-label="全选"
                    checked={rows.length > 0 && selectedIds.size === rows.length}
                    onChange={toggleAll}
                    className="h-4 w-4 cursor-pointer rounded border-slate-300"
                  />
                </th>
              )}
              {config.columns.map(c => (
                <th key={c.key} className="th">{c.title}</th>
              ))}
              {config.statusField && <th className="th">状态</th>}
              <th className="th text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr>
                <td colSpan={config.columns.length + (config.statusField ? 2 : 1) + (canDelete ? 1 : 0)} className="td text-center text-slate-400 py-12">
                  加载中…
                </td>
              </tr>
            )}
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={config.columns.length + (config.statusField ? 2 : 1) + (canDelete ? 1 : 0)} className="td text-center text-slate-400 py-12">
                  暂无数据
                </td>
              </tr>
            )}
            {!loading && rows.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-colors">
                {canDelete && (
                  <td className="td">
                    <input
                      type="checkbox"
                      aria-label={`选择 ${row.id}`}
                      checked={selectedIds.has(row.id as number | string)}
                      onChange={() => toggleRow(row.id as number | string)}
                      className="h-4 w-4 cursor-pointer rounded border-slate-300"
                    />
                  </td>
                )}
                {config.columns.map(c => (
                  <td key={c.key} className="td">
                    {(() => {
                      if (c.render) return c.render(row);
                      const val = row[c.key];
                      if (c.type === 'photo') {
                        return val ? (
                          <img src={String(val)} alt="" className="h-10 w-10 rounded-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                        ) : <span className="text-slate-300">—</span>;
                      }
                      if (c.type === 'currency') {
                        return typeof val === 'number' ? `¥${val.toFixed(2)}` : (val ? `¥${Number(val).toFixed(2)}` : '—');
                      }
                      if (c.dictCode) {
                        return dicts.labelOf(c.dictCode, val) ?? String(val ?? '—');
                      }
                      return String(val ?? '—');
                    })()}
                  </td>
                ))}
                {config.statusField && (
                  <td className="td">
                    <StatusBadge value={row[statusKey]} />
                  </td>
                )}
                <td className="td">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      onClick={() => handleShowDetail(row)}
                      className="btn-ghost"
                    >
                      详情
                    </button>
                    {canWrite && (
                      <>
                        <button
                          onClick={() => { setEditing(row); setEditForm(initForm(config.formFields, row)); }}
                          className="btn-ghost"
                        >
                          编辑
                        </button>
                        {config.statusField && (
                          <button
                            onClick={() => handleStatusChange(row)}
                            className="btn-ghost text-slate-500"
                          >
                            切换状态
                          </button>
                        )}
                      </>
                    )}
                    {config.rowActions?.map(action => {
                      const canAct = !action.permission || permissions.includes(action.permission);
                      if (!canAct) return null;
                      return (
                        <button
                          key={action.key}
                          onClick={() => action.onClick(row, { token, reload: load, showToast, prompt: openPrompt })}
                          className={`btn-ghost ${action.variant === 'danger' ? 'text-rose-500' : action.variant === 'primary' ? 'text-blue-500' : ''}`}
                        >
                          {action.label}
                        </button>
                      );
                    })}
                    {canDelete && (
                      <button
                        onClick={() => setConfirmDelete({ ids: [row.id as number | string], label: '此条' })}
                        className="btn-ghost text-rose-500"
                      >
                        删除
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="mt-4 flex items-center justify-end gap-2">
        <Button variant="secondary" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page <= 1}>
          上一页
        </Button>
        <span className="hint px-2">{page} / {totalPages}</span>
        <Button variant="secondary" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page >= totalPages}>
          下一页
        </Button>
      </div>

      {/* Create Modal */}
      {showCreate && (
        <Modal
          title={`新建${config.title}`}
          onClose={() => setShowCreate(false)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setShowCreate(false)}>取消</Button>
              <Button onClick={handleCreate}>确认创建</Button>
            </>
          }
        >
          <div className="space-y-4">
            {config.formFields.map(f => (
              <div key={f.key}>
                <label className="label">
                  {f.label}
                  {f.required && <span className="ml-0.5 text-rose-500">*</span>}
                </label>
                {f.type === 'select' || f.type === 'dict-select' ? (
                  <Select
                    value={createForm[f.key] ?? ''}
                    onChange={(v) => setCreateForm(prev => ({ ...prev, [f.key]: v }))}
                    options={f.type === 'dict-select' ? dictOptions(f.dictCode) : f.options ?? []}
                    placeholder={f.placeholder ?? '请选择'}
                  />
                ) : f.type === 'textarea' ? (
                  <textarea
                    value={createForm[f.key] ?? ''}
                    placeholder={f.placeholder ?? `请输入${f.label}`}
                    onChange={e => setCreateForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                    className="input w-full min-h-[80px]"
                    rows={3}
                  />
                ) : f.type === 'photo' ? (
                  <PhotoUploader
                    value={createForm[f.key] ?? ''}
                    onChange={url => setCreateForm(prev => ({ ...prev, [f.key]: url }))}
                  />
                ) : f.type === 'datetime' ? (
                  <Input
                    type="datetime-local"
                    value={createForm[f.key] ?? ''}
                    onChange={e => setCreateForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                  />
                ) : f.type === 'custom' && f.customRender ? (
                  f.customRender(
                    createForm[f.key] ?? '',
                    (next) => setCreateForm(prev => ({ ...prev, [f.key]: next })),
                    'create',
                    {
                      getFormValue: (k) => createForm[k] ?? '',
                      setFormValue: (k, v) => setCreateForm(prev => ({ ...prev, [k]: v })),
                      patchForm: (patch) => setCreateForm(prev => ({ ...prev, ...patch })),
                      permissions,
                    }
                  )
                ) : (
                  <Input
                    type={f.type === 'password' ? 'password' : f.type === 'number' ? 'number' : 'text'}
                    value={createForm[f.key] ?? ''}
                    placeholder={f.placeholder ?? `请输入${f.label}`}
                    onChange={e => setCreateForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                  />
                )}
              </div>
            ))}
          </div>
        </Modal>
      )}

      {/* Edit Modal */}
      {editing && (
        <Modal
          title={`编辑${config.title}`}
          onClose={() => setEditing(null)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setEditing(null)}>取消</Button>
              <Button onClick={handleUpdate}>保存</Button>
            </>
          }
        >
          <div className="space-y-4">
            {config.formFields.map(f => {
              // Skip password fields in edit mode
              if (f.type === 'password') return null;
              return (
                <div key={f.key}>
                  <label className="label">
                    {f.label}
                    {f.required && <span className="ml-0.5 text-rose-500">*</span>}
                  </label>
                  {f.type === 'select' || f.type === 'dict-select' ? (
                    <Select
                      value={editForm[f.key] ?? ''}
                      onChange={(v) => setEditForm(prev => ({ ...prev, [f.key]: v }))}
                      options={f.type === 'dict-select' ? dictOptions(f.dictCode) : f.options ?? []}
                      placeholder={f.placeholder ?? '请选择'}
                    />
                  ) : f.type === 'textarea' ? (
                    <textarea
                      value={editForm[f.key] ?? ''}
                      placeholder={f.placeholder ?? `请输入${f.label}`}
                      onChange={e => setEditForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                      className="input w-full min-h-[80px]"
                      rows={3}
                    />
                  ) : f.type === 'photo' ? (
                    <PhotoUploader
                      value={editForm[f.key] ?? ''}
                      onChange={url => setEditForm(prev => ({ ...prev, [f.key]: url }))}
                    />
                  ) : f.type === 'datetime' ? (
                    <Input
                      type="datetime-local"
                      value={editForm[f.key] ?? ''}
                      onChange={e => setEditForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                    />
                  ) : f.type === 'custom' && f.customRender ? (
                    f.customRender(
                      editForm[f.key] ?? '',
                      (next) => setEditForm(prev => ({ ...prev, [f.key]: next })),
                      'edit',
                      {
                        getFormValue: (k) => editForm[k] ?? '',
                        setFormValue: (k, v) => setEditForm(prev => ({ ...prev, [k]: v })),
                        patchForm: (patch) => setEditForm(prev => ({ ...prev, ...patch })),
                        permissions,
                      }
                    )
                  ) : (
                    <Input
                      type={f.type === 'number' ? 'number' : 'text'}
                      value={editForm[f.key] ?? ''}
                      placeholder={f.placeholder ?? `请输入${f.label}`}
                      onChange={e => setEditForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                    />
                  )}
                </div>
              );
            })}
          </div>
        </Modal>
      )}

      {/* Detail Modal */}
      {detail && (
        <Modal
          title="详情"
          onClose={() => setDetail(null)}
          footer={<Button variant="secondary" onClick={() => setDetail(null)}>关闭</Button>}
        >
          <div className="space-y-3">
            {(() => {
              // Render in labelMap order when defined (filters out internal/english
              // fields), otherwise fall back to the raw entry list for configs
              // that haven't defined a labelMap yet.
              const orderedKeys = config.labelMap
                ? Object.keys(config.labelMap).filter((k) => k in detail)
                : Object.keys(detail);
              return orderedKeys.map((k) => {
                const v = (detail as Record<string, unknown>)[k];
                const label = config.labelMap?.[k] ?? k;
                const dictLabel = detailDictMap[k] ? dicts.labelOf(detailDictMap[k], v) : null;
                return (
                  <div key={k} className="flex gap-4">
                    <span className="w-36 flex-shrink-0 text-sm font-semibold text-slate-500">{label}</span>
                    <div className="min-w-0 flex-1 text-sm text-slate-800">
                      {dictLabel ? <span>{dictLabel}</span> : renderDetailValue(k, v)}
                    </div>
                  </div>
                );
              });
            })()}
          </div>
        </Modal>
      )}

      {/* Prompt Modal (replaces window.prompt for rowActions) */}
      {promptState && (
        <Modal
          title={promptState.title}
          onClose={() => closePrompt(null)}
          footer={
            <>
              <Button variant="secondary" onClick={() => closePrompt(null)}>取消</Button>
              <Button onClick={confirmPrompt}>{promptState.confirmText ?? '确认'}</Button>
            </>
          }
        >
          <div className="space-y-2">
            {promptState.label && <label className="label">{promptState.label}</label>}
            <Input
              type={promptState.type ?? 'text'}
              value={promptValue}
              placeholder={promptState.placeholder ?? ''}
              autoFocus
              onChange={e => { setPromptValue(e.target.value); if (promptError) setPromptError(''); }}
              onKeyDown={e => { if (e.key === 'Enter') confirmPrompt(); }}
            />
            {promptError && <p className="text-xs text-rose-600">{promptError}</p>}
          </div>
        </Modal>
      )}

      {/* Confirm Delete Modal */}
      {confirmDelete && (
        <Modal
          title="确认删除"
          onClose={() => setConfirmDelete(null)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setConfirmDelete(null)}>取消</Button>
              <Button
                onClick={() => runDelete(confirmDelete.ids)}
                className="bg-rose-600 text-white hover:bg-rose-700"
              >
                确认删除
              </Button>
            </>
          }
        >
          <p className="text-sm text-slate-700">
            即将删除 <span className="font-semibold text-rose-600">{confirmDelete.label}</span>，此操作不可撤销，是否继续？
          </p>
        </Modal>
      )}
    </AppShell>
  );
}
