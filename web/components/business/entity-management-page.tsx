'use client';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AppShell } from '@/components/layout/app-shell';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { changeEntityStatus, createEntity, currentUser, EntityRecord, getEntity, listEntities, updateEntity } from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { useRouter } from 'next/navigation';

export type EntityFormField = { key: string; label: string; required?: boolean; placeholder?: string; defaultValue?: string };
export type EntityColumn = { key: string; title: string; render?: (row: EntityRecord) => string };
export type EntityPageConfig = {
  title: string;
  route: string;
  permissionPrefix: string;
  columns: EntityColumn[];
  formFields: EntityFormField[];
  statusField?: 'enabled' | 'status';
  createPayload: (form: Record<string, string>) => Record<string, unknown>;
  updatePayload: (form: Record<string, string>, row: EntityRecord) => Record<string, unknown>;
};

function initForm(fields: EntityFormField[], row?: EntityRecord | null) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    const value = row?.[field.key];
    acc[field.key] = typeof value === 'string' ? value : field.defaultValue ?? '';
    return acc;
  }, {});
}

export function EntityManagementPage({ config }: { config: EntityPageConfig }) {
  const router = useRouter(); const [token, setToken] = useState(''); const [rows, setRows] = useState<EntityRecord[]>([]); const [error, setError] = useState('');
  const [q, setQ] = useState(''); const [detail, setDetail] = useState<EntityRecord | null>(null); const [editing, setEditing] = useState<EntityRecord | null>(null);
  const [page, setPage] = useState(1); const [size] = useState(20); const [total, setTotal] = useState(0);
  const [canRead, setCanRead] = useState(false); const [canWrite, setCanWrite] = useState(false);
  const [createForm, setCreateForm] = useState<Record<string, string>>(() => initForm(config.formFields));
  const [editForm, setEditForm] = useState<Record<string, string>>(() => initForm(config.formFields));

  const statusKey = config.statusField ?? 'enabled';
  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / size)), [total, size]);

  const load = useCallback(async () => {
    try { const r = await listEntities(token, config.route, { keyword: q, page, size }); setRows(r.records); setTotal(r.total); }
    catch (e) { setError((e as Error).message); }
  }, [token, config.route, q, page, size]);

  useEffect(() => {
    const t = localStorage.getItem('access_token'); if (!t) { router.replace('/login'); return; }
    setToken(t);
    currentUser(t).then(u => { setCanRead(hasPermission(u, `${config.permissionPrefix}:read`)); setCanWrite(hasPermission(u, `${config.permissionPrefix}:write`)); }).catch(() => router.replace('/login'));
  }, [router, config.permissionPrefix]);
  useEffect(() => { if (token && canRead) { void load(); } }, [token, canRead, load]);
  useEffect(() => { setCreateForm(initForm(config.formFields)); }, [config.formFields]);

  if (!canRead) return <AppShell title={config.title}><Card><p className='error'>权限不足：缺少 {config.permissionPrefix}:read</p></Card></AppShell>;

  return <AppShell title={config.title} description={`${config.title}列表、详情、创建、编辑、状态变更`}><Card className='space-y-3'><div className='flex gap-2'><Input value={q} onChange={e => { setQ(e.target.value); setPage(1); }} placeholder='搜索关键词' />
    {canWrite && <Button onClick={async () => { await createEntity(token, config.route, config.createPayload(createForm)); setCreateForm(initForm(config.formFields)); load(); }}>创建</Button>}</div>
    {canWrite && <div className='grid grid-cols-2 gap-2'>{config.formFields.map(f => <Input key={f.key} value={createForm[f.key] ?? ''} placeholder={f.placeholder ?? f.label} onChange={e => setCreateForm({ ...createForm, [f.key]: e.target.value })} />)}</div>}
    {error && <p className='error'>{error}</p>}<div className='space-y-2'>{rows.map(r => <div key={r.id} className='rounded border p-2 flex justify-between'><div>
      {config.columns.map(c => <p key={c.key}><span className='font-semibold'>{c.title}:</span> {c.render ? c.render(r) : String(r[c.key] ?? '-')}</p>)}
      <p className='hint'>状态:{String(r[statusKey] ?? 'unknown')}</p></div><div className='flex gap-2'><Button variant='secondary' onClick={async () => setDetail(await getEntity(token, config.route, r.id))}>详情</Button>{canWrite && <><Button variant='secondary' onClick={() => { setEditing(r); setEditForm(initForm(config.formFields, r)); }}>编辑</Button><Button onClick={async () => { await changeEntityStatus(token, config.route, r.id, !(r.enabled ?? true)); load(); }}>状态变更</Button></>}</div></div>)}</div>
    <div className='flex items-center justify-end gap-2'><Button variant='secondary' onClick={() => setPage(Math.max(1, page - 1))} disabled={page <= 1}>上一页</Button><span className='hint'>{page}/{totalPages}</span><Button variant='secondary' onClick={() => setPage(Math.min(totalPages, page + 1))} disabled={page >= totalPages}>下一页</Button></div>
  </Card>{detail && <Card><h3 className='font-semibold'>详情抽屉（模拟）</h3><pre>{JSON.stringify(detail, null, 2)}</pre><Button variant='secondary' onClick={() => setDetail(null)}>关闭</Button></Card>}
    {editing && <Card><h3 className='font-semibold'>编辑</h3><div className='grid grid-cols-2 gap-2'>{config.formFields.map(f => <Input key={f.key} value={editForm[f.key] ?? ''} placeholder={f.placeholder ?? f.label} onChange={e => setEditForm({ ...editForm, [f.key]: e.target.value })} />)}</div><div className='flex gap-2 mt-2'><Button onClick={async () => { await updateEntity(token, config.route, editing.id, config.updatePayload(editForm, editing)); setEditing(null); load(); }}>保存</Button><Button variant='secondary' onClick={() => setEditing(null)}>取消</Button></div></Card>}</AppShell>;
}
