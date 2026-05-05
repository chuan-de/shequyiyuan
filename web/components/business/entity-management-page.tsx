'use client';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/layout/app-shell';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { changeEntityStatus, createEntity, currentUser, EntityRecord, getEntity, listEntities, updateEntity } from '@/lib/api';
import { hasPermission } from '@/lib/permissions';
import { useRouter } from 'next/navigation';

export function EntityManagementPage({ title, route, permissionPrefix }: { title: string; route: string; permissionPrefix: string }) {
  const router = useRouter(); const [token, setToken] = useState(''); const [rows, setRows] = useState<EntityRecord[]>([]); const [error, setError] = useState('');
  const [q,setQ]=useState(''); const [detail,setDetail]=useState<EntityRecord|null>(null); const [editing,setEditing]=useState<EntityRecord|null>(null);
  const [canRead,setCanRead]=useState(false); const [canWrite,setCanWrite]=useState(false);
  const load = useCallback(async () => { try { const r = await listEntities(token, route, { keyword: q, page: 1, size: 20 }); setRows(r.records); } catch (e) { setError((e as Error).message); } }, [token, route, q]);
  useEffect(()=>{const t=localStorage.getItem('access_token'); if(!t){router.replace('/login');return;} setToken(t); currentUser(t).then(u=>{setCanRead(hasPermission(u,`${permissionPrefix}:read`)); setCanWrite(hasPermission(u,`${permissionPrefix}:write`));}).catch(()=>router.replace('/login'));},[router,permissionPrefix]);
  useEffect(()=>{ if(token&&canRead) { void load(); } },[token,canRead,load]);
  if(!canRead) return <AppShell title={title}><Card><p className='error'>权限不足：缺少 {permissionPrefix}:read</p></Card></AppShell>;
  return <AppShell title={title} description={`${title}列表、详情、创建、编辑、状态变更`}><Card className='space-y-3'><div className='flex gap-2'><Input value={q} onChange={e=>setQ(e.target.value)} placeholder='搜索关键词' />{canWrite&&<Button onClick={async()=>{await createEntity(token,route,{name:'新建记录'});load();}}>创建</Button>}</div>{error&&<p className='error'>{error}</p>}<div className='space-y-2'>{rows.map(r=><div key={r.id} className='rounded border p-2 flex justify-between'><div><p className='font-semibold'>{String(r.name??`#${r.id}`)}</p><p className='hint'>状态:{String(r.enabled ?? r.status ?? 'unknown')}</p></div><div className='flex gap-2'><Button variant='secondary' onClick={async()=>setDetail(await getEntity(token,route,r.id))}>详情</Button>{canWrite&&<><Button variant='secondary' onClick={()=>setEditing(r)}>编辑</Button><Button onClick={async()=>{await changeEntityStatus(token,route,r.id,!(r.enabled??true));load();}}>状态变更</Button></>}</div></div>)}</div></Card>{detail&&<Card><h3 className='font-semibold'>详情抽屉（模拟）</h3><pre>{JSON.stringify(detail,null,2)}</pre><Button variant='secondary' onClick={()=>setDetail(null)}>关闭</Button></Card>}{editing&&<Card><h3 className='font-semibold'>编辑</h3><Input value={String(editing.name ?? '')} onChange={e=>setEditing({...editing,name:e.target.value})}/><div className='flex gap-2 mt-2'><Button onClick={async()=>{await updateEntity(token,route,editing.id,{name:editing.name});setEditing(null);load();}}>保存</Button><Button variant='secondary' onClick={()=>setEditing(null)}>取消</Button></div></Card>}</AppShell>;
}
