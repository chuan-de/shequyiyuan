'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AppShell } from '@/components/layout/app-shell';
import { Card } from '@/components/ui/card';
import { LinkButton, Button } from '@/components/ui/button';
import { CurrentUserResponse, currentUser } from '@/lib/api';
import { readToken, clearToken } from '@/lib/token-storage';
import { hasPermission } from '@/lib/permissions';

const ENTRIES = [
  { href: '/dictionaries', label: 'Dictionaries', permission: 'dictionary:read' },
  { href: '/medications', label: 'Medications', permission: 'medications:read' },
  { href: '/family-doctors', label: 'Family Doctors', permission: 'family-doctors:read' },
  { href: '/visits', label: 'Visits', permission: 'visits:read' },
  { href: '/medical-records', label: 'Medical Records', permission: 'medical-records:read' },
  { href: '/health-records', label: 'Health Records', permission: 'health-records:read' },
  { href: '/doctors', label: 'Doctors', permission: 'doctors:read' },
  { href: '/configs', label: 'Configs', permission: 'configs:read' }
];

export default function DashboardPage() { const router = useRouter(); const [user, setUser] = useState<CurrentUserResponse | null>(null); const [loading, setLoading] = useState(true);
  useEffect(() => { const token = readToken(); if (!token) return router.replace('/login'); currentUser(token.accessToken).then(setUser).catch(()=>{clearToken();router.replace('/login');}).finally(()=>setLoading(false)); }, [router]);
  if (loading) return <AppShell title='Dashboard'><Card>Loading...</Card></AppShell>;
  return <AppShell title='Dashboard' description='按权限展示业务入口'><Card className='space-y-3'><p>用户：{user?.username}</p><div className='flex flex-wrap gap-2'>{ENTRIES.map(e=>hasPermission(user,e.permission)?<LinkButton key={e.href} href={e.href} variant='secondary'>{e.label}</LinkButton>:<span key={e.href} className='hint'>无权限：{e.label}</span>)}</div><Button onClick={()=>{clearToken();router.replace('/login');}}>Sign Out</Button></Card></AppShell>; }
