'use client';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AppShell } from '@/components/layout/app-shell';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  changeEntityStatus, createEntity, currentUser, EntityRecord,
  getEntity, listEntities, updateEntity,
} from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { useRouter } from 'next/navigation';

export type EntityFormField = {
  key: string;
  label: string;
  required?: boolean;
  placeholder?: string;
  defaultValue?: string;
  type?: 'text' | 'password' | 'textarea' | 'number' | 'select';
  options?: { value: string; label: string }[];
};

export type EntityColumn = {
  key: string;
  title: string;
  type?: 'text' | 'photo' | 'currency' | 'badge';
  render?: (row: EntityRecord) => React.ReactNode;
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
  searchFields?: { key: string; label: string; type?: 'text' | 'select'; options?: { value: string; label: string }[] }[];
  rowActions?: {
    key: string;
    label: string;
    permission?: string;
    variant?: 'primary' | 'danger';
    onClick: (row: EntityRecord, helpers: { token: string; reload: () => void; showToast: (msg: string, type?: 'success' | 'error') => void }) => void;
  }[];
  labelMap?: Record<string, string>;
};

function initForm(fields: EntityFormField[], row?: EntityRecord | null): Record<string, string> {
  return fields.reduce<Record<string, string>>((acc, f) => {
    const v = row?.[f.key];
    acc[f.key] = typeof v === 'string' ? v : (typeof v === 'number' ? String(v) : f.defaultValue ?? '');
    return acc;
  }, {});
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
  const [permissions, setPermissions] = useState<string[]>([]);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [searchParams, setSearchParams] = useState<Record<string, string>>({});
  const searchDebounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState<Record<string, string>>(() => initForm(config.formFields));
  const [editing, setEditing] = useState<EntityRecord | null>(null);
  const [editForm, setEditForm] = useState<Record<string, string>>(() => initForm(config.formFields));
  const [detail, setDetail] = useState<EntityRecord | null>(null);

  const statusKey = config.statusField ?? 'enabled';
  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / size)), [total, size]);

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
        setPermissions(u.permissions ?? []);
      })
      .catch(() => router.replace('/login'));
  }, [router, config.permissionPrefix]);

  useEffect(() => { if (token && canRead) void load(); }, [token, canRead, load]);
  useEffect(() => { setCreateForm(initForm(config.formFields)); }, [config.formFields]);

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
            sf.type === 'select' ? (
              <select
                key={sf.key}
                defaultValue=""
                onChange={e => handleSearchChange(sf.key, e.target.value)}
                className="input max-w-[160px]"
              >
                <option value="">{sf.label}（全部）</option>
                {sf.options?.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
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
        <span className="ml-auto hint">共 {total} 条</span>
      </div>

      {/* Table */}
      <div className="table-wrap">
        <table className="w-full border-collapse">
          <thead className="border-b border-slate-200 bg-slate-50">
            <tr>
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
                <td colSpan={config.columns.length + (config.statusField ? 2 : 1)} className="td text-center text-slate-400 py-12">
                  加载中…
                </td>
              </tr>
            )}
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={config.columns.length + (config.statusField ? 2 : 1)} className="td text-center text-slate-400 py-12">
                  暂无数据
                </td>
              </tr>
            )}
            {!loading && rows.map(row => (
              <tr key={row.id} className="hover:bg-slate-50 transition-colors">
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
                          onClick={() => action.onClick(row, { token, reload: load, showToast })}
                          className={`btn-ghost ${action.variant === 'danger' ? 'text-rose-500' : action.variant === 'primary' ? 'text-blue-500' : ''}`}
                        >
                          {action.label}
                        </button>
                      );
                    })}
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
                {f.type === 'select' ? (
                  <select
                    value={createForm[f.key] ?? ''}
                    onChange={e => setCreateForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                    className="input w-full"
                  >
                    <option value="">请选择</option>
                    {f.options?.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                ) : f.type === 'textarea' ? (
                  <textarea
                    value={createForm[f.key] ?? ''}
                    placeholder={f.placeholder ?? `请输入${f.label}`}
                    onChange={e => setCreateForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                    className="input w-full min-h-[80px]"
                    rows={3}
                  />
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
                  {f.type === 'select' ? (
                    <select
                      value={editForm[f.key] ?? ''}
                      onChange={e => setEditForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                      className="input w-full"
                    >
                      <option value="">请选择</option>
                      {f.options?.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                  ) : f.type === 'textarea' ? (
                    <textarea
                      value={editForm[f.key] ?? ''}
                      placeholder={f.placeholder ?? `请输入${f.label}`}
                      onChange={e => setEditForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                      className="input w-full min-h-[80px]"
                      rows={3}
                    />
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
            {Object.entries(detail).map(([k, v]) => (
              <div key={k} className="flex gap-4">
                <span className="w-36 flex-shrink-0 text-sm font-semibold text-slate-500">{config.labelMap?.[k] ?? k}</span>
                <span className="text-sm text-slate-800 break-all">{String(v ?? '-')}</span>
              </div>
            ))}
          </div>
        </Modal>
      )}
    </AppShell>
  );
}
