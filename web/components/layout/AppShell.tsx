'use client';

import Link from 'next/link';
import { ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { CurrentUserResponse } from '@/lib/api';
import { logout } from '@/lib/auth';
import { Button } from '@/components/ui/button';

export function AppShell({ user, children }: { user: CurrentUserResponse | null; children: ReactNode }) {
  const router = useRouter();

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white px-6 py-4">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <h1 className="text-lg font-semibold">Shequyiyuan Console</h1>
          <div className="flex items-center gap-3 text-sm text-slate-600">
            <span>{user ? `当前用户：${user.username}` : '用户加载中...'}</span>
            <Button onClick={() => logout(router)}>退出登录</Button>
          </div>
        </div>
      </header>
      <div className="mx-auto grid max-w-6xl grid-cols-1 gap-6 px-6 py-6 md:grid-cols-[220px_1fr]">
        <aside className="rounded-2xl border border-slate-200 bg-white p-4">
          <nav className="space-y-2 text-sm">
            <Link className="block rounded-lg px-3 py-2 hover:bg-slate-100" href="/dashboard">
              Dashboard
            </Link>
          </nav>
        </aside>
        <main>{children}</main>
      </div>
    </div>
  );
}
